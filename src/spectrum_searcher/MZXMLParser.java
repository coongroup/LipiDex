package spectrum_searcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import lib_gen.CustomException;
import org.xml.sax.helpers.DefaultHandler;

public class MZXMLParser extends DefaultHandler {

	private ArrayList<MZXMLScan> scanList = new ArrayList<MZXMLScan>();
	public ArrayList<SampleSpectrum> sampleSpecArray = new ArrayList<SampleSpectrum>();

	//Constructor
	public MZXMLParser()
	{

	}

	public void readFile(String filepath) throws IOException, CustomException
	{
		scanList.clear();
		sampleSpecArray.clear();
		
		MZXMLScan scan;				//Temp scan object
		String line = "";			//String holding currently read line
		int scanNum = 0;			//Number of scan;
		boolean centroided = false;	//Boolean if data is centroided
		int msLevel = -1;			//Intever of ms level
		String polarity = "";		//polarity of scan
		Double retentionTime = 0.0;	//retention time of scan in seconds
		Double basePeakMZ = 0.0;	//Mass to charge of most intense peak
		int precision = 0;			//precision used for mz array
		String byteOrder = "";		//byte order for mzArray
		Double precursor = 0.0;		//Precursor mass if MS2
		String mzArray = "";		//Encoded mz array as string

		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		File file = new File(filepath);
		String filename = file.getName();

		//read line if not empty
		while ((line = reader.readLine()) != null)
		{
			if (line.contains("scan num")) scanNum = 
					Integer.valueOf(line.substring(line.indexOf("=")+2, line.lastIndexOf("\"")));
			if (line.contains("centroided"))
			{
				if (line.contains("1")) centroided = true;
				else centroided = false;
			}
			if (line.contains("msLevel")) msLevel = 
					Integer.valueOf(line.substring(line.indexOf("=")+2, line.lastIndexOf("\"")));
			if (line.contains("polarity")) polarity = 
					line.substring(line.indexOf("=")+2, line.lastIndexOf("\""));
			if (line.contains("retentionTime")) retentionTime = 
					Double.valueOf(line.substring(line.indexOf("PT")+2, line.lastIndexOf("S\"")))/60.0;
			if (line.contains("basePeakMz")) basePeakMZ = 
					Double.valueOf(line.substring(line.indexOf("=")+2, line.lastIndexOf("\"")));
			if (line.contains("precision")) precision = 
					Integer.valueOf(line.substring(line.indexOf("=")+2, line.lastIndexOf("\"")));
			if (line.contains("byteOrder")) byteOrder = 
					line.substring(line.indexOf("=")+2, line.lastIndexOf("\""));
			if (line.contains("<precursorMz") && msLevel>1) precursor = 
					Double.valueOf(line.substring(line.indexOf(">")+1, line.indexOf("</precursorMz>")));
			if (line.contains("m/z-int"))
			{
				if (line.contains("==</peaks>")) mzArray = line.substring(line.indexOf(">")+1, line.indexOf("==</peaks>"));
				else if (line.contains("=</peaks>")) mzArray = line.substring(line.indexOf(">")+1, line.indexOf("=</peaks>"));
				else if (line.contains("</peaks>")) mzArray = line.substring(line.indexOf(">")+1, line.indexOf("</peaks>"));
			}

			if (line.contains("</scan>") && msLevel>1)
			{
				//Create new scan object
				scan = new MZXMLScan(scanNum, centroided, msLevel, polarity, 
						retentionTime, basePeakMZ, precision, byteOrder, 
						filename, precursor, mzArray);

				//Add to scan list
				scanList.add(scan);
				sampleSpecArray.add(scan.convertToSampleSpectrum());
			}
		}

		reader.close();
	}
}
