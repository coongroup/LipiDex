package spectrum_searcher;

public class DotProductTransition 
{
	Double mass;			//Mass of common fragment
	Double intensity1;		//Intensity of first fragment
	Double intensity2;		//Intensity of second fragment
	
	//Constructor
	public DotProductTransition(Double mass, Double intensity)
	{
		this.mass = mass;
		this.intensity1 = intensity;
		intensity2 = 0.0;
	}
	
	//Return first fragment intensity
	public Double getIntensity1()
	{
		return intensity1;
	}
	
	//Return second fragment intensity
	public Double getIntensity2()
	{
		return intensity2;
	}
	
	//Add intensity for second frag
	public void addIntensity2(Double intensity)
	{
		intensity2 = intensity;
	}
	
	//Return mass
	public Double getMass()
	{
		return mass;
	}
	
	//Calculate the dot product
	public Double calcDPProduct()
	{
		Double result = (intensity1*mass)*(intensity2*mass);
		
		return result;
	}
	
}
