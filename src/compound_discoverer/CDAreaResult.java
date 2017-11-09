package compound_discoverer;

import peak_finder.Sample;

public class CDAreaResult 
{
	Sample file;			//Associated Sample object
	Double area;			//Area of peak
	String mergedFileName;	//Merged file name for separate polarity runs	
	boolean merged;			//True iff the result has been merged
	String polarity;		//Polarity if not polarity switching
	
	//Constructor
	public CDAreaResult(Sample file, Double area)
	{
		this.file = file;
		this.area = area;
		mergedFileName = "";
		merged = false;
	}
	
	//Add merged file name if merged with opposite polarity
	public void addMergedFileName(String name)
	{
		this.mergedFileName = name;
	}
	
	//Set polarity
	public void setPolarity(String polarity)
	{
		this.polarity = polarity;
	}
}
