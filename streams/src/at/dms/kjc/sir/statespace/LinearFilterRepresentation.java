package at.dms.kjc.sir.statespace;

/**
 * A LinearFilterRepresentation represents the computations performed by a filter
 * on its input values as four matrices. 
 *
 * This class holds the A, B, C, D in the equations y = Ax+Bu, x' = Cx + Du which calculates the output
 * vector y and new state vector x' using the input vector u and the old state vector x.<br>
 *
 * $Id: LinearFilterRepresentation.java,v 1.5 2004-02-24 19:56:12 sitij Exp $
 * Modified to state space form by Sitij Agrawal  2/9/04
 **/


public class LinearFilterRepresentation {
    /** the A in y=Ax+Bu. **/
    private FilterMatrix A;
    /** the B in y=Ax+Bu. **/
    private FilterMatrix B;
    /** the C in x'=Cx+Du. **/
    private FilterMatrix C;
    /** the D in x'=Cx+Du. **/
    private FilterMatrix D;
    /** the initial state values. **/
    private FilterVector initVec;
    /** the cost of this node */
    private LinearCost cost;

    /**
     * The peek count of the filter. This is necessary for doing pipeline combinations
     * and it is information not stored in the dimensions of the
     * representation matrix or vector. peekCount - popCount is the number of variables that
     * must be popped initially
     **/
    private int peekCount;

    /**
     * Create a new linear filter representation with matrices A, B, C, and D.
     * Note that we use a copy of all matrices so that we don't end up with
     * an aliasing problem. peekc is the peek count of the filter that this represenation is for,
     * which we need for combining filters together (because the difference between
     * the peek count and the pop count tells us about the buffers that the program is using).
     **/
    public LinearFilterRepresentation(FilterMatrix matrixA,
				      FilterMatrix matrixB,
				      FilterMatrix matrixC,
				      FilterMatrix matrixD,
				      FilterVector vec,
				      int peekc) {

	this.A = matrixA.copy();
	this.B = matrixB.copy();
	this.C = matrixC.copy();
	this.D = matrixD.copy();
	this.initVec = (FilterVector)vec.copy();	
	this.peekCount = peekc;
	// we calculate cost on demain (with the linear partitioner)
	this.cost = null;
    }
    //////////////// Accessors ///////////////////
    
    /** Get the A matrix. **/
    public FilterMatrix getA() {return this.A;}
    /** Get the B matrix. **/
    public FilterMatrix getB() {return this.B;}
    /** Get the C matrix. **/
    public FilterMatrix getC() {return this.C;}
    /** Get the D matrix. **/
    public FilterMatrix getD() {return this.D;}

    /** Get the initialization matrix. **/
    public FilterVector getInit() {return this.initVec;}

    /** Get the peek count.  **/
    public int getPeekCount() {return this.peekCount;}
    /** Get the push count. (#rows of D or C) **/
    public int getPushCount() {return this.D.getRows();}
    /** Get the pop count. (#cols of D) **/
    public int getPopCount() {return this.D.getCols();}
    /** Get the number of state variables (#rows of A or B, # cols of A or C) **/
    public int getStateCount() { return this.A.getRows();}

    //////////////// Utility Functions  ///////////////////

    /**
     * Returns true if at least one constant component is non-zero.
     **/
    
    
    public boolean hasConstantComponent() {
	/*
        int index = A.zeroRow();
	if(index == -1)
	    return false;
	else
	  return initVec.getElement(index) != ComplexNumber.ZERO;
	*/


	return false;
    }
    

    /**
     * Expands this linear representation by factor 
     **/

    public LinearFilterRepresentation expand(int factor) {
    
	// do some argument checks
	if (factor < 1) {
	    throw new IllegalArgumentException("need a positive multiplier");
	}

	// pull out old values for ease in understanding the code.
	int stateCount = this.getStateCount();
	int oldPush = this.getPushCount();
	int oldPop  = this.getPopCount();
	int peekCount = this.getPeekCount();

	// newA = oldA^(factor);
	// newB = [oldA^(factor-1) * B  oldA^(factor-2) * B  ...  oldA * B  B]
	// newC = [C  C * A  C * A^2  ...  C * A^(factor-2)  C * A^(factor-1)] (transposed)

	/* 
	   newD = |D                     0               0          0  ...  0      |       
                  |C * B                 D               0          0  ...  0      |
                  |C * A * B             C * B           D          0  ...  0      |
                  |C * A^2 * B           C * A * B       C * B      D  ...  0      |
                  |  ...
                  |C*A^(factor-2)*B   C*A^(factor-3)*B                 ...  C*B  D | 

         */

	FilterMatrix oldA = this.getA();
	FilterMatrix oldB = this.getB();
	FilterMatrix oldC = this.getC();
        FilterMatrix oldD = this.getD();

	FilterMatrix newA = oldA.copy();
	FilterMatrix newB = new FilterMatrix(stateCount, oldPop*factor);
	FilterMatrix newC = new FilterMatrix(oldPush*factor, stateCount);
        FilterMatrix newD = new FilterMatrix(oldPush*factor, oldPop*factor);

	newB.copyAt(0,oldPop*(factor-1),oldB);
	newC.copyAt(0,0,oldC);

	FilterMatrix tempB = oldB.copy();
	FilterMatrix tempC = oldC.copy();
	FilterMatrix tempD = oldC.times(oldB);

	for(int i=0; i<factor; i++) 
	  newD.copyAt(oldPush*i,oldPop*i,oldD);

	for(int i=1; i<factor; i++)
	  newD.copyAt(oldPush*i,oldPop*(i-1),tempD);

	for(int i=1; i<factor; i++) {
	  tempB = newA.times(oldB);
	  tempC = oldC.times(newA);
	  tempD = tempC.times(oldB); 

	  newB.copyAt(0,oldPop*(factor-i-1),tempB);
	  newC.copyAt(oldPush*(factor-i-1),0,tempC);

	  if(i < factor-1) {
	    for(int j=i; j<factor; j++)
	      newD.copyAt(oldPush*(i+1),oldPop*i,tempD);
	  }

	  newA = newA.times(oldA);
	}

	
	
	//System.err.println("--------");
	//System.err.println("new rows: " + newPeek);
	//System.err.println("new cols: " + newPush);
	//System.err.println("new pop: "  + newPop);
	//System.err.println("old rows: " + oldPeek);
	//System.err.println("old cols: " + oldPush);
	//System.err.println("old pop: "  + oldPop);
	//System.err.println("num copies: " + numCompleteCopies);
	

	// initial vector is the same
	FilterVector newInitVec = (FilterVector)this.getInit().copy();

	// create a new Linear rep for the expanded filter
	LinearFilterRepresentation newRep;
	newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newInitVec,peekCount);
	return newRep;
    }
    
					
    /**
     * Returns true if this filter is an FIR filter. A linear filter is FIR  
     * if push=pop=1 and no constant component.
     **/
    /*
    public boolean isFIR() {
	return ((this.getPopCount() == 1) &&
		(this.getPushCount() == 1) &&
		(this.getb().getElement(0,0).equals(ComplexNumber.ZERO)));
    }
    */

    /**
     * returns a LinearCost object that represents the number
     * of multiplies and adds that are necessary to implement this
     * linear filter representation.
     **/
    
    public LinearCost getCost() {
	if (this.cost==null) {
	  LinearCost tempCost = calculateCost(this.A);
	  tempCost = tempCost.plus(calculateCost(this.B));
	  tempCost = tempCost.plus(calculateCost(this.C));
	  tempCost = tempCost.plus(calculateCost(this.D));
	  this.cost = tempCost;
	}
	return this.cost;
    }

    /**
     * Calculates cost of this.
     */
    
    private LinearCost calculateCost(FilterMatrix M) {
	// add up multiplies and adds that are necessary for each column of the matrix. 
	int muls = 0;
	int adds = 0;

	int matRows = M.getRows();
	int matCols = M.getCols();
	
	for (int col=0; col<matCols; col++) {
	    // counters for the colums (# muls, adds)
	    int rowAdds = 0;
	    int rowMuls =  0;
	    for (int row=0; row<matRows; row++) {
		ComplexNumber currentElement = M.getElement(row,col);
		if (!currentElement.isReal()) {
		    throw new RuntimeException("Non real matrix elements are not supported in cost .");
		}
		// flags on whether or not to increment the counters
		boolean incAdd = true;
		boolean incMul = true;
		// if it is zero, no add or mult is necessary
		if (currentElement.equals(ComplexNumber.ZERO)) {
		    incAdd = false;
		    incMul = false;
		// if one, no need to do a multiplication.
		} else if (currentElement.equals(ComplexNumber.ONE)) {
		    incMul = false;
		}
		// now, increment if our increment flags are set.
		if (incAdd) {rowAdds++;}
		if (incMul) {rowMuls++;}
	    }


	    // basically, we need one less add per row because adds take two operands
	    // however, we don't want to blindly subtract one, because that might give
	    // us a negative number
	    if (rowAdds > 0) {rowAdds--;}
	    // stick row counters onto overall counters
	    muls += rowMuls;
	    adds += rowAdds;
	}
	return new LinearCost(muls, adds, matRows, matCols);
    }	    
    

    /** Returns true if and only if all coefficients in this filter rep are real valued. **/
    public boolean isPurelyReal() {
	// check the matrix(A), element by element.
	for (int i=0; i<A.getRows(); i++) {
	    for (int j=0; j<A.getCols(); j++) {
		if (!A.getElement(i,j).isReal()) {
		    return false;
		}
	    }
	}
	// check the matrix(B), element by element.
	for (int i=0; i<B.getRows(); i++) {
	    for (int j=0; j<B.getCols(); j++) {
		if (!B.getElement(i,j).isReal()) {
		    return false;
		}
	    }
	}
	// check the matrix(C), element by element.
	for (int i=0; i<C.getRows(); i++) {
	    for (int j=0; j<C.getCols(); j++) {
		if (!C.getElement(i,j).isReal()) {
		    return false;
		}
	    }
	}
	// check the matrix(D), element by element.
	for (int i=0; i<D.getRows(); i++) {
	    for (int j=0; j<D.getCols(); j++) {
		if (!D.getElement(i,j).isReal()) {
		    return false;
		}
	    }
	}	

	// if we get here, there are only real elemets in this rep
	return true;
    }
}



