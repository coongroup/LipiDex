package mzmine;
import java.util.ArrayList;

import peak_finder.GaussianModel;
import peak_finder.Lipid;
import peak_finder.Sample;
import peak_finder.Utilities;


public class MzFeature extends Utilities
{
	String status;								//MzMine assigned statis (Detected, estimated)
	int charge;									//MzMine calculated charge
	Double mass; 								//mass to charge of precursor
	Double realRetention;						//unaligned retention time
	Double fwhm;								//Peak FWHM
	Double area;								//Peak Area
	Double height;  							//Peak height
	Sample sample;								//Sample object associated
	String polarity;							//Polarity
	ArrayList<MzFeature> isobaricNeighbors;		//Arraylist of all isobaric features nearby
	ArrayList <Lipid> lipidCandidates;			//ArrayList of all putative IDs assigned to feature
	ArrayList<MzLipidCandidate> uniqueLipids;	//All unique lipids associated with feature
	GaussianModel peakModel;					//GaussianModel object

	//Constructor
	public MzFeature(String status, int charge, Double mass, 
			Double realRetention, Double fwhm, Double area, Double height, 
			Sample sample, String polarity)
	{
		//Initialize feature class variables
		this.charge = charge;
		this.mass = mass;
		this.realRetention = realRetention;
		this.fwhm = fwhm;
		this.area = area;
		this.sample = sample;
		this.polarity = polarity;
		this.height = height;

		//Create a gaussian model of peak
		peakModel = new GaussianModel(fwhm, null, realRetention, height);

		//Initizlize arrays
		lipidCandidates = new ArrayList<Lipid>();
		uniqueLipids = new ArrayList<MzLipidCandidate>();
		isobaricNeighbors = new ArrayList<MzFeature>();

		//Update sample feature max RT
		sample.checkMaxRT(realRetention);
	}

	//Calculates multiplier for peak assignment weighting using gaussian model
	public void calculateGaussianScore()
	{
		//Iterate through all ids
		for (int i=0; i<lipidCandidates.size(); i++)
		{
			if (lipidCandidates.get(i).keep)
				lipidCandidates.get(i).gaussianScore = 
				peakModel.getNormalizedHeight(lipidCandidates.get(i).correctedRetention);
		}
	}

	//Returns true iff a feature has the same sample, precursor, polarity, and retention time
	public boolean checkFeature(Double precursor, Double retention, String polarity, Sample sample)
	{
		boolean result = true;

		//Check File
		if (!this.sample.file.equals(sample.file)) result = false;

		//Check Polarity
		else if (!polarity.equals(this.polarity)) result = false;

		//Check Precursor
		else if (!(calcPPMDiff(precursor,this.mass)<MAXPPMDIFF)) result = false;
		
		//Check Retention Time
		else if ((retention < this.realRetention-(this.fwhm*MINRTMULTIPLIER)) 
				|| (retention > this.realRetention+(this.fwhm*MINRTMULTIPLIER)))
		{
			result = false;
		}

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
		if (!isMostIntensePeak(lipid.retention)) result = false;

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
			if (isobaricNeighbors.get(i).peakModel.getCalculatedHeight(retention) 
					> featureIntensityAtRT)
				return false;
		}
		
		return true;
	}

	public void filterID()
	{	
		//For each lipid candidate
		for (int i=0; i<uniqueLipids.size(); i++)
		{
			//Filter Dot Product
			if (uniqueLipids.get(i).maxDotProd<MINDOTPRODUCT) uniqueLipids.get(i).keep = false;

			//Filter Rev Dot Product
			if (uniqueLipids.get(i).maxRevDot<MINREVDOTPRODUCT) uniqueLipids.get(i).keep = false;
		}
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

	/*
	public boolean checkLipid(Lipid lipid)
	{
		boolean result = true;

		//Check polarity
		if (!lipid.polarity.equals(this.polarity)) result = false;

		//Check Precursor
		if (!(calcPPMDiff(lipid.precursor,this.mass)<MAXPPMDIFF)) result = false;

		//Check Retention Time
		if ((lipid.retention < this.retention-this.fwhm*MINRTMULTIPLIER) || (lipid.retention > this.retention+this.fwhm*MINRTMULTIPLIER)) result = false;

		//Check File
		//if (!this.sample.file.equals(lipid.sample.file)) result = false;

		return result;
	}
	*/

	//Method to associate lipid ID with feature
	public void addLipid(Lipid lipid)
	{
		boolean found = false;

		//Add to candidate list
		lipidCandidates.add(lipid);

		//Calculate corrected retention time for lipid
		lipid.correctedRetention = sample.getCorrectedRT(lipid.retention);

		//Add rtdev to sample stats
		sample.addRTDev(Math.abs(lipid.retention-realRetention));

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

		if (!found)
		{
			uniqueLipids.add(new MzLipidCandidate(lipid));

			//Increment Sample ID count
			sample.addLipid();
		}
	}

	public String toString()
	{
		String result = mass+","+realRetention+","+polarity+","+fwhm+","+area+","+sample.file+",";

		for (int i=0; i<uniqueLipids.size(); i++)
		{
			result = result + uniqueLipids.get(i).lipidName+uniqueLipids.get(i).count + ",";
		}

		return result;
	}
}
