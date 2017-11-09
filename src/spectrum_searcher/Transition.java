package spectrum_searcher;

import lib_gen.TransitionType;

public class Transition implements Comparable<Transition>
{
	public Double mass;					//Mass of fragment
	public Double intensity;			//Intensity of fragment
	public String type;					//Type of fragment
	public String formula;				//Elemental formula of fragment
	public TransitionType typeObject;	//Transition type object
	public String fattyAcid = "";		//String for associate fatty acid
	
	//Constructor
	public Transition(Double mass, Double intensity, TransitionType typeObject)
	{
		this.mass = mass;
		this.intensity = intensity;
		this.typeObject = typeObject;
	}
	
	//Add fragment type
	public void addType(String type)
	{
		this.type = type.replace("\"", "").substring(0,  type.replace("\"", "").lastIndexOf("_"));
		this.fattyAcid = type.substring(type.lastIndexOf("[")+1, type.lastIndexOf("]"));
		this.formula = type.substring(0,type.indexOf("_"));	
	}
	
	//Return intensity
	public Double getIntensity()
	{
		return intensity;
	}
	
	//Set intensity
	public void setIntensity(Double i)
	{
		this.intensity = i;
	}
	
	//Compares transitions by mass
	public int compareTo(Transition t)
	{
		if (mass>t.mass) return 1;
		else if (mass<t.mass) return -1;
		else return 0;
	}
	
	public String toString()
	{
		return mass+" "+intensity;
	}
}
