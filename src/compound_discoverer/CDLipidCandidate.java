package compound_discoverer;
import java.util.ArrayList;
import java.util.Collections;

import peak_finder.Lipid;
import peak_finder.Utilities;


public class CDLipidCandidate extends Utilities implements Comparable<CDLipidCandidate>
{
	public Double maxDotProd;					//Maximum dot product for all associate IDs
	public Double maxRevDot;					//Maximum reverse dot produce for all associated IDs
	public Double avgPrecursor;					//Average precursor mass for all IDs
	public Integer count;						//Number of associated IDs
	public ArrayList<Lipid> identifications;	//ArrayList of all IDs
	public String lipidName;					//String representation of ID
	public String sumLipidName;					//Sum ID
	public Integer purity;						//Purity of identification
	public boolean keep;						//True iff ID will be retained
	public boolean preferredPolarity;			//True iff ID found in preferred polarity for lipid class

	//Constructor
	public CDLipidCandidate(Lipid lipid)
	{
		this.lipidName = lipid.lipidName;
		this.sumLipidName = lipid.sumLipidName;
		maxDotProd = lipid.dotProduct;
		maxRevDot = lipid.revDotProduct;
		count = 1;
		identifications = new ArrayList<Lipid>();
		identifications.add(lipid);
		keep = true;
		purity = 0;

		if (lipid.preferredPolarity) this.preferredPolarity = true;
		else this.preferredPolarity = false;
	}

	//Assigns purity of highest ranked ID to LipidCandidate
	public void assignPurity()
	{
		//Assign purity of top hit
		if (identifications.size()>0)
			purity = identifications.get(0).purity;
	}

	//Add lipid to LipidCandidate and update object variables
	public void addLipid(Lipid lipid)
	{
		//Update max dot product fields
		if (lipid.dotProduct>maxDotProd)
			maxDotProd = lipid.dotProduct;

		if (lipid.revDotProduct>maxRevDot)
			maxRevDot = lipid.revDotProduct;

		//Add lipid and sort
		identifications.add(lipid);
		Collections.sort(identifications);
		count++;

		this.preferredPolarity = identifications.get(0).preferredPolarity;

		if (!this.preferredPolarity) 
			lipidName.equals(identifications.get(0).sumLipidName);

		purity = identifications.get(0).purity;
	}

	//Return string representation of ID based on purity
	public String toString()
	{
		String result = "";

		if (purity>MINFAPURITY)
			result = lipidName;
		else
			result = sumLipidName;
		return result;
	}

	//Compares to lipidCandidates based on preferred polarity and dot product
	public int compareTo(CDLipidCandidate lc)
	{
		int result = 0;

		//Negative polarity given priority
		if (identifications.get(0).preferredPolarity 
				&& !lc.identifications.get(0).preferredPolarity) return -1;
		else if (!identifications.get(0).preferredPolarity 
				&& lc.identifications.get(0).preferredPolarity) return 1;

		//If polarity is the same, sort by dot product
		else if (identifications.get(0).polarity.equals(lc.identifications.get(0).polarity))
		{
			//If polarity is the same, sort by count
			if (count>lc.count) return -1;
			else if (count<lc.count) return 1;
		}

		return result;
	}
}
