package compound_discoverer;
import java.util.ArrayList;
import java.util.Collections;

import peak_finder.Lipid;
import peak_finder.PurityResult;
import peak_finder.RTDeviation;
import peak_finder.Utilities;


public class CDCompoundGroup extends Utilities implements Comparable<CDCompoundGroup>
{
	String lipid;								//String identifier for lipid
	Double mw;									//Compound Discoverer mw calculation
	Double quantIon;							//Most intense ion used for quantitation
	String quantPolarity;						//Polarity of quant ion
	Double retention;							//Corrected retention time of CG
	Double maxArea;								//Maximum area for all features associated with CG
	Double purity;								//Calculated purity of peak from associated MS2 id's
	String sumID;								//Sum identifier for lipid
	ArrayList<CDAreaResult> results;			//ArrayList of all area results from CD
	ArrayList<CDCompound> compounds;			//ArrayList of all associated compounds
	ArrayList<Lipid> lipidCandidates;			//ArrayList of all candidate identifications
	ArrayList<String> sumIDs;					//Array list of all associate sum identifications
	ArrayList<Integer> sumIDsCount;				//Number of times a unique sum ID has been associated
	ArrayList<Double> quantIonArray;			//ArrayList of all quantIons
	CDLipidCandidate identification = null;		//Final identification
	Lipid finalLipidID = null;					//Final identification lipid object					
	boolean keep;								//True iff the CG will be retained for final peak table
	boolean ms2Sampled;							//True iff the peak was samples for MS2 analysis
	boolean displaySumID;						//True iff the peak will be given sum ID for final table
	boolean plasmenylEtherConflict;				//True iff the associated IDs contain both plasmenyl and ether fragments
	Double avgFWHM;								//Average peak FWHM of all associated peak groups
	boolean noCoelutingPeaks;					//True iff no coeluting peaks of same mass were detected
	int maxIsotopeNumber;						//Maximum number of isotopes detected in isotope cluster
	int featuresAdded;							//Number of features associated with CG 
	boolean positiveFeature;					//True iff a positive polarity feature was added
	boolean negativeFeature;					//True iff a negative polarity feature was added
	ArrayList<PurityResult> summedPurities;		//ArrayList of all purity results
	String filterReason;						//Reason compound group was removed


	//Constructur
	public CDCompoundGroup (Double mw, Double retention, Double maxArea)
	{
		this.mw = mw;
		this.retention = retention;
		this.maxArea = maxArea;
		results = new ArrayList<CDAreaResult>();
		compounds = new ArrayList<CDCompound>();
		summedPurities = new ArrayList<PurityResult>();
		sumIDs = new ArrayList<String>();
		sumIDsCount = new ArrayList<Integer>();
		lipidCandidates = new ArrayList<Lipid>();
		quantIonArray = new ArrayList<Double>();
		sumID = "";
		keep = true;
		plasmenylEtherConflict = false;
		maxIsotopeNumber = 0;
		avgFWHM = 0.0;
		ms2Sampled = false;
		featuresAdded = 0;
		displaySumID = false;
		filterReason = "";
	}

	//Merge compound group with another compound group
	public void mergeCompoundGroup(CDCompoundGroup cg)
	{
		//Merge compounds
		for (int i=0; i<cg.compounds.size(); i++)
		{
			this.compounds.add(cg.compounds.get(i));
		}

		//Reperform id routine
		filterSumIDs();
		calculateWeightedPurity();
		getBestLipidID();

		//Filter ID based on purity
		if (purity<Utilities.MINFAPURITY) displaySumID = true;
		else displaySumID = false;

		//Invalidate target compound group
		cg.keep = false;
		cg.filterReason = "Redundant Identification";
	}

	//Assign first ID in sorted ID array as final identification
	public void getBestLipidID()
	{
		if (lipidCandidates.size()>0)
		{
			Collections.sort(lipidCandidates);
			this.finalLipidID = lipidCandidates.get(0);
		}
	}

	//Calculate weighted purity based on gaussian peak profile
	public void calculateWeightedPurity()
	{
		Double weightSum = 0.0;
		Double puritySum = 0.0;

		//Reset purity array
		summedPurities = new ArrayList<PurityResult>();

		//Populate summed purity result array and find weight sum
		for (int i=0; i<compounds.size(); i++)
		{
			//For all features
			for (int j=0; j<compounds.get(i).features.size(); j++)
			{
				//For all lipid candidates
				for (int k=0; k<compounds.get(i).features.get(j).lipidCandidates.size(); k++)
				{
					//If in preferred polarity
					if (compounds.get(i).features.get(j).lipidCandidates.get(k).keep 
							&& compounds.get(i).features.get(j).lipidCandidates.get(k).preferredPolarity)
					{
						//Add to weight and purity sum
						weightSum += compounds.get(i).features.get(j).lipidCandidates.get(k).gaussianScore;
						puritySum += compounds.get(i).features.get(j).lipidCandidates.get(k).gaussianScore*compounds.get(i).features.get(j).lipidCandidates.get(k).purity;

						//Add purity to arry if unique lipid identification
						for (int l=0; l<compounds.get(i).features.get(j).lipidCandidates.get(k).purityArray.size(); l++)
						{
							addPurityIfUnique(compounds.get(i).features.get(j).lipidCandidates.get(k).purityArray.get(l));
						}
					}
				}
			}
		}

		//Scale purities based on weifhting factor
		for (int i=0; i<summedPurities.size(); i++)
		{
			summedPurities.get(i).purity = (int)Math.round(summedPurities.get(i).purity/weightSum);
		}

		//Sory by purity
		Collections.sort(summedPurities);

		//If purity array contains a value
		if (summedPurities.size()>0) 
			this.purity = (double)summedPurities.get(0).purity;
		else 
			this.purity = 0.0;

		//Check for plasmenyl ether conflict
		checkPlasmenylEther();

		if (this.purity<Utilities.MINFAPURITY) this.displaySumID = true;
	}

	//Add purity to summed purities array if unique.  If not, add purity value
	private void addPurityIfUnique(PurityResult p)
	{
		//For all summed purities
		for (int i=0; i<summedPurities.size(); i++)
		{
			//If not unique, end method
			if (summedPurities.get(i).name.equals(p.name))
			{
				summedPurities.get(i).purity += p.purity;
				return;
			}
		}

		//If unique, add to summed purities array
		summedPurities.add(new PurityResult(p.name, p.purity));
	}

	//Add a peak area result fo compound group
	public void addResult(CDAreaResult result)
	{
		results.add(result);
	}

	//Associate a compound with a compound group
	public void addCompound(CDCompound compound)
	{
		compounds.add(compound);

		//Update polarity boolean
		if (compound.negativeFeature) negativeFeature = true;
		else if (compound.positiveFeature) positiveFeature = true;
	}

	//Calculate average FWHM of compound group
	public void calcFWHM()
	{
		Double result = 0.0;

		for (int i=0; i<compounds.size(); i++)
		{
			result = result + compounds.get(i).fwhm;
		}

		result = result/compounds.size();

		avgFWHM = result;
	}

	//Merge separate compound groups which from separate polarity files
	public void mergePolarities()
	{
		ArrayList<CDAreaResult> newResults 
		= new ArrayList<CDAreaResult>();
		CDAreaResult resultTemp = null;
		boolean matchFound = false;
		boolean posGreater = false;

		//Iterate through all results
		for (int i=0; i<results.size(); i++)
		{
			matchFound = false;

			//Iterate through remaining results
			for (int j=i+1; j<results.size(); j++)
			{
				//If the files are linked via the polarity file number
				if (results.get(i).file.polarityFileNumber > -1
						&& results.get(i).file.polarityFileNumber == results.get(j).file.polarityFileNumber 
						&& !results.get(i).merged 
						&& !results.get(j).merged)
				{
					if (!matchFound)
					{
						if (results.get(i).area > results.get(j).area && results.get(i).file.polarity.equals("+")) posGreater = true;
						else if (results.get(i).area < results.get(j).area && results.get(j).file.polarity.equals("+")) posGreater = true;
						matchFound = true;
					}
					
					//Retain area result with best polarity
					if (results.get(i).file.polarity.equals("+") && posGreater)		
						resultTemp = new CDAreaResult(null, results.get(i).area);
					else if (results.get(j).file.polarity.equals("+") && posGreater)	
						resultTemp = new CDAreaResult(null, results.get(j).area);
					else if (results.get(i).file.polarity.equals("-") && !posGreater)		
						resultTemp = new CDAreaResult(null, results.get(i).area);
					else if (results.get(j).file.polarity.equals("-") && !posGreater)	
						resultTemp = new CDAreaResult(null, results.get(j).area);

					//Merge results
					resultTemp.addMergedFileName(results.get(i).file.file+"+"+results.get(j).file.file);
					newResults.add(resultTemp);
					resultTemp.merged = true;
					results.get(i).merged = true;
					results.get(j).merged = true;
					matchFound = true;
					break;
				}
			}

			if (!matchFound && !results.get(i).merged)
			{
				newResults.add(results.get(i));
			}
		}

		this.results = newResults;
	}

	//Method to iterate through all features and find neighboring isobaric peaks
	public void checkIsobaricFeature(CDFeature feature)
	{
		//Iterate through compounds
		for (int i=0; i<compounds.size(); i++)
		{
			//Iterate through features
			for (int j=0; j<compounds.get(i).features.size(); j++)
			{
				//If isobaric within user-defined ppm tolerance
				if (calcPPMDiff(feature.mass,compounds.get(i).features.get(j).mass)<MAXPPMDIFF 
						&& feature.sample.cdFileID.equals(compounds.get(i).features.get(j).sample.cdFileID)
						&& feature.polarity.equals(compounds.get(i).features.get(j).polarity)
						&& !feature.equals(compounds.get(i).features.get(j)))
				{
					//add feature to neighbor array
					compounds.get(i).features.get(j).isobaricNeighbors.add(feature);
				}
			}
		}
	}

	//Method to match unaligned features with aligned features.  This allows calculation of actual retention time
	//which CD does not output
	public boolean matchUnalignedFeature(Double mass, Double retention, String polarity, Double area, String sample)
	{
		//For all compounds
		for (int i=0; i<compounds.size(); i++)
		{
			//For al features
			for (int j=0; j<compounds.get(i).features.size(); j++)
			{
				//If the feature is the same mass, polarity, retention, sample, and rea
				if (Math.abs(calcPPMDiff(mass, compounds.get(i).features.get(j).mass))<5.0
						&& Math.abs(retention-compounds.get(i).features.get(j).retention)<1.0
						&& polarity.equals(compounds.get(i).features.get(j).polarity)
						&& area/compounds.get(i).features.get(j).area>0.95
						&& compounds.get(i).features.get(j).sample.cdFileID.equals(sample))
				{
					//Add corrected retention time
					compounds.get(i).features.get(j).realRetention = retention;
					return true;
				}
			}
		}
		return false;
	}

	//Method to filter all potentional sum IDs and find most common
	public void filterSumIDs()
	{
		//Find most common sum ID
		String mostCommon = getMostCommonSumID();

		//If no best sum ID exists
		if (!mostCommon.equals("None"))
		{
			this.sumID = mostCommon;
			//Iterate through compounds
			for (int i=0; i<compounds.size(); i++)
			{
				//Iterate through features
				for (int j=0; j<compounds.get(i).features.size(); j++)
				{
					//Iterate through lipid IDs
					for (int k=0; k<compounds.get(i).features.get(j).lipidCandidates.size(); k++)
					{
						//Keep only those with correct Sum ID
						if (!compounds.get(i).features.get(j).lipidCandidates.get(k).sumLipidName.equals(mostCommon))
						{
							compounds.get(i).features.get(j).lipidCandidates.get(k).keep = false;
						}
						//If a match add to candidate array
						else
						{
							this.lipidCandidates.add(compounds.get(i).features.get(j).lipidCandidates.get(k));
							Collections.sort(lipidCandidates);
						}
					}
				}
			}
		}
	}

	//Returns the most common sum ID from all putative IDs
	public String getMostCommonSumID()
	{
		int index = 0;

		//If no identifications made, return "None"
		if (sumIDs.size()<1) return "None";

		//Iterate through all sumIDs
		for (int i=0; i<sumIDs.size(); i++)
		{
			//If id with highest count, save
			if (sumIDsCount.get(i)>sumIDsCount.get(index))
			{
				index = i;
			}
		}

		//Return most common
		return sumIDs.get(index);
	}

	//Method to add unique sum IDs to array
	public void addSumID(String sumID)
	{
		//Iterate through all sum IDS
		for (int i=0; i<sumIDs.size(); i++)
		{
			//If match found increment count
			if (sumIDs.get(i).equals(sumID))
				sumIDsCount.set(i, sumIDsCount.get(i)+1);
		}

		//if no match found, add to end of list
		sumIDs.add(sumID);
		sumIDsCount.add(1);
	}

	//Once unaligned features are associated, add all retention time deviations 
	//to array
	public void calculateCGRTDev()
	{
		for (int i=0; i<compounds.size(); i++)
		{
			for (int j=0; j<compounds.get(i).features.size(); j++)
			{
				//Add to rtdev array
				if (compounds.get(i).features.get(j).realRetention != 0.0)
				{
					compounds.get(i).features.get(j).sample.featureRTDevArray.add
					(new RTDeviation(compounds.get(i).features.get(j).realRetention - 
							compounds.get(i).features.get(j).retention,compounds.get(i).features.get(j).retention, compounds.get(i).features.get(j).area));

					//Sort rtDev array
					Collections.sort(compounds.get(i).features.get(j).sample.featureRTDevArray);
				}

			}
		}
	}

	//Returns true iff two compound groups share isobaric features
	public boolean ppmMatch(CDCompoundGroup target)
	{
		boolean result = false;

		ArrayList<Double> array1 = new ArrayList<Double>();
		ArrayList<String> array1Pol = new ArrayList<String>();
		ArrayList<Double> array2 = new ArrayList<Double>();
		ArrayList<String> array2Pol = new ArrayList<String>();

		//Iterate through compounds and add to array 2
		for (int i=0; i<compounds.size(); i++)
		{
			//Iterate through features
			for (int j=0; j<compounds.get(i).features.size(); j++)
			{
				array2.add(compounds.get(i).features.get(j).mass);
				array2Pol.add(compounds.get(i).features.get(j).polarity);
			}
		}

		//Iterate through and add to array 1
		for (int i=0; i<target.compounds.size(); i++)
		{
			//Iterate through features
			for (int j=0; j<target.compounds.get(i).features.size(); j++)
			{
				array1.add(target.compounds.get(i).features.get(j).mass);
				array1Pol.add(target.compounds.get(i).features.get(j).polarity);
			}
		}

		//Iterate through mass array looking for matches
		for (int i=0; i<array1.size(); i++)
		{
			for (int j=0; j<array2.size(); j++)
			{
				if (calcPPMDiff(array1.get(i),array2.get(j))<MAXPPMDIFF && array1Pol.get(i).equals(array2Pol.get(j)))
				{
					return true;
				}
			}
		}

		return result;
	}

	//Finds the maximum isotope number from the associated compounds
	public void findMaxIsotopeNumber()
	{
		//Iterate through compounds and find max # of isotopes
		for (int i=0; i<compounds.size(); i++)
		{
			if (maxIsotopeNumber<compounds.get(i).maxMI)
			{
				maxIsotopeNumber = compounds.get(i).maxMI;
			}
		}
	}

	//Checks to see if both plasmenyl and ether species are present above purity level
	private void checkPlasmenylEther()
	{
		double pSum = 0.0;
		double eSum = 0.0;

		//If any summed purities exist
		if (summedPurities.size()>0)
		{
			//If purity above threshold, end method
			if (summedPurities.get(0).purity>Utilities.MINFAPURITY)
			{
				return;
			}
			//If not and either of the best identifications contains plasmenyl or ether
			else if (summedPurities.get(0).name.contains("Plasmenyl") ||
					summedPurities.get(0).name.contains("Ether"))
			{
				//Sum all purities for each class
				for (int i=0; i<summedPurities.size(); i++)
				{
					if (summedPurities.get(i).name.contains("Plasmenyl"))
						pSum += summedPurities.get(i).purity;
					else if (summedPurities.get(i).name.contains("Ether"))
						eSum += summedPurities.get(i).purity;
				}

				//If the sum of either plasmenyl or ether ID's is not greater than treshold, change sum ID
				if (pSum<Utilities.MINFAPURITY && eSum<Utilities.MINFAPURITY
						&& pSum>0.0 && eSum > 0.0) plasmenylEtherConflict = true;
			}
		}
	}

	//Method to find most intense ion for quantitation
	public void findQuantIon()
	{
		Double maxArea = 0.0;
		String polarity = "";
		Double quantIon = 0.0;

		//Iterate through compounds
		for (int i=0; i<compounds.size(); i++)
		{
			//Iterate through features
			for (int j=0; j<compounds.get(i).features.size(); j++)
			{
				//If quantIon not in quant Ion array, add it
				if (isUniqueQuantIon(compounds.get(i).features.get(j).mass)) quantIonArray.add(compounds.get(i).features.get(j).mass);

				//Find max area
				if (compounds.get(i).features.get(j).area > maxArea)
				{
					maxArea = compounds.get(i).features.get(j).area;
					polarity = compounds.get(i).features.get(j).polarity;
					quantIon = compounds.get(i).features.get(j).mass;
				}
			}
		}

		//Set quant ion as max feature
		this.quantIon = quantIon;
		this.quantPolarity = polarity;
	}

	//Returns true iff the quan ion is unique
	public boolean isUniqueQuantIon(Double mass)
	{
		for (int i=0; i<quantIonArray.size(); i++)
		{
			if (calcPPMDiff(mass, quantIonArray.get(i))<Utilities.MAXPPMDIFF)
			{
				return false;
			}
		}
		return true;
	}

	//Returns true iff the target CG's RT is within the FWHM range
	public boolean targetInFWHM(CDCompoundGroup cG, Double fwhmDivisor)
	{
		boolean result = false;

		double rtDiff = Math.abs(cG.retention - this.retention);

		if (rtDiff < (avgFWHM/fwhmDivisor)) result = true;

		return result;
	}

	//REturns the unknown bond version of ether or plasmenyl sum ID
	private String getTopEther()
	{
		String result = "";

		result += sumID.substring(sumID.indexOf("-")+1, sumID.indexOf(" "));
		result += " O";
		result += sumID.substring(sumID.lastIndexOf("-"));

		return result;
	}

	//Returns string representation of CG
	public String toString()
	{
		String result = retention+","+quantIon+","+quantPolarity+","+maxArea+",";

		if (lipidCandidates.size() > 0)
		{
			if(plasmenylEtherConflict) result = result + getTopEther()+",";
			else if (purity.equals(0.0) && sumID.contains("Plasmenyl")) result = result + getTopEther()+",";
			else if (true) result = result+sumID+",";
			//else result = result+lipidCandidates.get(0).lipidName+",";
			result = result+lipidCandidates.get(0).lipidClass+",";
		}
		else
		{
			result = result+",,";
		}

		result = result+compounds.size()+",";

		for (int i=0; i<results.size(); i++)
		{
			result= result+results.get(i).area+",";
		}

		return result;
	}

	//Returns string representation of CG
	public String toStringFilterReason()
	{
		String result = retention+","+quantIon+","+quantPolarity+","+maxArea+",";

		if (lipidCandidates.size() > 0)
		{
			if(plasmenylEtherConflict) result = result + getTopEther()+",";
			else if (purity.equals(0.0) && sumID.contains("Plasmenyl")) result = result + getTopEther()+",";
			else if (true) result = result+sumID+",";
			//TODO:
			//else result = result+lipidCandidates.get(0).lipidName+",";
			result = result+lipidCandidates.get(0).lipidClass+",";
		}
		else
		{
			result = result+",,";
		}

		result = result+compounds.size()+",";

		result = result + filterReason+",";

		for (int i=0; i<results.size(); i++)
		{
			result= result+results.get(i).area+",";
		}

		return result;
	}

	//Compares CG by retention
	public int compareTo(CDCompoundGroup target)
	{
		int result = 0;

		if (retention<target.retention) return -1;

		else if (retention>target.retention) return 1;

		return result;
	}
}
