package compound_discoverer;
import java.util.ArrayList;

import peak_finder.Sample;
import peak_finder.GaussianModel;
import peak_finder.Lipid;
import peak_finder.Utilities;


public class CDFeature extends Utilities
{
	String adduct;								//String representation of adduct
	int charge;									//Charge of feature
	Double mw;									//Calculated molecular weight of feature
	Double mass;								//Monoisotopic mass of feature
	Double retention;							//Aligned retention time in mins
	Double fwhm;								//Feature FWHM in mins
	int mi;										//Number of isotopes detected
	Double area;								//Peak area
	Double parentAreaPercent;					//Percent of total compound area
	Sample sample;								//Associated sample object
	ArrayList<CDFeature> isobaricNeighbors;		//Arraylist of all isobaric features nearby
	ArrayList <Lipid> lipidCandidates;			//ArrayList of all putative IDs assigned to feature
	ArrayList<CDLipidCandidate> uniqueLipids;	//All unique lipids associated with feature
	String polarity;							//Polarity of feature
	Double realRetention;						//Real retention time before alignment
	GaussianModel peakModel;					//GaussianModel object

	//Constructor
	public CDFeature(String adduct, int charge, Double mw, Double mass, 
			Double retention, Double fwhm, int mi, Double area, 
			Double parentAreaPercent, Sample sample)
	{
		this.adduct = adduct;
		this.charge = charge;
		this.mw = mw;
		this.mass = mass;
		this.retention = retention;
		this.fwhm = fwhm;
		this.mi = mi;
		this.area = area;
		this.parentAreaPercent = parentAreaPercent;
		this.sample = sample;
		this.realRetention = 0.0;
		peakModel = new GaussianModel(fwhm, area, null, retention);

		lipidCandidates = new ArrayList<Lipid>();
		uniqueLipids = new ArrayList<CDLipidCandidate>();
		isobaricNeighbors = new ArrayList<CDFeature>();

		//Parse polarity
		if (charge > 0) 
			polarity = "+";
		else
			polarity = "-";

		//Update sample feature max RT
		sample.checkMaxRT(retention);
	}

	//Calculates the height along the gaussian profile where each MS2 was taken
	public void calculateGaussianScore()
	{
		//Iterate through all ids
		for (int i=0; i<lipidCandidates.size(); i++)
		{
			//Calculate gaussian score
			if (lipidCandidates.get(i).keep)
				lipidCandidates.get(i).gaussianScore = 
				peakModel.getNormalizedHeight(lipidCandidates.get(i).correctedRetention);
		}
	}

	//Returns true iff a feature has the same sample, precursor, and retention time
	public boolean checkFeature(Double precursor, Double retention, String polarity, Sample sample)
	{
		boolean result = true;

		//Check File
		if (!this.sample.file.equals(sample.file)) result = false;

		//Check Precursor
		else if (!(calcPPMDiff(precursor,this.mass)<MAXPPMDIFF)) result = false;

		//Check Retention Time
		else if ((retention < this.retention-(this.fwhm*MINRTMULTIPLIER)) 
				|| (retention > this.retention+(this.fwhm*MINRTMULTIPLIER)))
			result = false;

		return result;
	}

	/*
	 * Returns true iff a lipid ID has the same polarity, precursor, sample,
	 * and retention time as the current feature
	 */
	public boolean checkLipid(Lipid lipid, boolean noCoelutingPeak)
	{
		boolean result = true;

		//Check polarity
		if (!lipid.polarity.equals(this.polarity)) result = false;

		//Check Precursor
		if (!(calcPPMDiff(lipid.precursor,this.mass)<MAXPPMDIFF)) result = false;

		//Check File

		if (!this.sample.file.equals(lipid.sample.file)) result = false;

		//Check that peak is the most intense isobaric peak at that retention using a gaussian peak model
		if (!isMostIntensePeak(sample.getCorrectedRT(lipid.retention))) result = false;

		//Check Retention Time
		if ((lipid.retention < this.realRetention-this.fwhm*MINRTMULTIPLIER) 
				|| (lipid.retention > this.realRetention+this.fwhm*MINRTMULTIPLIER)) result = false;

		return result;
	}

	/*
	 * Returns true iff the current feature is the most intense peak at a given retention time
	 * compared to all of neighboring isobaric peaks
	 */
	public boolean isMostIntensePeak(Double retention)
	{
		//Calculate peak height from gaussian profile
		Double featureIntensityAtRT = peakModel.getCalculatedHeight(retention);

		//Iterate through isobaric neighbors
		for (int i=0; i<isobaricNeighbors.size(); i++)
		{
			//If not the most intense peak, return false
			if (isobaricNeighbors.get(i).peakModel.getCalculatedHeight(retention) 
					> featureIntensityAtRT)
				return false;

		}
		return true;
	}

	//Filters each identification based on dot product and reverse dot produce thresholds
	public void filterIDByDotProduct()
	{	
		//For each lipid candidate
		for (int i=0; i<lipidCandidates.size(); i++)
		{
			//Filter Dot Product
			if (lipidCandidates.get(i).dotProduct<MINDOTPRODUCT) lipidCandidates.get(i).keep = false;

			//Filter Rev Dot Product
			if (lipidCandidates.get(i).revDotProduct<MINREVDOTPRODUCT) lipidCandidates.get(i).keep = false;
		}
	}

	//Method to associate lipid ID with feature
	public void addLipid(Lipid lipid)
	{
		boolean found = false;

		//Add to candidate list
		lipidCandidates.add(lipid);

		//Calculate corrected retention time for lipid
		lipid.correctedRetention = sample.getCorrectedRT(lipid.retention);

		//Add rtdev to sample stats
		sample.addRTDev(Math.abs(lipid.retention-retention));

		//If identification already made, merge with previous ID
		for (int i=0; i<uniqueLipids.size(); i++)
		{
			//if match, iterate count and merge result
			if (uniqueLipids.get(i).lipidName.equals(lipid.lipidName))
			{
				uniqueLipids.get(i).addLipid(lipid);
				found = true;
			}
		}

		//If unique  add to uniqueLipids array
		if (!found)
		{
			uniqueLipids.add(new CDLipidCandidate(lipid));
			sample.addLipid();
		}
	}

	//Return string representation of feature
	public String toString()
	{
		String result = mass+","+retention+","+polarity+","+fwhm+","+area+","+sample.file+",";

		for (int i=0; i<uniqueLipids.size(); i++)
		{
			result = result + uniqueLipids.get(i).lipidName+uniqueLipids.get(i).count + ",";
		}

		return result;
	}
}
