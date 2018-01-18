package mzmine;

import lib_gen.Adduct;
import lib_gen.CustomError;
import lib_gen.CustomException;
import mzmine.MzAreaResult;
import mzmine.MzCompoundGroup;
import mzmine.MzFeature;
import peak_finder.PurityResult;
import peak_finder.Sample;
import peak_finder.Lipid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JProgressBar;

import peak_finder.Utilities;

public class MzPeakFinder 
{
	//Constants

	public Double MINPPMDIFF = 20.0; 			//Max mass differece in ppm for feature association
	public Double MINRTMULTIPLIER = 1.0;		//Minimum fwhm multiplier for ID association
	public Double MINDOTPRODUCT = 500.0;		//Minimum dot product for spectral matching
	public Double MINREVDOTPRODUCT = 700.0;		//Minimum reverse dot product for spectral matching
	public Double MINFAPURITY = 101.0;			//Minimum lipid spectral purity for molecular composition assignment
	public Integer MINIDNUM = 1;				//Minimum number of compounds per compound group

	public static Utilities util = new Utilities();											//Utilities object
	public JProgressBar progressBar = null; 												//Progress Bar Quant Tab
	public JLabel ProgressStatus = null;													//Status
	static ArrayList<MzCompoundGroup> compoundGroups = new ArrayList<MzCompoundGroup>();	//Array of all compound groups
	static ArrayList<Sample> samples = new ArrayList<Sample>();								//Array of all samples
	ArrayList<String> filenames = new ArrayList<String>();									//Array of all string filenames
	static ArrayList<Integer> cGIndexArray = new ArrayList<Integer>();						//Array of CG header indeces	
	static ArrayList<Integer> fGIndexArray = new ArrayList<Integer>();						//Array of feature header indeces	
	static int cdAreaStart = 99999;															//Area for result parsing
	static ArrayList<Lipid> importedLipids = new ArrayList<Lipid>();						//Array of all imported lipid IDs
	static ArrayList<Lipid> unassignedLipids = new ArrayList<Lipid>();						//Array of unassigned lipid IDs
	static ArrayList<ArrayList<Integer>> cgIndexMap;										//Hash table for compound groupds
	ArrayList<File> resultFiles;															//Array of MS2 result files
	ArrayList<Integer> samplePairNumbers;													//Array of sample pairs for separate polarity runs
	String posTable;																		//Filename for positive polarity table
	String negTable;																		//Filename for negative polarity table
	boolean rtFilter;																		//Array of sample pairs for separate polarity runs
	double fwhmSum = 0.0;																	//Sum of all peak FWHMs												
	double avgFWHM = 0.0;																	//Average of all peak FWHMs
	int progressInt = 0;																	//Progress for progress bar
	int numFeatures = 0;																	//Total number of features loaded
	static ArrayList<Adduct> adductsDB;														//ArrayList of all adducts from active lib

	//Constructor
	public MzPeakFinder(String posTable, String negTable, ArrayList<File> resultFiles, int minFeatureCount, 
			boolean rtFilter, double rtFilterMult, JProgressBar progressBar, ArrayList<Integer> samplePairNumbers,
			ArrayList<Adduct> adductsDB) throws CustomException
	{
		this.progressBar = progressBar;
		this.posTable = posTable;
		this.resultFiles = resultFiles;
		this.negTable = negTable;
		this.MINIDNUM = minFeatureCount;
		this.rtFilter = rtFilter;
		this.MINRTMULTIPLIER = rtFilterMult;
		this.samplePairNumbers = samplePairNumbers;
		MzPeakFinder.adductsDB = adductsDB;
	}

	//Update progress bar
	public void updateProgress(int bar, int progress, String message)
	{
		if (progress != progressInt)
		{
			progressBar.setValue(progress);
			progressBar.setString(progress+message);
			progressBar.repaint();
		}
	}

	public void runQuantitation(boolean separatePolarities, 
			boolean adductFiltering, boolean inSourceFiltering) throws IOException, CustomException
	{
		//Clear arrays
		compoundGroups.clear();
		samples.clear(); 
		importedLipids.clear(); 
		unassignedLipids.clear(); 

		//Initialize samples array
		samples = new ArrayList<Sample>();

		if (posTable.contains("."))
		{
			//Import positive results from mzmine
			try
			{
				importMzResults(posTable,MINIDNUM, separatePolarities, "+");
			}
			catch (CustomException e)
			{
				CustomError ce = new CustomError(e.getMessage(), null);
			}
			catch (Exception e)
			{
				CustomError ce = new CustomError("Error reading peak tables", e);
			}
		}

		if (negTable.contains("."))
		{
			//Import negative results from mzmine
			importMzResults(negTable,MINIDNUM, separatePolarities, "-");
		}

		//Sort all CG's by retention time
		Collections.sort(compoundGroups);

		//Create CG index hash array
		createIndexMap(compoundGroups.get(compoundGroups.size()-1).retention);

		//Find Isobaric neighbors
		findIsobaricFeatures();

		//Find coeluting peaks 
		findClosestPeak();

		//Load spectral match IDs
		loadIDS(resultFiles, separatePolarities); 

		//Link separate polarity files
		linkFiles(separatePolarities);

		//Associate LB Results to Features
		associateIDSHashMap(0.5);

		//Filter IDs by dot product
		filterFeatureLipidsByDotProduct();

		//Find best ID
		findBestID();

		//Filter results for duplicates and dimers
		filterResults(adductFiltering, inSourceFiltering);

		//Filter IDs by RT
		if (rtFilter) checkClassRTDist(MINRTMULTIPLIER);

		//Write filtering reasons
		writeFilterDB(new File(posTable).getParent()+"\\Unfiltered_Results.csv");

		//Calculate Statistics
		calculateStatistics();

		//Associate MS2s
		//checkMS2Sampling(rawFolder);

		//Merge polarity results
		mergePolarities();

		//Write Results;
		writeDB((new File(posTable).getParent())+"\\Final_Results.csv");
		writeStats((new File(posTable).getParent())+"\\Sample_Information.csv");
	}

	//Method to link files when collected with separate polarities
	private void linkFiles(boolean separatePolarities)
	{
		//If separate polarity files
		if (separatePolarities)
		{
			//Iterate through samples
			for (int i=0; i<samples.size(); i++)
			{
				for (int j=0; j<samples.size(); j++)
				{
					//If linked files
					if (samples.get(i).polarityFileNumber == samples.get(j).polarityFileNumber
							&& !samples.get(i).file.equals(samples.get(j).file))
					{

						//Create merged name and add to both
						if (samples.get(i).mergedName.equals("")
								&& samples.get(j).mergedName.equals(""))
						{
							samples.get(i).mergedName = samples.get(i).file+"+"+samples.get(j).file;
							samples.get(j).mergedName = samples.get(i).file+"+"+samples.get(j).file;
						}
					}
				}
			}
		}
	}

	//Parse header titles to avoid errors from table re-arrangement
	private void parseHeaders(String line, boolean separatePolarities) throws CustomException
	{	
		ArrayList<String> cGArray = new ArrayList<String>(Arrays.asList("row ID", "row m/z", 
				"row retention time", "row number of detected peaks"));
		ArrayList<String> fArray = new ArrayList<String>(Arrays.asList("Peak status","Peak m/z",
				"Peak RT","Peak area", "Peak charge","Peak FWHM","Peak height"));
		ArrayList<Integer> featureListTemp = new ArrayList<Integer>();
		//Clear arrays
		cGIndexArray.clear();
		fGIndexArray.clear();

		//Split line
		String[] split = line.split(",");

		//Iterate through header array
		for (int j=0; j<cGArray.size(); j++)
		{
			//Iterate through parsed line
			for (int i=0; i<split.length; i++)
			{
				//If cell contains row (is a compound group)
				if (split[i].contains("row"))
				{
					//If a match is found add index to header array
					if (split[i].contains(cGArray.get(j)))
					{
						cGIndexArray.add(i);
					}
				}
			}
		}
		//If cell is from a feature
		//Iterate through header array
		for (int j=0; j<fArray.size(); j++)
		{
			//Iterate through parsed line
			for (int i=0; i<split.length; i++)
			{
				//If cell contains row (is a compound group)
				if (split[i].contains("Peak"))
				{
					//If a match is found add index to header array
					if (split[i].contains(fArray.get(j)))
					{
						fGIndexArray.add(i);
					}
				}
			}
		}

		if (cGIndexArray.size() != cGArray.size() || fGIndexArray.size()%fArray.size() != 0)
			throw new CustomException("Required fields are missing from data table.  Table must include: row ID, row m/z, row retention time, "
					+ "row number of detected peaks, Peak status, Peak m/z, Peak RT, Peak area, Peak charge, Peak FWHM, and Peak height", null);

		//Reorganize header list //0,3,6,9
		for (int j=0; j<fGIndexArray.size()/fArray.size(); j++) //0,1,2
		{
			for (int i=0; i<fArray.size(); i++) //0,1,2,3,4,5,6,7
			{
				featureListTemp.add(fGIndexArray.get(i*(fGIndexArray.size()/fArray.size())+j));
			}
		}

		fGIndexArray = featureListTemp;

		//Parse sample names from headers
		parseFiles(split, separatePolarities);
	}

	private void importMzResults(String fileString, int minFeatureCount, 
			boolean separatePolarities, String polarity) throws IOException, CustomException
	{
		String line = "";								//String for storing currently read line
		String split[];									//Array for parsed line
		String headerSplit[] = null;					//Array for header parsing
		String sampleName = "";							//String of sample id
		MzCompoundGroup compoundGroupTemp = null;		//Temp compound group object
		MzFeature featureTemp = null;					//Temp feature object
		int featureNumber = 0;							//Number of features
		int fwhmCount = 0;

		//Update Status
		updateProgress(2,0,"% - Importing Peak Data");

		//Create file buffer
		File file = new File(fileString);
		BufferedReader reader = new BufferedReader(new FileReader(file));

		//Check if file exists
		if (!file.exists())
		{
			reader.close();
			throw new CustomException(fileString+" does not exist", null);
		}

		//read line if not empty
		while ((line = reader.readLine()) != null)
		{
			//If not the first line, add compound group to array
			if (compoundGroupTemp!= null) compoundGroups.add(compoundGroupTemp);

			//Parse line
			split = line.split(",");

			//Parse headers and sample names
			if (line.contains("row"))
			{
				parseHeaders(line, separatePolarities);
				headerSplit = split;
			}

			//import line if it does not contain header
			else if (!line.contains("row") && !line.contains("null"))
			{
				featureNumber = 0;

				//Create compound group
				compoundGroupTemp = new MzCompoundGroup(Double.valueOf(split[cGIndexArray.get(1)]), 
						Double.valueOf(split[cGIndexArray.get(2)]), Integer.valueOf(split[cGIndexArray.get(0)]), polarity);

				//Iterate through feature cell indeces
				for (int i=fGIndexArray.get(0); i<split.length; i=i+7)
				{

					sampleName = headerSplit[fGIndexArray.get(featureNumber*7)]
							.substring(0,headerSplit[fGIndexArray.get(featureNumber*7)].indexOf(" "));

					if ((int)(Math.round(Math.floor(i/7.0)))>featureNumber)
					{

						//Create feature
						featureTemp = new MzFeature(split[fGIndexArray.get(featureNumber*7)],
								Integer.valueOf(split[fGIndexArray.get(featureNumber*7+4)]),
								Double.valueOf(split[fGIndexArray.get(featureNumber*7+1)]),
								Double.valueOf(split[fGIndexArray.get(featureNumber*7+2)]),
								Double.valueOf(split[fGIndexArray.get(featureNumber*7+5)]),
								Double.valueOf(split[fGIndexArray.get(featureNumber*7+3)]),
								Double.valueOf(split[fGIndexArray.get(featureNumber*7+6)]),
								matchSample(sampleName), polarity);

						//Set file polarity
						featureTemp.sample.polarity = polarity;

						//Add feature to compoundGroup
						compoundGroupTemp.addFeature(featureTemp);

						//Increment featureNumber
						featureNumber = (int)(Math.round(Math.floor(i/7.0)));
					}
				}

				sampleName = headerSplit[fGIndexArray.get(featureNumber*7)]
						.substring(0,headerSplit[fGIndexArray.get(featureNumber*7)].indexOf(" "));

				//Add remaining feature
				featureTemp = new MzFeature(split[fGIndexArray.get(featureNumber*7)], //status
						Integer.valueOf(split[fGIndexArray.get(featureNumber*7+4)]), 	//charge
						Double.valueOf(split[fGIndexArray.get(featureNumber*7+1)]),	//mass
						Double.valueOf(split[fGIndexArray.get(featureNumber*7+2)]),	//retention
						Double.valueOf(split[fGIndexArray.get(featureNumber*7+5)]),	//fwhm
						Double.valueOf(split[fGIndexArray.get(featureNumber*7+3)]),	//area
						Double.valueOf(split[fGIndexArray.get(featureNumber*7+6)]),	//height
						matchSample(sampleName), polarity);							//Sample, polarity

				//Add feature to compoundGroup
				compoundGroupTemp.addFeature(featureTemp);
			}
		}
		reader.close();

		//Add final compound group
		compoundGroups.add(compoundGroupTemp);

		//Calculate average FWHM and quant Ion and look for max Isotope number
		for (int i=0; i<compoundGroups.size(); i++)
		{

			updateProgress(2,(int)(Double.valueOf(i+1)
					/Double.valueOf(compoundGroups.size())*100.0),"% - Importing Peak Data");

			compoundGroups.get(i).calcFWHM();
			compoundGroups.get(i).findQuantIon();
			if (compoundGroups.get(i).avgFWHM<10.0)
			{
				fwhmCount++;
				fwhmSum += compoundGroups.get(i).avgFWHM;
			}

		}
		avgFWHM = fwhmSum/fwhmCount;
	}

	//Returns matching sample object to sample name
	private Sample matchSample(String name)
	{
		Sample result = null;

		for (int i=0; i<samples.size(); i++)
		{
			if (samples.get(i).file.equals(name))
				return samples.get(i);
		}

		return result;
	}

	//Parse sample names from table headers
	private void parseFiles(String[] line, boolean separatePolarities)
	{
		//Iterate through cells
		for (int i=0; i<line.length; i++)
		{
			//If a feature header
			if (!line[i].contains("row") && line[i].contains("Peak"))
			{
				//If a unique entry
				if (!filenames.contains(line[i].substring(0,line[i].indexOf(" "))))
				{
					//Add sample to arrays
					filenames.add(line[i].substring(0,line[i].indexOf(" ")));
					samples.add(new Sample(line[i].substring(0,line[i].indexOf(" ")),-1,separatePolarities));
				}
			}
		}
	}

	//Returns the corresponding sample pair number for a specific sample name
	private int getSamplePairNumber(String name)
	{
		int result = -1;

		for (int i=0; i<resultFiles.size(); i++)
		{
			if (name.equals(resultFiles.get(i).getAbsolutePath()))
			{
				return samplePairNumbers.get(i);
			}
		}

		return result;
	}

	//Merges polarities
	private void mergePolarities()
	{
		for (int i=0; i<compoundGroups.size(); i++)
		{
			compoundGroups.get(i).mergePolarities();
		}
	}

	//Load all MS2 identifications and associate with features
	private void loadIDS(ArrayList<File> filesArray, boolean separatePolarities) throws IOException, CustomException
	{	
		String line = "";	//Currently read line
		String[] split;		//Array for parsed entries
		Lipid lipidTemp;	//Temporary lipid object
		int numResults;		//Number of result files to read

		//Associate result files with sample objects
		numResults = findResultFiles(filesArray, separatePolarities);

		//Update status bar
		updateProgress(2,0,"% - Loading IDs");

		//For all samples
		for (int i=0; i<samples.size(); i++)
		{
			try
			{
				if (!samples.get(i).filename.equals(""))
				{
					//Create ms2 results file buffer
					File file = new File(samples.get(i).filename);
					BufferedReader resultReader = new BufferedReader(new FileReader(file));

					//read line if not empty
					while ((line = resultReader.readLine()) != null)
					{
						if (!line.contains("MS2 ID"))
						{
							//Split
							split = line.split(",");

							//Catch Glycans
							if (!line.contains("glycan"))
							{
								//Add Lipid
								lipidTemp = new Lipid(Double.valueOf(split[1]),
										Double.valueOf(split[4]), samples.get(i),
										Double.valueOf(split[7]), Double.valueOf(split[8]), 
										split[3], Double.valueOf(split[5]),
										Integer.valueOf(split[9]), Boolean.valueOf(split[11]), 
										Boolean.valueOf(split[12]), parsePurity(split[10]),
										parseMatchedMasses(split[14]));

								if (lipidTemp.dotProduct>MINDOTPRODUCT && lipidTemp.revDotProduct>MINREVDOTPRODUCT)
								{	
									importedLipids.add(lipidTemp);
									samples.get(i).addPurity(Integer.valueOf(split[9]));
								}
							}
						}
					}
					resultReader.close();

					//Update progress Bar
					updateProgress(2, (int)(Double.valueOf(i+1)
							/Double.valueOf(numResults)*100.0),"% - Loading IDs");
				}
			}
			catch (Exception e)
			{
				throw new CustomException("Error reading "+samples.get(i).filename+".  Please check formatting", e);
			}
		}
	}

	//Returns array of parsed purity values
	private ArrayList<PurityResult> parsePurity(String s)
	{
		ArrayList<PurityResult> purities = new ArrayList<PurityResult>();
		PurityResult pTemp;

		//Split string
		String[] split = s.split(" / ");

		for (int i=0; i<split.length; i++)
		{
			if (!split[i].equals(""))
			{
				//Create new purity result
				pTemp = new PurityResult(split[i].substring(0, split[i].indexOf("(")), 
						Integer.valueOf(split[i].substring(split[i].indexOf("(")+1,split[i].indexOf(")"))));

				purities.add(pTemp);
			}
		}

		Collections.sort(purities);

		return purities;
	}

	//Parse out masses from the matched mass column
	public ArrayList<Double> parseMatchedMasses(String s)
	{
		ArrayList<Double> result = new ArrayList<Double>();
		String[] split = s.split(" | ");

		for (int i=0; i<split.length; i++)
		{
			if (!split[i].equals("") && !split[i].contains("|"))
				result.add(Double.valueOf(split[i]));
		}

		return result;
	}

	//Returns number of result files and associates with Sample objects
	private int findResultFiles(ArrayList<File> resultFiles, boolean separatePolarities)
	{
		int count = 0;	//Total number of result files

		//For all result files
		for (int i=0; i<resultFiles.size(); i++) 
		{
			//If a valid ms2 result file
			if (resultFiles.get(i).isFile() && resultFiles.get(i).getName().contains("_Results.csv")) 
			{
				//Look for match in samples array
				for (int j=0; j<samples.size(); j++)
				{
					//If a match with current sample
					if (samples.get(j).file.substring(0, samples.get(j).file.lastIndexOf(".")).equals
							(resultFiles.get(i).getName().substring(0,resultFiles.get(i).getName().indexOf("_Results.csv"))))
					{
						//Store absolute file paths and fine paired sample for separate polarity runs
						samples.get(j).filename = resultFiles.get(i).getAbsolutePath();
						if (separatePolarities)samples.get(j).polarityFileNumber = getSamplePairNumber(samples.get(j).filename);
						count++;
					}
				}
			}
		}
		return count;
	}

	//Create feature maps of isobaric features for ms2 association
	private void findIsobaricFeatures()
	{
		ArrayList<MzCompoundGroup> tempGroupArray = new ArrayList<MzCompoundGroup>();

		//Iterate through all compound groups sorted by retention time
		for (int i=0; i<compoundGroups.size(); i++)
		{
			updateProgress(2,(int)(Double.valueOf(i+1)
					/Double.valueOf(compoundGroups.size())*100.0),"% - Finding Coeluting Peaks");

			//Clear array
			tempGroupArray = new ArrayList<MzCompoundGroup>();

			for (int j=0; j<compoundGroups.size(); j++)
			{
				//If CG is within one minute, add to temporary array
				if (Math.abs(compoundGroups.get(i).retention-compoundGroups.get(j).retention)<0.5)
					tempGroupArray.add(compoundGroups.get(j));
			}

			//For all compound groups
			for (int j=0; j<tempGroupArray.size(); j++)
			{
				//For all features
				for (int l=0; l<tempGroupArray.get(j).features.size(); l++)
				{
					//For each feature, find those peaks which are within global ppm tolerance
					//If withing RT and ppm tolerance, add to feature neighbor array in feature object
					compoundGroups.get(i).checkIsobaricFeature(tempGroupArray.get(j).features.get(l));
				}
			}
		}
	}

	//Method to associate IDs with features using a hashmap to speed up processing
	private void associateIDSHashMap(Double timeWindow)
	{
		boolean found = false;
		int minIndex;
		int maxIndex;
		int cgIndex = 0;
		updateProgress(2,0,"% - Associating IDs");

		//Iterate through all imported lipids
		for (int i=0; i<importedLipids.size(); i++)
		{
			//Filter by dot product
			if (importedLipids.get(i).dotProduct>Utilities.MINDOTPRODUCT
					&& importedLipids.get(i).revDotProduct>Utilities.MINREVDOTPRODUCT)
			{	
				//Find min and max index for CG hash map
				minIndex = calculateMapIndex(importedLipids.get(i).retention-avgFWHM*3.0);

				if (minIndex<0) minIndex = 0;
				else if (minIndex>cgIndexMap.size()) minIndex = cgIndexMap.size()-2;

				maxIndex = calculateMapIndex(importedLipids.get(i).retention+avgFWHM*3.0);

				if (maxIndex>cgIndexMap.size()) maxIndex = cgIndexMap.size()-1;

				//Add keys to key array
				for (int j=minIndex; j<maxIndex; j++)
				{
					for (int k=0; k<cgIndexMap.get(j).size(); k++)
					{
						cgIndex = cgIndexMap.get(j).get(k);

						//Iterate through compounds which contain correct polarity
						if (((importedLipids.get(i).polarity.equals("+") && compoundGroups.get(cgIndex).positiveFeature) || 
								(importedLipids.get(i).polarity.equals("-") && compoundGroups.get(cgIndex).negativeFeature))
								&& !compoundGroups.get(cgIndex).isUniqueQuantIon(importedLipids.get(i).precursor))
						{
							//Iterate through features
							for (int l=0; l<compoundGroups.get(cgIndex).features.size(); l++)
							{	
								//If RT is not within 1 minute, break loop to save iteration time
								if (Math.abs(importedLipids.get(i).retention-compoundGroups.get(cgIndex).features.get(l).realRetention)>1.0 && 
										compoundGroups.get(cgIndex).features.get(l).realRetention>0.01) break;
								
								if (compoundGroups.get(cgIndex).features.get(l).checkLipid(importedLipids.get(i),true))
								{
									//Add lipid to feature
									compoundGroups.get(cgIndex).features.get(l).addLipid((importedLipids.get(i)));

									//Add sum ID to compound for later collapsing
									compoundGroups.get(cgIndex).addSumID(importedLipids.get(i).sumLipidName);

									//Increment count
									compoundGroups.get(cgIndex).featuresAdded++;
									found = true;
								}
								if (found) break;
							}
							if (found) break;
						}
					}
					if (found) break;
				}
				if (!found)
				{
					unassignedLipids.add(importedLipids.get(i));
				}

				found = false;

				//Update progress Bar
				updateProgress(2, (int)(Double.valueOf(i+1)
						/Double.valueOf(importedLipids.size())*100.0),"% - Associating IDs");
			}
		}
	}

	/*
	 * Find best lipid ID for each compound group by first finding
	 * most common sum ID, calculating the purity of identification
	 * based on MS2 purity and gaussian profile
	 */
	private void findBestID()
	{
		//Iterate throuh compound groups and retain sum ID with most matches
		for (int i=0; i<compoundGroups.size(); i++)
		{
			compoundGroups.get(i).filterSumIDs();
			compoundGroups.get(i).calculateWeightedPurity();
			compoundGroups.get(i).getBestLipidID();
		}
	}

	private void filterFeatureLipidsByDotProduct()
	{
		//For each compound group
		for (int j=0; j<compoundGroups.size(); j++)
		{
			updateProgress(2,(int)(Double.valueOf(j+1)
					/Double.valueOf(compoundGroups.size())*100.0),"% - Filtering Identifications");

			//For each feature
			for (int k=0; k<compoundGroups.get(j).features.size(); k++)
			{
				//Filter IDs
				compoundGroups.get(j).features.get(k).filterIDByDotProduct();
				compoundGroups.get(j).features.get(k).calculateGaussianScore();
			}
		}
	}

	//Calculate FWHM and number of compounds for each sample
	private void calculateStatistics()
	{
		//Iterate through compound groups
		for (int i=0; i<compoundGroups.size(); i++)
		{
			updateProgress(2,(int)(Double.valueOf(i+1)
					/Double.valueOf(compoundGroups.size())*100.0),"% - Calculating Sample Stats");

			//Iterate compounds
			for (int j=0; j<compoundGroups.get(i).features.size(); j++)
			{
				//Iterate through samples
				for (int k=0; k<samples.size(); k++)
				{
					//If the sample is a match
					if (samples.get(k).file.equals(compoundGroups.get(i).features.get(j).sample.file))
					{
						//Increment compound count and add FWHM to average
						samples.get(k).addCompound();
						samples.get(k).addFWHM(compoundGroups.get(i).features.get(j).fwhm);
					}
				}
			}
		}

		//Iterate through samples and calculate stats
		for (int i=0; i<samples.size(); i++)
		{
			samples.get(i).calculateAverages();
		}
	}

	//Remove identifications which fall outside of class rt cluster
	//Uses standard deviations supplied by user input
	private void checkClassRTDist(double multiplier)
	{
		ArrayList<String> classArray = new ArrayList<String>();
		ArrayList<Double> classRTMinArray = new ArrayList<Double>();
		ArrayList<Double> classRTMaxArray = new ArrayList<Double>();
		ArrayList<ArrayList<Double>> rtArray = new ArrayList<ArrayList<Double>>();

		boolean matchFound;

		//Iterate through compound groups
		for (int i=0; i<compoundGroups.size(); i++)
		{

			updateProgress(2, (int)(Double.valueOf(i+1)
					/Double.valueOf(compoundGroups.size())*100.0),"% - Filtering Retention Times");

			//Iterate through compoundGroups
			if (compoundGroups.get(i).finalLipidID != null)
			{
				matchFound = false;

				//Iterate through class array
				for (int j=0; j<classArray.size(); j++)
				{
					//Add retention to corresponsing class
					if (classArray.get(j).equals(compoundGroups.get(i).finalLipidID.lipidClass))
					{
						rtArray.get(j).add(compoundGroups.get(i).retention);
						matchFound = true;
					}
				}

				//If no match found, add class to class array
				if (!matchFound)
				{
					classArray.add(compoundGroups.get(i).finalLipidID.lipidClass);
					rtArray.add(new ArrayList<Double>(Arrays.asList(compoundGroups.get(i).retention)));
				}
			}
		}

		//Create min and max retention time array
		for (int i=0; i<classArray.size(); i++)
		{
			classRTMinArray.add(util.findMedian(rtArray.get(i))-getMaxDevOutlier(rtArray.get(i), Utilities.MINRTMULTIPLIER));
			classRTMaxArray.add(util.findMedian(rtArray.get(i))+getMaxDevOutlier(rtArray.get(i), Utilities.MINRTMULTIPLIER));
		}

		//Filter identifications
		for (int i=0; i<compoundGroups.size(); i++)
		{
			//Iterate through compoundGroups
			if (compoundGroups.get(i).keep && compoundGroups.get(i).finalLipidID != null)
			{
				//Iterate through class array
				for (int j=0; j<classArray.size(); j++)
				{
					//If lipid class matches and retention is outside of bounds, remove
					if (classArray.get(j).equals(compoundGroups.get(i).finalLipidID.lipidClass)
							&& rtArray.get(j).size()>4
							&& (compoundGroups.get(i).retention < classRTMinArray.get(j) 
									|| compoundGroups.get(i).retention > classRTMaxArray.get(j)))
					{
						compoundGroups.get(i).keep = false;
						compoundGroups.get(i).filterReason = "RT out of class range";
					}
				}
			}
		}
	}

	//Return min and max outlier bounds using the Iglewicz and Hoaglin methodology
	private Double getMaxDevOutlier(ArrayList<Double> rtArray, Double threshold)
	{
		Double maxDev;
		ArrayList<Double> absDev = new ArrayList<Double>();

		//Calculate average
		Double mean = util.findMean(rtArray);

		//Calculate median
		Double median = util.findMedian(rtArray);

		//Calculate absolute deviation array
		for (int i=0; i<rtArray.size(); i++)
		{
			absDev.add(Math.abs(rtArray.get(i)-median));
		}

		//Calculate median absolute deviation
		Double medAbsDev = util.findMedian(absDev);

		maxDev = Math.abs((threshold*medAbsDev)/0.6745);

		return maxDev;
	}


	//Method to create hashmap structure for compound groups
	private void createIndexMap(Double rtMax)
	{
		int maxIndex = calculateMapIndex(rtMax+1.0);
		int indexTemp;

		//Initialize index hash map using 0.1 min bins
		cgIndexMap = new ArrayList<ArrayList<Integer>>();

		for (int i=0; i<(maxIndex+1); i++)
		{
			cgIndexMap.add(new ArrayList<Integer>());
		}

		/*
		 * Iterate through all compound groups sorted by retention time
		 * and populate array with keys
		 */
		for (int i=0; i<compoundGroups.size(); i++)
		{
			indexTemp = calculateMapIndex(compoundGroups.get(i).retention);
			cgIndexMap.get(indexTemp).add(i);
		}
		updateProgress(2,0,"% - Importing Feature List");
	}

	//Calculate the hash map index based on retention time
	private Integer calculateMapIndex(Double rt)
	{
		return (int) Math.round(rt/0.1);
	}


	//Write sample statistics file
	private void writeStats(String filename) throws FileNotFoundException
	{
		//Write file
		try
		{
			PrintWriter pw = new PrintWriter(filename);

			pw.println("File,Feature Groups,Avg. Peak FWHM (min.),"
					+ "Peak Capacity,Identified Lipids,"
					+ "Avg. MS2 Purity,Avg. Lipid Mass Error"
					+ "(ppm)");

			for (int i=0; i<samples.size(); i++)
			{
				pw.println(samples.get(i));
			}

			pw.close();
		}
		catch (Exception e)
		{
			writeDB(filename.substring(0, filename.lastIndexOf("."))+"_"+Calendar.getInstance().getTimeInMillis()/1000+".csv");
		}
	}


	//Finds all compound groups which have been samples for MS2 analysis
	@SuppressWarnings("unused")
	private void checkMS2Sampling(String folder) throws IOException
	{
		String line = null;
		String mgfFolder = folder+"//MGF";
		Double retention_mgf = 0.0;		
		Double precursor_mgf = 0.0;
		String polarity = "";

		//Update Status Bar
		updateProgress(2,0,"% - Calculating Statistics");

		for (int i=0; i<samples.size(); i++)
		{

			if (!samples.get(i).filename.equals(""))
			{
				//Create file buffer from .mgf file
				File file = new File(mgfFolder+"\\"+samples.get(i).filename.substring(samples.get(i).filename.lastIndexOf("\\")+1, 
						samples.get(i).filename.lastIndexOf(".m"))+".mgf");
				BufferedReader reader = new BufferedReader(new FileReader(file));

				//read line if not empty
				while ((line = reader.readLine()) != null)
				{

					//read in retention time
					if (line.contains("RTINSECONDS"))
					{
						retention_mgf = Double.valueOf(line.substring(line.lastIndexOf("=")+1))/60.0;
					}

					//read in polarity
					if (line.contains("CHARGE"))
					{
						if (line.contains("+")) polarity = "+";
						else polarity = "-";
					}

					//read in precursor mass
					if (line.contains("PEPMASS"))
					{

						if (line.contains(" "))
						{
							precursor_mgf = Double.valueOf(line.substring(line.lastIndexOf("=")+1,line.lastIndexOf(" ")));
						}
						else
						{
							precursor_mgf = Double.valueOf(line.substring(line.lastIndexOf("=")+1));
						}
					}

					if (line.contains("END IONS"))
					{

						//Look for rt match and precursor match, if found, associate with compound group
						for (int j=0; j<compoundGroups.size(); j++)
						{
							//If RT is not within 1 minute, break loop to save iteration time
							if (Math.abs(retention_mgf-compoundGroups.get(j).retention)<1.0 && !compoundGroups.get(j).ms2Sampled)
							{
								//Iterate through all features
								for (int k=0; k<compoundGroups.get(j).features.size(); k++)
								{
									if (compoundGroups.get(j).features.get(k).checkFeature(precursor_mgf, retention_mgf, polarity, samples.get(i)))
									{
										compoundGroups.get(j).ms2Sampled = true;
									}
								}
							}
						}

						retention_mgf = 0.0;
						precursor_mgf = 0.0;
						polarity = "";
					}
				}
				reader.close();
			}

			updateProgress(2, (int)(Double.valueOf(i+1)
					/Double.valueOf(samples.size())*100.0),"% - Calculating Statistics");
		}
	}

	//Find closest peak within ppm difference and FWHM+rtdev
	private void findClosestPeak()
	{
		Double minRT = 9999.0;
		Double maxRT = 0.000;
		Double minRTFWHM = 0.0;
		Double maxRTFWHM = 0.0;
		Double targetMinRT;
		Double targetMaxRT;

		//Iterate through compounds groups
		for (int i=0; i<compoundGroups.size(); i++)
		{
			updateProgress(2,(int)(Double.valueOf(i+1)
					/Double.valueOf(compoundGroups.size())*100.0),"% - Finding Neighbor Peaks");

			//For all compounds, find lowest and highest corrected retention time
			for (int j=0; j<compoundGroups.get(i).features.size(); j++)
			{
				//Update min and max corrected rt
				if (compoundGroups.get(i).retention<minRT)
				{
					minRT = compoundGroups.get(i).retention;
					minRTFWHM = compoundGroups.get(i).avgFWHM;
				}
				if (compoundGroups.get(i).retention>maxRT)
				{
					maxRT = compoundGroups.get(i).retention;
					maxRTFWHM = compoundGroups.get(i).avgFWHM;
				}
			}

			//Set rt bounds including FWHM
			minRT = minRT - minRTFWHM;
			maxRT = maxRT - maxRTFWHM;

			//Iterate through all compound groups
			for (int j=0; j<compoundGroups.size(); j++)
			{
				targetMinRT = compoundGroups.get(j).retention-compoundGroups.get(j).avgFWHM;
				targetMaxRT = compoundGroups.get(j).retention+compoundGroups.get(j).avgFWHM;

				//If not comparing to itself
				if (i != j && compoundGroups.get(i).noCoelutingPeaks && compoundGroups.get(j).noCoelutingPeaks)
				{
					//If within RT bounds
					if ((targetMinRT > minRT && targetMinRT < maxRT) || (targetMaxRT > minRT && targetMaxRT < maxRT))
					{
						//Run ppm scan
						if (compoundGroups.get(i).ppmMatch(compoundGroups.get(j)))
						{
							compoundGroups.get(i).noCoelutingPeaks = false;
							compoundGroups.get(j).noCoelutingPeaks = false;
						}
					}
				}
			}
		}
	}

	//Writes compound groups when boolean keep is true
	public void writeDB(String filename) throws FileNotFoundException
	{
		//Write file
		try
		{
			PrintWriter pw = new PrintWriter(filename);

			//Print header
			pw.print("Retention Time (min),Quant Ion,Polarity,Area (max),Identification,Lipid Class,Features Found,");

			//Print sample names
			for (int i=0; i<compoundGroups.get(0).results.size(); i++)
			{
				if (compoundGroups.get(0).results.get(i).merged) pw.print(compoundGroups.get(0).results.get(i).mergedFileName);
				else pw.print(compoundGroups.get(0).results.get(i).file.file);
				pw.print(",");
			}

			pw.println();

			//Update Status
			updateProgress(2, 0,"% - Writing Results");

			//Print compound group
			for (int j=0; j<compoundGroups.size(); j++)
			{
				updateProgress(2, (int)(Double.valueOf(j+1)
						/Double.valueOf(compoundGroups.size())*100.0),"% - Writing Results");

				if (compoundGroups.get(j).keep)
				{
					pw.println(compoundGroups.get(j));
				}
			}
			pw.close();
		}
		catch (Exception e)
		{
			writeDB(filename.substring(0, filename.lastIndexOf("."))+"_"+Calendar.getInstance().getTimeInMillis()/1000+".csv");
		}
	}

	//Writes compound groups with reason for removal
	public void writeFilterDB(String filename) throws FileNotFoundException
	{
		//Write file
		try
		{
			PrintWriter pw = new PrintWriter(filename);

			//Print header
			pw.print("Retention Time (min),Quant Ion,Polarity,Area (max),Identification,Lipid Class,Features Found,Filter Status,");

			//Print sample names
			for (int i=0; i<compoundGroups.get(0).results.size(); i++)
			{
				if (compoundGroups.get(0).results.get(i).merged) pw.print(compoundGroups.get(0).results.get(i).mergedFileName);
				else pw.print(compoundGroups.get(0).results.get(i).file.file);
				pw.print(",");
			}

			pw.println();

			//Update Status
			updateProgress(2, 0,"% - Writing Results");

			//Print compound group
			for (int j=0; j<compoundGroups.size(); j++)
			{
				updateProgress(2, (int)(Double.valueOf(j+1)
						/Double.valueOf(compoundGroups.size())*100.0),"% - Writing Results");
				pw.println(compoundGroups.get(j).toStringFilterReason());
			}
			pw.close();
		}
		catch (Exception e)
		{
			writeDB(filename.substring(0, filename.lastIndexOf("."))+"_"+Calendar.getInstance().getTimeInMillis()/1000+".csv");
		}
	}

	private void filterResults(boolean adductFiltering, boolean inSourceFiltering)
	{
		int counter = 0;
		ArrayList<Lipid> candidates;

		//Iterate through all compound groups
		for (int i=1; i<compoundGroups.size(); i++)
		{
			updateProgress(2,(int)(Double.valueOf(i+1)
					/Double.valueOf(compoundGroups.size())*100.0),"% - Filtering results");

			//Find those targetes within FWHM range before peak
			counter = i-1;

			//if the two groups are within the FWHM
			while (counter >= 0 && compoundGroups.get(i).targetInFWHM(compoundGroups.get(counter),2.0))
			{			
				//Remove misidentified isotopes If the masses differ by 1 mass unit and neither have an identification, remove
				if (Math.abs(1.003-Math.abs(compoundGroups.get(i).quantIon-compoundGroups.get(counter).quantIon))<.01 
						&& compoundGroups.get(i).targetInFWHM(compoundGroups.get(counter),0.5)
						&& compoundGroups.get(i).quantPolarity.equals(compoundGroups.get(counter).quantPolarity))
				{
					//Remove the higher mass group
					if(compoundGroups.get(i).quantIon>compoundGroups.get(counter).quantIon)
					{
						if (compoundGroups.get(i).finalLipidID == null)
						{
							compoundGroups.get(i).keep = false;
							compoundGroups.get(i).filterReason = "M+1 isotope";
						}
					}
					else if(compoundGroups.get(i).quantIon<compoundGroups.get(counter).quantIon)
					{
						if (compoundGroups.get(counter).finalLipidID == null)
						{
							compoundGroups.get(counter).keep = false;
							compoundGroups.get(counter).filterReason = "M+1 isotope";
						}
					}
				}

				if (adductFiltering)
				{
					//Remove adducts against identified peaks
					if (compoundGroups.get(i).quantIon != null & compoundGroups.get(counter).quantIon!= null)
					{
						//Check adduct for cg 1 as reference
						if ((compoundGroups.get(i).targetInFWHM(compoundGroups.get(counter),0.5) && 
								util.checkAdductKnown(adductsDB, compoundGroups.get(i).quantIon,compoundGroups.get(counter).quantIon, 
										compoundGroups.get(i).finalLipidID))
										||
										(compoundGroups.get(i).targetInFWHM(compoundGroups.get(counter),0.5) && 
												util.checkAdductKnown(adductsDB, compoundGroups.get(counter).quantIon,compoundGroups.get(i).quantIon, 
														compoundGroups.get(counter).finalLipidID)))		
						{
							//Remove the unidentified compound groups
							if(compoundGroups.get(i).finalLipidID==null && compoundGroups.get(counter).finalLipidID != null)
							{
								if (compoundGroups.get(i).finalLipidID == null)
								{
									compoundGroups.get(i).keep = false;
									compoundGroups.get(i).filterReason = "Adduct of existing peak";
								}
							}
							else if(compoundGroups.get(i).finalLipidID!=null && compoundGroups.get(counter).finalLipidID == null)
							{
								if(compoundGroups.get(counter).finalLipidID == null)
								{
									compoundGroups.get(counter).keep = false;
									compoundGroups.get(counter).filterReason = "Adduct of existing peak";
								}
							}
						}
					}

					//Remove adducts against unidentified peaks
					if (compoundGroups.get(i).targetInFWHM(compoundGroups.get(counter),0.5) &&
							compoundGroups.get(i).quantIon != null & compoundGroups.get(counter).quantIon!= null
							&& compoundGroups.get(i).finalLipidID==null && compoundGroups.get(counter).finalLipidID==null)
					{
						//Check adduct
						if (compoundGroups.get(i).targetInFWHM(compoundGroups.get(counter),0.5) && 
								util.checkAdductUnknown(adductsDB, compoundGroups.get(i).quantIon,compoundGroups.get(counter).quantIon))	
						{
							//Remove the lower intensity compound group
							if(compoundGroups.get(i).maxArea > compoundGroups.get(counter).maxArea)
							{
								compoundGroups.get(counter).keep = false;
								compoundGroups.get(counter).filterReason = "Adduct of existing peak";
							}
							else if(compoundGroups.get(i).maxArea < compoundGroups.get(counter).maxArea)
							{
								compoundGroups.get(i).keep = false;
								compoundGroups.get(i).filterReason = "Adduct of existing peak";
							}
						}
					}
				}

				//Remove in-source fragments
				if (inSourceFiltering)
				{
					if (compoundGroups.get(i).quantIon != null & compoundGroups.get(counter).quantIon!= null)
					{
						if (compoundGroups.get(i).targetInFWHM(compoundGroups.get(counter),0.5) && 
								util.checkFragment(compoundGroups.get(i).finalLipidID,compoundGroups.get(counter).quantIonArray)
								&& compoundGroups.get(i).quantPolarity.equals(compoundGroups.get(counter).quantPolarity))
						{
							compoundGroups.get(counter).keep = false;
							compoundGroups.get(counter).filterReason = "In-source fragment";
						}
						else if (compoundGroups.get(i).targetInFWHM(compoundGroups.get(counter),0.5) && 
								util.checkFragment(compoundGroups.get(counter).finalLipidID,compoundGroups.get(i).quantIonArray)
								&& compoundGroups.get(i).quantPolarity.equals(compoundGroups.get(counter).quantPolarity))
						{
							compoundGroups.get(i).keep = false;
							compoundGroups.get(i).filterReason = "In-source fragment";
						}
					}
				}


				//Remove Dimers
				if (adductFiltering)
				{
					if (compoundGroups.get(i).quantIon != null & compoundGroups.get(counter).quantIon!= null)
					{
						if (compoundGroups.get(i).targetInFWHM(compoundGroups.get(counter),0.5) && 
								util.checkDimer(adductsDB, compoundGroups.get(i).quantIon,compoundGroups.get(counter).quantIon,
										compoundGroups.get(i).quantPolarity))
						{
							//Remove higher mass
							if(compoundGroups.get(i).quantIon>compoundGroups.get(counter).quantIon)
							{
								if (compoundGroups.get(i).finalLipidID == null)
								{
									compoundGroups.get(i).keep = false;
									compoundGroups.get(i).filterReason = "Dimer";
								}
							}
							else
							{
								if(compoundGroups.get(counter).finalLipidID == null)
								{
									compoundGroups.get(counter).keep = false;
									compoundGroups.get(counter).filterReason = "Dimer";
								}
							}
						}
					}
				}

				//If the same identification
				if (compoundGroups.get(i).finalLipidID != null && compoundGroups.get(counter).finalLipidID != null && 
						compoundGroups.get(i).targetInFWHM(compoundGroups.get(counter),0.823))
				{
					//If sum identifications are the same pick the one with the maximum area
					if (compoundGroups.get(i).finalLipidID.sumLipidName.equals(compoundGroups.get(counter).finalLipidID.sumLipidName))
					{
						//Populate candidates array
						candidates = new ArrayList<Lipid>();
						candidates.add(compoundGroups.get(i).finalLipidID);
						candidates.add(compoundGroups.get(counter).finalLipidID);

						//Sort candidates array
						Collections.sort(candidates);

						//Find Max Area and inactivate lower area, merge identifications
						if (compoundGroups.get(i).maxArea > compoundGroups.get(counter).maxArea)
							compoundGroups.get(i).mergeCompoundGroup(compoundGroups.get(counter));
						else
							compoundGroups.get(counter).mergeCompoundGroup(compoundGroups.get(i));
					}
				}
				counter --;
			}
		}
	}
}
