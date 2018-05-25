package spectrum_searcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lib_gen.TransitionType;


public class LibrarySpectrum implements Comparable<LibrarySpectrum>
{
	Double precursor;						//Mass of lipid
	String polarity;						//Polarity
	String name;							//Spectrum name
	String lipidClass;						//String of lipid class
	String adduct;							//String of adduct
	String sumID;							//String of sum ID
	ArrayList<Transition> transitionArray;	//Array of all ms2 transitions
	Double maxIntensity;					//Maximum intensity 
	Double maxIntensityMass;				//Mass of most intensity fragment
	String libary;							//Library
	boolean isLipidex;						//True iff ID came from LipiDex
	boolean optimalPolarity;				//True iff the ID is from the optimal polarity

	//Constructor
	public LibrarySpectrum(Double precursor, String polarity,
			String name, String library, boolean isLipidex,
			boolean optimalPolarity)
	{
		//Initialize variables
		this.precursor = precursor;
		this.polarity = polarity;
		this.name = name.substring(1);
		transitionArray = new ArrayList<Transition>();
		maxIntensity = 0.0;
		maxIntensityMass = 0.0;	
		this.libary = library;
		this.isLipidex = isLipidex;
		if (isLipidex) this.sumID = getSumID(this.name);
		this.optimalPolarity = optimalPolarity;

		//Parse lipid name
		parseName();
	}

	//Return String representation of spectrum
	public String toString()
	{
		String result = precursor+","+polarity;
		return result;
	}

	//Parses lipid name and returns Sum ID
	private String getSumID(String id)
	{
		String result = "";
		int numDB = 0;
		int numC = 0;
		String lipidClass;
		String faString;
		String toAddToFA = "";
		String[] faSplit;
		int j;

		lipidClass = id.substring(0, id.indexOf(" "));

		//Remove class and adduct
		faString = id.substring(id.indexOf(" ")+1, id.lastIndexOf(" "));
		//TODO:
		/*
		//Parse plasmenyl
		if (faString.contains("P-"))
		{
			faString = faString.replace("P-", "");
			toAddToFA = "P-";
		}
		//Parse ether
		else if (faString.contains("O-"))
		{
			faString = faString.replace("O-", "");
			toAddToFA = "O-";
		}
		//Parse sphingoid
		else if (faString.contains("d"))
		{
			faString = faString.replace("d", "");
			toAddToFA = "d";
		}
		//Parse methyl fatty acids
		else if (faString.contains("m"))
		{
			faString = faString.replace("m", "");
			toAddToFA = "m";
		}
		 */


		//Split string based on fatty acids
		faSplit = faString.split("_");



		for (int i=0; i<faSplit.length; i++)
		{
			//if lipid string does not contain "-"
			if (faSplit[i].contains("-"))
			{
				j = faSplit[i].lastIndexOf("-")+1;

				//Find integer for first number in string
				Pattern pattern = Pattern.compile("^\\D*(\\d)");
				Matcher matcher = pattern.matcher(faSplit[i].substring(j));
				matcher.find();
				j = j + matcher.start(1);

			}
			else
			{
				//Find integer for first number in string
				Pattern pattern = Pattern.compile("^\\D*(\\d)");
				Matcher matcher = pattern.matcher(faSplit[i]);
				matcher.find();
				j = matcher.start(1);
			}


			//Remove modifier and add in later
			if (j>0)
			{
				toAddToFA += faSplit[i].substring(0, j);
				faSplit[i] = faSplit[i].replace(faSplit[i].substring(0, j), "");
			}
			
			numC += Double.valueOf(faSplit[i].substring(0, faSplit[i].indexOf(":")));
			numDB += Double.valueOf(faSplit[i].substring(faSplit[i].indexOf(":")+1));
		}

		result = lipidClass+" "+toAddToFA+numC+":"+numDB;

		return result;
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

	//Parse lipid name
	private void parseName()
	{
		String[] split;

		//Split name
		split = name.split(" ");
		this.lipidClass = split[0];

		//Capitalize lipid Class
		this.lipidClass = this.lipidClass.substring(0, 1).toUpperCase() + this.lipidClass.substring(1);

		this.adduct = split[2];
		this.adduct = this.adduct.replace(";","");
	}

	//Add lipid name
	public void addName(String name)
	{
		this.name = name;
	}

	//Compares library spectra based on precursor mass
	public int compareTo(LibrarySpectrum target)
	{
		if (target.precursor>this.precursor) return 1;
		else if (target.precursor<this.precursor) return -1;
		else return 0;
	}

	//Calculates mass difference in ppm
	public  Double calcPPMDiff(Double mass1, Double mass2)
	{
		Double result = 0.0;

		result = (Math.abs(mass1 -  mass2)/(mass2))*1000000;

		return result;
	}

	//Add fragment to transition array
	public void addFrag(Double mass, Double intensity, String type, TransitionType transitionType)
	{
		Transition t = new Transition(mass, intensity, transitionType);
		if (!type.equals("")) t.addType(type);
		transitionArray.add(t);
		Collections.sort(transitionArray);

		if (intensity>maxIntensity)
		{
			maxIntensity = intensity;
			maxIntensityMass = mass;
		}
	}

	//Returns true if a fragment is present in array
	public boolean checkFrag(Double mass)
	{
		for (int i=0; i<transitionArray.size(); i++)
		{
			if (calcPPMDiff(mass,transitionArray.get(i).mass)<15.0) return true;
		}

		return false;
	}
}
