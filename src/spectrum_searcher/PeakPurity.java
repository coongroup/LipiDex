package spectrum_searcher;
import java.util.ArrayList;
import java.util.Collections;

import lib_gen.FattyAcid;

public class PeakPurity extends peak_finder.Utilities
{
	ArrayList<Transition> transitions = new ArrayList<Transition>();
	ArrayList<LibrarySpectrum> lipids = new ArrayList<LibrarySpectrum>();
	ArrayList<Double> intensities = new ArrayList<Double>();
	ArrayList<Double> matchedMasses = new ArrayList<Double>();
	ArrayList<Integer> purities = new ArrayList<Integer>();
	ArrayList<Double> comboIntensities = new ArrayList<Double>();
	ArrayList<Double> topMasses = new ArrayList<Double>();
	ArrayList<Double> intensityCorrectionArray = new ArrayList<Double>();
	double intensityCorrection = 0.0;
	Double minPurity = 5.0;
	int carbonNumber = 0;
	int unsatNumber = 0;
	int numFattyAcids = 0;
	int totalFattyAcids = 0;
	String chainGreatest = "";

	public int calcPurity(String lipidName, String sumLipidName,ArrayList<Transition> transitions, Double precursor, String polarity, 
			String lipidClass, String adduct, ArrayList<FattyAcid> faDB, LibrarySpectrum ls, ArrayList<LibrarySpectrum> isobaricIDs)
	{
		Double purity = 0.0;
		int purityInt = 0;
		Double sumInt = 0.0;
		this.transitions = transitions;

		//Count number of glycerol substitutions
		for(int i=0; i<lipidName.length(); i++ ) 
		{
			if( lipidName.charAt(i) == ':' )  
				totalFattyAcids++;
		}

		//Calculate purity for first match
		purity = getPurityIntensity(ls, faDB, true);

		if (purity == 0.0) return 0;
		else
		{
			intensities.add(purity);
			lipids.add(ls);

			//Add matches mass for in-source fragment screening
			for (int j=0; j<ls.transitionArray.size(); j++)
			{
				matchedMasses.add(ls.transitionArray.get(j).mass);
			}
		}

		//Calculate purity intensity for all other isobaric ids
		for (int i=0; i<isobaricIDs.size(); i++)
		{
			purity = getPurityIntensity(isobaricIDs.get(i), faDB, false);
			if (purity > 0.0 && isUniqueLipid(isobaricIDs.get(i)))
			{
				intensities.add(purity);
				lipids.add(isobaricIDs.get(i));
				//If purity>10 add matches mass for in-source fragment screening
				if (purity > minPurity)
				{
					for (int j=0; j<isobaricIDs.get(i).transitionArray.size(); j++)
					{
						matchedMasses.add(isobaricIDs.get(i).transitionArray.get(j).mass);
					}
				}
			}
		}

		//Calculate purity percentage
		for (int i=0; i<intensities.size(); i++)
		{
			sumInt += intensities.get(i);
		}

		purityInt = (int)Math.round((intensities.get(0)/sumInt)*100);

		//Calculate purity percentage for all species
		for (int i=0; i<intensities.size(); i++)
		{
			purities.add((int)Math.round((intensities.get(i)/sumInt)*100));
		}

		return purityInt;
	}

	//Returns true iff lipid not contained in purity array
	private boolean isUniqueLipid(LibrarySpectrum l)
	{
		for (int i=0; i<lipids.size(); i++)
			if (lipids.get(i).name.contains(l.name)) return false;
		return true;
	}

	private ArrayList<FattyAcid> parseFattyAcids(LibrarySpectrum ls, ArrayList<FattyAcid> faDB, String fattyAcidType)
	{
		ArrayList<FattyAcid> lipidFAs = new ArrayList<FattyAcid>();

		//Remove class name
		String faString = ls.name.substring(ls.name.indexOf(" ")+1,ls.name.lastIndexOf(" "));

		//Split name into FAs
		String[] split = faString.split("_");

		//Iterate and find alkyl matches
		for (int i=0; i<split.length; i++)
		{
			//Iterate through fadb
			for (int j=0; j<faDB.size(); j++)
			{
				//Add to temp array
				if (faDB.get(j).name.equals(split[i]) && faDB.get(j).type.equals(fattyAcidType))
				{
					lipidFAs.add(faDB.get(j));
				}
			}
		}

		return lipidFAs;
	}

	//Returns the intensity of each moiety-based fragment
	private ArrayList<Double> findFAIntensities (LibrarySpectrum ls, ArrayList<FattyAcid> faDB, String transitionType, Double minInt, boolean topMatch)
	{
		Double mass = 0.0;
		Double intensity = 0.0;
		int massIndex = 0;
		Double correctionIntensity;
		ArrayList<Double> matchedIntensities = new ArrayList<Double>();
		int faCount = 0;


		//For all library transitions
		for (int i=0; i<ls.transitionArray.size(); i++)
		{
			//Add matching entries to libmasses array
			if (ls.transitionArray.get(i).type.equals(transitionType))
			{
				mass = ls.transitionArray.get(i).mass;

				//Divide intensity by number of times fa occurs
				intensity = findTransitionMatch(mass);

				//Add if found
				if (intensity>minInt)
				{
					//Apply intensity correction
					if (getIndexOf(topMasses, mass)>-1)
					{
						intensity = intensity -  intensityCorrectionArray.get(getIndexOf(topMasses, mass));
						if (intensity<0.0) intensity = 0.0;
					}

					//Divide by number of occurences if less than 3 fatty acids
					//intensity = intensity/numOccurences(ls.transitionArray.get(i).fattyAcid,faDB);

					for (int j=0; j<numOccurences(ls.transitionArray.get(i).fattyAcid, faDB); j++)
					{
						matchedIntensities.add(intensity);
						faCount ++;
					}
				}
			}
		}

		if (faCount != faDB.size()) return null;

		//If all found and top hit, add intensities and masses to top arrays
		for (int i=0; i<ls.transitionArray.size(); i++)
		{
			//index of mass in topMasses array
			massIndex = getIndexOf(topMasses,ls.transitionArray.get(i).mass.doubleValue());

			//Correction is smallest of matched intensities
			Collections.sort(matchedIntensities);
			correctionIntensity = getMedianChainIntensity(matchedIntensities);

			//If unique and correct transition type, add to array
			if (!topMasses.contains(ls.transitionArray.get(i).mass.doubleValue()) 
					&& ls.transitionArray.get(i).type.equals(transitionType))
			{
				topMasses.add(ls.transitionArray.get(i).mass.doubleValue());
				intensityCorrectionArray.add(correctionIntensity);
			}
			else if (ls.transitionArray.get(i).type.equals(transitionType))
			{
				//Add correction intensity to array
				Double newCorrection = intensityCorrectionArray.get(massIndex) + correctionIntensity;

				//If greater than actual spectral peak, change to original spectrum intensity
				if (newCorrection > findTransitionMatch(ls.transitionArray.get(i).mass))
					newCorrection = findTransitionMatch(ls.transitionArray.get(i).mass);

				//Change correction
				intensityCorrectionArray.set(massIndex, newCorrection);
			}
		}

		return matchedIntensities;
	}

	private int numOccurences(String fattyAcid, ArrayList<FattyAcid> faDB)
	{
		int count = 0;

		for (int i=0; i<faDB.size(); i++)
		{
			if (faDB.get(i).name.equals(fattyAcid)) count ++;
		}
		return count;
	}

	private double getPurityIntensity(LibrarySpectrum ls, ArrayList<FattyAcid> faDB, boolean topMatch)
	{
		String transitionType = "";
		double maxFragType = 0;
		ArrayList<FattyAcid> lipidFAs = new ArrayList<FattyAcid>();
		ArrayList<Double> matchedIntensities = new ArrayList<Double>();

		if (!ls.isLipidex) return 0.0;

		//Iterate through transitions to find most intense fatty acid fragment type
		try
		{
			for (int i=0; i<ls.transitionArray.size(); i++)
			{
				if (!ls.transitionArray.get(i).type.contains("_Fragment") && !ls.transitionArray.get(i).type.contains("_Neutral Loss")
						&& !ls.transitionArray.get(i).type.contains("DG Fragment") && !ls.transitionArray.get(i).type.contains("PUFA"))
				{
					if (ls.transitionArray.get(i).intensity>maxFragType)
					{
						maxFragType = ls.transitionArray.get(i).intensity;
						transitionType = ls.transitionArray.get(i).type;
					}
				}
			}
		}
		catch (Exception e)
		{
			return 0.0;
		}

		if (transitionType.equals("")) return 0.0;

		//Parse out moiety
		if (transitionType.contains(" Fragment"))
			lipidFAs = parseFattyAcids(ls, faDB, transitionType.substring(transitionType.indexOf("_")+1).replace(" Fragment",""));
		else if (transitionType.contains(" Neutral Loss"))
			lipidFAs = parseFattyAcids(ls, faDB, transitionType.substring(transitionType.indexOf("_")+1).replace(" Neutral Loss",""));

		//Find intensity for all fatty acid peaks
		matchedIntensities = findFAIntensities(ls, lipidFAs, transitionType, maxFragType*.05, topMatch);

		//Check if all fragments were found
		if (matchedIntensities == null) return 0.0;
		
		//For 2 or less fatty acids, return median
		if (matchedIntensities.size()<3)
			return getMedianChainIntensity(matchedIntensities);

		else
			return matchedIntensities.get(matchedIntensities.size()-1);
	}

	/*
	 * If the putative mass is found within the normal ppm tolerance, 
	 * add to FA and intensity array.  The number of times added corresponds
	 * to the number of possible fa sights available for purity analysis
	 */
	private Double findTransitionMatch(Double mass)
	{
		for (int i=0; i<transitions.size(); i++)
		{
			if (calcPPMDiff(transitions.get(i).mass,mass)<MAXPPMDIFF)
			{
				return transitions.get(i).intensity;
			}
		}

		return 0.0;
	}


	public static void skipPermutation(int[] limits, int[] counters)
	{
		//Set first counter to maximum
		counters[0] = limits[0];
	}

	//Calculate approprioate medial spectral intensity
	private double getMedianChainIntensity(ArrayList<Double> intTemp)
	{
		if (intTemp.size()>0)
		{
			int middle;

			//Sort intensity array
			Collections.sort(intTemp);

			//Calculate median, if no middle point, move to the lower intensity
			middle = intTemp.size()/2;

			if (intTemp.size()%2 == 1) 
			{
				return intTemp.get(middle);
			} 
			else 
			{
				return ((intTemp.get(middle)+intTemp.get(middle-1))/2.0);
			}
		}
		else
		{
			return 0.0;
		}
	}
	//Returns the n value of an array
	private double getNChainIntensity(ArrayList<Double> intTemp, int num)
	{
		//Sort intensity array
		Collections.sort(intTemp);

		//Return third
		return intTemp.get(intTemp.size()-num);
	}

	//Returns the n value of an array, if not above threshold return n+1
	private double getNChainIntensityAboveMin(ArrayList<Double> intTemp, int num, Double min)
	{
		//Sort intensity array
		Collections.sort(intTemp);

		if (intTemp.size()-num > min) 
			return intTemp.get(intTemp.size()-num);
		else
			return intTemp.get(intTemp.size()-num+1);
	}

	//Returns the lowest value above zero
	private double getMinIntensityAboveZero(ArrayList<Double> intTemp)
	{
		//Sort intensity array
		Collections.sort(intTemp);

		for (int i=0; i<intTemp.size(); i++)
		{
			if (intTemp.get(i)>0.001) return intTemp.get(i);
		}

		return 0.0;
	}

	//Returns index of double value
	private int getIndexOf(ArrayList<Double> array, double num)
	{
		for (int i=0; i<array.size(); i++)
		{
			if (Math.abs(array.get(i)-num)<0.000001)
			{
				return i;
			}
		}

		return -1;
	}
}