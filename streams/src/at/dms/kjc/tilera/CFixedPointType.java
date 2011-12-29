package at.dms.kjc.tilera;

import at.dms.kjc.CIntType;

/**
 * Fixed point type used when converting floating point to fixed point.
 * 
 * @author mgordon
 *
 */
public class CFixedPointType extends CIntType {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6907023532950072281L;
	public static CFixedPointType FixedPoint = new CFixedPointType();
    
    /**
     * Returns a string representation of this type.
     */
    @Override
	public String toString() {
        return "fixed";
    }
    
}
