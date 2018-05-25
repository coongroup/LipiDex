package lib_gen;
import java.util.*;

//Class with parameters and utilities used by all classes
public class Utilities 
{
	//User-supplied parameters
	public static final Double ANNOTATIONPPMTOL = 5.0;										//Maximum ppm diff for annotation
	public static final Double DOTPRODUCTPPMTOL = 10.0;										//Minimum ppm diff for dot product calculation
	public static final Double MINANNOTATIONMZTOL = 0.01;									//Minimum mz different for annotionat
	public String[] elements = {"C","H","O","P","N","S","Na"};	
	public Double[] masses = {1.007825035,2.014101779,7.016003,11.0093055,12.0,
			13.00335483,14.003074,15.00010897,15.99491463,17.9991603,18.99840322,
			22.9897677,23.9850423,30.973762,31.9720707,34.96885272,38.9637074,39.9625906,
			51.9405098,54.9380471,55.9349393,57.9353462,58.9331976,62.9295989,63.9291448,
			74.9215942,78.9183361,79.9165196,97.9054073,105.903478,106.905092,113.903357,
			126.904473,196.966543,201.970617};												//Accurate masses for all elements
	public static String[] elementsArray = {"H","Xa","Li","B","C","Xb","N","Xc","O",
		"Xd","F","Na","Mg","P","S","Cl","K","Ca","Cr","Mn","Fe","Ni","Co","Cu",
		"Zn","As","Br","Se","Mo","Pd","Ag","Cd","I","Au","Hg"};								//All allowed elements
	public static String[][] heavyElementArray = {{"(2H)","Xa"},{"(13C)","Xb"},
		{"(15N)","Xc"},{"(18O)","Xd"}};														//Symbols for heavy elements
	public static final Double MASSOFELECTRON = 0.00054858026;								//Mass of an electron

	//Calculate the mass difference in ppm
	public  Double calcPPMDiff(Double mass1, Double mass2)
	{
		Double result = 0.0;
		result = (Math.abs(mass1 -  mass2)/(Math.abs(mass2)))*1000000;
		return result;
	}

	//Removes heavy elements from formula for later calculations
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

	//Round a number to 4 decimal placed
	public Double roundToFourDecimals(Double input)
	{
		double result = Math.round(input * 10000);
		result = result/10000;
		return result;
	}

	//Returns true iff a string is a valid elemental formula
	public boolean isFormula(String formulaString)
	{
		String newFormula = removeHeavyElements(formulaString);
		
		if (formulaString.equals("-")) return true;
		
		for (int i=0; i<elementsArray.length; i++)
		{
			if (newFormula.contains(elementsArray[i])) return true;
		}

		return false;
	}

	//Returns the putative elementa formula of a mass based on user-prodvided constrains
	public String annotateMassWithMZTolerance(Double fragMass, String formula, Double minMZTol, Double minRDBE)
	{
		String[] elements = formulaToElementArray(formula);
		int[] limits = formulaToCountArray(formula);
		int[] counters = new int[elements.length];
		Double massTemp = 0.0;
		String formulaMinPPM = "";
		Double mzDiffTemp = 0.0;
		Double minMZ = minMZTol;

		while (true) {

			//If mass is greater than frag mass, stop iteration of element
			massTemp =  calculateMassFromFormula(arrayToFormula(elements, counters));

			if (massTemp > fragMass+1)
			{
				skipPermutation(limits, counters);
			}

			//Check if formula minimizes ppm error
			mzDiffTemp = Math.abs(fragMass- massTemp);

			if (mzDiffTemp<minMZ && calcRDBE(arrayToFormula(elements, counters)) >= minRDBE)
			{
				minMZ = mzDiffTemp;
				formulaMinPPM = arrayToFormula(elements, counters);
			}


			// Advance permutation
			if (!nextPermutation(limits, counters)) break;
		}

		return formulaMinPPM;
	}

	//Check if a formula is a valid elemental formula
	public boolean validElementalFormula(String input) throws CustomException
	{
		boolean result = true;
		int[] intArray;
		String[] elementArray;
		//Possible elements
		boolean[] validArray;

		try
		{
			if (input.equals("-")) return true;
			
			if (!input.equals(""))
			{
				
				String newFormula = removeHeavyElements(input);
				
				//parse out integers
				intArray = formulaToCountArray(newFormula);

				//Parse out elements
				elementArray = formulaToElementArray(newFormula);
				validArray = new boolean[elementArray.length];

				//Iterate through elements
				for (int i=0; i<intArray.length; i++)
				{
					validArray[i] = false;

					//For each, find MI mass and add
					for (int j=0; j<elementsArray.length; j++)
					{
						//If element match, add to final result
						if (elementsArray[j].equals(elementArray[i]))
						{
							validArray[i] = true;
						}
					}

					if (!validArray[i]) result = false;
				}


				if ((elementArray.length != intArray.length) || elementArray.length<1) result = false;
			}
			else return false;

			return result;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	//Calculate the monoisotopic mass from a formula, 
	public Double calculateMassFromFormula(String input)
	{
		Double result = 0.0; //mass
		int[] intArray;
		String[] elementArray;

		if (input.equals("-")) return 0.0;
		
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

	//Converts a formula to an array of all contained elements
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

	//Returns an integer array of the count of all elements in formula
		public static int[] formulaToCountArray(String formula)
		{
			String tempString = removeHeavyElements(formula);
			String[] elementArray = formulaToElementArray(tempString);

			//Remove elements from formula and replace with comma
			for (int i=0; i<elementArray.length; i++)
			{
				tempString.replace(elementArray[i], ", ");
				tempString = tempString.substring(0,tempString.indexOf(elementArray[i]))
						+", "+tempString.substring(tempString.indexOf(elementArray[i])+elementArray[i].length());		
			}

			//Split into array
			String[] temp = tempString.split(",");
			
			
			int[] result = new int[elementArray.length];

			if (temp.length == 0)
			{
				//Add counts to array
				for (int i=0; i<result.length-1; i++)
				{
						result[i] = 1;
				}
			}

			else
			{
				//Add counts to array
				for (int i=0; i<temp.length-1; i++)
				{
					if (!temp[i+1].equals(" ")) 
						result[i] = Integer.valueOf(temp[i+1].substring(1));
					else
						result[i] = 1;
				}
			}

			//Trim array
			if (temp.length<elementArray.length+1) result[result.length-1] = 1;

			return result;
		}

	//Annotate mass with putatuve elements formula within mass tolerance
	public String annotateMassWithMZTolerance(Double fragMass, String formula)
	{
		String[] elements = formulaToElementArray(formula);
		int[] limits = formulaToCountArray(formula);
		int[] counters = new int[elements.length];
		Double massTemp = 0.0;
		String formulaMinPPM = "";
		Double mzDiffTemp = 0.0;
		Double minMZ = MINANNOTATIONMZTOL;
		
		while (true) {

			//If mass is greater than frag mass, stop iteration of element
			massTemp =  calculateMassFromFormula(arrayToFormula(elements, counters));

			if (massTemp > fragMass+1)
			{
				skipPermutation(limits, counters);
			}

			//Check if formula minimizes ppm error
			mzDiffTemp = Math.abs(fragMass- massTemp);

			if (mzDiffTemp<minMZ)
			{
				minMZ = mzDiffTemp;
				formulaMinPPM = arrayToFormula(elements, counters);
			}


			// Advance permutation
			if (!nextPermutation(limits, counters)) break;
		}

		return formulaMinPPM;
	}

	//Annotate mass
	public String annotateMass(Double fragMass, String formula)
	{
		String[] elements = formulaToElementArray(formula);
		int[] limits = formulaToCountArray(formula);
		int[] counters = new int[elements.length];
		Double massTemp = 0.0;
		String formulaMinPPM = "";
		Double ppmTemp = 0.0;
		Double minPPM = ANNOTATIONPPMTOL;
		
		while (true) {

			//If mass is greater than frag mass, stop iteration of element
			massTemp =  calculateMassFromFormula(arrayToFormula(elements, counters));

			if (massTemp > fragMass+1)
			{
				skipPermutation(limits, counters);
			}

			//Check if formula minimizes ppm error
			ppmTemp = ppmDiff(fragMass, massTemp);

			if (ppmTemp<minPPM)
			{
				minPPM = ppmTemp;
				formulaMinPPM = arrayToFormula(elements, counters);
			}


			// Advance permutation
			if (!nextPermutation(limits, counters)) break;
		}
		return formulaMinPPM;
	}

	public String annotateMassWithExceptions(Double fragMass, String formula, String[] forbiddenElements)
	{
		String[] elements = formulaToElementArray(formula);
		int[] limits = formulaToCountArray(formula);
		int[] counters = new int[elements.length];
		Double massTemp = 0.0;
		String formulaMinPPM = "";
		String formulaTemp = "";
		Double ppmTemp = 0.0;
		Double minPPM = DOTPRODUCTPPMTOL;
		Boolean containsForbidden = false;
		
		while (true) {

			containsForbidden = false;

			//If mass is greater than frag mass, stop iteration of element
			massTemp =  calculateMassFromFormula(arrayToFormula(elements, counters));
			if (massTemp > fragMass+1)
			{
				skipPermutation(limits, counters);
			}

			//Check if formula minimizes ppm error
			ppmTemp = ppmDiff(fragMass, massTemp);
			formulaTemp = arrayToFormula(elements, counters);


			//Check for forbidden Elements
			for (int i=0; i<elements.length; i++)
			{
				for (int j=0; j<forbiddenElements.length; j++)
				{
					if (counters[i]>0 && elements[i].equals(forbiddenElements[j]))
					{
						containsForbidden = true;
					}
				}
			}

			if (ppmTemp<minPPM && !containsForbidden)
			{
				minPPM = ppmTemp;
				formulaMinPPM = formulaTemp;
			}


			// Advance permutation
			if (!nextPermutation(limits, counters)) break;
		}
		return formulaMinPPM;
	}

	//Method to convert element array string to formula string
	public static String elementArrayToString (String input)
	{
		String result = ""; //result string
		ArrayList<String> elements= new ArrayList<String>(); //array of all unique elements
		ArrayList<Integer> counts = new ArrayList<Integer>(); //array of element counts
		String temp;

		//iterate through string
		for (int i=0; i<input.length(); i++)
		{
			//if a unique character add the count and element
			if (!elements.contains(String.valueOf(input.charAt(i))))
			{
				temp = input;
				elements.add(String.valueOf(input.charAt(i)));
				counts.add(temp.length() - temp.replaceAll(String.valueOf(input.charAt(i)), "").length());
			}
		}

		//Create result string
		for (int i=0; i<elements.size(); i++)
		{
			result = result + elements.get(i);
			result = result + counts.get(i);
		}

		return result;
	}

	//Method to calculate mass difference in ppm
	public static double ppmDiff(double massA, double massB)
	{
		double ppmResult = 0.0;

		ppmResult = Math.abs((Math.abs(massA) - Math.abs(massB))/Math.abs(massB))*1000000;

		return ppmResult;
	}

	//Method to advance array of counters for generation of all permutations
	public static boolean nextPermutation(int[] limits, int[] counters)
	{
		int c = 0; // the current counter
		counters[c]++; // increment the first counter
		while (counters[c] > limits[c]) // if counter c overflows
		{
			counters[c] = 0; // reset counter c
			c++; // increment the current counter
			if (c >= limits.length) return false; // if we run out of counters, we're done
			counters[c]++;
		}
		return true;
	}

	//Method to skip counters
	public static void skipPermutation(int[] limits, int[] counters)
	{
		//Set first counter to maximum
		counters[0] = limits[0];
	}
	
	//Method to calculate the ring double bond equivalent
	public static Double calcRDBE(String formula)
	{
		Double result = 1.0;
		
		String [] elements = formulaToElementArray(formula);
		int [] count = formulaToCountArray(formula);

		for (int i=0; i<elements.length; i++)
		{
			if (elements[i].equals("H")) result -= count[i]/2.0;
			else if (elements[i].equals("N")) result += count[i]/2.0;
			else if (elements[i].equals("C")) result += count[i];
			else if (elements[i].equals("P")) result += count[i]/2.0;
		}
		
		return result;
	}

	//Converts arrays of elements and counts to formula
	public static String arrayToFormula(String[] elements, int[] counts)
	{
		String result = "";

		for (int i=0; i<elements.length; i++)
		{
			if (counts[i]>0)
			{
				result = result+elements[i];
				result = result+counts[i]; 
			}
		}

		return result;
	}

	//Invert Elemental Formula
	public static String invertFormula(String formula)
	{
		String result = "";
		int[] intArray = formulaToCountArray(formula);
		String[] elementArray = formulaToElementArray(formula);
		
		//Iterate through elements
		for (int i=0; i<intArray.length; i++)
		{
			intArray[i] = intArray[i]*-1;
		}

		result = arrayToFormula(elementArray, intArray);

		return result;
	}

	//Decides if mass could come from a fatty acid fragment or neutral loss
	public String findFattyAcidFragment(Double mass, FattyAcid fattyAcid)
	{
		String fattyAcidFormula = fattyAcid.getFormula();
		String annotatedFormula = "";

		//if mass is at least 0.75 mass of total fatty acid
		if ((mass/fattyAcid.getMass())>.75 && mass<fattyAcid.getMass()+20.0)
		{
			//Attempt to annotate fragments as fatty acid, include +2h
			annotatedFormula = annotateMass(mass, mergeFormulas(fattyAcidFormula,"H2"));
		}

		return annotatedFormula;
	}
}
