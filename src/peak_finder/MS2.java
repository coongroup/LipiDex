package peak_finder;
import java.util.ArrayList;


public class MS2 
{
	public ArrayList<Transition> transitions = null;	//Arraylist of all transitions
	public Double precursor = 0.0;						//Precursor samples for ms2
	public Double ms2Time = 0.0;						//Retention time of ms2 acquisition

	//Constructor
	public MS2 (Double ms2Time, Double precursor)
	{
		this.ms2Time = ms2Time;
		this.precursor = precursor;
	}
}
