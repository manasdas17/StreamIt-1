package at.dms.kjc.cluster;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.partition.*;

public class GenerateWorkEst {

    public static void generateWorkEst() {

        int threadNumber = NodeEnumerator.getNumberOfNodes();
        CodegenPrintWriter p = new CodegenPrintWriter();

        for (int i = 0; i < threadNumber; i++) {

	    SIROperator oper = NodeEnumerator.getOperator(i);	   
	    FlatNode node = NodeEnumerator.getFlatNode(i);	    
	    int steady_counts = ((Integer)ClusterBackend.steadyExecutionCounts.get(node)).intValue();

	    int w = 0;

	    if (oper instanceof SIRFilter) {
		WorkEstimate w_est = WorkEstimate.getWorkEstimate((SIRFilter)oper);
		WorkList w_list = w_est.getSortedFilterWork();
		w = w_list.getWork(0);
	    }
	   
 	    if (oper instanceof SIRJoiner) {
		SIRJoiner j = (SIRJoiner)oper;
		w = j.getSumOfWeights() * 8;
	    }

	    if (oper instanceof SIRSplitter) {
		SIRSplitter s = (SIRSplitter)oper;
		w = s.getSumOfWeights() * 8;
	    }

	    p.print(i+" " + (w * steady_counts) + "\n");

        }

        try {
            FileWriter fw = new FileWriter("work-estimate.txt");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write work estimation file");
        }   
    }
}
