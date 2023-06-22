package ch.lsimedia.zammad;

/**
 * @author  sbodmer
 */
public class ZammadSys  {

    /**
     * Some major error occured
     */
    public static final String STATE_ERROR = "error";
    /**
     * Everything is fine
     */
    public static final String STATE_OK = "ok";
    /**
     * Some warning was detected
     */
    public static final String STATE_WARNING = "warning";
    /**
     * Simple info message
     */
    public static final String STATE_INFO = "info";
 
	protected String state = STATE_OK;    //--- state (error,waring,granted,...)
    /**
     * The sys message
     */
    protected String message = "";
    
    public ZammadSys(String state, String message) {
        this.state = state;
		this.message = message;
    }

    
    public String toString() {
		return "SYS ("+state+") "+message;
	}
    //*****************************
    //*** API
    //*****************************

	public String getState() {
		return state;
	}
	
	public String getMessage() {
		return message;
	}
    
}
