package at.dms.kjc.sir.lowering;

import streamit.scheduler.*;

import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.reordering.*;
import at.dms.kjc.sir.linear.*; 
import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

import java.util.*; 

/**
 * This is the main class for decomposing the high SIR into
 * lower-level function calls for the uniprocessor backend.
 */
public class Flattener {
    /**
     * This variable is toggled once SIRInitStatements have been
     * eliminated in favor of a hierarchical stream represenation
     * within the SIRContainers.
     */
    public static boolean INIT_STATEMENTS_RESOLVED = false;

    /**
     * Flattens <str> into a low IR representation, given that <interfaces>
     * are all the top-level interfaces declared in the program and 
     * <interfaceTables> represents the mapping from interfaces to methods
     * that implement a given interface in a given class.
     */
    public static void flatten(SIRStream str,
			       JInterfaceDeclaration[] 
			       interfaces,
			       SIRInterfaceTable[]
			       interfaceTables,
			       SIRStructure[] structs) {
	/* DEBUGGING PRINTING
        System.out.println("--------- ON ENTRY TO FLATTENER ----------------");
	SIRPrinter printer1 = new SIRPrinter();
	IterFactory.createIter(str).accept(printer1);
	printer1.close();
	*/

	// move field initializations into init function
	FieldInitMover.moveStreamInitialAssignments(str);
	
	// propagate constants and unroll loops
	System.err.print("Expanding graph... ");
	ConstantProp.propagateAndUnroll(str);
	System.err.println("done.");

	// Convert Peeks to Pops
	if (KjcOptions.poptopeek) {
	    System.err.print("Converting pop to peek... ");
	    PopToPeek.removeAllPops(str);
	    ConstantProp.propagateAndUnroll(str);
	    System.err.println("done.");
	}
	
	// construct stream hierarchy from SIRInitStatements
	ConstructSIRTree.doit(str);
	INIT_STATEMENTS_RESOLVED = true;

	// dump the original graph to a dot format
	StreamItDot.printGraph(str, "before.dot");

	AdjustGranularity.doit(str, -1);

	if (KjcOptions.partition || KjcOptions.ilppartition || KjcOptions.dppartition) {
	    System.err.println("Partitioning...");
	    Partitioner.doit(str, 
			     KjcOptions.raw * KjcOptions.raw);
	    System.err.println("...done with Partitioning.");
	}

	/* Not general code: Just a test for sync-removal on TwoWeightedRR.java */ 
	/* StreamItDot.printGraph(str, "before-syncremov.dot");
	SIRPipeline parentPipe = (SIRPipeline)str; 
	SyncRemovalSJPair.diffuseSJPair((SIRSplitJoin)parentPipe.get(1), (SIRSplitJoin)parentPipe.get(2)); 
	StreamItDot.printGraph(str, "after-syncremov.dot"); */ 

	/*
	SIRFilter toDuplicate = ((SIRFilter)
				 ((SIRPipeline)
				  ((SIRPipeline)str).get(1)).get(0));
	System.err.println("Trying to duplicate " + toDuplicate);
	StatelessDuplicate.doit(toDuplicate, 2);
	*/

	if (KjcOptions.fusion) {
	    System.err.println("Running FuseAll...");
	    FuseAll.fuse(str);
	    System.err.println("...done with Fuseall.");
	    /* DEBUGGING PRINTING
	    System.out.println("--------- AFTER FUSION ------------");
	    printer1 = new SIRPrinter();
	    IterFactory.createIter(str).accept(printer1);
	    printer1.close();
	    */
	    
	}

	// dump the original graph to a dot format
	StreamItDot.printGraph(str, "after.dot");

	//Raise NewArray's up to top
	System.err.print("Raising variable declarations... ");
	new VarDeclRaiser().raiseVars(str);
	System.err.println("done.");
	
        // do constant propagation on fields
        if (KjcOptions.constprop) {
	    System.err.print("Propagating fields... ");
	    FieldProp.doPropagate(str);
	    System.err.println("done.");
	}

	// move field initializations into init function
	System.err.print("Moving initial assignments... ");
	FieldInitMover.moveStreamInitialAssignments(str);
	System.err.println("done.");

	/* DEBUGGING PRINTING
	System.out.println("--------- AFTER CONSTANT PROP / FUSION --------");
	printer1 = new SIRPrinter();
	IterFactory.createIter(str).accept(printer1);
	printer1.close();
	*/
	
	if (KjcOptions.constprop) {
	    //Flatten Blocks
	    System.err.print("Flattening blocks... ");
	    new BlockFlattener().flattenBlocks(str);
	    System.err.println("done.");
	    //Analyze Branches
	    //System.err.print("Analyzing branches... ");
	    //new BranchAnalyzer().analyzeBranches(str);
	    System.err.println("done.");
	}
	//Destroys arrays into local variables if possible
	System.err.print("Destroying arrays... ");
	//new ArrayDestroyer().destroyArrays(str);
	System.err.println("done.");
	//Raise variables to the top of their block
	System.err.print("Raising variables... ");
	new VarDeclRaiser().raiseVars(str);
	System.err.println("done.");

	// if someone wants to run any of the linear tools/optimizations
	// we need to run linear analysis first to extract the information
	// we are working with.
	if (KjcOptions.linearanalysis ||
	    KjcOptions.linearreplacement ||
	    (KjcOptions.frequencyreplacement != -1)) {
	    // run the linear analysis and stores the information garnered in the lfa
	    System.err.print("Running linear analysis... ");
	    LinearAnalyzer lfa = LinearAnalyzer.findLinearFilters(str, KjcOptions.debug);
	    System.err.println("done.");

	    // now, print out the graph using the LinearPrinter which colors the graph
	    // nodes based on their linearity.
	    LinearDot.printGraph(str, "linear.dot", lfa);
	    
	    // if we are supposed to transform the graph
	    // by replacing work functions with their linear forms, do so now 
	    if (KjcOptions.linearreplacement) {
		System.err.print("Running linear replacement... ");
		LinearReplacer.doReplace(lfa, str);
		System.err.println("done.");
		// print out the stream graph after linear replacement
		LinearDot.printGraph(str, "linear-replace.dot", lfa);
	    }

	    // and finally, if we want to run frequency analysis
	    // 0 means stupid implementation, 1 means nice implemenation
	    if (KjcOptions.frequencyreplacement == 0) {
		System.err.print("Running (stupid) frequency replacement... ");
		StupidFrequencyReplacer.doReplace(lfa, str, KjcOptions.targetFFTSize);
		System.err.println("done.");
		LinearDot.printGraph(str, "linear-frequency-stupid.dot", lfa);
	    }
	    if (KjcOptions.frequencyreplacement == 1) {
		System.err.print("Running (smart) frequency replacement... ");
		FrequencyReplacer.doReplace(lfa, str, KjcOptions.targetFFTSize);
		System.err.println("done.");
		LinearDot.printGraph(str, "linear-frequency.dot", lfa);
	    }

	    
	}

	// make single structure
	SIRIterator iter = IterFactory.createIter(str);
	System.err.print("Structuring... ");
	JClassDeclaration flatClass = Structurer.structure(iter,
							   interfaces,
							   interfaceTables,
                                                           structs);
	System.err.println("done.");
	// build schedule as set of higher-level work functions
	System.err.print("Scheduling... ");
	Schedule schedule = SIRScheduler.buildWorkFunctions(str, flatClass);
	System.err.println("done.");
	// add LIR hooks to init and work functions
	System.err.print("Annotating IR for uniprocessor... ");
	LowerInitFunctions.lower(iter, schedule);
        LowerWorkFunctions.lower(iter);
	System.err.println("done.");

	/* DEBUGGING PRINTING
	System.out.println("----------- AFTER FLATTENER ------------------");
	IRPrinter printer = new IRPrinter();
	flatClass.accept(printer);
	printer.close();
	*/

	System.err.println("Generating code...");
	LIRToC.generateCode(flatClass);
	//System.err.println("done.");
    }
    
}
