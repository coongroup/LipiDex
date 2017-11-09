package lib_gen;
import java.util.ArrayList;
import java.util.Collections;


public class Lipid extends Utilities
{
	ArrayList<FattyAcid> fattyAcids; 	//array of FAs
	LipidClass lClass; 					//Class
	Adduct adduct; 						//Adduct
	String formula; 					//Formula
	Double mass; 						//monoisotopic mass
	String polarity; 					//Polarity of lipid
	String name; 						//canonical name
	MS2 generatedMS2; 					//generated MS2

	//Constructor
	public Lipid (ArrayList<FattyAcid> fattyAcids, LipidClass lClass, Adduct adduct)
	{
		this.fattyAcids = fattyAcids;
		this.lClass = lClass;
		this.adduct = adduct;
		this.polarity = adduct.polarity;
		generateName();
		calculateFormula();
	}

	//Return mass
	public Double getMass()
	{
		return mass;
	}

	//Return adduct
	public Adduct getAdduct()
	{
		return adduct;
	}

	//Return lipidClass object
	public LipidClass getLipidClass()
	{
		return lClass;
	}
	
	//Return arrayList of fatty acids in lipid
	public ArrayList<FattyAcid> getFattyAcids()
	{
		return fattyAcids;
	}

	//Get formula
	public String getFormula()
	{
		return formula;
	}

	//Get canonical name
	public String getName()
	{
		return name;
	}

	//Return adduct name as string
	public String getAdductName()
	{
		return adduct.getName();
	}

	//Adds a generator ms2 object
	public void addGeneratedMS2(MS2 ms2)
	{
		this.generatedMS2 = ms2;
	}

	//Sort FA Array
	public void sortFattyAcids()
	{
		Collections.sort(fattyAcids);
	}

	//Calculate Elemental Composition
	public void calculateFormula()
	{
		String tempFormula = "";

		//add in backbone + headgroup
		tempFormula = lClass.getFormula();

		//add in FAs
		for (int i=0; i<fattyAcids.size(); i++)
		{
			tempFormula =  mergeFormulas(tempFormula, fattyAcids.get(i).getFormula());
		}

		//add in adduct
		tempFormula =  mergeFormulas(tempFormula, adduct.getFormula());

		formula = tempFormula;
		
		calculateMass();
	}

	//Calculate Monoisotopic mass
	public void calculateMass()
	{
		mass = calculateMassFromFormula(formula)/adduct.charge;
	}

	//Generate canonical name
	public void generateName()
	{
		//Add class name
		name = lClass.getAbbreviation()+" ";

		//Add fatty acids
		for (int i=0; i<fattyAcids.size(); i++)
		{
			name = name + fattyAcids.get(i).getName();
			if ((i+1)<fattyAcids.size()) name = name +"_";
		}

		name = name+" "+adduct.getName();
	}

	//Generatre msp entry for library generation
	public String generateMSPResult()
	{
		String result = "";
		boolean optimalPolarity = false;
		
		if (lClass.optimalPolarity.contains(this.polarity)) optimalPolarity = true;
		
		//Name field
		result += "Name: "+name+";\n";

		//MW field
		result += "MW: "+roundToFourDecimals(mass)+"\n";

		//Precursor MZ field
		result += "PRECURSORMZ: "+roundToFourDecimals(mass)+"\n";

		//Comment Field
		result += "Comment: "+"Name="+name
				+" Mass="+roundToFourDecimals(mass)
				+" Formula="+formula
				+" OptimalPolarity="+optimalPolarity
				+" Type=LipiDex"+"\n";

		//NumPeaks Field
		result += "Num Peaks: "+generatedMS2.getTransitions().size()+"\n";

		//MS2 array
		for (int i=0; i<generatedMS2.getTransitions().size(); i++)
		{
			result += roundToFourDecimals(generatedMS2.getTransitions().get(i).getMass())+" "+
					Math.round(generatedMS2.getTransitions().get(i).getIntensity())
					+" \""+generatedMS2.getTransitions().get(i).getType()+"\"\n";
		}

		return result;
	}
}
