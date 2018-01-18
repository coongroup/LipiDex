package lib_gen;
import java.util.ArrayList;

//Class representing an individual lipid class
public class LipidClass extends Utilities {

	String className; 								//Full Class Name
	public String classAbbrev; 							//Abbreviated class name
	String headGroup; 								//elemental formula of head group
	public Adduct[] adducts; 								//Array of adduct objects allowed for each class
	boolean sterol;									//true iff backbone of lipid is sterol
	boolean glycerol; 								//true iff backbone of lipid is glycerol
	boolean sphingoid; 								//true iff sphingoid base
	String backboneFormula;							//Elemental formula of backbone							
	int numFattyAcids; 								//number of allowed fatty acids
	String optimalPolarity; 						//Fragment informative polarity
	public ArrayList<ArrayList<FattyAcid>> possibleFA; 	//Array of possible fatty acids
	String formula; 								//Elemental formula of lipid class - fatty acids - adduct
	ArrayList<String> fattyAcidTypes; 				//Arraylist of all possible fatty acid classes for class

	//Constructor
	public LipidClass (String className, String classAbbrev, String headGroup, 
			Adduct[] adducts, boolean sterol, boolean glycerol, boolean sphingoid, String backboneFormula,
			int numFattyAcids, String optimalPolarity, ArrayList<String> fattyAcidTypes)
	{
		//Instantiate class variables
		this.className = className;
		this.classAbbrev = classAbbrev; 
		this.headGroup = headGroup;
		this.backboneFormula = backboneFormula;
		this.adducts = adducts; 
		this.sterol = sterol;
		this.glycerol = glycerol; 
		this.sphingoid = sphingoid;
		this.numFattyAcids = numFattyAcids;
		this.optimalPolarity = optimalPolarity;
		this.fattyAcidTypes = fattyAcidTypes;
		this.possibleFA = new ArrayList<ArrayList<FattyAcid>>();
	
		//Calculate elemental formula
		calculateFormula();
	}

	//Returns string array representation of class for table display
	public String[] getTableArray()
	{
		String adductString = "";
		
		String[] result = new String[11];
		
		//Name
		result[0] = className;
		
		//Abbreviation
		result[1] = classAbbrev;
		
		//Head Group
		result[2] = headGroup;
		
		//Adducts
		for (int i=0; i<adducts.length; i++)
		{
			adductString += adducts[i].name;
			if (i<adducts.length-1) adductString += ";";
		}
		result[3] = adductString;
		
		//Backbone
		if (glycerol) result[4] = "Glycerol";
		else if (sterol) result[4] = "Sterol";
		else if (sphingoid) result[4] = "Sphingoid";
		else result[4] = this.backboneFormula;
		
		
		
		//Num Fatty Acids
		result[5] = String.valueOf(numFattyAcids);
		
		//Optimal Polarity
		result[6] = optimalPolarity;
		
		//Add in fatty acid types
		result[7] = fattyAcidTypes.get(0);
		if (numFattyAcids >= 2) result[8] = fattyAcidTypes.get(1);
		else result[8] = "-";
		if (numFattyAcids >= 3) result[9] = fattyAcidTypes.get(2);
		else result[9] = "-";
		if (numFattyAcids >= 4) result[10] = fattyAcidTypes.get(3);
		else result[10] = "-";
		
		return result;
	}
	
	//Retuen class abbreviation as string
	public String getAbbreviation()
	{
		return classAbbrev;
	}
	
	//Return array of all fatty acid types
	public ArrayList<String> getFattyAcidTypes()
	{
		return fattyAcidTypes;
	}
	
	//Returns the number of fatty acids allowed for class
	public int getNumFattyAcids()
	{
		return numFattyAcids;
	}
	
	//Returns headgroup elemental formula
	public String getHeadGroup()
	{
		return headGroup;
	}
	
	//Returns the number of fatty acids of each type allowed for class
	public int getNumFattyAcidsofType(String type)
	{
		int count = 0;
		
		for (int i=0; i<fattyAcidTypes.size(); i++)
		{
			if (fattyAcidTypes.get(i).equals(type))
				count++;
		}
		return count;
	}
	
	//Returns elemental formula
	public String getFormula()
	{
		return formula;
	}
	
	//Populate 2d FA array with all possible fatty acids
	public void populateFattyAcids(ArrayList<FattyAcid> allFattyAcids)
	{
		possibleFA.clear();
		ArrayList<FattyAcid> faTemp;
		ArrayList<FattyAcid> faArray;

		//If lipid class is a CL, remove fatty acids types
		if (classAbbrev.contains("CL") || className.contains("ardiol"))
		{
			faArray = new ArrayList<FattyAcid>();
			
			String allowedFA = "_14:0_16:0_16:1_17:1_18:0_18:1_18:2_18:3_20:0_20:1_20:2_20:3_20:5_20:4_22:0_22:6_";
			
			for (int i=0; i<allFattyAcids.size(); i++)
			{
				if (allowedFA.contains("_"+allFattyAcids.get(i).name+"_"))
					faArray.add(allFattyAcids.get(i));
			}
		}
		else
			faArray = allFattyAcids;
		
		//Iterate through possible fatty acid slots
		for (int i=0; i<fattyAcidTypes.size(); i++)
		{
			//Reset temp holder
			faTemp = new ArrayList<FattyAcid>();
			
			//Iterate through
			for (int j=0; j<faArray.size(); j++)
			{
				//Check if matching fa is found.  If so, add to temp array
				if (faArray.get(j).type.equals(fattyAcidTypes.get(i)) && faArray.get(j).enabled)
					faTemp.add(faArray.get(j));
			}
			//Add populated temp array to faTemp
			possibleFA.add(faTemp);
		}
	}
	

	
	//Calculate elemental composition of lipid class
	public void calculateFormula()
	{
		formula = headGroup;
		
		if (glycerol)
			formula =  mergeFormulas(formula, "C3H5");
		else if (sphingoid)
			formula =  mergeFormulas(formula, "C3H2");
		else if (sterol)
			formula =  mergeFormulas(formula, "C27H45");
		else 
			formula =  mergeFormulas(formula, this.backboneFormula);
	} 
	
	//Returns string representation of class
	public String toString()
	{
		String result = "";
		
		result += className+",";
		result += classAbbrev+",";
		result += headGroup+",";

		for (int i=0; i<adducts.length; i++)
		{
			result += adducts[i].name;
			
			if (i<adducts.length-1) result += ";";
			else result += ",";
		}
		
		if (sterol) result += "TRUE,";
		else result += "FALSE,";
		
		if (glycerol) result += "TRUE,";
		else result += "FALSE,";
		
		if (sphingoid) result += "TRUE,";
		else result += "FALSE,";
		
		result += backboneFormula+",";
		
		result += numFattyAcids+",";
		result += optimalPolarity+",";
		
		for (int i=0; i<fattyAcidTypes.size(); i++)
		{
			result += fattyAcidTypes.get(i);
			if (i < 3) result += ",";
		}
		
		for (int i=fattyAcidTypes.size(); i<4; i++)
		{
			result += "none";
			if (i<3) result += ",";
		}
		
		return result;
	}
}
