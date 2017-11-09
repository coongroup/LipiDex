package compound_discoverer;
import java.util.ArrayList;

import peak_finder.Sample;


public class CDCompound 
{
	Double mw;									//Predicted molecular weight of compound
	Double retention;							//Aligned retention time of compound
	Double fwhm;								//Compound FWHM in minutes
	int maxMI;									//Maximum isotopes detected
	int numAdducts;								//Number of adducts collapsed into compound
	Double area;								//Peak area of compound
	Sample sample;								//Associated sample object
	boolean bestCompound;						//True iff compound is best representative for compound group
	ArrayList<CDFeature> features;				//ArrayList of all associated features
	ArrayList<CDLipidCandidate> candidates;		//ArrayList of putative identifications
	boolean positiveFeature;					//True iff a positive polarity feature has been added
	boolean negativeFeature;					//True iff a negative polarity feature has been added

	//Constructor
	public CDCompound (Double mw, Double retention, Double fwhm, 
			int maxMI, int numAdducts, Double area, Sample sample, 
			boolean bestCompound)
	{
		this.mw = mw;
		this.retention = retention;
		this.fwhm = fwhm;
		this.maxMI = maxMI;
		this.numAdducts = numAdducts;
		this.area = area;
		this.sample = sample;
		this.bestCompound = bestCompound;
		features = new ArrayList<CDFeature>();
		candidates = new ArrayList<CDLipidCandidate>();
		positiveFeature = false;
		negativeFeature = false;
	}
	
	//Associates a feature and updates boolen values
	public void addFeature(CDFeature feature, boolean separatePolarities)
	{
		//Calculate rt deviation
		features.add(feature);
		
		//Update polarity boolean
		if (feature.polarity.equals("-")) negativeFeature = true;
		else if (feature.polarity.equals("+")) positiveFeature = true;
		
		//Update polarity for sample if non-polarity switching
		if (separatePolarities)
		{
			if (negativeFeature) feature.sample.polarity = "-";
			else feature.sample.polarity = "+";
		}
	}
	
	//Returns string representation of compound
	public String toString()
	{
		String result = mw+","+retention+",";
		
		if (candidates.size()>0)
		{
			result = result+candidates.get(0).lipidName+" "+candidates.get(0).count;
		}
		
		return result;
	}
}
