package at.dms.kjc.slir;

import at.dms.kjc.backendSupport.*;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * This class will unroll the splitting and joining distribution of the slice (or slices)
 * so that all of the weight's array entries are 1 and the number of entries in the
 * distribution is equal to the number of items pushed (for splitter) or pop'ed (for
 * a joiner).
 * 
 * @author mgordon
 *
 */
public class DistributionUnroller {
   
    public static void test(Filter[] slices) {
        for (Filter slice : slices) {
            InputNode input = slice.getInputNode();
            OutputNode output = slice.getOutputNode();
            
            System.out.println(slice);
            String inBefore = "nothing";
            inBefore = input.debugString(false, SchedulingPhase.STEADY);
            String outBefore = "nothing";
            outBefore = output.debugString(false, SchedulingPhase.STEADY);
            unroll(slice);
            //System.out.println(input.debugString(false));
            //System.out.println(output.debugString(false));
            roll(slice);
           
            System.out.println(inBefore);
            System.out.println(input.debugString(false, SchedulingPhase.STEADY));
            System.out.println(outBefore);
            System.out.println(output.debugString(false, SchedulingPhase.STEADY));
            System.out.println("=============");
            
        }
    }
    
    /**
     * Unroll the distribution pattern for the input and output slice nodes
     * of all the slices in <slices>.
     */
    public static void roll(Filter[] slices) {
        for (Filter slice : slices) {
            roll(slice);
        }
    }
    
    /**
     * Unroll the distribution pattern for the input and output slice nodes
     * of <slice>.   
     */
    public static void roll(Filter slice) {
        InputNode input = slice.getInputNode();
        OutputNode output = slice.getOutputNode();
        
        input.canonicalize(SchedulingPhase.STEADY);
        output.canonicalize(SchedulingPhase.STEADY);
        
      
        input.canonicalize(SchedulingPhase.INIT);
        
        output.canonicalize(SchedulingPhase.INIT);
        
    }
    
    /**
     * Unroll the distribution pattern for the input and output slice nodes
     * of all the slices in <slices>.
     */
    public static void unroll(Filter[] slices) {
        for (Filter slice : slices) {
            unroll(slice);
        }
    }
    
    /**
     * Unroll the distribution pattern for the input and output slice nodes
     * of <slice>.   
     */
    public static void unroll(Filter slice) {
        InputNode input = slice.getInputNode();
        OutputNode output = slice.getOutputNode();
        unroll(input);
        unroll(output);
    }
    
    public static void unroll(InputNode input) {
        WorkNodeInfo fi = WorkNodeInfo.getFilterInfo(input.getNextFilter());
       
        //unroll the init joining schedule
        InterFilterEdge[] initSrcs = 
            new InterFilterEdge[fi.totalItemsReceived(SchedulingPhase.INIT)];
        unrollHelperInput(input.getSources(SchedulingPhase.INIT), 
                input.getWeights(SchedulingPhase.INIT), 
                initSrcs);
        input.setInitSources(initSrcs);
        input.setInitWeights(makeOnesArray(initSrcs.length));

        
        //unroll the steady joining schedule
        if (input.getSources(SchedulingPhase.STEADY).length > 0) {
            InterFilterEdge[] srcs = 
                new InterFilterEdge[fi.totalItemsReceived(SchedulingPhase.STEADY)];
            unrollHelperInput(input.getSources(SchedulingPhase.STEADY), 
                    input.getWeights(SchedulingPhase.STEADY), 
                    srcs);
            input.setSources(srcs);
            input.setWeights(makeOnesArray(srcs.length));
        }
    }
    
    public static void unroll(OutputNode output) {
        //unroll the steady splitting schedule
        WorkNodeInfo fi = WorkNodeInfo.getFilterInfo(output.getPrevFilter());
        

        InterFilterEdge[][] initDests = 
            new InterFilterEdge[fi.totalItemsSent(SchedulingPhase.INIT)][];
        unrollHelperOutput(output.getDests(SchedulingPhase.INIT),
                output.getWeights(SchedulingPhase.INIT),
                initDests);
        output.setInitDests(initDests);
        output.setInitWeights(makeOnesArray(initDests.length));


       
        if (output.getDests(SchedulingPhase.STEADY).length > 0) {
            InterFilterEdge[][] dests = 
                new InterFilterEdge[fi.totalItemsSent(SchedulingPhase.STEADY)][];
            unrollHelperOutput(output.getDests(SchedulingPhase.STEADY),
                    output.getWeights(SchedulingPhase.STEADY),
                    dests);
            output.setDests(dests);
            output.setWeights(makeOnesArray(dests.length));
        }
    }
    
    private static int[] makeOnesArray(int size) {
        int[] ret = new int[size];
        Arrays.fill(ret, 1);
        return ret;
    }
    
    /**
     * Fill in <unrolled> with the unrolled <src> and <weights>.
     */
    private static void unrollHelperInput(InterFilterEdge[] src, int weights[], 
            InterFilterEdge[] unrolled) {
        assert src.length == weights.length;
        
        int weightsSum = 0;
        for (int weight : weights) 
            weightsSum += weight;
        
        if (weightsSum == 0)
            return;
        
        assert unrolled.length % weightsSum == 0;
        int index = 0;
        
        for (int rep = 0; rep < unrolled.length / weightsSum; rep++) {
            for (int w = 0; w < weights.length; w++) {
                InterFilterEdge obj = src[w];
                for (int i = 0; i < weights[w]; i++) {
                    unrolled[index++] = obj; 
                }                 
            }
        }
        
        assert index == unrolled.length;
    }
    
    /**
     * Fill in <unrolled> with the unrolled <src> and <weights>.
     */
    private static void unrollHelperOutput(InterFilterEdge[][] src, int weights[], 
            InterFilterEdge[][] unrolled) {
        assert src.length == weights.length;
        
        int weightsSum = 0;
        for (int weight : weights) 
            weightsSum += weight;

        if (weightsSum == 0)
            return;

        assert unrolled.length % weightsSum == 0;
                
        int index = 0;
        
        for (int rep = 0; rep < unrolled.length / weightsSum; rep++) {
            for (int w = 0; w < weights.length; w++) {
                InterFilterEdge[] obj = src[w].clone();
                for (int i = 0; i < weights[w]; i++) {
                    unrolled[index++] = obj; 
                }                 
            }
        }
        
        assert index == unrolled.length;
    }
}
