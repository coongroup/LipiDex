package lib_gen;

@SuppressWarnings("serial")
public class CustomException extends Exception
{
	Exception e;
	
    //Parameterless Constructor
    public CustomException(Exception e)
    {
    	this.e = e;
    }

    //Constructor that accepts a message
    public CustomException(String message, Exception e)
    {
       super(message);
       this.e = e;
    }
    
    //Returns original exception
    public Exception getException()
    {
    	return e;
    }
}
