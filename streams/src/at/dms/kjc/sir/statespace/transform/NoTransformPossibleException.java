package at.dms.kjc.sir.statespace.transform;


/**
 * Exception that is thrown when we can't compute a transform for some reason.
 * This is a checked exception to ensure that the case of an impossible transformation is
 * explicity checked for and so that the compiler doesn't die (this optimization just
 * stops where it is).<br>
 *
 * $Id: NoTransformPossibleException.java,v 1.2 2006-01-25 17:02:33 thies Exp $
 **/
public class NoTransformPossibleException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 9066485883444863911L;

	public NoTransformPossibleException(String message) {
        super(message);
    }
}
