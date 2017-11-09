package lib_gen;
import java.util.ArrayList;

//Class representing MS2
public class MS2 extends Utilities
{
	Double precursor; 					//precursor sampled for ms2
	String polarity;					//Polarity of ms2
	int charge;							//Charge of samples precursor
	Double maxIntensity;				//Intensity of most intense fragment
	Double sn; 							//signal to noise
	ArrayList<Transition> transitions;	//ArrayList of all transitions

	//Constructor
	public MS2 (Double precursor, String polarity, int charge)
	{
		this.precursor = precursor;
		this.polarity = polarity;
		this.charge = charge;
		transitions = new ArrayList<Transition>();
		sn = 0.0;
	}

	//Return arraylist of all transitions
	public ArrayList<Transition> getTransitions()
	{
		return transitions;
	}
	
	//Add transition to array
	public void addTransition (Transition t)
	{
		transitions.add(t);
	}

	//Returns string representation of ms2
	public String toString()
	{
		return transitions.toString();
	}
}
