package peak_finder;
import java.util.ArrayList;


public class GaussianModel 
{
	Double fwhm;			//Peak FWHM in minutes
	Double area;			//Peak area
	Double height;			//Peak height
	ArrayList<Double> x;	//ArrayList of x coordinates (time)
	ArrayList<Double> y;	//ArrayList of y coordinated (signal, normalize to 1)
	Double trapezoidSum;	//Sum of areas using trapezoid methof
	public Double apexRT;			//Apex Retention

	//Constructor for compound discoverer gaussian fitting
	public GaussianModel(Double fwhm, Double area, Double height, Double apexRT)
	{
		//Initialize variables
		this.fwhm = fwhm;
		this.area = area;
		this.apexRT = apexRT;
		this.height = height;
		x = new ArrayList<Double>();
		y= new ArrayList<Double>();
		trapezoidSum = 0.0;

		//Calculate gaussian profile for compound discoverer
		if (height == null)
		{
			//Create time array
			populateArraysNoHeight();

			//Integrate gaussian function
			integrateFunction();

			//Calculate height from area and integral
			calculateHeight();
		}
		
		//Calculate gaussian profile for mzmine2
		if (area == null)
		{
			//Create time array
			populateArraysNoArea();

			//Integrate gaussian function
			integrateFunction();
		}
	}

	//Populate time and signal arrays when solving for area
	private void populateArraysNoArea()
	{
		double time = 0-fwhm*3.0;

		while (time<(fwhm*3.0))
		{
			x.add(time);
			y.add(Math.pow(2.71828,-((Math.pow(time,2.0)/(2.0*(Math.pow((fwhm/2.3548),2.0)))))));
			time = time+(fwhm/12.0);
		}
	}
	
	//Populate time and signal arrays when solving for height
	private void populateArraysNoHeight()
	{
		double time = 0-fwhm*3.0;

		while (time<(fwhm*3.0))
		{
			x.add(time);
			y.add(Math.pow(2.71828,-((Math.pow(time,2.0)/(2.0*(Math.pow((fwhm/2.3548),2.0)))))));
			time = time+(fwhm/12.0);
		}
	}

	//Integrate gaussian function using trapezoid method
	private void integrateFunction()
	{
		for (int i=0; i<x.size()-1; i++)
		{
			trapezoidSum += ((y.get(i)+y.get(i+1))/2.0)*(x.get(i+1)-x.get(i)); 
		}
	}

	//Calculate peak hight
	private void calculateHeight()
	{
		height = area/trapezoidSum;
	}

	//Returns the normalized peak height at any timepoint along the gaussian profile
	public Double getNormalizedHeight(Double time)
	{
		//Convert retention time to normalized time
		Double normalizedTime = time-this.apexRT;

		//Find closest 
		for (int i=0; i<x.size()-1; i++)
		{
			if (x.get(i)<normalizedTime && x.get(i+1)>normalizedTime)
			{
				if (Math.abs(normalizedTime-x.get(i)) > Math.abs(normalizedTime-x.get(i+1))) return (y.get(i+1));
				else return (y.get(i));
			}
		}

		return 0.001;
	}

	//Returns the actual peak height at any timepoint along gaussian profile
	public Double getCalculatedHeight(Double time)
	{
		//Convert retention time to normalized time
		Double normalizedTime = time-this.apexRT;
		
		//Find closest 
		for (int i=0; i<x.size()-1; i++)
		{
			if (x.get(i)<normalizedTime && x.get(i+1)>normalizedTime)
			{
				if (Math.abs(normalizedTime-x.get(i)) > Math.abs(normalizedTime-x.get(i+1))) return (height*y.get(i+1));
				else return (height*y.get(i));
			}
		}

		return 0.001;
	}
}


