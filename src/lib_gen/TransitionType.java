package lib_gen;

import java.util.ArrayList;

public class TransitionType extends Utilities
{
	public String name;				//Name of transition type
	public String fattyAcidType;	//Type of fatty acid
	public boolean isFattyAcid;		//True iff the transition type involves fatty acid moieties
	public boolean isNeutralLoss;	//True iff the transition type is a loss from a precursor
	public int numFattyAcids;		//Number of fatty acids involved in transition

	//Constructor
	public TransitionType(String name, String fattyAcidType, boolean isFattyAcid, boolean isNeutralLoss, int numFattyAcids)
	{
		this.name = name;
		this.fattyAcidType = fattyAcidType;
		this.isFattyAcid = isFattyAcid;
		this.isNeutralLoss = isNeutralLoss;
		this.numFattyAcids = numFattyAcids;
	}

	//Method to return mass based on rule
	public Double calculateMass(ArrayList<FattyAcid> faArray, Lipid l, Double mass, int charge, String polarity) throws CustomException
	{
		Double result = 0.0;

		//Validate format
		if (!isValid(faArray))
		{
			//throw new CustomException ("Invalid transition definition supplied", null);
		}

		//If moiety-based fragment
		if (!isNeutralLoss && isFattyAcid)
		{
			Double fragSum = 0.0;

			//Sum moiety masses
			for (int i=0; i<faArray.size(); i++)
			{
				fragSum += faArray.get(i).mass;
			}

			//(Electrons + moiety mass + formula mass) / charge
			result = addElectrons((fragSum+mass),charge, polarity)/charge;
		}
		//If static fragment
		else if (!isNeutralLoss && !isFattyAcid)
		{
			//(Electrons + formula mass) / charge
			result = addElectrons(mass,charge, polarity)/charge;
		}
		//If moeity-based neutral loss
		else if (isNeutralLoss && isFattyAcid)
		{
			//(Electrons + (precursor - moiety + formula mass)) / charge
			result = addElectrons((l.mass-faArray.get(0).mass+mass),charge, polarity)/charge;
		}
		//If static neutral loss
		else if (isNeutralLoss && !isFattyAcid)
		{
			//(Electrons + (precursor + formula mass)) / charge
			result = addElectrons((l.mass+mass),charge, polarity)/charge;
		}

		return result;
	}

	//Returns true iff the fatty acid types and numbers are valid
	private boolean isValid(ArrayList<FattyAcid> faArray)
	{
		//Validate number
		if (faArray.size() != numFattyAcids) return false;

		//Validate type for PUFAs
		if (faArray.size()>0 && fattyAcidType.contains("PUFA"))
		{
			for (int i=0; i<faArray.size(); i++)
			{
				if (!faArray.get(i).pufa) return false;
			}
		}
		//Validate type for all other moieties
		else
		{
			for (int i=0; i<faArray.size(); i++)
			{
				if (!faArray.get(i).type.equals(fattyAcidType)) return false;
			}
		}
		
		return true;
	}

	//Add electron mass to fragment based on charge and polarity
	public Double addElectrons(Double mass, Integer charge, String polarity)
	{
		//If adding electrons
		if (polarity.equals("+"))
			return mass-charge*MASSOFELECTRON;
		//If removing electrons
		else
			return mass+charge*MASSOFELECTRON;
	}

	//Return string representation of object
	public String toString()
	{
		return name;
	}

}
