package peak_finder;

public class Library 
{
	String name;	//Name of library
	String file;	//File location of library
	boolean active;	//True iff the library is active
	
	//Constructor
	public Library(String name, String file)
	{
		this.name = name;
		this.file = file;
	}
	
	//Activate library
	public void changeActive()
	{
		if (active) active = false;
		else active = true;
	}
}
