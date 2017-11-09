package lib_gen;

public class Adduct 
{
	String formula; 	//Elemental Composition
	String name;		//Name of adduct
	boolean loss; 		//True iff adduct is a NL
	String polarity; 	//polarity
	int charge; 		//charge state of adduct
	
	//Constructor
	public Adduct(String name, String formula, boolean loss, String polarity, int charge)
	{
		this.name = name;
		this.formula = formula;
		this.loss = loss;
		this.polarity = polarity;
		this.charge = charge;
	}
	
	//Returns polarity
	public String getPolarity()
	{
		return polarity;
	}
	
	//Returns charge
	public int getCharge()
	{
		return charge;
	}
	
	//Returns name
	public String getName()
	{
		return name;
	}
	
	
	//Returns chemical formula
	public String getFormula()
	{
		return formula;
	}
	
	//Returns string array representation of adduct for table
	public String[] getTableArray()
	{
		String[] result = new String[5];
		
		result[0] = name;
		result[1] = formula;
		if (loss) result[2] = "true";
		else result[2] = "false";
		result[3] = polarity;
		result[4] = String.valueOf(charge);
		
		return result;
	}
	
	//Returns string representation of adduct
	public String toString()
	{
		String result = "";
		
		result += name+",";
		result += formula+",";
		if (loss) result += "TRUE"+",";
		else result += "FALSE"+",";
		result += polarity+",";
		result += charge+",";
		
		return result;
	}
}
