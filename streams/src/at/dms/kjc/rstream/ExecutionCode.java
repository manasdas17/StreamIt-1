package at.dms.kjc.rstream;

import java.util.HashMap;
import java.util.List;

import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.Constants;
import at.dms.kjc.JBlock;
import at.dms.kjc.JBooleanLiteral;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JStatement;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.JWhileStatement;
import at.dms.kjc.ObjectDeepCloner;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRTwoStageFilter;

/**
 * This class creates the main() function for the rstream 
 * code in SIR and places the function in the filter's function list.
 * The function first calls the init() function of the fused filter,
 * then if necessary, the pre-work function, and finally, we loop 
 * the work() function.  The work function and the init function
 * are inlined in the code.  This class also checks the validity of
 * the schedule generated by the scheduler for the single fused filter.
 *
 * @author Michael Gordon
 * 
 */

public class ExecutionCode extends at.dms.util.Utils 
    implements Constants
{

    private ExecutionCode(){}
    
    /**
     * This static method creates the main function as described 
     * in the class comment.  It first checks that the filter 
     * has no inputs/outputs and does not declare any I/O rates
     * Then it creates the function and attaches it to the filter.
     * 
     * @param top The single flatnode that contains the single filter of the
     *            app.
     * @param executionCounts The schedule for the stream graph as calculated
     *                        by the scheduler.
     */

    public static void doit(FlatNode top, HashMap[] executionCounts) 
    {
        assert top.contents instanceof SIRFilter :
            "Error top of graph is not filter";
    
        assert top.inputs == 0 && top.ways == 0 :
            "Error: Fused filter contains neighbors";
    
        SIRFilter filter = (SIRFilter)top.contents;
    
        assert filter.getPushInt() == 0 &&
            filter.getPopInt() == 0 &&
            filter.getPeekInt() == 0 : 
            "Error: fused filter declares non-zero I/O rate(s)";

        //make sure there are no push, pops or peeks...
        assert CheckForCommunication.check(filter) == false : 
            "Error: Communication expression found in filter";

        //  assert !(filter instanceof SIRTwoStageFilter) :
        //    "Error: Fused filter is a two stage";

        ExecutionCode exeCode = new ExecutionCode();
    
        //check to see if the schedule is valid and
        //set the number of times the filter fires in the init
        //stage (weird)
        exeCode.checkSchedule(executionCounts, filter);
    

    
        //create the main function of the C code that will call
        // the filter
        JBlock block = exeCode.mainFunction(filter);
    
        //create the method and add it to the filter
        JMethodDeclaration mainFunct = 
            new JMethodDeclaration(null, 
                                   at.dms.kjc.Constants.ACC_PUBLIC,
                                   CStdType.Void,
                                   Names.main,
                                   JFormalParameter.EMPTY,
                                   CClassType.EMPTY,
                                   block,
                                   null,
                                   null);
        //make the main the new work function
        filter.setWork(mainFunct);
    }

    /**
     * Construct the main function.
     *
     * @param filter The single SIRFilter of the application.
     *
     * @return A JBlock with the statements of the main function.
     *
     */

    private JBlock mainFunction(SIRFilter filter)
    {

        JBlock statements = new JBlock(null, new JStatement[0], null);

        //create the params list, for some reason 
        //calling toArray() on the list breaks a later pass
        List paramList = filter.getParams();
        JExpression[] paramArray;
        if (paramList == null || paramList.size() == 0)
            paramArray = new JExpression[0];
        else
            paramArray = (JExpression[])paramList.toArray(new JExpression[0]);
    
        //add the call to the init function
        statements.addStatement
            (new 
             JExpressionStatement(null,
                                  new JMethodCallExpression
                                  (null,
                                   new JThisExpression(null),
                                   filter.getInit().getName(),
                                   paramArray),
                                  null));

        //add the call to the pre(init)work function if this filter
        //is a two stage..
        if (filter instanceof SIRTwoStageFilter) {
            SIRTwoStageFilter two = (SIRTwoStageFilter)filter;
            statements.addStatement
                (new JExpressionStatement(null,
                                          new JMethodCallExpression
                                          (null,
                                           new JThisExpression(null),
                                           two.getInitWork().getName(),
                                           new JExpression[0]),
                                          null));
        }

    
        //add the call to the work function
        statements.addStatement(generateSteadyStateLoop(filter));
    
        return statements;
    }

    
    /**
     * generate the code for the steady state loop.  Inline the
     * work function inside of an infinite while loop.
     *
     *
     * @param filter The single fused filter of the application.
     *
     *
     * @return A JStatement that is a while loop with the 
     *         work function inlined.
     * 
     */
    JStatement generateSteadyStateLoop(SIRFilter filter)
    {
    
        JBlock block = new JBlock(null, new JStatement[0], null);


        JBlock workBlock = 
            (JBlock)ObjectDeepCloner.
            deepCopy(filter.getWork().getBody());

        //add the cloned work function to the block
        block.addStatement(workBlock);
    
        //return the infinite loop
        return new JWhileStatement(null, 
                                   new JBooleanLiteral(null, true),
                                   block, 
                                   null);
    }

    /**
     * Check the schedule to see if it is correctly formed.
     * First check to see that the single filter does not 
     * execute in the init stage, and it is the only filtered
     * scheduled.
     *
     * @param executionCounts The execution counts hashmaps created
     by the scheduler
     * @param filter The single fused filter representing the application
     *
     * @return Returns true if the constructed schedule fits the 
     *         criteria.
     *
     */    
    private void checkSchedule(HashMap[] executionCounts,
                               SIRFilter filter) 
    {
        //executionCounts[0] = init
        //executionCounts[1] = steady
    
        //first check the initialization schedule 
        //it should be null or the only thing in there should
        //be the filter with a zero execution count
        assert executionCounts[0].keySet().size() == 0 || 
            executionCounts[0].keySet().size() == 1;
    
        if (executionCounts[0].keySet().size() == 1) {
            assert executionCounts[0].keySet().contains(filter);
            //implication, if a filter is a two stage, then
            //its prework is always scheduled in the init stage, 
            //so make sure the prework is the only thing scheduled.
            if (filter instanceof SIRTwoStageFilter)
                assert ((int[])executionCounts[0].get(filter))[0] == 1;
            else  //not twostage, shouldn't be schedule in init
                assert ((int[])executionCounts[0].get(filter))[0] == 0;
        }

        //now check the steady-state schedule and make sure
        //that the filter is the only thing in there and that
        //it executes
        assert executionCounts[1].keySet().size() == 1;
        assert executionCounts[1].keySet().contains(filter);
        assert ((int[])executionCounts[1].get(filter))[0] > 0;
    }
 
}


