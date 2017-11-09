package peak_finder;
import java.util.ArrayList;

import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;

public class Sample 
{
	public String file;				//String of filepath
	public String filename;			//String of filename
	public Double peakCapacity;		//Avg. peak capacity
	public String cdFileID;			//File ID from Compound Discoverer
	public boolean ms2Counted;		//True iff ms2 associated
	public int polarityFileNumber;	//File number for joining separate polarity files
	public String mergedName;		//Merged file name
	public String polarity;			//Polarity of file if separate polarities collected
	
	//Iterable Variables
	public int lipids;				//Number of associated lipids
	public int compoundsFound;		//Number of compounds found by CD or MzMine
	public Double maxFeatureRT;		//Maximum retention time

	//Avg. Variables
	public Double ppmError;			//Average ID mass error in ppm
	public Double ms2RTDev;			//Average ms2 retention tim deviation
	public Double avgPurity;		//Average spectral purity
	public Double avgFWHM;			//Average peak FWHM

	//Avg. Variable Arrays
	public ArrayList<Double> ppmErrorArray;			//ArrayList of ppm errors for averaging
	public ArrayList<Double> ms2RTDevArray;			//ArrayList of rt devs for averaging
	public ArrayList<Integer> avgPurityArray;		//ArrayList of purities for averaging
	public ArrayList<Double> avgFWHMArray;			//ArrayList of peak widhts for averaging
	public ArrayList<RTDeviation> featureRTDevArray;//ArrayList of feature retention time devs

	//Constructor
	public Sample(String file, int polarityFileNumber, boolean separatePolarities)
	{
		this.file = file;
		filename = "";
		maxFeatureRT = 0.0;
		ms2Counted = false;
		if (file.contains("(")) cdFileID = file.substring(file.lastIndexOf("(")+1, file.lastIndexOf(")"));
		if (separatePolarities) this.polarityFileNumber = polarityFileNumber;
		else this.polarityFileNumber = -1;
		mergedName = "";
		
		//Initialize iterable variables
		lipids = 0;
		compoundsFound = 0;

		//Initialize arrays
		ppmErrorArray = new ArrayList<Double>();
		ms2RTDevArray = new ArrayList<Double>();
		avgPurityArray = new ArrayList<Integer>();
		avgFWHMArray = new ArrayList<Double>();
		featureRTDevArray = new ArrayList<RTDeviation>();
	}

	//Create loess fit of retention time and rt dev to recapitulate rt correctopm
	public void loessFit()
	{
		double x[] = new double[featureRTDevArray.size()];
		double y[] = new double[featureRTDevArray.size()];

		//Ensure monotonicity
		for (int i=0; i<featureRTDevArray.size()-1; i++)
		{
			if (featureRTDevArray.get(i).retention >= featureRTDevArray.get(i+1).retention)
				featureRTDevArray.get(i+1).retention = featureRTDevArray.get(i).retention+0.00001;
		}

		//Populate arrays
		for (int i=0; i<featureRTDevArray.size(); i++)
		{
			x[i] = featureRTDevArray.get(i).retention;
			y[i] = featureRTDevArray.get(i).deviation;
		}
		LoessInterpolator loessInterpolator=new LoessInterpolator(0.1, 1);
		try
		{
			double[] y2 = loessInterpolator.smooth(x, y);
			for (int i=0; i<y2.length; i++)
			{
				featureRTDevArray.get(i).avgDeviation = y2[i];
			}
		}
		catch (Exception e)
		{
			
		}
	}

	//For any retention, find calculate allignment offset
	public double findRTOffset(Double rt)
	{
		//Find closest rt and return corresonding deviation
		for (int i=0; i<featureRTDevArray.size(); i++)
		{
			if ((rt-featureRTDevArray.get(i).retention)<0)
			{
				return featureRTDevArray.get(i).avgDeviation;
			}
		}
		return 0.0;
	}

	//Correct a retention time using offset
	public double getCorrectedRT(Double rt)
	{
		for (int i=0; i<featureRTDevArray.size(); i++)
		{
			if ((rt-featureRTDevArray.get(i).retention)<0)
			{
				return rt-featureRTDevArray.get(i).avgDeviation;
			}
		}
		return 0.0;
	}

	//Sets maximum retention time
	public void checkMaxRT(Double rt)
	{
		if (rt>maxFeatureRT)
		{
			maxFeatureRT = rt;
		}
	}

	//Calculate average statistics
	public void calculateAverages()
	{
		//Calculate Averages
		ppmError = calcAvgFromDoubleArray(ppmErrorArray);
		ms2RTDev = calcAvgFromDoubleArray(ms2RTDevArray);
		avgPurity = calcAvgFromIntArray(avgPurityArray);
		avgFWHM = calcAvgFromDoubleArray(avgFWHMArray);

		//Calculate Peak Capacities
		peakCapacity =  Math.floor((maxFeatureRT/avgFWHM)* 1000) / 1000;
	}

	//For any integer arraylist, return the average
	public Double calcAvgFromIntArray(ArrayList<Integer> array)
	{
		Double result = 0.0;
		Double sum = 0.0;

		for (int i=0; i<array.size(); i++)
		{
			sum = sum + array.get(i);
		}

		result = sum/array.size();
		result = Math.floor((result)* 1000) / 1000;
		return result;
	}

	//For any double arraylist, return the average
	public Double calcAvgFromDoubleArray(ArrayList<Double> array)
	{
		Double result = 0.0;
		Double sum = 0.0;

		for (int i=0; i<array.size(); i++)
		{
			sum = sum + array.get(i);
		}

		result = sum/array.size();
		result = Math.floor((result)* 1000) / 1000;
		return result;
	}

	//Increment lipid count
	public void addLipid()
	{
		lipids++;
	}

	//Increment compound count
	public void addCompound()
	{
		compoundsFound++;
	}

	//Add ppm error to array
	public void addPPMError(Double error)
	{
		ppmErrorArray.add(error);
	}

	//Add rt dev to array
	public void addRTDev (Double dev)
	{
		ms2RTDevArray.add(dev);
	}

	//Add purity to array
	public void addPurity(Integer purity)
	{
		avgPurityArray.add(purity);
	}

	//Add FWHM to array
	public void addFWHM(Double FWHM)
	{
		avgFWHMArray.add(FWHM);
	}

	//Return string representation of array
	public String toString()
	{
		String result = "";

		result += file;
		result += ",";
		result += compoundsFound;
		result += ",";
		result += avgFWHM;
		result += ",";
		result += peakCapacity;
		result += ",";
		result += lipids;
		result += ",";
		result += avgPurity;
		result += ",";
		result += ppmError;

		return result;
	}
}
