package compound_discoverer;

import lib_gen.Adduct;
import lib_gen.CustomException;
import peak_finder.PurityResult;
import peak_finder.Sample;
import peak_finder.Lipid;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JProgressBar;

import compound_discoverer.CDAreaResult;
import compound_discoverer.CDCompound;
import compound_discoverer.CDCompoundGroup;
import compound_discoverer.CDFeature;

import peak_finder.Utilities;

public class CDPeakFinder extends Utilities
{
	//Constants
	public Double MINPPMDIFF = 20.0; 			//Max mass differece in ppm for feature association
	public Double MINRTMULTIPLIER = 1.0;		//Minimum fwhm multiplier for ID association
	public Double MINDOTPRODUCT = 500.0;		//Minimum dot product for spectral matching
	public Double MINREVDOTPRODUCT = 700.0;		//Minimum reverse dot product for spectral matching
	public Double MINFAPURITY = 75.0;			//Minimum lipid spectral purity for molecular composition assignment
	public Integer MINIDNUM = 1;				//Minimum number of compounds per compound group

	public static Utilities util = new Utilities();											//Utilities object
	public JProgressBar progressBar = null; 												//Progress Bar for Quant Tab
	public JLabel ProgressStatus = null;													//Status
	static ArrayList<CDCompoundGroup> compoundGroups = new ArrayList<CDCompoundGroup>();	//Array of all compound groups
	static ArrayList<Sample> samples = new ArrayList<Sample>();								//Array of all samples
	static int[] cGIndexArray = new int[3];													//Array of CD CG header indeces							
	static int[] cIndexArray = new int[8];													//Array of CD C header indeces
	static int[] fGIndexArray = new int[10];												//Array of CD feature header indeces
	static int cdAreaStart = 99999;															//Area for result parsing
	static ArrayList<Lipid> importedLipids = new ArrayList<Lipid>();						//Array of all imported lipid IDs
	static ArrayList<Lipid> unassignedLipids = new ArrayList<Lipid>();						//Array of unassigned lipid IDs
	static ArrayList<ArrayList<Integer>> cgIndexMap;										//Hash table for compound groupds
	ArrayList<File> resultFiles;															//Array of MS2 result files
	ArrayList<Integer> samplePairNumbers;													//Array of sample pairs for separate polarity runs
	String csvFile;																			//Filename for .csv of aligned peak table
	String featureFileString;																//Filename for .csv of unaligned feature table
	boolean rtFilter;																		//Array of sample pairs for separate polarity runs
	double fwhmSum = 0.0;																	//Sum of all peak FWHMs
	double avgFWHM = 0.0;																	//Average of all peak FWHMs
	int progressInt = 0;																	//Progress for progress bar
	int numFeatures = 0;																	//Total number of features loaded
	static ArrayList<Adduct> adductsDB;														//ArrayList of all adducts from active lib

	//CD PeakFinder constructor
	public CDPeakFinder(String csvFile, String featureFileString, ArrayList<File> resultFiles, int minFeatureCount, 
			boolean rtFilter, double rtFilterMult, JProgressBar progressBar, ArrayList<Integer> samplePairNumbers, 
			ArrayList<Adduct> adductsDB)
	{
		CDPeakFinder.adductsDB = adductsDB;
		this.progressBar = progressBar;
		this.csvFile = csvFile;
		this.resultFiles = resultFiles;
		this.featureFileString = featureFileString;
		this.MINIDNUM = minFeatureCount;
		this.rtFilter = rtFilter;
		this.MINRTMULTIPLIER = rtFilterMult;
		this.samplePairNumbers = samplePairNumbers;
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

	//Run lipid quantitation
	public void runQuantitation(boolean separatePolarities, 
			boolean adductFiltering, boolean inSourceFiltering) throws IOException, CustomException
	{
		//TODO:
		compoundGroups.clear();
		samples.clear(); 
		importedLipids.clear(); 
		unassignedLipids.clear(); 

		//Import results from compound discoverer
		importCDResults(csvFile,MINIDNUM, separatePolarities);
		
		//Sort all CG's by retention time
		Collections.sort(compoundGroups);
		updateProgress(2,0,"% - Importing Feature List");

		//Create CG index hash array
		createIndexMap(compoundGroups.get(compoundGroups.size()-1).retention);
		
		//Import unalligned feature list 
		importFeatures(featureFileString);
		
		//Update avg. rt deviation values 
		calculateAvgRTDev();
		
		//Interpolate real retention times of unmatched features
		fillRTGaps();
		
		//Find Isobaric neighbors
		findIsobaricFeatures();
		
		//Find coeluting peaks 
		findClosestPeak();
		
		//Load spectral match IDs
		loadIDS(resultFiles, separatePolarities); 
	
		//Associate LB Results to Features
		associateIDSHashMap(0.5);

		//printAssociations();

		//Filter IDs by dot product
		filterFeatureLipidsByDotProduct();

		//Find best ID
		findBestID();

		//Filter results for duplicates and dimers
		filterResults(adductFiltering, inSourceFiltering);

		//Filter IDs by RT
		if (rtFilter) checkClassRTDist(MINRTMULTIPLIER);

		//Write filtering reasons
		writeFilterDB(new File(csvFile).getParent()+"\\Filtered_Results.csv");

		//Calculate Statistics
		calculateStatistics();

		//Associate MS2s
		//checkMS2Sampling("C:\\Users\\Alicia\\Desktop\\FPS_H3K");

		//Merge polarity results
		if (separatePolarities) mergePolarities();

		//Write Results;
		writeDB(new File(csvFile).getParent()+"\\Final_Results.csv");
		//writeUnassignedLipids(rawFolder+"\\Unassigned_Spectra.csv");
		writeStats(new File(csvFile).getParent()+"\\Sample_Information.csv");
	}

	//Parse Compound Discoverer headers
	private void parseHeaders(int checkedCount, String line)
	{
		String[] split = line.split(",");
		ArrayList<String> cGArray = new ArrayList<String>(Arrays.asList("Molecular Weight", "RT [min]", "Area"));
		ArrayList<String> cArray = new ArrayList<String>(Arrays.asList("Molecular Weight","RT [min]","FWHM [min]",
				"Max. # MI","# Adducts","Area","Study File ID","Best Compound"));
		ArrayList<String> fArray = new ArrayList<String>(Arrays.asList("Ion","Charge","Molecular Weight","m/z",
				"RT [min]","FWHM [min]","# MI","Area","Parent Area [%]","Study File ID"));

		//Find indeces for all compound group headers
		if (checkedCount == 1)
		{
			for (int i=0; i<split.length; i++)
			{
				for (int j=0; j<cGArray.size(); j++)
				{
					if (split[i].contains(cGArray.get(j)) && !split[i].contains(":"))
					{
						cGIndexArray[j] = i;
					}
					if (split[i].contains(":") && i < cdAreaStart) cdAreaStart = i;
				}
			}
		}
		//Find indeces for all compound headers
		else if (checkedCount == 2)
		{
			for (int i=0; i<split.length; i++)
			{
				for (int j=0; j<cArray.size(); j++)
				{
					if (split[i].equals(cArray.get(j)))
					{
						cIndexArray[j] = i;
					}
				}
			}
		}
		//Find indeces for all feature headers
		else
		{
			for (int i=0; i<split.length; i++)
			{
				for (int j=0; j<fArray.size(); j++)
				{
					if (split[i].equals(fArray.get(j)))
					{
						fGIndexArray[j] = i;
					}
				}
			}
		}
	}

	//Import Compound Discovere result tables
	private void importCDResults(String fileString, int minFeatureCount, boolean separatePolarities) throws IOException, CustomException
	{
		String line = "";							//String for storing currently read line
		CDCompoundGroup compoundGroupTemp = null;	//Temp compound group object
		CDCompound compoundTemp = null;				//Temp compound object
		int groupCount = 0;							//Number of compound groups
		int compCount = 0;							//Number of compounds
		int checkedCount = 0;						//Spreadsheet level

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
			try
			{
				if (line.contains("Checked"))
				{
					checkedCount ++;
				}

				//Parse header indeces
				if (line.contains("Checked") && checkedCount <= 3)
				{
					parseHeaders(checkedCount,line);
				}

				//Parse Samples
				if (line.contains("Name"))
				{
					samples = parseFiles(line,separatePolarities);
				}

				//import line if it does not contain header
				if (!line.contains("Checked") && !line.contains("Name") && !line.equals(""))
				{
					//Parse feature
					if (line.substring(0, 2).equals(",,")) 
						compoundTemp.addFeature(parseFeature(line),separatePolarities);

					//Parse compound
					else if(line.substring(0,1).equals(","))
					{
						if (compCount>0)
							compoundGroupTemp.addCompound(compoundTemp);
						compoundTemp = parseCompound(line);
						compCount++;
					}

					//Parse compound group
					else
					{
						//Add in compound to compoundGroup
						if (compCount>0)
						{
							//Update 
							compoundGroupTemp.addCompound(compoundTemp);
							compCount = 0;
						}
						//Add previous compound if new group found
						if (groupCount>0 && compoundGroupTemp.compounds.size() >= minFeatureCount) 
							compoundGroups.add(compoundGroupTemp);

						//Parse Line
						compoundGroupTemp = parseCG(line);
						groupCount++;

						//Parse area results
						parseAreas(compoundGroupTemp, line);
					}
				}
			}
			catch (Exception e)
			{
				reader.close();
				throw new CustomException("Error reading peak table, please check formatting", e);
			}
		}

		reader.close();

		//Add final compound group
		compoundGroupTemp.addCompound(compoundTemp);
		if (compoundGroupTemp.compounds.size() >= minFeatureCount) 
			compoundGroups.add(compoundGroupTemp);

		//Calculate average FWHM and quant Ion and look for max Isotope number
		for (int i=0; i<compoundGroups.size(); i++)
		{

			updateProgress(2,(int)(Double.valueOf(i+1)
					/Double.valueOf(compoundGroups.size())*100.0),"% - Importing Peak Data");

			compoundGroups.get(i).calcFWHM();
			fwhmSum += compoundGroups.get(i).avgFWHM;
			compoundGroups.get(i).findQuantIon();
			compoundGroups.get(i).findMaxIsotopeNumber();
		}
		avgFWHM = fwhmSum/compoundGroups.size();
	}

	//Import unaligned features from .csv and associate with aligned features
	private void importFeatures(String filename) throws IOException, CustomException
	{
		String line = "";			//Currently read line
		String[] split;				//Array for line parsing
		Double mass = 0.0;			//Precursor mass
		Double retention = 0.0;		//Apex etention time
		String polarity = "";		//Polarity
		Double area = 0.0;			//Area
		String sample = "";			//Sample name
		int massIndex = -1;			//Index for line parsing
		int retentionIndex = -1;	//Index for line parsing
		int areaIndex = -1;			//Index for line parsing
		int sampleIndex = -1;		//Index for line parsing
		int adductIndex = -1;		//Index for line parsing
		int counter = 0;			//Current feature number	

		//Update Status
		updateProgress(2,0,"% - Importing Feature Data");

		//Create file buffer
		File file = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(file));

		//Check if file exists
		if (!file.exists())
		{
			reader.close();
			throw new CustomException(filename+" does not exist", null);
		}

		//Count number of features
		numFeatures = countLines(filename);

		//read line if not empty
		while ((line = reader.readLine()) != null)
		{
			try
			{
				//If a header line
				if (line.contains("Checked"))
				{
					split = line.split(",");

					//Find header indeces
					for (int i=0; i<split.length; i++)
					{
						if (split[i].equals("m/z")) massIndex = i;
						else if (split[i].contains("RT")) retentionIndex = i;
						else if (split[i].equals("Area")) areaIndex = i;
						else if (split[i].contains("File ID")) sampleIndex = i;
						else if (split[i].contains("Ion")) adductIndex = i;
					}
				}
				else
				{
					//TODO: add to mzmine
					//Update progress
					counter ++;
					updateProgress(2,(int)((Double.valueOf(counter)
							/numFeatures)*100.0),"% - Importing Feature Data");

					//Split by commas
					split = line.split(",");

					//Populate feature info
					mass = Double.valueOf(split[massIndex]);
					retention = Double.valueOf(split[retentionIndex]);
					area = Double.valueOf(split[areaIndex]);
					sample = split[sampleIndex];
					if (split[adductIndex].contains("]+")) polarity = "+";
					else polarity = "-";

					//Iterate through all compound groups and query
					matchUnalignedFeatureToCompound(mass, retention, polarity, area, sample);
				}
			}
			catch (Exception e)
			{
				reader.close();
				throw new CustomException ("Error reading feature peak table.  Please check formatting", e);
			}
		}

		updateProgress(2,100,"% - Importing Feature Data");

		reader.close();
	}

	//Match unaligned features to aligned features
	private void matchUnalignedFeatureToCompound(Double mass, Double retention, 
			String polarity, Double area, String sample)
	{
		int minIndex;										//minimum hash map index
		int maxIndex; 										//maximum hash map index
		int cgIndex;										//Key for hash map

		//Find min and max index for CG hash map
		minIndex = calculateMapIndex(retention-avgFWHM*3.0);

		if (minIndex<0) minIndex = 0;
		else if (minIndex>cgIndexMap.size()) minIndex = cgIndexMap.size()-2;

		maxIndex = calculateMapIndex(retention+avgFWHM*3.0);

		if (maxIndex>cgIndexMap.size()) maxIndex = cgIndexMap.size()-1;

		//For all compound groups withing 3xFWHM apex RT
		for (int j=minIndex; j<maxIndex; j++)
		{
			for (int k=0; k<cgIndexMap.get(j).size(); k++)
			{
				cgIndex = cgIndexMap.get(j).get(k);

				//Check for matching feature
				if (Math.abs(compoundGroups.get(cgIndex).retention-retention)<2.0 
						&& calcPPMDiff(mass, compoundGroups.get(cgIndex).quantIon)<MINPPMDIFF) //TODO: Add to mzmine
				{
					//If matching feature found, update actual RT value and break iteration to save time
					if (compoundGroups.get(cgIndex).matchUnalignedFeature(mass, retention, polarity, area, sample))
					{
						break;
					}
				}
			}
		}
	}

	//Parse compound group line and return new compound group object
	private CDCompoundGroup parseCG(String line)
	{
		CDCompoundGroup result;
		String[] split;

		//Split line
		split = line.split(",");

		//Parse line
		result = new CDCompoundGroup(Double.valueOf(split[cGIndexArray[0]]),
				Double.valueOf(split[cGIndexArray[1]]),Double.valueOf(split[cGIndexArray[2]]));

		return result;
	}

	//Return matching Sample object based on file ID number from compound discoverer
	private Sample matchSample(String name)
	{
		return samples.get(Integer.valueOf(name.substring(name.indexOf("F")+1))-1);
	}

	//Parse compound line and return new compound object
	private CDCompound parseCompound(String line)
	{
		CDCompound result;
		String[] split;

		//Split line
		split = line.split(",");

		//Create new compound object
		result = new CDCompound(Double.valueOf(split[cIndexArray[0]]),Double.valueOf(split[cIndexArray[1]]),
				Double.valueOf(split[cIndexArray[2]]), Integer.valueOf(split[cIndexArray[3]]), Integer.valueOf(split[cIndexArray[4]]),
				Double.valueOf(split[cIndexArray[5]]), matchSample(split[cIndexArray[6]]), Boolean.valueOf(split[cIndexArray[7]]));

		return result;
	}

	//Return array of Sample object based on file header from compound discoverer results
	private ArrayList<Sample> parseFiles(String line, boolean separatePolarities)
	{
		ArrayList<Sample> result = new ArrayList<Sample>();
		String[] split;

		//Split line
		split = line.split(",");

		//Add new samples
		for (int i=0; i<split.length; i++)
		{
			if (split[i].contains("Area:"))
			{
				result.add(new Sample(split[i].substring(split[i].indexOf(": ")+2),-1,separatePolarities));
			}
		}
		return result;
	}

	//Return the sample pair number for a files name based on user input
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

	//Merge polarities for each compound group when non-polarity switching methods used
	private void mergePolarities()
	{
		for (int i=0; i<compoundGroups.size(); i++)
		{
			compoundGroups.get(i).mergePolarities();
		}
	}

	//Parse feature line and return a feature object
	private CDFeature parseFeature(String line)
	{
		CDFeature result;
		String[] split;

		split = line.split(",");

		result = new CDFeature(split[fGIndexArray[0]], Integer.valueOf(split[fGIndexArray[1]]), 
				Double.valueOf(split[fGIndexArray[2]]), Double.valueOf(split[fGIndexArray[3]]), 
				Double.valueOf(split[fGIndexArray[4]]), Double.valueOf(split[fGIndexArray[5]]), 
				Integer.valueOf(split[fGIndexArray[6]]), Double.valueOf(split[fGIndexArray[7]]),
				Double.valueOf(split[fGIndexArray[8]]), samples.get(
						Integer.valueOf(split[fGIndexArray[9]].substring(split[fGIndexArray[9]].indexOf("F")+1))-1));

		return result;
	}

	//Parse all peak area results for each compound group and store in object
	private void parseAreas(CDCompoundGroup temp, String line)
	{
		String[] split;
		int j=0;

		//Split line
		split = line.split(",");

		//For all area in CG line, add new AreaResult object
		for (int i=cdAreaStart; i<split.length; i++)
		{
			if (split[i].equals(""))
			{
				temp.addResult(new CDAreaResult(samples.get(j), 0.0));
				j++;
			}
			else
			{
				temp.addResult(new CDAreaResult(samples.get(j), Double.valueOf(split[i])));
				j++;
			}	
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

								if (lipidTemp.dotProduct>MINDOTPRODUCT)
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
		ArrayList<CDCompoundGroup> tempGroupArray = new ArrayList<CDCompoundGroup>();

		//Iterate through all compound groups sorted by retention time
		for (int i=0; i<compoundGroups.size(); i++)
		{
			//Update progress bar
			updateProgress(2,(int)(Double.valueOf(i+1)
					/Double.valueOf(compoundGroups.size())*100.0),"% - Finding Coeluting Peaks");

			//Clear array
			tempGroupArray = new ArrayList<CDCompoundGroup>();

			//For all compound groups
			for (int j=0; j<compoundGroups.size(); j++)
			{
				//If CG is within one minute, add to temporary array
				if (Math.abs(compoundGroups.get(i).retention-compoundGroups.get(j).retention)<0.5)
					tempGroupArray.add(compoundGroups.get(j));
			}

			//For all CGs withing one minute
			for (int j=0; j<tempGroupArray.size(); j++)
			{
				//For all compounds
				for (int k=0; k<tempGroupArray.get(j).compounds.size(); k++)
				{
					//For all features
					for (int l=0; l<tempGroupArray.get(j).compounds.get(k).features.size(); l++)
					{
						//For each feature, find those peaks which are within global ppm tolerance
						//If withing RT and ppm tolerance, add to feature neighbor array in feature object
						compoundGroups.get(i).checkIsobaricFeature(tempGroupArray.get(j).compounds.get(k).features.get(l));
					}
				}
			}
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
			for (int j=0; j<compoundGroups.get(i).compounds.size(); j++)
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

	//After re-calculating RT alignment, calculate actual rt for any features which could not be matched
	private void fillRTGaps()
	{
		for (int i=0; i<compoundGroups.size(); i++)
		{
			//Update progress bar
			updateProgress(2,(int)(Double.valueOf(i+1)
					/Double.valueOf(compoundGroups.size())*100.0),"% - Filling Retention Time Gaps");

			//Iterate through all compounds
			for (int j=0; j<compoundGroups.get(i).compounds.size(); j++)
			{
				//Iterate through all features
				for (int k=0; k<compoundGroups.get(i).compounds.get(j).features.size(); k++)
				{
					//If actual retention time has not been found, calculate
					if (compoundGroups.get(i).compounds.get(j).features.get(k).realRetention.equals(0.0))
					{
						compoundGroups.get(i).compounds.get(j).features.get(k).realRetention = 
								compoundGroups.get(i).compounds.get(j).features.get(k).sample.findRTOffset
								(compoundGroups.get(i).compounds.get(j).features.get(k).retention)+
								compoundGroups.get(i).compounds.get(j).features.get(k).retention;
					}
				}
			}
		}
	}

	//Method to calculate the acerage RT deviation for each compound group
	private void calculateAvgRTDev()
	{
		//Calculate RTDev for all compound groups
		for (int i=0; i<compoundGroups.size(); i++)
		{
			compoundGroups.get(i).calculateCGRTDev();
		}

		//Create moving average curve for sampels
		for (int i=0; i<samples.size(); i++)
		{
			updateProgress(2,(int)(Double.valueOf(i+1)
					/Double.valueOf(samples.size())*100.0),"% - Calculating RT Deviation");
			samples.get(i).loessFit();
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
				minIndex = calculateMapIndex(importedLipids.get(i).correctedRetention-avgFWHM*3.0);

				if (minIndex<0) minIndex = 0;
				else if (minIndex>cgIndexMap.size()) minIndex = cgIndexMap.size()-2;

				maxIndex = calculateMapIndex(importedLipids.get(i).correctedRetention+avgFWHM*3.0);

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
							for (int l=0; l<compoundGroups.get(cgIndex).compounds.size(); l++)
							{
								//If RT is not within 1 minute, break loop to save iteration time
								if (Math.abs(importedLipids.get(i).correctedRetention-compoundGroups.get(cgIndex).compounds.get(l).retention)>1.0) break;

								//Iterate through all features
								for (int m=0; m<compoundGroups.get(cgIndex).compounds.get(l).features.size(); m++)
								{
									if (compoundGroups.get(cgIndex).compounds.get(l).features.get(m).checkLipid(importedLipids.get(i),compoundGroups.get(cgIndex).noCoelutingPeaks))
									{
										//Add lipid to feature
										compoundGroups.get(cgIndex).compounds.get(l).features.get(m).addLipid((importedLipids.get(i)));

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
							if (found) break;
						}
					}
					if (found) break;
				}
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

	//Filters each identification by dot product
	private void filterFeatureLipidsByDotProduct()
	{
		//For each compound group
		for (int j=0; j<compoundGroups.size(); j++)
		{
			//Update progress
			updateProgress(2,(int)(Double.valueOf(j+1)
					/Double.valueOf(compoundGroups.size())*100.0),"% - Filtering Identifications");

			//For each compound
			for (int k=0; k<compoundGroups.get(j).compounds.size(); k++)
			{
				//For each feautre
				for (int l=0; l<compoundGroups.get(j).compounds.get(k).features.size(); l++)
				{
					//Filter IDs
					compoundGroups.get(j).compounds.get(k).features.get(l).filterIDByDotProduct();
					compoundGroups.get(j).compounds.get(k).features.get(l).calculateGaussianScore();
				}
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
			for (int j=0; j<compoundGroups.get(i).compounds.size(); j++)
			{
				//Iterate through samples
				for (int k=0; k<samples.size(); k++)
				{
					//If the sample is a match
					if (samples.get(k).file.equals(compoundGroups.get(i).compounds.get(j).sample.file))
					{
						//Increment compound count and add FWHM to average
						samples.get(k).addCompound();
						samples.get(k).addFWHM(compoundGroups.get(i).compounds.get(j).fwhm);
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

			//System.out.println(compoundGroups.get(i).identification.identifications.get(0).lipidClass);

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
			classRTMinArray.add(util.findMedian(rtArray.get(i))-multiplier*util.sd(rtArray.get(i)));
			classRTMaxArray.add(util.findMedian(rtArray.get(i))+multiplier*util.sd(rtArray.get(i)));
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
					+ "Peak Capacity,Identified Lipids"
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


	//Finds all compound groups which have been samples for MS2 analysis, for development use
	@SuppressWarnings("unused")
	private void checkMS2Sampling(String folder) throws IOException
	{
		String line = null;
		String mgfFolder = folder;
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
				File file = new File(new String(samples.get(i).filename.replace("_Results.csv",".mgf")));

				BufferedReader reader = new BufferedReader(new FileReader(file));

				//read line if not empty
				while ((line = reader.readLine()) != null)
				{

					//read in retention time
					if (line.contains("RTINSECONDS"))
						retention_mgf = Double.valueOf(line.substring(line.lastIndexOf("=")+1))/60.0;

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
							precursor_mgf = Double.valueOf(line.substring(line.lastIndexOf("=")+1,line.lastIndexOf(" ")));
						else
							precursor_mgf = Double.valueOf(line.substring(line.lastIndexOf("=")+1));
					}

					if (line.contains("END IONS"))
					{

						//Look for rt match and precursor match, if found, associate with compound group
						for (int j=0; j<compoundGroups.size(); j++)
						{
							//If RT is not within 1 minute, break loop to save iteration time
							if (Math.abs(retention_mgf-compoundGroups.get(j).retention)<1.0 && !compoundGroups.get(j).ms2Sampled)
							{
								//Iterate through all compounds
								for (int k=0; k<compoundGroups.get(j).compounds.size(); k++)
								{
									//Iterate through all features
									for (int l=0; l<compoundGroups.get(j).compounds.get(k).features.size(); l++)
									{
										if (compoundGroups.get(j).compounds.get(k).features.get(l).checkFeature(precursor_mgf, retention_mgf, polarity, samples.get(i)))
										{
											compoundGroups.get(j).ms2Sampled = true;
										}
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


	/*
	 * Filters all identifications by removing coeluting identifications with same sum ID,
	 * removing dimers, and removing misidentified isotopes
	 */
	private void filterResults(boolean adductFiltering, boolean inSourceFiltering)
	{
		int counter = 0;
		ArrayList<Lipid> candidates;

		//Iterate through all compound groups
		for (int i=1; i<compoundGroups.size(); i++)
		{
			updateProgress(2,(int)(Double.valueOf(i+1)
					/Double.valueOf(compoundGroups.size())*100.0),"% - Filtering Results");

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
						if ((compoundGroups.get(i).targetInFWHM(compoundGroups.get(counter),0.5) && 
								util.checkFragment(compoundGroups.get(i).finalLipidID,compoundGroups.get(counter).quantIon)
								&& compoundGroups.get(i).quantPolarity.equals(compoundGroups.get(counter).quantPolarity))
								||
								(compoundGroups.get(i).targetInFWHM(compoundGroups.get(counter),0.5) && 
										util.checkFragment(compoundGroups.get(counter).finalLipidID,compoundGroups.get(i).quantIon)
										&& compoundGroups.get(i).quantPolarity.equals(compoundGroups.get(counter).quantPolarity)))
						{
							//Remove the unidentified compound groups
							if(compoundGroups.get(i).finalLipidID==null && compoundGroups.get(counter).finalLipidID != null)
							{
								if (compoundGroups.get(i).finalLipidID == null)
								{
									compoundGroups.get(i).keep = false;
									compoundGroups.get(i).filterReason = "In-source fragment";
								}
							}
							else if(compoundGroups.get(i).finalLipidID!=null && compoundGroups.get(counter).finalLipidID == null)
							{
								if(compoundGroups.get(counter).finalLipidID == null)
								{
									compoundGroups.get(counter).keep = false;
									compoundGroups.get(counter).filterReason = "In-source fragment";
								}
							}
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

	//Returns the number of lines in a text file.  Used for progress bar 
	public int countLines(String filename) throws IOException 
	{
		InputStream is = new BufferedInputStream(new FileInputStream(filename));
		try {
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;
			while ((readChars = is.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}
			return (count == 0 && !empty) ? 1 : count;
		} finally {
			is.close();
		}
	}
}
