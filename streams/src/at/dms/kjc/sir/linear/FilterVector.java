package at.dms.kjc.sir.linear;

import java.util.*;

/**
 * A FilterVector is, at its most basic level, a simple, one dimensional
 * row vector. In the linear dataflow analysis, FilterVectors are used to
 * keep track of the combinations of inputs that are used to compute a 
 * specific intermediate value in the program flow.
 *
 * $Id: FilterVector.java,v 1.1 2002-08-14 18:13:19 aalamb Exp $
 **/

public class FilterVector extends FilterMatrix {
    /** Creates a vector of size i **/
    public FilterVector(int size) {
	// just make a matrix with one row and size cols
	super(1,size);
    }

    /**
     * Override the superclass's setElement because we want to regulate access to the data via
     * set element with only one argument
     **/
    public void setElement(int row, int col, ComplexNumber value) {
	throw new RuntimeException("Do not use the two index version of setElement");
    }
    /**
     * Override the superclass's getElement because we want to regulate access to the data via
     * set element with only one argument
     **/
    public ComplexNumber getElement(int row, int col) {
	throw new RuntimeException("Do not use the two index version of getElement");
    }

    //////// Accessors/Modifiers
    
    
    /** Get the element at index in the vector **/
    public ComplexNumber getElement(int index) {
	//access via the superclass
	return super.getElement(0,index);
    }

    /** Sets the element at position index to be the specified complex number **/
    public void setElement(int index, ComplexNumber value) {
	// use the superclass's version
	super.setElement(0,index,value);
    }
    
}
	


    
