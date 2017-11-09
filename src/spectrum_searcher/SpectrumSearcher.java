package spectrum_searcher;

import spectrum_searcher.LibrarySpectrum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collections;

import lib_gen.CustomError;
import lib_gen.CustomException;
import lib_gen.TransitionType;

import javax.swing.JProgressBar;

import lib_gen.FattyAcid;

public class SpectrumSearcher 
{
	private  ArrayList <String> mgfFiles; 						//Array of mgf files to analyze
	private  ArrayList <String> mzxmlFiles; 					//Array of mzxml files to analyze
	private  ArrayList <File> lipidLibs;					    //Array of libraries to search
	static ArrayList<FattyAcid> fattyAcidsDB;					//Array of fatty acid objects
	private  ArrayList<LibrarySpectrum> librarySpectra;			//All library spectra
	private  Double ms1Tol;										//MS1 search tolerance in Th
	private   Double ms2Tol;									//MS2 search tolerance in Th
	private  Double massBinSize;								//Size of each mass bin
	private int maxResults;										//Maximum number of search results returned
	private static JProgressBar progressBar;					//Progress bar from SpectrumSearcher GUI to update
	private  ArrayList<SampleSpectrum> sampleMS2Spectra;		//All experimental MS2 spectra
	private  int[] countBin;									//Array which holds the number of spectra in each bin
	private  int[] addedBin; 									//Array storing the number of spectra added
	private  LibrarySpectrum[][] massBin; 						//2D array to store spectra objects
	private  Double maxMass;									//Highest precursor mass from positive spectra
	private  Double minMass;									//Highest precursor mass from positive spectra
	Double intWeight = 0.5;										//Weight for dot product calculations
	Double massWeight = 1.0;									//Weight for dot product calculations
	Double minMS2Mass = 60.0;									//Minimum mass for spectral searching
	static ArrayList<TransitionType> transitionTypes; 			//Arraylist of all class adduct combos from active library
	static ArrayList<String> transitionTypeStrings = 
			new ArrayList<String>();							//Arraylist of all types of ms2 transitions possible
	long timeStart;
	long timeStop;
	int numSpectra;
	static int progressInt = 0;

	//Creates a new instance of SpectrumSearcher
	public SpectrumSearcher(ArrayList <File> lipidLibs, ArrayList <String> mgfFiles, 
			ArrayList <String> mzxmlFile, JProgressBar progressBar, Double ms1Tol, 
			Double ms2Tol, int maxResults, Double minMassCutoff)
	{
		//Update class fields
		this.minMS2Mass = minMassCutoff;
		this.lipidLibs = lipidLibs;
		SpectrumSearcher.progressBar = progressBar;
		this.ms1Tol = ms1Tol;
		this.ms2Tol = ms2Tol;
		this.mgfFiles = mgfFiles;
		this.mzxmlFiles = mzxmlFile;
		massBinSize = ms1Tol/10.0;
		minMass = 999.0;
		maxMass = 0.0;
		librarySpectra = new ArrayList<LibrarySpectrum>();
		fattyAcidsDB = new ArrayList<FattyAcid>();
		sampleMS2Spectra = new ArrayList<SampleSpectrum>();
		this.maxResults = maxResults;
		numSpectra = 0;
	}

	//Method to populate hash array and search all spectra
	public  void runSpectraSearch(ArrayList <File> lipidLibs) throws CustomException, IOException
	{
		this.lipidLibs = lipidLibs;

		//Verifies no result files are open
		checkFileStatus();

		//Read fatty acids
		readFattyAcids("src\\backup\\FattyAcids.csv");

		//Read in MSP Files
		try
		{
			readMSP();
		}
		catch (Exception e)
		{
			CustomError ce = new CustomError("Error loading library .msp", e);
		}

		//Sort arrays by mass, lowest to highest
		Collections.sort(librarySpectra);
		updateProgress(100,"% - Sorting Libraries",true);

		//Bin MSP LibrarySpectra
		binMasses();

		updateProgress(0,"% - Searching Spectra",true);

		//Read in all mgf files
		for (int i=0; i<mgfFiles.size(); i++)
		{
			timeStart = System.nanoTime();
			numSpectra = 0;

			//Read in spectra and search
			readMGF(mgfFiles.get(i));

			//Iterate through spectra and match
			for (int j=0; j<sampleMS2Spectra.size(); j++)
			{
				matchLibrarySpectra(sampleMS2Spectra.get(j), massBinSize, ms1Tol, ms2Tol);
			}

			//Calculate purity values
			for (int j=0; j<sampleMS2Spectra.size(); j++)
			{			
				sampleMS2Spectra.get(j).calcPurityAll(fattyAcidsDB);
			}

			//Write ouput files for mgf
			try
			{
				writeResults(sampleMS2Spectra,mgfFiles.get(i),maxResults);
			}
			catch (IOException e)
			{
				CustomError ce = new CustomError("Please close "+mgfFiles.get(i)+" and re-search the data",null);
			}

			//Clear sample spectra
			sampleMS2Spectra = new ArrayList<SampleSpectrum>();

			updateProgress((int)((Double.valueOf(i+1)
					/(Double.valueOf(mgfFiles.size())))*100.0),"% - Searching Spectra",true);
		}

		//Create mzxmlparser
		MZXMLParser parser = new MZXMLParser();

		//Read in mzxml files
		for (int i=0; i<mzxmlFiles.size(); i++)
		{
			timeStart = System.nanoTime();
			numSpectra = 0;

			//Parse MZXML
			parser.readFile(mzxmlFiles.get(i));

			//Create SampleSpectrum objects
			for (int j=0; j<parser.sampleSpecArray.size(); j++)
			{
				sampleMS2Spectra.add(parser.sampleSpecArray.get(j));
			}

			//Iterate through spectra and match
			for (int j=0; j<sampleMS2Spectra.size(); j++)
			{
				matchLibrarySpectra(sampleMS2Spectra.get(j), massBinSize, ms1Tol, ms2Tol);				
			}

			//Calculate purity values
			for (int j=0; j<sampleMS2Spectra.size(); j++)
			{			
				sampleMS2Spectra.get(j).calcPurityAll(fattyAcidsDB);
			}

			//Write ouput files for mzxml
			writeResults(sampleMS2Spectra,mzxmlFiles.get(i),maxResults);

			//Clear sample spectra
			sampleMS2Spectra = new ArrayList<SampleSpectrum>();

			updateProgress((int)((Double.valueOf(i+1)
					/(Double.valueOf(mgfFiles.size())))*100.0),"% - Searching Spectra",true);
		}
		updateProgress(100,"% - Completed",true);
	}

	//Throws exception if a file is open
	private void checkFileStatus() throws CustomException, IOException
	{
		//For all .mgf files
		for (int i=0; i<mgfFiles.size(); i++)
		{

			String resultFileName = 
					mgfFiles.get(i).substring(0,mgfFiles.get(i).lastIndexOf("."))+"_Results.csv";
			//TODO: Cannot write files if checking for file.  
			/*
			if (isFileUnlocked(resultFileName))
				throw new CustomException("Please close "+resultFileName, null);
			 */
		}
	}

	@SuppressWarnings("resource")
	private boolean isFileUnlocked(String filename) throws IOException
	{
		File file = new File(filename);
		FileChannel channel = null;

		try 
		{
			channel = new RandomAccessFile(file, "rw").getChannel();
			FileLock lock = channel.lock();
			lock = channel.tryLock();
			lock.release();
			channel.close();
			return true;
		} 
		catch (Exception e) 
		{
			return false;
		}
	}

	//Method to populate hash array and search all spectra
	public  void runSpectraSearchOptimizer()
	{
		//Load in information from each library file
		try
		{
			//Read in MSP Files
			readMSP();

			//Sort arrays by mass, lowest to highest
			Collections.sort(librarySpectra);
			updateProgress(100,"% - Sorting Libraries",true);

			//Bin MSP LibrarySpectra
			binMasses();

			for (double l=0.1; l<3.1; l=l+.1)
			{
				for (double m=0.1; m<3.1; m=m+.1)
				{
					this.intWeight = l;
					this.massWeight = m;

					//Read in all mgf files
					for (int i=0; i<mgfFiles.size(); i++)
					{
						updateProgress((int)(Double.valueOf(i+1)
								/(Double.valueOf(mgfFiles.size())*100.0)),"% - Loading Spectra",true);

						timeStart = System.nanoTime();
						numSpectra = 0;

						//Read in spectra and search
						readMGF(mgfFiles.get(i));

						//Iterate through spectra and match
						for (int j=0; j<sampleMS2Spectra.size(); j++)
						{
							matchLibrarySpectra(sampleMS2Spectra.get(j), massBinSize, ms1Tol, ms2Tol);		
						}

						//Write ouput files for mgf
						writeResults(sampleMS2Spectra,mgfFiles.get(i)+"_"+l+"_"+m+".csv",maxResults);

						//Clear sample spectra
						sampleMS2Spectra = new ArrayList<SampleSpectrum>();
					}

					//Create mzxmlparser
					MZXMLParser parser = new MZXMLParser();

					//Read in mzxml files
					for (int i=0; i<mzxmlFiles.size(); i++)
					{

						timeStart = System.nanoTime();
						numSpectra = 0;

						//Parse MZXML
						parser.readFile(mzxmlFiles.get(i));

						//Create SampleSpectrum objects
						for (int j=0; j<parser.sampleSpecArray.size(); j++)
						{
							sampleMS2Spectra.add(parser.sampleSpecArray.get(j));
						}

						//Iterate through spectra and match
						for (int j=0; j<sampleMS2Spectra.size(); j++)
						{
							matchLibrarySpectra(sampleMS2Spectra.get(j), massBinSize, ms1Tol, ms2Tol);				
						}

						//Write ouput files for mzxml
						writeResults(sampleMS2Spectra,mzxmlFiles.get(i)+l+"_"+m+".csv",maxResults);

						//Clear sample spectra
						sampleMS2Spectra = new ArrayList<SampleSpectrum>();
					}
					updateProgress(100,"% - Completed",true);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void readFattyAcids(String filename) throws IOException
	{
		String line = null;
		String [] split = null;
		String name;
		String type;
		String enabled;
		String formula;
		ArrayList<String> faTypes = new ArrayList<String>();

		//Create file buffer
		File file = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(file));

		//Clear FA DB
		fattyAcidsDB = new ArrayList<FattyAcid>();

		//read line if not empty
		while ((line = reader.readLine()) != null)
		{
			if (!line.contains("Name"))
			{
				split = line.split(",");

				name = split[0];
				type = split[1];
				formula = split[2];
				enabled = split[3];

				fattyAcidsDB.add(new FattyAcid(name, type, formula, enabled));

				if (!faTypes.contains(type)) faTypes.add(type);
			}
		}

		//Create transition type objects
		transitionTypes = createTransitionTypes(faTypes);

		//Populate string array
		for (int i=0; i<transitionTypes.size(); i++)
		{
			transitionTypeStrings.add(transitionTypes.get(i).toString());
		}
		reader.close();
	}

	//Returns array of all transition type definition objects for given fatty acids
	private static ArrayList<TransitionType> createTransitionTypes(ArrayList<String> typeArray)
	{
		ArrayList<TransitionType> definitions = new ArrayList<TransitionType>();

		//Create static fragment class
		definitions.add(new TransitionType("Fragment",null,false,false,0));

		//Create static neutral loss class
		definitions.add(new TransitionType("Neutral Loss",null,false,true,0));

		//For all fatty acid type strings
		for (int i=0; i<typeArray.size(); i++)
		{
			//Create moiety fragment class
			definitions.add(new TransitionType(typeArray.get(i)+" Fragment",typeArray.get(i),true,false,1));

			//Create moiety neutral loss class
			definitions.add(new TransitionType(typeArray.get(i)+" Neutral Loss",typeArray.get(i),true,true,1));
		}

		//Create cardiolipin DG fragment class
		definitions.add(new TransitionType("Cardiolipin DG Fragment","Alkyl",true,false,2));

		//Create PUFA neutral loss class
		definitions.add(new TransitionType("PUFA Fragment","PUFA",true,false,1));

		//Create PUFA fragment class
		definitions.add(new TransitionType("PUFA Neutral Loss","PUFA",true,true,1));

		return definitions;
	}

	public void writeResults(ArrayList<SampleSpectrum> spectra, String mgfFilename, int maxResults) throws FileNotFoundException
	{
		//write results to folder where .mgf file is
		File mgfFile = new File(mgfFilename);
		String parentFolder = mgfFile.getParent();
		String resultFileName = mgfFile.getName().substring(0,mgfFile.getName().lastIndexOf("."))+"_Results.csv";

		//Check if results exists and if not create results folder in mgf file folder
		File resultsDir = new File(parentFolder);
		if (!resultsDir.exists()) resultsDir.mkdir();

		PrintWriter pw = new PrintWriter(resultsDir.toString()+"\\"+resultFileName);

		pw.println("MS2 ID,Retention Time (min),Rank,Identification,"
				+ "Precursor Mass,Library Mass,Delta m/z,Dot Product,Reverse Dot Product,Purity,Spectral Components,Optimal Polarity,LipiDex Spectrum,Library,Potential Fragments");

		for (int i=0; i<spectra.size(); i++)
		{
			if (spectra.get(i).idArray.size() > 0)
			{ 
				if(spectra.get(i).idArray.get(0).dotProduct>1)
				{
					pw.println(spectra.get(i).toString(maxResults));
				}
			}
		}

		pw.close();
	}

	/*
	 * Create array of bins in which to store objects
	 * Bins are indexed by precursor mass
	 */
	public  void createBins(int arraySize)
	{
		//Check if any spectra from each polarity has been created
		if (librarySpectra.size()>0)
		{
			countBin = new int[arraySize]; //Array to store number of positive spectra stored in each bin
			addedBin = new int[arraySize]; //Array to store number of positive spectra stored in each bin

			//Fill all positive bins with zeroes
			for (int i=0; i<countBin.length; i++)
			{
				countBin[i] = 0;
				addedBin[i] = 0;
			}

			//Create the array to store the actual spectra objects
			massBin = new LibrarySpectrum[arraySize][];
		}
	}

	//Convert a precursor mass to the correct bin number
	public  int findBinIndex(Double precursor, Double binSize, Double minMass)
	{
		return (int)((precursor-minMass)/binSize);
	}

	//Calculate the correct array size based on a mass range
	public  int calcArraySize(Double binSize, Double minMass, Double maxMass)
	{
		return (int)((maxMass - minMass)/binSize)+1;
	}

	//Calculate the difference between two masses in ppm
	public  Double calcPPMDiff(Double mass1, Double mass2)
	{
		return (Math.abs(mass1 -  mass2)/(mass2))*1000000;
	}

	//Find the minimum value of a range needed to search based on a precursor mass
	public  Double findMinPPMRange(Double mass, Double ppm)
	{
		return (mass - ((ppm/1000000.0)*mass));
	}

	//Find the maximum value of a range needed to search based on a precursor mass
	public  Double findMaxPPMRange(Double mass, Double ppm)
	{
		return (mass + ((ppm/1000000.0)*mass));
	}

	//Find the minimum value of a range needed to search based on a precursor mass
	public  Double findMinMassRange(Double mass, Double mzTol)
	{
		return (mass - mzTol);
	}

	//Find the maximum value of a range needed to search based on a precursor mass
	public  Double findMaxMassRange(Double mass, Double mzTol)
	{
		return (mass + mzTol);
	}


	//A method to bin all precursor masses
	public  void binMasses()
	{
		//Create arrays
		createBins(calcArraySize(massBinSize,minMass, maxMass));

		//Check if any spectra from each polarity has been created
		if (librarySpectra.size()>0)
		{
			//Populate count array to correctly initialize array size for positive library spectra
			for (int i=0; i<librarySpectra.size(); i++)
			{
				countBin[findBinIndex(librarySpectra.get(i).precursor,massBinSize,minMass)] ++;
			}
			updateProgress(17,"% - Creating Composite Library",true);

			//Use count bin to initialize new arrays to place positive spectra into hash table
			for (int i=0; i<countBin.length; i++)
			{
				massBin[i] = new LibrarySpectrum[countBin[i]];
			}
			updateProgress(33,"% - Creating Composite Library",true);


			//Populate spectrum arrays for positive spectra
			for (int i=0; i<librarySpectra.size(); i++)
			{
				massBin[findBinIndex(librarySpectra.get(i).precursor,massBinSize,minMass)]
						[addedBin[findBinIndex(librarySpectra.get(i).precursor,massBinSize,minMass)]] = librarySpectra.get(i);
				addedBin[findBinIndex(librarySpectra.get(i).precursor,massBinSize,minMass)]++;
			}
			updateProgress(50,"% - Creating Composite Library",true);
		}
	}

	//Search all  experimental mass spectra against libraries
	public void matchLibrarySpectra(SampleSpectrum ms2, Double massBinSize, Double ms1Tol, Double ms2Tol)
	{
		Double dotProd = 0.0;
		Double reverseDotProd = 0.0;

		if (minMass < 9998.0 && ms2.precursor<maxMass && ms2.precursor>minMass)
		{
			//Scale MS2s to maximum peak in spectra (0-1000) and remove peaks below .5%
			ms2.scaleIntensities();

			// Find range of mass bins which need to be searched
			int minIndex = findBinIndex(findMinMassRange(ms2.precursor,ms1Tol),massBinSize,minMass);
			int maxIndex = findBinIndex(findMaxMassRange(ms2.precursor,ms1Tol),massBinSize,minMass);

			if (minIndex<0) minIndex = 0;
			if (maxIndex>(massBin.length-1)) maxIndex = massBin.length-1;

			//Iterate through this mass bin range
			for (int i=minIndex; i<=maxIndex; i++)
			{
				//If the bin contains library spectra
				if (countBin[i]>0)
				{
					//For all spectra which are in the same mass bin
					for (int j=0; j<addedBin[i]; j++)
					{
						if (Math.abs(massBin[i][j].precursor-ms2.precursor)<ms1Tol)
						{
							//Calculate the dot product (spectral similarity) between the two spectra 
							dotProd = ms2.calcDotProduct(massBin[i][j].transitionArray,ms2Tol,false, massWeight, intWeight);
							reverseDotProd = ms2.calcDotProduct(massBin[i][j].transitionArray,ms2Tol,true, massWeight, intWeight);

							if (dotProd>1)
							{
								//Add identification to array
								ms2.addID(massBin[i][j], dotProd, reverseDotProd);
							}
						}
					}
				}
			}

			//Sort by dot product
			Collections.sort(ms2.idArray);
		}
	}


	//Method to read in all .msp files and add them to positive and negative arrays
	public  void readMSP() throws IOException
	{
		String line = "";						//Line for reading
		String fragType = "";					//Fragment type
		String polarity = "";					//Polarity of entry
		Double precursor = 0.0;					//Precursor mass
		String name = "";						//Lipid name
		LibrarySpectrum entryTemp = null;		//Temp library entry
		boolean peakStart = false;				//Boolean if peak list beginning
		boolean isLipidex = false;				//Boolean if spectra was generated using LipiDex
		boolean optimalPolarity = false;		//Boolean if optimal polarity for class
		String[] split;							//String array for parsing transitions

		for (int i=0; i<lipidLibs.size(); i++)
		{
			updateProgress((int)(Double.valueOf(i+1)
					/Double.valueOf(lipidLibs.size())*100.0),"% - Reading Libraries",true);

			BufferedReader reader = new BufferedReader(new FileReader(lipidLibs.get(i)));

			//read line if not empty
			while ((line = reader.readLine()) != null)
			{
				//read in retention time
				if (line.contains("Name:") || line.contains("NAME:"))
				{
					//Add entry
					if (precursor>0.0) 
						addSpectrum(entryTemp);

					//Erase entry
					polarity = "";
					precursor = 0.0;
					name = "";
					fragType = "";
					peakStart = false;
					isLipidex = false;
					optimalPolarity = false;

					if (line.contains("]+")) polarity = "+";
					else polarity = "-";

					name = line.substring(line.indexOf(":")+1);
				}

				//read in optimal polarity
				if (line.contains("OptimalPolarity=true"))
				{
					optimalPolarity = true;
				}

				if (line.contains("LipiDex")) isLipidex = true;

				if (line.contains("Num Peaks:"))
				{
					peakStart = true;
					entryTemp = new LibrarySpectrum(precursor, polarity, name, lipidLibs.get(i).getName(), isLipidex, optimalPolarity);
				}

				if (peakStart && line.contains(".") && !line.contains("Num"))
				{
					if (line.contains("	")) 
						split = line.split("	");
					else 
						split = line.split(" ");

					if (isLipidex)
						fragType = line.substring(line.indexOf("\"")+1,line.lastIndexOf("\""));

					if (precursor-Double.valueOf(split[0])>2.0)
						entryTemp.addFrag(Double.valueOf(split[0]), Double.valueOf(split[1]), fragType, getTransitionType(fragType));

				}

				if (line.contains("PRECURSORMZ:"))
				{
					precursor = Double.valueOf(line.substring(line.lastIndexOf(" ")+1));
				}
			}

			addSpectrum(entryTemp);
			reader.close();
		}
	}

	//Method to add spectrum to proper array and update mass ranges
	public  void addSpectrum(LibrarySpectrum spectrum)
	{
		librarySpectra.add(spectrum);
		if (spectrum.precursor > maxMass) maxMass = spectrum.precursor;
		if (spectrum.precursor < minMass) minMass = spectrum.precursor;
		spectrum.scaleIntensities();
	}

	//Parse MGF file
	public  void readMGF(String filename) throws IOException
	{
		String line = "";					//String for reading in .mgf
		String polarity = "";				//Polarity of ms2
		Double precursor = 0.0;				//Precursor mass
		SampleSpectrum specTemp = null;		//Stores ms2 to be added
		String[] split;						//String array for parsing transitions
		Double retention = 0.0;				//Retention time in minutes

		BufferedReader reader = new BufferedReader(new FileReader(filename));
		File file = new File(filename);

		//read line if not empty
		while ((line = reader.readLine()) != null)
		{
			if (line.contains("PEPMASS="))
			{

				if (line.contains(" ")) precursor = Double.valueOf(line.substring(line.indexOf("=")+1,line.lastIndexOf(" ")));
				else precursor = Double.valueOf(line.substring(line.indexOf("=")+1));
			}
			else if (line.contains("RTINSECONDS"))
			{
				if (line.contains(" ")) retention = (Double.valueOf(line.substring(line.indexOf("=")+1,line.lastIndexOf(" "))))/60.0;
				else retention = Double.valueOf(line.substring(line.indexOf("=")+1))/60.0;
			}
			else if (line.contains("CHARGE"))
			{
				//peakStart = true;

				if (line.contains("-")) polarity = "-";
				else polarity = "+";
			}
			else if (line.contains("END IONS"))
			{
				numSpectra++;

				if (specTemp!= null) sampleMS2Spectra.add(specTemp);

				specTemp = null;
				polarity = "+";
				precursor = 0.0;
				retention = 0.0;
			}
			else if (line.contains(".") && !line.contains("PEPMASS") && !line.contains("CHARGE")  && !line.contains("TITLE"))
			{
				split = line.split(" ");

				if (specTemp == null)
				{
					specTemp = new SampleSpectrum(precursor,polarity,file.getName(),retention,numSpectra);
				}

				if ((precursor-Double.valueOf(split[0]))>1.5 && Double.valueOf(split[0])>minMS2Mass)
				{
					specTemp.addFrag(Double.valueOf(split[0]), Double.valueOf(split[1]));
				}
			}
		}

		reader.close();
	}

	//Update progress bar
	public static void updateProgress(int progress, String message, boolean visible)
	{
		if (progress != progressInt)
		{
			progressBar.setValue(progress);
			progressBar.setString(progress+message);
			progressBar.repaint();

		}
	}

	//Return transition type object corresponding to provided string
	private static TransitionType getTransitionType(String s)
	{
		for (int i=0; i<transitionTypes.size(); i++)
		{
			if (transitionTypes.get(i).name.equals(s)) return transitionTypes.get(i);
		}

		return null;
	}
}
