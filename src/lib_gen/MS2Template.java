package lib_gen;
import java.util.ArrayList;
import java.util.Collections;

//Class representing fragmentation rules for lipid class and adduct
public class MS2Template extends Utilities implements Comparable<MS2Template>
{
	ConsensusLipidClass lipidClass; 				//Lipid class
	ArrayList<TransitionDefinition> transitions; 	//Arraylist for transition definitions
	ArrayList<FattyAcid> possibleFattyAcids; 		//Array of all theoretically possible fatty acids
	ArrayList<Lipid> theoreticalLipids; 			//Array of all theoretical lipids

	//Constructor
	public MS2Template(ConsensusLipidClass lipidClass, ArrayList<TransitionDefinition> transitions)
	{
		this.lipidClass = lipidClass;
		this.transitions = transitions;
		possibleFattyAcids = new ArrayList<FattyAcid>();
		theoreticalLipids = new ArrayList<Lipid>();
	}

	//Add fatty acid to possible array
	public void addFA (FattyAcid fa)
	{
		possibleFattyAcids.add(fa);
	}

	//Returns transition array
	public ArrayList<TransitionDefinition> getTransitions()
	{
		return transitions;
	}

	//Generate theoretical spectra from fragmentation rules
	public void generateInSilicoMS2(ArrayList<TransitionType> transitionTypes) throws CustomException
	{
		//Iterate  through all lipids
		for (int i=0; i<theoreticalLipids.size(); i++)
		{
			theoreticalLipids.get(i).addGeneratedMS2(generateMS2(theoreticalLipids.get(i),transitionTypes));
		}
	}

	//Add a theoretical lipid to array
	public void addTheoreticalLipid(Lipid l)
	{
		theoreticalLipids.add(l);
	}

	//Clear theoretical lipid array
	public void clearTheoreticalLipids()
	{
		theoreticalLipids = new ArrayList<Lipid>();
	}

	//Generate all possible lipids for lipid class based on active fatty acids
	public void generateLipids()
	{
		ArrayList<ArrayList<FattyAcid>> faArray = lipidClass.lClass.possibleFA;
		ArrayList<Lipid> result = new ArrayList<Lipid>();
		ArrayList<FattyAcid> faTemp = new ArrayList<FattyAcid>();
		ArrayList<FattyAcid> cLTemp1 = new ArrayList<FattyAcid>();
		ArrayList<FattyAcid> cLTemp2 = new ArrayList<FattyAcid>();
		ArrayList<String> shadowArray = new ArrayList<String>();
		String fattyAcidString;
		Lipid lipidTemp;

		int[] limits = new int[faArray.size()];
		int[] counters = new int[faArray.size()];

		//Populate limits
		for (int i=0; i<counters.length; i++)
		{
			limits[i] = faArray.get(i).size()-1;
		}

		while (true) {
			faTemp = new ArrayList<FattyAcid>();
			cLTemp1 = new ArrayList<FattyAcid>();
			cLTemp2 = new ArrayList<FattyAcid>();;
			fattyAcidString = new String("");

			//Populate faTemp array
			for (int i=0; i<counters.length; i++)
			{
				faTemp.add(faArray.get(i).get(counters[i]));
			}

			//Parse cardiolipins 
			if (lipidClass.lClass.classAbbrev.equals("CL"))
			{
				//Create 2x2 fatty acid array for CL
				cLTemp1.add(faTemp.get(0));
				cLTemp1.add(faTemp.get(1));
				cLTemp2.add(faTemp.get(2));
				cLTemp2.add(faTemp.get(3));

				//Sort arrays
				Collections.sort(cLTemp1);
				Collections.sort(cLTemp2);

				//Create string representing faArray
				fattyAcidString += cLTemp1.get(0)+"_";
				fattyAcidString += cLTemp1.get(1)+"_";
				fattyAcidString += cLTemp2.get(0)+"_";
				fattyAcidString += cLTemp2.get(1)+"_";

				//Load into faTemp
				faTemp = new ArrayList<FattyAcid>();
				faTemp.add(cLTemp1.get(0));
				faTemp.add(cLTemp1.get(1));
				faTemp.add(cLTemp2.get(0));
				faTemp.add(cLTemp2.get(1));

				//Check if string is unique in shadow array
				if (!shadowArray.contains(fattyAcidString))
				{
					//Add lipid
					lipidTemp = new Lipid (faTemp, lipidClass.lClass, lipidClass.adduct);
					lipidTemp.generateName();
					result.add(lipidTemp);
					shadowArray.add(fattyAcidString);
					shadowArray.add(cLTemp2.get(0)+"_"+
							cLTemp2.get(1)+"_"+
							cLTemp1.get(0)+"_"+
							cLTemp1.get(1)+"_");
				}
			}
			else
			{
				//Sort fatty acid array
				Collections.sort(faTemp);

				//Create string representing FAArray
				for (int i=0; i<faTemp.size(); i++)
				{
					fattyAcidString += faTemp.get(i)+"_";
				}

				//Check if string is unique in shadow array
				if (!shadowArray.contains(fattyAcidString))
				{
					//Add lipid
					lipidTemp = new Lipid (faTemp, lipidClass.lClass, lipidClass.adduct);
					lipidTemp.generateName();
					result.add(lipidTemp);
					shadowArray.add(fattyAcidString);
				}
			}

			// Advance permutation
			if (!nextPermutation(limits, counters)) break;
		}

		theoreticalLipids = result;

	}

	//Add electron mass to fragment based on charge and polarity
	public Double addElectrons(Double mass, Integer charge, String polarity)
	{
		Double result = 0.0;

		//If adding electrons
		if (polarity.equals("+"))
		{
			result = mass-charge*MASSOFELECTRON;
		}	
		//If removing electrons
		else
		{
			result = mass+charge*MASSOFELECTRON;
		}

		return result;
	}

	//Add transition to ms2 if mass is unique
	public void addIfUnique(MS2 ms2, Transition t)
	{
		boolean massMatchFound = false;

		if (t!=null)
		{
			//Iterate through looking for identical mass
			for (int i=0; i<ms2.getTransitions().size(); i++)
			{
				if (ms2.getTransitions().get(i).getMass().equals(t.mass))
				{
					massMatchFound = true;
				}
			}

			//If none found, add to ms2
			if (!massMatchFound)
			{
				ms2.addTransition(t);
			}
		}
	}

	//Returns ms2 object based on lipid
	public MS2 generateMS2(Lipid lipid, ArrayList<TransitionType> transitionTypes) throws CustomException
	{
		int faCounter = 0;
		ArrayList<FattyAcid> faArray;

		//Find theoretical precursor mass (mass/charge)
		Double precursor = addElectrons(lipid.getMass(),lipid.adduct.getCharge(),lipid.polarity);

		//Generate MS2
		MS2 result = new MS2 (precursor, lipid.getAdduct().getPolarity(), lipid.getAdduct().getCharge());

		//For all possible fatty acids
		for (int j=0; j<lipid.fattyAcids.size(); j++)
		{			
			//Add in all fragments to MS2 array
			for (int i=0; i<transitions.size(); i++)
			{
				faArray = new ArrayList<FattyAcid>();

				String type = transitions.get(i).type;

				//Generate cardiolipin dg transitions
				if (type.equals("Cardiolipin DG Fragment"))
				{
					if (faCounter == 0 || faCounter == 2)
					{
						faArray.add(lipid.fattyAcids.get(faCounter));
						faArray.add(lipid.fattyAcids.get(faCounter+1));
						addIfUnique(result, parseTransition(transitions.get(i),lipid,faArray));
					}
				}
				//For all other transitions
				else
				{
					if (transitions.get(i).typeObject.isFattyAcid)
					{
						faArray.add(lipid.fattyAcids.get(faCounter));

						//For PUFA transitions
						if (transitions.get(i).typeObject.name.contains("PUFA"))
						{
							if (faArray.get(0).pufa)
								addIfUnique(result, parseTransition(transitions.get(i),lipid,faArray));
						}
						else
						{
							if (transitions.get(i).typeObject.fattyAcidType.equals(faArray.get(0).type))
								addIfUnique(result, parseTransition(transitions.get(i),lipid,faArray));
						}
					}
					else
						addIfUnique(result, parseTransition(transitions.get(i),lipid,faArray));
				}
			}
			faCounter++;
		}

		return result;
	}

	//Returns transition object based on definition and lipid
	public Transition parseTransition(TransitionDefinition td, Lipid lipid, ArrayList<FattyAcid> faArray) throws CustomException
	{
		Double mass;
		if (td.isFormula)
			mass = td.typeObject.calculateMass(faArray, lipid, calculateMassFromFormula(td.formula), 
					lipid.adduct.charge, lipid.adduct.polarity);
		else
			mass = td.typeObject.calculateMass(faArray, lipid, td.mass, lipid.adduct.charge, 
					lipid.adduct.polarity);

		return new Transition(mass,td.relativeIntensity,td.massFormula+"_"+td.typeObject.name+"_"+faArray);
	}


	//Return string representation of ms2 template
	public String toString()
	{
		String result = "";

		//Print Name
		result += lipidClass.getName()+"\n";

		//Print Transition Masses and intensities
		for (int i=0; i<transitions.size(); i++)
		{
			result += transitions.get(i)+"\n";
		}

		return result;
	}

	@Override
	public int compareTo(MS2Template arg0)
	{
		if (arg0.lipidClass.name.compareTo(this.lipidClass.name)<0) return 1;
		else return -1;
	}
}
