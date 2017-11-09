package spectrum_searcher;

import java.util.ArrayList;

import peak_finder.Utilities;
import lib_gen.FattyAcid;

public class Identification extends lib_gen.Utilities implements Comparable<Identification>
{
	LibrarySpectrum librarySpectrum;	//LibrarySpectrumObject from array
	Double deltaMass;					//Mass error between library and sample spectra in ppm
	Double dotProduct;					//Dot product between library and sample spectra
	Double reverseDotProduct;			//Reverse dot product between library and sample spectra
	int purity;							//Spectral purity

	//Constructor
	public Identification(LibrarySpectrum ls, Double deltaMass, 
			Double dotProduct, Double reverseDotProduct)
	{
		this.librarySpectrum = ls;
		this.deltaMass = Math.round(deltaMass * 10000.0) / 10000.0;
		this.dotProduct = dotProduct;
		this.reverseDotProduct = reverseDotProduct;
		purity = 0;
	}
	
	//calcPurity for between different classes
	public PeakPurity calcPurityAll(ArrayList<FattyAcid> faDB, ArrayList<LibrarySpectrum> lipidDB, ArrayList<Transition> ms2)
	{
		//TODO:
		ArrayList<LibrarySpectrum> isobaricIDs = new ArrayList<LibrarySpectrum>();
		PeakPurity calculator = new PeakPurity();
		
		//Find isobaric ids
		for (int i=0; i<lipidDB.size(); i++)
		{
			//If isobaric, add to array
			if (calcPPMDiff(librarySpectrum.precursor, lipidDB.get(i).precursor)<Utilities.MAXPPMDIFF)
				isobaricIDs.add(lipidDB.get(i));
		}
		
		//Verify the spectrum is a lipidex ID
		if (librarySpectrum.isLipidex)
		{
			if (librarySpectrum.optimalPolarity) 
				this.purity = calculator.calcPurity(librarySpectrum.name, librarySpectrum.sumID, ms2, librarySpectrum.precursor, 
						librarySpectrum.polarity, librarySpectrum.lipidClass, librarySpectrum.adduct, faDB, librarySpectrum, isobaricIDs);
			else this.purity = 0;
		}
		
		return calculator;
	}

	//Compares identifications based on dot product
	public int compareTo(Identification i)
	{
		if (dotProduct>i.dotProduct) return -1;
		else if (dotProduct<i.dotProduct) return 1;
		else return 0;
	}

	//Returns strin representation of ID
	public String toString()
	{
		String result = librarySpectrum.name+" "+dotProduct;
		return result;
	}

}
