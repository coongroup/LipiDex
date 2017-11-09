package peak_finder;

public class RTDeviation implements Comparable<RTDeviation> 
{
	public Double deviation;		//Deviation (min)
	public Double retention;		//Retention time (min)
	public Double avgDeviation;		//Rolling average deviation (min)
	public Double intensity;		//Signal intensity
	
	//Constructor
	public RTDeviation (Double deviation, Double retention, Double  intensity)
	{
		this.deviation = deviation;
		this.retention = retention;
		avgDeviation = 0.0;
		this.intensity = intensity;
	}
	
	//Compares deviations based on retention
	public int compareTo(RTDeviation rt)
	{
		if (rt.retention>retention) return -1;
		else return 1;
	}
	
	//Returns string representation of deviation
	public String toString()
	{
		String result = this.retention+","+this.deviation+","+this.avgDeviation+"\n";
		return result;
	}
}
