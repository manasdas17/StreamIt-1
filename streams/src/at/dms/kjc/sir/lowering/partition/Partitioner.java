package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.dynamicprog.*;
import at.dms.kjc.flatgraph.*;

public class Partitioner {
    /**
     * Tries to adjust <str> into <targetCount> pieces of equal work, and
     * return new stream.
     */
    public static SIRStream doit(SIRStream str, int targetCount) {
	// detect number of tiles we have
	int curCount = new GraphFlattener(str).getNumTiles();
	return doit(str, curCount, targetCount);
    }


    /**
     * Tries to adjust <str> into <targetCount> pieces of equal work, given
     * that <str> currently requires <curCount> tiles.  Return new
     * stream.
     */
    public static SIRStream doit(SIRStream str, int curCount, int targetCount) {
	// Lift filters out of pipelines if they're the only thing in
	// the pipe
	Lifter.lift(str);

	// make work estimate
	WorkEstimate work = WorkEstimate.getWorkEstimate(str);
	work.printGraph(str, "work-before-partition.dot");
	work.getSortedFilterWork().writeToFile("work-before-partition.txt");

	System.err.println("  Found "+curCount+" tiles.");

	// for statistics gathering
	if (KjcOptions.dpscaling) {
	    DynamicProgPartitioner.saveScalingStatistics(str, work, 256);
	}

	// do the partitioning
	if (curCount < targetCount) {
	    // need fission
	    if (KjcOptions.partition_dp) {
		str = new DynamicProgPartitioner(str, work, targetCount).toplevel();
	    } else {
		new GreedyPartitioner(str, work, targetCount).toplevelFission(curCount);
	    }
	} else {
	    // need fusion
	    if (KjcOptions.partition_ilp) {
		new ILPPartitioner(str, work, targetCount).toplevelFusion();
	    } else if (KjcOptions.partition_dp) {
		str = new DynamicProgPartitioner(str, work, targetCount).toplevel();
	    } else {
		Utils.assert(KjcOptions.partition_greedy);
		new GreedyPartitioner(str, work, targetCount).toplevelFusion();
	    }
	}

	// lift the result
	Lifter.lift(str);

	// get the final work estimate
	work = WorkEstimate.getWorkEstimate(str);
	work.printGraph(str, "work-after-partition.dot");
	work.getSortedFilterWork().writeToFile("work-after-partition.txt");
	work.printWork();

	return str;
    }
}
