package spectrum_searcher;

import java.util.ArrayList;
import java.util.Collections;
import lib_gen.FattyAcid;



public class SampleSpectrum extends lib_gen.Utilities
{
	Double precursor;										//Precursor of MS2
	Double retention;										//Retention time
	String polarity;										//Polarity of MS2
	String file;											//.mgf file
	ArrayList<Transition> transitionArray;					//Array of all transitions
	ArrayList<Identification> idArray;						//Array of all identifications
	ArrayList<Double> allMatchedMassesArray;				//Array of all identifications
	Double maxIntensity;									//Intensity of most intense fragment
	Double maxIntensityMass;								//Mass of most intense fragment
	Integer spectraNumber = 0;								//Spectra number
	PeakPurity peakPurity = null;					//Object for storing peak purity information

	//Constructor
	public SampleSpectrum(Double precursor, String polarity, String file, Double retention, Integer spectraNumber)
	{
		//Initialize variables
		this.precursor = Math.round(precursor * 1000.0) / 1000.0;
		this.polarity = polarity;
		this.file = file;
		this.transitionArray = new ArrayList<Transition>();
		this.idArray = new ArrayList<Identification>();
		allMatchedMassesArray = new ArrayList<Double>();
		maxIntensity = 0.0;
		maxIntensityMass = 0.0;	
		this.retention = Math.round(retention * 1000.0) / 1000.0;
		this.spectraNumber = spectraNumber;
	}

	//Method to add fragments to proper arrays
	public void addFrag(Double mass, Double intensity)
	{
		//Add mass and intensity to arrays
		transitionArray.add(new Transition(mass, intensity, null));

		//Update max intensity and mass
		if (intensity>maxIntensity)
		{
			maxIntensity = intensity;
			maxIntensityMass = mass;
		}
	}

	//A method to scale intensities to max. intensity on scale of 0-999
	public void scaleIntensities()
	{
		for (int i=0; i<transitionArray.size(); i++)
		{
			transitionArray.get(i).intensity = (transitionArray.get(i).intensity/maxIntensity)*999;

			if (transitionArray.get(i).intensity<5)
			{
				transitionArray.remove(i);
				i--;
			}
		}
	}

	//A method to check if a mass array contains a certain mass within mass tolerance
	public boolean checkFrag(Double mass, Double massTol)
	{
		for (int i=0; i<transitionArray.size(); i++)
		{
			if (Math.abs(transitionArray.get(i).mass - mass) < massTol) return true;
		}

		return false;
	}


	//Method to add id to array
	public void addID(LibrarySpectrum ls, Double dotProduct, Double reverseDotProduct, double mzTol)
	{
		Identification id = new Identification(ls, ((precursor-ls.precursor)/ls.precursor)*1000000, 
				dotProduct, reverseDotProduct);
		idArray.add(id);
	}

	public void calcPurityAll(ArrayList<FattyAcid> faDB, double mzTol)
	{
		Collections.sort(idArray);
		ArrayList<LibrarySpectrum> ls = new ArrayList<LibrarySpectrum>();
		if (idArray.size()>0)
		{
			if (!idArray.get(0).librarySpectrum.name.equals("")) 
			{
				//Add in all library spectra to temp array
				for (int i=1; i<idArray.size(); i++)
				{
					if (idArray.get(i).librarySpectrum.polarity.equals(idArray.get(0).librarySpectrum.polarity) && 
							idArray.get(i).librarySpectrum.precursor.equals(idArray.get(0).librarySpectrum.precursor))
						ls.add(idArray.get(i).librarySpectrum);
				}

				//Calculate purity
				peakPurity = idArray.get(0).calcPurityAll(faDB, ls, transitionArray, mzTol);
			}

		}
	}

	//Calculate dot product between to sample spectra
	public Double calcDotProduct(ArrayList<Transition> libArray, Double mzTol, boolean reverse, Double massWeight, Double intWeight) 
	{
		Double result = 0.0;
		Double numerSum = 0.0;
		Double libSum = 0.0;
		Double sampleSum = 0.0;
		Double massDiff = -1.0;
		ArrayList<Double> libMasses = new ArrayList<Double>();
		ArrayList<Double> sampleMasses = new ArrayList<Double>();
		ArrayList<Double> libIntensities = new ArrayList<Double>();
		ArrayList<Double> sampleIntensities = new ArrayList<Double>();	

		//Sort sample array
		Collections.sort(transitionArray);

		int i = 0, j = 0;

		while (i < libArray.size() && j < transitionArray.size())
		{
			massDiff = transitionArray.get(j).mass- libArray.get(i).mass;

			if (Math.abs(massDiff)>mzTol)
			{		
				if (libArray.get(i).mass < transitionArray.get(j).mass)
				{
					libIntensities.add(libArray.get(i).intensity);
					sampleIntensities.add(0.0);
					sampleMasses.add(0.0);
					libMasses.add(libArray.get(i++).mass);
				}
				else if (libArray.get(i).mass > transitionArray.get(j).mass)
				{
					sampleIntensities.add(transitionArray.get(j).intensity);
					libIntensities.add(0.0);
					sampleMasses.add(transitionArray.get(j++).mass);
					libMasses.add(0.0);
				}
			}
			else
			{
				sampleMasses.add(transitionArray.get(j).mass);
				libMasses.add(libArray.get(i).mass);
				libIntensities.add(libArray.get(i++).intensity);
				sampleIntensities.add(transitionArray.get(j++).intensity);
			}
		}

		/* Print remaining elements of the larger array */
		while(i < libArray.size())
		{
			libIntensities.add(libArray.get(i).intensity);
			sampleIntensities.add(0.0);
			sampleMasses.add(0.0);
			libMasses.add(libArray.get(i++).mass);
		}
		while(j < transitionArray.size())
		{
			sampleIntensities.add(transitionArray.get(j).intensity);
			libIntensities.add(0.0);
			sampleMasses.add(transitionArray.get(j++).mass);
			libMasses.add(0.0);
		}

		//Iterate through unique masses
		for (int k=0; k<libMasses.size(); k++)
		{
			//If only in sample
			if (libIntensities.get(k) == 0.0)
			{
				if (!reverse) sampleSum += Math.pow(massWeight*sampleMasses.get(k)*Math.pow(sampleIntensities.get(k)/2.0,intWeight), 2);
			}
			//If only in lib
			else if (sampleIntensities.get(k) == 0.0)
			{
				if (!reverse) libSum += Math.pow(massWeight*libMasses.get(k)*Math.pow(libIntensities.get(k),intWeight), 2);
			}
			//If in both
			else
			{
				libSum += Math.pow(Math.pow(libMasses.get(k), massWeight)*Math.pow(libIntensities.get(k),intWeight), 2);
				sampleSum += Math.pow(Math.pow(sampleMasses.get(k), massWeight)*Math.pow(sampleIntensities.get(k),intWeight), 2);
				numerSum += Math.pow(sampleMasses.get(k), massWeight)*Math.pow(sampleIntensities.get(k),intWeight)*Math.pow(libMasses.get(k), massWeight)*Math.pow(libIntensities.get(k),intWeight);
			}
		}

		//Calculate dot product
		if (numerSum > 0.0) result = 1000.0 * Math.pow(numerSum, 2)/(sampleSum*libSum);
		
		return result;
	}

	//Calculates the mass difference in ppm
	public  double calcPPMDiff(double massA, double massB)
	{
		double ppmResult = 0.0;

		ppmResult = ((massA - massB)/massB)*1000000;

		return ppmResult;
	}

	//Returns string representation of array
	public String toString(int maxResults)
	{
		String result = "";

		//Sort results by dot product
		Collections.sort(idArray);

		//Add in transitions up to max number of results
		for (int i=0; i<idArray.size() && i<maxResults; i++)
		{
			result += spectraNumber+","
					+retention+","+(i+1)+","
					+idArray.get(i).librarySpectrum.name+","
					+precursor+","
					+idArray.get(i).librarySpectrum.precursor+","
					+idArray.get(i).deltaMass+","
					+Math.round(idArray.get(i).dotProduct)+","
					+Math.round(idArray.get(i).reverseDotProduct)+","
					+idArray.get(i).purity+",";

			if (peakPurity != null)
			{
				for (int j=0; j<peakPurity.lipids.size(); j++)
				{
					result+= peakPurity.lipids.get(j).name.replace(";", "")+"("+peakPurity.purities.get(j)+")";
					if (j!=peakPurity.lipids.size()-1) result += " / ";
				}

				result += ",";
			}
			else result+= ",";

			result += idArray.get(i).librarySpectrum.optimalPolarity+","
					+idArray.get(i).librarySpectrum.isLipidex+","
					+idArray.get(i).librarySpectrum.libary+",";

			//If purity array has been made add in all matched masses
			if (idArray.get(i).purity>1 && peakPurity != null)
			{
				for (int j=0; j<peakPurity.matchedMasses.size(); j++)
				{
					if (!result.contains(String.valueOf(peakPurity.matchedMasses.get(j))))
						result+=peakPurity.matchedMasses.get(j)+" | ";
				}
			}

			//Else, add in best ID masses
			else if (idArray.get(i).purity<1)
			{
				for (int j=0; j<idArray.get(i).librarySpectrum.transitionArray.size(); j++)
				{
					result+=idArray.get(i).librarySpectrum.transitionArray.get(j).mass+" | ";
				}
			}

			if (idArray.size() > 1 && (i < (idArray.size()-1) && i < (maxResults-1))) result += "\n";
		}
		return result;
	}
}
