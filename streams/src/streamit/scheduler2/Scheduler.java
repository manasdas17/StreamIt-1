/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.scheduler2;

import streamit.misc.AssertedClass;
import streamit.scheduler2.iriter.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * <dl>
 * <dt>Purpose: Main scheduler class to be used as an interface for all 
 * schedulers
 * <dd>
 *
 * <dt>Description:
 * <dd> This class represents a scheduler which should be used by outside users
 * to obtain, optimize and find out information about a schedule. This is an
 * abstract class, and every particular scheduler should implement the missing
 * virtual classes.
 * </dl>
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class Scheduler extends AssertedClass
{
    protected Schedule initSchedule = null;
    protected Schedule steadySchedule = null;
    Schedule optimizedInitSchedule = null;
    Schedule optimizedSteadySchedule = null;
    ScheduleBuffers scheduleBuffers = null;
    protected Iterator root;
    ScheduleOptimizer optimizer;

    public Scheduler(Iterator _root)
    {
        root = _root;
    }

    /**
     * compute a schedule.
     * This function computes a schedule corresponding to a particular
     * stream structure. It must reset optimizedSchedule and scheduleBuffers
     * to null (or compute them).
     */
    abstract public void computeSchedule();

    void optimizeSchedule()
    {
        if (optimizer == null)
        {
            optimizer = new ScheduleOptimizer(initSchedule, steadySchedule, this);
            optimizer.optimize();
            optimizedInitSchedule = optimizer.getOptimizedInitSched();
            optimizedSteadySchedule = optimizer.getOptimizedSteadySched();
        }
    }

    public Schedule getOptimizedInitSchedule()
    {
        assert initSchedule != null && steadySchedule != null;

        optimizeSchedule();
        return optimizedInitSchedule;
    }

    public Schedule getOptimizedSteadySchedule()
    {
        assert initSchedule != null && steadySchedule != null;

        optimizeSchedule();
        return optimizedSteadySchedule;
    }

    public void computeBufferUse()
    {
        if (scheduleBuffers == null)
        {
            scheduleBuffers = new ScheduleBuffers(root);
            scheduleBuffers.computeBuffersFor(getOptimizedInitSchedule ());
            scheduleBuffers.computeBuffersFor(getOptimizedSteadySchedule ());
        }
    }

    public int getBufferSizeBetween(
        Iterator userBefore,
        Iterator userAfter)
    {
        computeBufferUse();
        return scheduleBuffers.getBufferSizeBetween(userBefore, userAfter);
    }
    
    /*
     * Schedule printing utilities
     */

    public void printSchedule(Schedule initSched, Schedule steadySched)
    {
        computeSchedule();

        Map scheds = new HashMap();

        System.out.println("init = [");
        printSched(initSched, scheds);
        System.out.println("]");

        System.out.println("steady = [");
        printSched(steadySched, scheds);
        System.out.println("]");
    }

    public void printUnoptimizedSchedule()
    {
        computeSchedule();
        printSchedule(initSchedule, steadySchedule);
    }

    public void printOptimizedSchedule()
    {
        printSchedule(
            getOptimizedInitSchedule(),
            getOptimizedSteadySchedule());
    }

    private void printSched(Schedule sched, Map scheds)
    {
        // don't print duplicates
        if (scheds.containsKey(sched))
            return;

        if (!sched.isBottomSchedule())
        {
            for (int nPhase = 0; nPhase < sched.getNumPhases(); nPhase++)
            {
                // print the children first
                printSched(sched.getSubSched(nPhase), scheds);
            }

            int symbolicIdx = scheds.size();
            scheds.put(sched, new Integer (symbolicIdx));

            // and now print self:
            System.out.print("$" + symbolicIdx + " = { ");
            for (int nPhase = 0; nPhase < sched.getNumPhases(); nPhase++)
            {
                int times = sched.getSubSchedNumExecs(nPhase);
                int idx =
                    ((Integer)scheds.get(sched.getSubSched(nPhase)))
                        .intValue();

                if (times > 1)
                    System.out.print("{" + times + " $" + idx + "} ");
                else
                    System.out.print("$" + idx + " ");

            }
            System.out.println("}");
        }
        else
        {
            // this is an actual leaf - create a vector with just
            // a single entry - the schedule
            int symbolicIdx = scheds.size();
            scheds.put(sched, new Integer (symbolicIdx));
            System.out.println(
			       "$"
			       + symbolicIdx
			       + " = "
			       + sched.getStream().getObject() + "@" + sched.getStream().getObject().hashCode()
			       + "."
			       + sched.getWorkFunc());
        }
    }

    /**
     * Returns a two-dimensional array HashMap's that map each
     * splitter, joiner, & filter in <str> to a 1-dimensional int
     * array containing the count for how many times that operator
     * executes:
     *
     *  result[0] = map for initializaiton schedule
     *  result[1] = map for steady-state schedule
     */     
    public HashMap[] getExecutionCounts() {
	// make the result
	HashMap[] result = { new HashMap(), new HashMap() } ;

	// fill in the init schedule
	fillExecutionCounts(getOptimizedInitSchedule(), result[0], 1);
	// fill in the steady-state schedule
	fillExecutionCounts(getOptimizedSteadySchedule(), result[1], 1);
	
	return result;
    }
    
    // Creates execution counts of filters in graph.
    private void fillExecutionCounts(Schedule schedule, HashMap counts, int numReps) {
	if (schedule.isBottomSchedule()) {
	    // tally up for this node.
	    Object target = schedule.getStream().getObject();
	    if (!counts.containsKey(target)) {
		// initialize counter
		int[] wrapper = { numReps };
		counts.put(target, wrapper);
	    } else {
		// add to counter
		int[] wrapper = (int[])counts.get(target);
		wrapper[0] += numReps;
	    }	    
	} else {
	    // otherwise we have a container, so simulate execution of
	    // children
	    for (int i=0; i<schedule.getNumPhases(); i++) {
		fillExecutionCounts(schedule.getSubSched(i), counts, numReps * schedule.getSubSchedNumExecs(i));
	    }
	}
    }

    /**
     * Prints repetition info to screen.
     */
    public void printReps() {
	HashMap[] counts = getExecutionCounts();
	// print init schedule
	for (int i=0; i<2; i++) {
	    System.out.println("Repetitions in " + (i==0 ? "initial" : "steady") + " schedule:");
	    java.util.Set keys = counts[i].keySet();
	    for (java.util.Iterator it = keys.iterator(); it.hasNext(); ) {
		Object obj = it.next();
		int[] reps = (int[])counts[i].get(obj);
		System.out.println(reps[0] + " reps for " + obj + " (hashcode=" + obj.hashCode() + ")");
	    }
	    System.out.println();
	}
    }
}
