package peak_finder;

public class PurityResult implements Comparable<PurityResult> {

	public int purity;		//Purity of mse
	public String name;		//Name of lipid
	public Lipid lipid;		//Associated lipid object
	
	//Constructor
	public PurityResult(String name, int purity)
	{
		this.purity = purity;
		this.name = name;
		lipid = null;
	}
	
	//Compares purity results based on purity
	public int compareTo(PurityResult p)
	{
		if (p.purity<this.purity) return -1;
		else if (p.purity>this.purity) return 1;
		else return 0;
	}
	
}
