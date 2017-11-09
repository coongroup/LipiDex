package lib_gen;

public class FattyAcid extends Utilities implements Comparable<FattyAcid> 
{
	public String formula; 				//Elemental formula
	public String name; 				//Abbreviated name
	public Double mass; 				//Mass for sorting purpose
	public Integer carbonNumber;		//Number of carbons in FA chain
	public Integer dbNumber;			//Number of double bonds in FA chain
	public boolean pufa = false;		//True iff fatty acids is a polyunsaturated fatty acid
	public boolean enabled = false;		//True iff the fatty acid will be used for library generation
	public String type;					//Type of fatty acid


	//Constructor
	public FattyAcid(String name, String type, String formula, String enabled)
	{
		//Initialize class variables
		this.name = name;
		this.mass = calculateMassFromFormula(formula);
		this.formula = formula;
		this.type = type;	
		if (enabled.equals("true")) this.enabled = true;
		else this.enabled = false;

		//Decide whether fatty acid is a PUFA
		if (Integer.valueOf(name.substring(name.indexOf(":")+1))>1 )
			pufa = true;

		//Parse fatty acid name for carbon and db number calculation
		parseFA();
	}

	//Return elemental formula
	public String getFormula()
	{
		return formula;
	}

	//Return name
	public String getName()
	{
		return name;
	}

	//Return FA mass
	public Double getMass()
	{
		return mass;
	}

	//Comparator for sorting fa by type and molecular weight
	public int compareTo (FattyAcid f)
	{
		if (Character.isLetter(f.name.charAt(0)) && !Character.isLetter(name.charAt(0))) return 1;
		else if (!Character.isLetter(f.name.charAt(0)) && Character.isLetter(name.charAt(0))) return -1;
		else if (f.type.equals(type))
		{
			if (mass>f.getMass()) return 1;
			else if (mass<f.getMass()) return -1;
			else return 0;
		}
		else return (type.compareTo(f.type));
	}

	//Returns string representation of FA for txt file generation
	public String saveString()
	{
		String result = "";

		result += name+",";
		result += type+",";
		result += formula+",";

		if (enabled) result += true;
		else result += false;

		return result;
	}
	//Parse carbon number and double bond number from FA name
	public void parseFA()
	{
		String split[] = name.split(":");

		//remove all letter characters
		for (int i=0; i<split.length; i++)
		{
			split[i] = split[i].replaceAll("[^\\d.]", "");
			split[i] = split[i].replaceAll("-", "");
		}


		//Find carbon number
		carbonNumber = Integer.valueOf(split[0]);

		//Find db number
		dbNumber = Integer.valueOf(split[1]);
	}

	//Returns FA name
	public String toString()
	{
		return name;
	}

}
