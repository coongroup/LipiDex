package mzmine;

import peak_finder.Sample;

public class MzAreaResult 
{
	Sample file;			//Associated Sample object
	Double area;			//Area of peak
	Double mass;			//Corresponding mass
	String mergedFileName;	//Merged file name for separate polarity runs
	boolean merged;			//True iff the result has been merged
	
	//Constructor
	public MzAreaResult(Sample file, Double area, Double mass)
	{
		this.file = file;
		this.area = area;
		mergedFileName = "";
		merged = false;
		this.mass = mass;
	}
	
	//Add merged file name if merged with opposite polarity
	public void addMergedFileName(String name)
	{
		this.mergedFileName = name;
	}
}
