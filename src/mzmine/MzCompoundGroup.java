package mzmine;
import java.util.ArrayList;
import java.util.Collections;

import peak_finder.Lipid;
import peak_finder.PurityResult;
import peak_finder.Utilities;


public class MzCompoundGroup extends Utilities implements Comparable<MzCompoundGroup>
{
	String lipid; 								//String identifier for lipid
	Double quantIon; 							//Mass for quantitation 
	String polarity;							//Polarity of quant ion (+/-)
	String quantPolarity;						//Polarity of quant ion
	Double retention;							//Aligned retention time of compound group
	Double maxArea;								//Maximum area for quant ion
	Double purity;								//Identification purity
	String sumID;								//Sum ID of lipid identification
	ArrayList<MzAreaResult> results;			//ArrayList of all area results from mzmine
	ArrayList<MzFeature> features;				//Array of all features assigned to CG
	ArrayList<MzLipidCandidate> candidates;		//Candidate lipid identifications
	ArrayList<Lipid> lipidCandidates;			//ArrayList of all candidate identifications
	ArrayList<String> sumIDs;					//Array list of all associate sum identifications
	ArrayList<Integer> sumIDsCount;				//Number of times a unique sum ID has been associated
	ArrayList<Double> quantIonArray;			//ArrayList of all quantIons
	MzLipidCandidate identification = null;		//Final identification
	Lipid finalLipidID = null;					//Final identification lipid object	
	boolean keep;								//True iff the CG will be retained for final peak table
	boolean ms2Sampled;							//True iff the peak was samples for MS2 analysis
	boolean displaySumID;						//True iff the peak will be given sum ID for final table
	boolean plasmenylEtherConflict;				//True iff the associated IDs contain both plasmenyl and ether fragments
	boolean noCoelutingPeaks;					//True iff no coeluting peaks of same mass were detected
	Double avgFWHM;								//Average peak FWHM of all associated peak groups
	int featuresAdded;							//Number of features associated with CG 
	boolean positiveFeature;					//True iff a positive polarity feature was added
	boolean negativeFeature;					//True iff a negative polarity feature was added
	ArrayList<PurityResult> summedPurities;		//ArrayList of all purity results
	String filterReason;						//Reason compound group was removed

	//Constructor
	public MzCompoundGroup (Double mass, Double retention, int rowID, String polarity)
	{
		this.retention = retention;
		results = new ArrayList<MzAreaResult>();
		features = new ArrayList<MzFeature>();
		sumIDs = new ArrayList<String>();
		sumIDsCount = new ArrayList<Integer>();
		candidates = new ArrayList<MzLipidCandidate>();
		lipidCandidates = new ArrayList<Lipid>();
		quantIonArray = new ArrayList<Double>();
		maxArea = 0.0;
		sumID = "";
		keep = true;
		plasmenylEtherConflict = false;
		avgFWHM = 0.0;
		ms2Sampled = false;
		featuresAdded = 0;
		displaySumID = false;
		filterReason = "";
	}

	//Merge compound group with another compound group
	public void mergeCompoundGroup(MzCompoundGroup cg)
	{

		//Merge compounds
		for (int i=0; i<cg.features.size(); i++)
		{
			this.features.add(cg.features.get(i));
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

	//Returns a string representation of features identifications
	public String getFeatureIDs()
	{
		String result = retention+","+quantIon+",";

		//Iterate through all features
		for (int i=0; i<features.size(); i++)
		{
			//Add to result string
			for (int j=0; j<features.get(i).lipidCandidates.size(); j++)
			{
				result = result + features.get(i).lipidCandidates.get(j).lipidName+" "+features.get(i).lipidCandidates.get(j).polarity;
				result = result + ",";
			}
		}

		return result;
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

	/*
	public void calculateWeightedPurity()
	{
		Double weightSum = 0.0;
		Double puritySum = 0.0;

		for (int i=0; i<features.size(); i++)
		{
			for (int j=0; j<features.get(i).lipidCandidates.size(); j++)
			{
				if (features.get(i).lipidCandidates.get(j).keep 
						&& features.get(i).lipidCandidates.get(j).preferredPolarity)
				{
					weightSum += features.get(i).lipidCandidates.get(j).gaussianScore;
					puritySum += features.get(i).lipidCandidates.get(j).gaussianScore*features.get(i).lipidCandidates.get(j).purity;
				}
			}
		}

		if (weightSum>0.0) this.purity = puritySum/weightSum;
		else this.purity = 0.0;

		if (this.purity<Utilities.MINFAPURITY) this.displaySumID = true;
	}
	 */

	//Calculate weighted purity based on gaussian peak profile
	public void calculateWeightedPurity()
	{
		Double weightSum = 0.0;
		Double puritySum = 0.0;

		//Reset purity array
		summedPurities = new ArrayList<PurityResult>();

		//Populate summed purity result array and find weight sum
		//For all features
		for (int j=0; j<features.size(); j++)
		{
			//For all lipid candidates
			for (int k=0; k<features.get(j).lipidCandidates.size(); k++)
			{
				//If in preferred polarity
				if (features.get(j).lipidCandidates.get(k).keep 
						&& features.get(j).lipidCandidates.get(k).preferredPolarity)
				{
					//Add to weight and purity sum
					puritySum += features.get(j).lipidCandidates.get(k).gaussianScore*features.get(j).lipidCandidates.get(k).purity;

					//Add purity to arry if unique lipid identification
					for (int l=0; l<features.get(j).lipidCandidates.get(k).purityArray.size(); l++)
					{
						weightSum += features.get(j).lipidCandidates.get(k).gaussianScore;
						addPurityIfUnique(features.get(j).lipidCandidates.get(k).purityArray.get(l), features.get(j).lipidCandidates.get(k).gaussianScore);
					}
				}
			}
		}
		
		//Scale purities based on weifhting factor
		for (int i=0; i<summedPurities.size(); i++)
		{
			summedPurities.get(i).purity = (int)Math.round(summedPurities.get(i).purity);
		}

		//Sory by purity
		Collections.sort(summedPurities);

		
		//If purity array contains a value
		if (summedPurities.size()>0) 
			this.purity = (double)summedPurities.get(0).purity/weightSum;
		else 
			this.purity = 0.0;

		//Check for plasmenyl ether conflict
		checkPlasmenylEther();

		if (this.purity<Utilities.MINFAPURITY) this.displaySumID = true;
	}

	//Add purity to summed purities array if unique.  If not, add purity value
	private void addPurityIfUnique(PurityResult p, Double weight)
	{
		//For all summed purities
		for (int i=0; i<summedPurities.size(); i++)
		{
			//If not unique, end method
			if (summedPurities.get(i).name.equals(p.name))
			{
				summedPurities.get(i).purity += (int)Math.round(((p.purity*1.0)*weight));
				return;
			}
		}

		//If unique, add to summed purities array
		summedPurities.add(new PurityResult(p.name, (int)Math.round(((p.purity*1.0)*weight))));
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
					summedPurities.get(0).name.contains("Plasmanyl"))
			{
				//Sum all purities for each class
				for (int i=0; i<summedPurities.size(); i++)
				{
					if (summedPurities.get(i).name.contains("Plasmenyl"))
						pSum += summedPurities.get(i).purity;
					else if (summedPurities.get(i).name.contains("Plasmanyl"))
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
		//Iterate through features
		for (int j=0; j<features.size(); j++)
		{
			//If quantIon not in quant Ion array, add it
			if (isUniqueQuantIon(features.get(j).mass)) quantIonArray.add(features.get(j).mass);

			//Find max area
			if (features.get(j).area > maxArea)
			{
				maxArea = features.get(j).area;
				polarity = features.get(j).polarity;
				quantIon = features.get(j).mass;
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

	public void addResult(MzAreaResult result)
	{
		results.add(result);
	}


	//Returns the unknown bond version of ether or plasmenyl sum ID
	private String getTopEther()
	{
		String result = "";

		result += sumID.substring(sumID.indexOf("-")+1, sumID.indexOf(" "));
		result += " O";
		result += sumID.substring(sumID.lastIndexOf("-"));

		return result;
	}

	public void addFeature(MzFeature feature)
	{
		//Add feature
		features.add(feature);

		//Add area result
		results.add(new MzAreaResult(feature.sample,feature.area, feature.mass));

		//Update polarity boolean
		if (feature.polarity.equals("-")) negativeFeature = true;
		else if (feature.polarity.equals("+")) positiveFeature = true;

		//Update maxArea
		if (feature.area>maxArea) maxArea = feature.area;
	}

	public void calcFWHM()
	{
		Double result = 0.0;
		int count = 0;

		for (int i=0; i<features.size(); i++)
		{
			if (features.get(i).fwhm > 0.0)
			{
				result = result + features.get(i).fwhm;
				count ++;
			}
		}

		result = result/count;

		avgFWHM = result;
	}

	//Merge separate compound groups which from separate polarity files
	public void mergePolarities()
	{
		ArrayList<MzAreaResult> newResults 
		= new ArrayList<MzAreaResult>();
		MzAreaResult resultTemp = null;
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
						&&results.get(i).file.polarityFileNumber == results.get(j).file.polarityFileNumber 
						&& !results.get(i).merged 
						&& !results.get(j).merged)
				{
					if (!matchFound)
					{
						if (results.get(i).area > results.get(j).area && results.get(i).file.polarity.equals("+")) posGreater = true;
						else if (results.get(i).area < results.get(j).area && results.get(j).file.polarity.equals("+")) posGreater = true;
						matchFound = true;
					}

					//Retain area result with largest area
					if (results.get(i).file.polarity.equals("+") && posGreater)						
						resultTemp = new MzAreaResult(null, results.get(i).area, results.get(i).mass);
					else if (results.get(j).file.polarity.equals("+") && posGreater)
						resultTemp = new MzAreaResult(null, results.get(j).area, results.get(i).mass);
					else if (results.get(i).file.polarity.equals("-") && !posGreater)	
						resultTemp = new MzAreaResult(null, results.get(i).area, results.get(i).mass);
					else if (results.get(j).file.polarity.equals("-") && !posGreater)	
						resultTemp = new MzAreaResult(null, results.get(j).area, results.get(i).mass);

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

	public void checkIsobaricFeature(MzFeature feature)
	{
		//Iterate through compounds
		for (int i=0; i<features.size(); i++)
		{
			//If isobaric within user-defined ppm tolerance
			if (calcPPMDiff(feature.mass,features.get(i).mass)<MAXPPMDIFF 
					&& feature.sample.file.equals(features.get(i).sample.file)
					&& feature.polarity.equals(features.get(i).polarity)
					&& !feature.equals(features.get(i)))
			{
				//add feature to neighbor array
				features.get(i).isobaricNeighbors.add(feature);
			}
		}
	}

	public void filterSumIDs()
	{
		String mostCommon = getMostCommonSumID();

		if (!mostCommon.equals("None"))
		{
			this.sumID = mostCommon;
			//Iterate through compounds
			for (int i=0; i<features.size(); i++)
			{
				//Iterate through lipid IDs
				for (int k=0; k<features.get(i).lipidCandidates.size(); k++)
				{
					//Keep only those with correct Sum ID
					if (!features.get(i).lipidCandidates.get(k).sumLipidName.equals(mostCommon))
					{
						features.get(i).lipidCandidates.get(k).keep = false;
					}
					//If a amtch add to candidate array
					else
					{
						this.lipidCandidates.add(features.get(i).lipidCandidates.get(k));
						Collections.sort(lipidCandidates);
					}
				}
			}
		}
	}

	public String getMostCommonSumID()
	{
		int index = 0;

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

		return sumIDs.get(index);
	}

	public void addSumID(String sumID)
	{
		//Iterate through all sum IDS
		for (int i=0; i<sumIDs.size(); i++)
		{
			//If match found increment count
			if (sumIDs.get(i).equals(sumID))
				sumIDsCount.set(i, sumIDsCount.get(i)+1);
		}

		//if not match found, add to end of list
		sumIDs.add(sumID);
		sumIDsCount.add(1);
	}

	//Matches compound groups based on the precursor masses of the features
	public boolean ppmMatch(MzCompoundGroup target)
	{
		boolean result = false;

		ArrayList<Double> array1 = new ArrayList<Double>();
		ArrayList<String> array1Pol = new ArrayList<String>();
		ArrayList<Double> array2 = new ArrayList<Double>();
		ArrayList<String> array2Pol = new ArrayList<String>();

		//Iterate through f and add to array 2
		for (int i=0; i<features.size(); i++)
		{
			array2.add(features.get(i).mass);
			array2Pol.add(features.get(i).polarity);
		}

		//Iterate through target features and add to array 1
		for (int i=0; i<target.features.size(); i++)
		{
			array1.add(target.features.get(i).mass);
			array1Pol.add(target.features.get(i).polarity);
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

	//Return true iff compound groups are significantly co-eluting
	public boolean targetInFWHM(MzCompoundGroup cG, Double fwhmDivisor)
	{
		boolean result = false;

		double rtDiff = Math.abs(cG.retention - this.retention);

		if (rtDiff < (avgFWHM/fwhmDivisor)) result = true;

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
			else if (displaySumID) result = result+sumID+",";
			else result = result+lipidCandidates.get(0).lipidName+",";
			result = result+lipidCandidates.get(0).lipidClass+",";
		}
		else
		{
			result = result+",,";
		}

		result = result+features.size()+",";

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
			else if (displaySumID) result = result+sumID+",";
			else result = result+lipidCandidates.get(0).lipidName+",";
			result = result+lipidCandidates.get(0).lipidClass+",";
		}
		else
		{
			result = result+",,";
		}

		result = result+features.size()+",";
		result = result + filterReason+",";

		for (int i=0; i<results.size(); i++)
		{
			result= result+results.get(i).area+",";
		}

		return result;
	}


	public String printResults(ArrayList<String> sampleNames, boolean separatePolarities)
	{
		String result = "";
		boolean matchFound;

		result += this.retention+",";
		result += this.quantIon+",";
		result += this.polarity+",";
		result += this.maxArea+",";

		if (lipidCandidates.size() > 0)
		{
			if (displaySumID) result = result+sumID+",";
			else result = result+lipidCandidates.get(0).lipidName+",";
		}
		else
		{
			result +=",";
		}

		result += this.features.size()+",";

		//If polarity switching
		if (!separatePolarities)
		{
			//Iterate through sample names
			for (int i=0; i<sampleNames.size(); i++)
			{
				matchFound = false;

				//Iterate through features
				for (int j=0; j<features.size(); j++)
				{
					//If a match print area
					if (sampleNames.get(i).equals(features.get(j).sample.file)
							&& calcPPMDiff(features.get(j).mass, this.quantIon)<Utilities.MAXPPMDIFF*4.0)
					{
						result += features.get(j).area+",";
						matchFound = true;
						break;
					}
				}
				//If no match found
				if (!matchFound) result += ",";
			}
		}
		//If separate polarities
		else
		{
			//Iterate through sample names
			for (int i=0; i<sampleNames.size(); i++)
			{
				matchFound = false;

				//Iterate through features
				for (int j=0; j<features.size(); j++)
				{
					//If the sample and polarity match, print area
					if (sampleNames.get(i).equals(features.get(j).sample.mergedName)
							&& features.get(j).polarity.equals(this.polarity) 
							&& calcPPMDiff(features.get(j).mass, this.quantIon)<Utilities.MAXPPMDIFF)
					{
						result += features.get(j).area+",";
						matchFound = true;
						break;
					}
				}
				//If no match found
				if (!matchFound) result += ",";
			}
		}

		return result;
	}

	public String printAreas()
	{
		String line1 = "";
		String line2 = "";

		for (int i=0; i<results.size(); i++)
		{
			line1 += results.get(i).file.filename+",";
			line2 += results.get(i).area+",";
		}

		return line1+"\n"+line2;
	}

	//Compares CG by retention
	public int compareTo(MzCompoundGroup target)
	{
		int result = 0;

		if (retention<target.retention) return -1;

		else if (retention>target.retention) return 1;

		return result;
	}
}
