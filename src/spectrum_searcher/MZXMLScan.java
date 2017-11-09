package spectrum_searcher;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

import lib_gen.CustomException;

public class MZXMLScan {

	Double precursor;		//Precursor mass
	String file;			//Filename
	int scanNum;			//Number of scan
	boolean centroided;		//Boolean if data is centroided
	int msLevel;			//Intever of ms level
	String polarity;		//polarity of scan
	Double retentionTime;	//retention time of scan in seconds
	Double basePeakMZ;		//Mass to charge of most intense peak
	int precision;			//precision used for mz array
	String byteOrder;		//byte order for mzArray
	String mzArray;			//Encoded array


	//Constructor
	public MZXMLScan(int scanNum, boolean centroided, int msLevel, String polarity, 
			Double retentionTime, Double basePeakMZ, int precision, String byteOrder, 
			String file, Double precursor, String mzArray) throws CustomException
	{
		//Initialize variables
		this.scanNum = scanNum;
		this.centroided = centroided;
		this.msLevel = msLevel;
		this.polarity = polarity;
		this.retentionTime = retentionTime;
		this.basePeakMZ = basePeakMZ;
		this.precision = precision;
		this.byteOrder = byteOrder;
		this.precursor = precursor;
		this.file = file;
		this.mzArray = mzArray;
		
		//Veryify that byteOrder is correct
		if (!byteOrder.equalsIgnoreCase(("network"))) 
				throw new CustomException("mzXML compression incorrect", null);
	}
	
	//Parse m/z array using byte buffer
	public void parseMZArray(SampleSpectrum spec) throws CustomException
	{
		double[] values;
		byte[] decoded = Base64.getDecoder().decode(mzArray);
		ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		

		values = new double[byteBuffer.asDoubleBuffer().capacity()];
		byteBuffer.asDoubleBuffer().get(values);
		
		if (values.length % 2 > 0)
			throw new CustomException("Different number of m/z and intensity values encountered in peak list.", null);
		
		for (int peakIndex = 0; peakIndex < values.length - 1; peakIndex += 2)
		{
			Double mz = values[peakIndex];
			Double intensity = values[peakIndex + 1];
			spec.addFrag(mz, intensity);
		}
	}
	
	//Convert MZXML file to a common sampleSpectrum type
	public SampleSpectrum convertToSampleSpectrum() throws CustomException
	{
		//Create new sample spectrum object
		SampleSpectrum spec = new SampleSpectrum
				(precursor,polarity,file,retentionTime,scanNum);
		
		//Parse MZ array
		parseMZArray(spec);
		
		return spec;
	}
}
