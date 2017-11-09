package lib_gen;

public class Transition extends Utilities implements Comparable<Transition>
{
	Double mass;		//Mass of fragment
	Double intensity;	//Relative intensity of fragment, scaled to 999
	String type;		//Type of transition

	//Constructor
	public Transition(Double mass, Double intensity, String type)
	{
		this.mass = mass;
		this.intensity = intensity;
		this.type = type;
	}

	//Returns mass
	public Double getMass()
	{
		return mass;
	}
	
	//Returns type
	public String getType()
	{
		return type;
	}

	//Returns intensity
	public Double getIntensity()
	{
		return intensity;
	}
	
	//Return string representation of transition
	public String toString()
	{
		String result = "";
		result = mass+" "+intensity+" \""+type+"\"";
		return result;
	}

	//Comparator for sorting by intensity
	public int compareTo(Transition t2)
	{	
		if (t2.getIntensity()>intensity) return 1;
		else if (t2.getIntensity()<intensity) return -1;	
		return 0;
	}
}
