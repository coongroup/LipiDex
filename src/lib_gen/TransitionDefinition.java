package lib_gen;

//Represents a peak in the MS2 spectra
public class TransitionDefinition extends Utilities implements Comparable<Transition>
{
	Double mass;					//Mass of fragment if applicable
	Double relativeIntensity;		//Relative intensity scaled to 1000
	String formula;					//Elemental formula
	public String displayName;				//Display name for Jtree display
	boolean isFormula;				//Boolean if a formula was supplied
	Integer charge;					//Charge on transition
	String massFormula;				//String to store supplied mass/formula string
	String type;					//String to store supplied type
	TransitionType typeObject;		//Transition type object					

	public TransitionDefinition(String massFormula, Double relIntensity, String displayName, String type, Integer charge, TransitionType typeObject)
	{
		//Initialize blank variables
		mass = -1.0;
		formula = "";
		this.type = type;
		this.charge = 1;
		this.typeObject = typeObject;

		//Initialize paramaterized variables
		relativeIntensity = relIntensity;
		this.displayName = parseDisplayName(displayName);
		this.charge = charge;

		//Parse massFormula field
		parseMassFormula(massFormula);

		//Update mass and formula field if applicable
		updateMassFormula();
	}

	public String getType()
	{
		return type;
	}
	
	public TransitionType getTypeObject()
	{
		return typeObject;
	}
	
	public void updateMassFormula()
	{
		//If a formula
		if (isFormula)
		{
			this.mass = calculateMassFromFormula(massFormula);
			this.formula = massFormula;

		}
		//If not a formula
		else
		{
			this.mass = Double.valueOf(massFormula);
		}
	}

	//Check mass formula for correct declaration
	@SuppressWarnings("unused")
	public void parseMassFormula(String massFormula)
	{
		boolean formula = false;

		for (int i=0; i<elements.length; i++)
		{
			if (massFormula.contains(elements[i])) formula = true;
		}

		if (massFormula.equals("-")) formula = true;
		
		//Check mass validity
		if (!formula)
		{
			try
			{
				this.isFormula = false;
				Double massDouble =  Double.parseDouble(massFormula);
				this.massFormula = massFormula;
			}
			catch (Exception e)
			{
				CustomError e1 = new CustomError(massFormula+" is not a valid mass", null);
			}
		}
		//Check formula validity
		else
		{
			try
			{
				this.isFormula = true;
				validElementalFormula(massFormula);
				this.massFormula = massFormula;
				this.formula = massFormula;
			}
			catch(Exception e)
			{
				CustomError e1 = new CustomError(massFormula+" is not a valid elemental formula", null);
			}
		}

	}

	public void updateValues(Double relInt, String massFormula, String type, String charge) throws CustomException
	{
		try
		{
		//Update charge
		this.charge = Integer.valueOf(charge);
		
		//Parse massFormula field
		parseMassFormula(massFormula);

		//Update relative intensity
		this.relativeIntensity = relInt;
		
		//Update type
		this.type = type;

		//Reparse display name
		this.displayName = parseDisplayName(massFormula+","+relInt+","+charge+","+type);

		//Update mass and formula field if applicable
		updateMassFormula();
		}
		catch (Exception e)
		{
			CustomError ce = new CustomError("Error updating entry.  Please check formatting", null);
		}
	}

	//Format transition for display in tree
	public String parseDisplayName(String name)
	{
		String result = "";
		String[] split;

		if (name.contains(","))
		{
			split = name.split(",");

			if (split[0].contains(".")) split[0] = String.valueOf(Math.round (Double.valueOf(split[0]) * 10000.0) / 10000.0); 
			result += String.format("%1$-" + 20 + "s", split[0]);
			result += String.format("%1$-" + 5 + "s", Math.round(relativeIntensity));
			result += String.format("%1$-" + 3 + "s", charge);
			result += split[3];
		}
		else
		{
			split = name.split(" +");

			if (split[0].contains(".")) split[0] = String.valueOf(Math.round (Double.valueOf(split[0]) * 10000.0) / 10000.0); 
			result += String.format("%1$-" + 20 + "s", split[0]);
			result += String.format("%1$-" + 5 + "s", Math.round(relativeIntensity));
			result += String.format("%1$-" + 3 + "s", charge);
			result += split[3];
		}
		return result;
	}

	//Adds elemental formula to transition
	public void addFormula(String formula)
	{
		this.formula = formula;
	}

	//Returns elemental formula
	public String getFormula()
	{
		return formula;
	}

	//Returns mass as double
	public Double getMass()
	{
		return mass;
	}

	//Returns relative intensity
	public Double getRelativeIntensity()
	{
		return relativeIntensity;
	}

	//Calculate elemental formula for transitions based on precursor formula
	public void calculateElementalComposition(String precursorFormula)
	{
		addFormula(annotateMassWithMZTolerance(mass, precursorFormula));

		if (!formula.equals(""))
			mass = calculateMassFromFormula(formula);
	}

	//Returns string representation of transition
	public String toString()
	{
		String result = "";
		result = massFormula+","+relativeIntensity+","+charge+","+type;
		return result;
	}

	//Comparator for sorting by intensity
	public int compareTo(Transition t2)
	{	
		if (t2.getIntensity()>relativeIntensity) return 1;
		else if (t2.getIntensity()<relativeIntensity) return -1;	
		return 0;
	}
}
