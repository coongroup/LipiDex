package peak_finder;
import java.util.ArrayList;

public class Lipid extends Utilities implements Comparable<Lipid> 
{
	public Double retention;						//Aligned retention time of ms2 in mins
	public Double correctedRetention;				//Unalligned retention time of ms2 in mins
	public Double precursor;						//Precursor mass
	public Sample sample;							//Associated sample object
	public Double dotProduct;						//Dot product of identification
	public Double revDotProduct;					//Reverse dot product of id
	public Double gaussianScore;					//Gaussian score from guassian profile
	public String lipidString;						//String representation of lipid 
	public String lipidClass;						//String representation of lipid class
	public String adduct;							//String representation of adduct
	public ArrayList <String> fattyAcids;			//ArrayList of fatty acid adduct
	public ArrayList <Double> fragmentMasses;		//ArrayList of masses from spectral matching
	public String lipidName;						//String representation of lipid name
	public String sumLipidName;						//String representation of sum lipid
	public String polarity;							//Polarity
	public Integer charge;							//Lipid charge
	public MS2 ms2 = null;							//Associate ms2 object
	public int purity = 0;							//Purity
	public boolean preferredPolarity;				//True iff lipid ID is in preferred polarity
	public Double ppmError;							//PPm error between ID and library entry
	public boolean keep;							//True iff ID will be retained
	public boolean isFattyAcidLipid;				//True iff the lipid follows standard lipid nomenclature
	public boolean isLipiDex;						//True iff the library entry was made with LipiDex
	public ArrayList<PurityResult> purityArray;		//Arraylist of all purity values associated with ID


	//Constructor
	public Lipid(Double retention, Double precursor, Sample sample, Double dotProduct,
			Double revDotProduct, String lipidString, Double libPrecursor, int purity,
			boolean preferredPolarity, boolean isLipiDex, ArrayList<PurityResult> purityArray,
			ArrayList<Double> fragmentMasses)
	{
		//Initialize
		fattyAcids = new ArrayList<String>();
		this.fragmentMasses = fragmentMasses;
		this.retention = retention;
		this.precursor = precursor;
		this.sample = sample;
		this.dotProduct = dotProduct;
		this.revDotProduct = revDotProduct;
		this.lipidString = lipidString;
		this.purity = purity;
		this.preferredPolarity = preferredPolarity;
		this.isLipiDex = isLipiDex;
		ppmError = ((precursor-libPrecursor)/libPrecursor)*1000000.0;
		keep = true;
		gaussianScore = 0.0;
		this.correctedRetention = sample.getCorrectedRT(retention);
		this.purityArray = purityArray;

		//Change sample statistics
		sample.addPPMError(ppmError);

		//Parse out class, adduct, and fatty acids
		parseLipidString();

		//Get polarity
		if (this.adduct.contains("]+")) polarity = "+";
		else polarity = "-";

		//Create lipid name
		lipidName = toString();

		//Get charge
		if (this.adduct.contains("2-")) charge = 2;
		else charge = 1;
	}

	//Parse adduct, lipid class, and fatty acids from lipid string
	public void parseLipidString()
	{
		String[] split;
		String[] faSplit;
		String prefixTemp = "";
		String classPrefixTemp = "";
		int carbonNumber = 0;
		int dbNumber = 0;

		try
		{
			//Parse lipid class
			split = lipidString.split(" ");
			isFattyAcidLipid = true;
			this.lipidClass = split[0];

			//Capitalize lipid Class
			this.lipidClass = this.lipidClass.substring(0, 1).toUpperCase() + this.lipidClass.substring(1);

			//Parse lipid adduct
			this.adduct = split[2];
			this.adduct = this.adduct.replace(";","");

			//Split lipid fatty acids
			split = split[1].split("_");

			//For each fatty acid
			for (int i=0; i<split.length; i++)
			{
				faSplit = split[i].split(":");

				if (!faSplit[0].replaceAll("[0-9]", "").equals(""))
				{
					//Parse out prefixes
					prefixTemp = faSplit[0].replaceAll("[0-9]", "");
					classPrefixTemp += prefixTemp;
				}

				//Add fatty acid to string array
				fattyAcids.add(prefixTemp+Integer.valueOf(faSplit[0].replaceAll("[^\\d.]", ""))
						+":"+Integer.valueOf(faSplit[1].replaceAll("[^\\d.]", "")));

				//Update total carbon and db number
				carbonNumber += Integer.valueOf(faSplit[0].replaceAll("[^\\d.]", ""));
				dbNumber += Integer.valueOf(faSplit[1].replaceAll("[^\\d.]", ""));

				//Clear prefix
				prefixTemp = "";
			}

			//Update sum composition
			this.sumLipidName = this.lipidClass+" "+classPrefixTemp+carbonNumber+":"+dbNumber;
		}
		//If formatted incorrectly, simply use provided name
		catch (Exception e)
		{
			split = lipidString.split("\\[");
			isFattyAcidLipid = false;
			this.lipidClass = "nonStandard";

			//Parse adduct
			this.adduct = "["+split[1];
			this.adduct = this.adduct.replace(";","");

			//Parse name
			lipidName = split[0];
			sumLipidName = split[0];
		}
	}

	//Returns string representation of lipid
	public String toString()
	{
		String result = "";

		if (this.isFattyAcidLipid)
		{
			result += lipidClass + " ";

			for (int i=0; i<fattyAcids.size(); i++)
			{
				result += fattyAcids.get(i);
				if (i<fattyAcids.size()-1)
					result += "_";
			}
		}
		else
			result = lipidName;

		return result;
	}

	//Compares lipids based on polarity and gaussian score
	public int compareTo(Lipid l)
	{
		//Preferred polarity given priority
		if (!preferredPolarity && l.preferredPolarity)
			return 1;
		else if (preferredPolarity && !l.preferredPolarity)
			return -1;

		//If polarity is the same, sort by gaussian score
		else
		{
			if (gaussianScore>l.gaussianScore)
				return -1;
			else
				return 1;
		}
	}
}
