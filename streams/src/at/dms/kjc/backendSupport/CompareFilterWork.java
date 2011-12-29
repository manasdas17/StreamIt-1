/**
 * 
 */
package at.dms.kjc.backendSupport;

import java.util.Comparator;

import at.dms.kjc.slir.Filter;
import at.dms.kjc.slir.StaticSubGraph;

/**
 * A Comparator for the work estimation of slices that compares slices
 * based on the amount of work in the bottleneck (the filter of the slice
 * that performs the most work).
 * 
 * @author mgordon
 *
 */
public class CompareFilterWork implements Comparator<Filter> {
    /** The partition we used */
    private StaticSubGraph ssg;
    
    /**
     * Create a new object that uses the work estimates of partitioner.
     * 
     * @param slicer
     */
    public CompareFilterWork(StaticSubGraph slicer) {
        this.ssg = slicer;
    }
    
    /**
     * Compare the bottleneck work of Slice <pre>o1</pre> with Slice <pre>o2</pre>.
     * 
     * @return The comparison 
     */
    public int compare(Filter o1, Filter o2) {
//        assert o1 instanceof Slice && o2 instanceof Slice;
        
        if (ssg.getWorkEstimate(o1.getWorkNodeContent()) < 
        		ssg.getWorkEstimate(o2.getWorkNodeContent()))
            return -1;
        else if (ssg.getWorkEstimate(o1.getWorkNodeContent()) == 
                ssg.getWorkEstimate(o2.getWorkNodeContent()))
            return 0;
        else
            return 1;
    }
}
