package lib_gen;
import java.util.ArrayList;


public class ConsensusLipidClass extends Utilities
{
	LipidClass lClass; 							//Associated lipidclass
	Adduct adduct; 								//Associated adduct class
	String name; 								//composite name
	ArrayList<Transition> faBasedTransitions;	//Array of all fatty acid transitions
	ArrayList<Lipid> possibleLipids;			//Array list of all possible lipids in class

	//Constructor
	public ConsensusLipidClass(LipidClass lClass, Adduct adduct)
	{
		this.lClass = lClass;
		this.adduct = adduct;
		name = lClass.getAbbreviation()+" "+adduct.getName();
		faBasedTransitions = new ArrayList<Transition>();
	}

	//Returns class name
	public String getName()
	{
		return name;
	}

	//Returns string representation of lipid class + adduct
	public String toString()
	{
		String result = "";

		result = result +lClass.getAbbreviation()+" "+adduct.getName()+"\n";

		return result;
	}
}
