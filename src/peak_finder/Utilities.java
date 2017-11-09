package peak_finder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import compound_discoverer.CDLipidCandidate;

import lib_gen.Adduct;


public class Utilities 
{
	public static Double MAXPPMDIFF = 20.0;				//Maximum mass different in ppm
	public static Double MINRTMULTIPLIER = 0.5;			//RT multplier for rt filtering
	public static Double MINDOTPRODUCT = 500.0;			//Minimum dot product for match
	public static Double MINREVDOTPRODUCT = 700.0;		//Minimum reverse dot product
	public static Double MINFAPURITY = 75.0;			//Minimum fatty acid purity
	public static Integer MINIDNUM = 1;					//Minimum features associated to a CG to retain

	public static ArrayList<Double> adducts = new ArrayList<Double>
	(Arrays.asList(-59.013,1.00727,-1.00727,22.9892,18.0338));								//Array of common adduct masses for dimer search
	public Double[] masses = {1.007825035,2.014101779,7.016003,11.0093055,12.0,
			13.00335483,14.003074,15.00010897,15.99491463,17.9991603,18.99840322,
			22.9897677,23.9850423,30.973762,31.9720707,34.96885272,38.9637074,39.9625906,
			51.9405098,54.9380471,55.9349393,57.9353462,58.9331976,62.9295989,63.9291448,
			74.9215942,78.9183361,79.9165196,97.9054073,105.903478,106.905092,113.903357,
			126.904473,196.966543,201.970617};												//Array of element accurate masses	
	public static String[] elementsArray = {"H","Xa","Li","B","C","Xb","N","Xc","O",
		"Xd","F","Na","Mg","P","S","Cl","K","Ca","Cr","Mn","Fe","Ni","Co","Cu",
		"Zn","As","Br","Se","Mo","Pd","Ag","Cd","I","Au","Hg"};							//Array of allowed elements
	public static String[][] heavyElementArray = {{"(2H)","Xa"},{"(13C)","Xb"},				//Array of heavy element names
		{"(15N)","Xc"},{"(18O)","Xd"}};
	public static final Double MASSOFELECTRON = 0.00054858026;								//Mass of electron

	//Calculates mass difference in ppm
	public  Double calcPPMDiff(Double mass1, Double mass2)
	{
		Double result = 0.0;

		result = (Math.abs(mass1 -  mass2)/(mass2))*1000000;

		return result;
	}

	//Advances counters to next permutation, below limits
	public static boolean nextPermutation(int[] limits, int[] counters)
	{
		int c = 0;
		counters[c]++;
		while (counters[c] > limits[c])
		{
			counters[c] = 0;
			c++;
			if (c >= limits.length) return false;
			counters[c]++;
		}
		return true;
	}

	//Converts element and count array to elemental formula string
	public static String arrayToFormula(String[] elements, int[] counts)
	{
		String result = "";

		for (int i=0; i<elements.length; i++)
		{
			result = result+elements[i];
			result = result+counts[i]; 
		}

		return result;
	}

	//Skip counter permutation
	public static void skipPermutation(int[] limits, int[] counters)
	{
		//Set first counter to maximum
		counters[0] = limits[0];
	}

	//Add two elemental compositions together
	public static String mergeFormulas(String formula1, String formula2)
	{
		String result = "";

		String merged = formula1+formula2;

		int[] intArray; //numbers for formula
		String[] elementArray; //elements for formula

		//parse out integers
		intArray = formulaToCountArray(merged);

		//Parse out elements
		elementArray = formulaToElementArray(merged);

		//iterate through element array and merge results
		for (int i=0; i<elementArray.length; i++)
		{
			//if not a blank look for duplicate
			if (!elementArray[i].equals("X"))
			{
				//iterate through looking for another entry
				for (int j=i+1; j<elementArray.length; j++)
				{
					//if match found, merge results
					if (elementArray[i].equals(elementArray[j]))
					{
						intArray[i] = (Integer.valueOf(intArray[i])+Integer.valueOf(intArray[j]));
						intArray[j] = 0;
						elementArray[j] = "X";
					}
				}
			}
		}

		//Iterate through and produce final string
		for (int i=0; i<elementArray.length; i++)
		{
			if (!elementArray[i].equals("X"))
			{
				result = result+elementArray[i]+intArray[i];
			}
		}

		return result;
	}

	//Calculate a mass from an elemental formula
	public Double calculateMassFromFormula(String input)
	{
		Double result = 0.0; //mass
		int[] intArray;
		String[] elementArray;

		//parse out integers
		intArray = formulaToCountArray(input);

		//Parse out elements
		elementArray = formulaToElementArray(input);

		//Iterate through elements
		for (int i=0; i<elementArray.length; i++)
		{
			//For each, find MI mass and add
			for (int j=0; j<elementsArray.length; j++)
			{
				//If element match, add to final result
				if (elementsArray[j].equals(elementArray[i]))
				{
					result = result + masses[j]*Double.valueOf(intArray[i]);
				}
			}
		}

		return result;
	}

	//Returns a string array containing all elements in formula
	public static String[] formulaToElementArray(String formula)
	{
		String[] split;
		String formulaTemp = removeHeavyElements(formula);
		split = formulaTemp.split("(?=\\p{Upper})");
		String[] elementArray = new String[split.length];

		//Split out elements
		for (int i=0; i<split.length; i++)
		{
			elementArray[i] = split[i].split("[^A-Za-z]+")[0];
		}


		return elementArray;
	}

	//Remove heavy isotopes elements for later processing
	public static String removeHeavyElements(String formula)
	{
		String result = formula;

		for (int i=0; i<heavyElementArray.length; i++)
		{
			if (formula.contains(heavyElementArray[i][0]))
			{
				result = result.replaceAll(heavyElementArray[i][0], heavyElementArray[i][1]);
				result = result.replaceAll("\\)", "");
				result = result.replaceAll("\\(", "");
			}
		}

		return result;
	}

	//Convert a formula to an array of all eelement counts
	public static int[] formulaToCountArray(String formula)
	{
		String tempString = removeHeavyElements(formula);
		String[] elementArray = formulaToElementArray(tempString);

		//Replace elements with commas
		for (int i=0; i<elementArray.length; i++)
		{
			tempString.replace(elementArray[i], ",");
			tempString = tempString.substring(0,tempString.indexOf(elementArray[i]))
					+","+tempString.substring(tempString.indexOf(elementArray[i])+elementArray[i].length());		
		}

		//Split array by commas
		String[] temp = tempString.split(",");
		int[] result = new int[elementArray.length];

		//Add numbers to result array
		for (int i=0; i<temp.length-1; i++)
		{
			if (!temp[i+1].equals("")) 
				result[i] = Integer.valueOf(temp[i+1]);
			else
				result[i] = 1;
		}

		if (temp.length<elementArray.length+1) result[result.length-1] = 1;

		return result;
	}

	//Returns the standard deviation of an arrayList of doubles
	public double sd (ArrayList<Double> table)
	{
		double mean = findMean(table);
		double temp = 0;

		for (int i = 0; i < table.size(); i++)
		{
			Double val = table.get(i);
			double squrDiffToMean = Math.pow(val - mean, 2);
			temp += squrDiffToMean;
		}

		double meanOfDiffs = (double) temp / (double) (table.size());

		return Math.sqrt(meanOfDiffs);
	}

	//Returns the mean of an arrayList of doubles
	public double findMean(ArrayList<Double> nums)
	{
		double sum = 0;

		for (int i = 0; i < nums.size(); i++)
		{
			sum += nums.get(i);
		}

		return sum / nums.size();

	} 

	//Returns the median of an arrayList of doubles
	public double findMedian(ArrayList<Double> nums)
	{
		Collections.sort(nums);
		int middle = nums.size()/2;
		if (nums.size()%2 == 1) {
			return nums.get(middle);
		} else {
			return (nums.get(middle-1) + nums.get(middle)) / 2.0;
		}
	}

	//Removes spaces from a string
	public String removeSpaces(String filename)
	{
		String result = "\"" + filename + "\"";
		return result;
	}

	//Returns true iff two masses could be potential dimers
	public boolean checkDimer(ArrayList<Adduct> adductArray, Double mass1, Double mass2, String polarity)
	{
		double massCheck1 = 0.0;
		double massCheck2 = 0.0;

		//For all possible adduct masses
		for (int i=0; i<adductArray.size(); i++)
		{
			if (adductArray.get(i).getPolarity().equals(polarity))
			{
				massCheck1 = (mass1 - calculateMassFromFormula(adductArray.get(i).getFormula()))*2.0;
				massCheck2 = mass2 - calculateMassFromFormula(adductArray.get(i).getFormula());

				if (calcPPMDiff(massCheck1, massCheck2)<MAXPPMDIFF)
					return true;

				massCheck2 = (mass2 - calculateMassFromFormula(adductArray.get(i).getFormula()))*2.0;
				massCheck1 = mass1 - calculateMassFromFormula(adductArray.get(i).getFormula());

				if (calcPPMDiff(massCheck1, massCheck2)<MAXPPMDIFF)
					return true;
			}
		}

		return false;
	}

	//Returns true iff two masses could be potential in-source fragment pair
	public boolean checkFragment(Lipid l, Double mass)
	{
		if (l!=null)
		{
			//For all possible adduct masses
			for (int i=0; i<l.fragmentMasses.size(); i++)
			{
				if (calcPPMDiff(l.fragmentMasses.get(i), mass)<MAXPPMDIFF)
					return true;
			}
		}

		return false;
	}

	//Returns true iff two masses could be potential adduct pairs
	public boolean checkAdductKnown(ArrayList<Adduct> adductArray, Double mass1, Double mass2, Lipid mass1ID)
	{
		double massCheck1 = 0.0;
		double massCheck2 = 0.0;
		Adduct mass1AdductObject = null;

		if (mass1ID == null) return false;

		//Find adduct match for mass 1
		for (int i=0; i<adductArray.size(); i++)
		{
			if (adductArray.get(i).getName().equals(mass1ID.adduct))
				mass1AdductObject = adductArray.get(i);
		}

		if (mass1AdductObject == null) return false;

		//For all possible adduct masses
		for (int i=0; i<adductArray.size(); i++)
		{
			massCheck1 = mass1 - calculateMassFromFormula(mass1AdductObject.getFormula());
			massCheck2 = mass2 - calculateMassFromFormula(adductArray.get(i).getFormula());;

			if (calcPPMDiff(massCheck2, massCheck1)<MAXPPMDIFF)
				return true;
		}

		return false;
	}

	//Returns true iff two masses could be potential adduct pairs
	public boolean checkAdductUnknown(ArrayList<Adduct> adductArray, Double mass1, Double mass2)
	{
		double massCheck1 = 0.0;
		double massCheck2 = 0.0;
		Adduct mass1AdductObject = null;

		//For all possible adduct masses
		for (int i=0; i<adductArray.size(); i++)
		{
			for (int j=0; j<adductArray.size(); j++)
			{
				if (i!=j)
				{
					massCheck1 = mass1 - calculateMassFromFormula(adductArray.get(i).getFormula());
					massCheck2 = mass2 - calculateMassFromFormula(adductArray.get(j).getFormula());
					if (calcPPMDiff(massCheck2, massCheck1)<MAXPPMDIFF)
						return true;
				}
			}
		}

		return false;
	}



	//Merge Candidates From Features
	public void mergeCandidates(ArrayList<CDLipidCandidate> candidates, CDLipidCandidate lipid)
	{
		boolean found = false;

		for (int i=0; i<candidates.size(); i++)
		{
			//if match found in preferred polarity, iterate count and merge result
			if (lipid.preferredPolarity && candidates.get(i).preferredPolarity)
			{
				if (candidates.get(i).lipidName.equals(lipid.lipidName))
				{
					found = true;		
					for (int j=0; j<lipid.identifications.size(); j++)
					{
						candidates.get(i).addLipid(lipid.identifications.get(j));
					}

					Collections.sort(candidates.get(i).identifications);
				}
			}
			else
			{
				if (candidates.get(i).sumLipidName.equals(lipid.sumLipidName))
				{
					found = true;		
					for (int j=0; j<lipid.identifications.size(); j++)
					{
						candidates.get(i).addLipid(lipid.identifications.get(j));
					}

					Collections.sort(candidates.get(i).identifications);
				}
			}
		}

		if (!found)
			candidates.add(lipid);
	}


	//Round a number to 4 decimal placed
	public Double roundToFourDecimals(Double input)
	{
		double result = Math.round(input * 10000);
		result = result/10000;
		return result;
	}
}
