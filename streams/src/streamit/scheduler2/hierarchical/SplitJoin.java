package streamit.scheduler2.hierarchical;

/* $Id: SplitJoin.java,v 1.7 2002-12-02 23:54:09 karczma Exp $ */

import streamit.scheduler2.iriter./*persistent.*/
SplitJoinIter;
import streamit.scheduler2.base.StreamFactory;
import streamit.scheduler2.Schedule;

/**
 * This class provides the required functions to implement a schduler
 * for a SplitJOin.  Mostly, it simply implements wrappers for functions
 * in StreamInterface and passes them on to the StreamAlgorithm.  This
 * is necessary, 'cause Java doesn't support multiple inheritance.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class SplitJoin
    extends streamit.scheduler2.base.SplitJoin
    implements StreamInterfaceWithSnJ
{
    final private StreamAlgorithmWithSnJ algorithm =
        new StreamAlgorithmWithSnJ(this);

    final private PhasingSchedule splitPhases[];
    final private PhasingSchedule joinPhases[];

    public SplitJoin(SplitJoinIter iterator, StreamFactory factory)
    {
        super(iterator, factory);

        // pre-compute the splitter phases:
        {
            splitPhases = new PhasingSchedule[splitjoin.getSplitterNumWork()];

            int nPhase;
            for (nPhase = 0;
                nPhase < splitjoin.getSplitterNumWork();
                nPhase++)
            {
                Schedule sched =
                    new Schedule(
                        splitjoin.getSplitterWork(nPhase),
                        splitjoin.getUnspecializedIter());
                int popAmount = splitjoin.getSplitPop(nPhase);
                splitPhases[nPhase] =
                    new PhasingSchedule(this, sched, popAmount, popAmount, 0);
            }
        }

        // pre-compute the joiner phases
        {
            joinPhases = new PhasingSchedule[splitjoin.getJoinerNumWork()];

            int nPhase;
            for (nPhase = 0; nPhase < splitjoin.getJoinerNumWork(); nPhase++)
            {
                Schedule sched =
                    new Schedule(
                        splitjoin.getJoinerWork(nPhase),
                        splitjoin.getUnspecializedIter());
                int pushAmount = splitjoin.getJoinPush(nPhase);
                joinPhases[nPhase] =
                    new PhasingSchedule(this, sched, 0, 0, pushAmount);
            }
        }
    }

    /**
     * compute the initialization and steady state schedules
     */
    abstract public void computeSchedule();

    /**
     * Return an appropriate hierarchical child.  All children of a 
     * hierarchical splitjoin must be hierarchical as well.  This function
     * asserts if a child is not hierarchical.
     * @return hierarchical child of the splitjoin
     */
    protected StreamInterface getHierarchicalChild(int nChild)
    {
        streamit.scheduler2.base.StreamInterface child;
        child = getChild(nChild);

        if (!(child instanceof StreamInterface))
        {
            ERROR("This splitjoin contains a child that is not hierarchical");
        }

        return (StreamInterface) child;
    }

    public int getNumSplitPhases()
    {
        return splitjoin.getSplitterNumWork();
    }

    public PhasingSchedule getSplitPhase(int nPhase)
    {
        ASSERT(nPhase >= 0 && nPhase < getNumSplitPhases());
        return splitPhases[nPhase];
    }

    public int getNumJoinPhases()
    {
        return splitjoin.getJoinerNumWork();
    }

    public PhasingSchedule getJoinPhase(int nPhase)
    {
        ASSERT(nPhase >= 0 && nPhase < getNumJoinPhases());
        return joinPhases[nPhase];
    }

    public streamit.scheduler2.base.StreamInterface getTop()
    {
        return this;
    }

    public streamit.scheduler2.base.StreamInterface getBottom()
    {
        return this;
    }

    // These functions implement wrappers for StreamAlgorithm
    // I have to use this stupid style of coding to accomodate
    // Java with its lack of multiple inheritance

    public int getInitPeek()
    {
        return algorithm.getInitPeek();
    }

    public int getInitPop()
    {
        return algorithm.getInitPop();
    }

    public int getInitPush()
    {
        return algorithm.getInitPush();
    }

    public int getNumInitStages()
    {
        return algorithm.getNumInitStages();
    }

    public int getInitStageNumPeek(int stage)
    {
        return algorithm.getInitStageNumPeek(stage);
    }

    public int getInitStageNumPop(int stage)
    {
        return algorithm.getInitStageNumPop(stage);
    }

    public int getInitStageNumPush(int stage)
    {
        return algorithm.getInitStageNumPush(stage);
    }

    public PhasingSchedule getInitScheduleStage(int stage)
    {
        return algorithm.getInitScheduleStage(stage);
    }

    public PhasingSchedule getPhasingInitSchedule()
    {
        return algorithm.getPhasingInitSchedule();
    }

    public Schedule getInitSchedule()
    {
        return algorithm.getInitSchedule();
    }

    public void addInitScheduleStage(PhasingSchedule newStage)
    {
        algorithm.addInitScheduleStage(newStage);
    }

    public int getNumSteadyPhases()
    {
        return algorithm.getNumSteadyPhases();
    }

    public int getSteadyPhaseNumPeek(int phase)
    {
        return algorithm.getSteadyPhaseNumPeek(phase);
    }

    public int getSteadyPhaseNumPop(int phase)
    {
        return algorithm.getSteadyPhaseNumPop(phase);
    }

    public int getSteadyPhaseNumPush(int phase)
    {
        return algorithm.getSteadyPhaseNumPush(phase);
    }

    public PhasingSchedule getSteadySchedulePhase(int phase)
    {
        return algorithm.getSteadySchedulePhase(phase);
    }

    public PhasingSchedule getPhasingSteadySchedule()
    {
        return algorithm.getPhasingSteadySchedule();
    }

    public Schedule getSteadySchedule()
    {
        return algorithm.getSteadySchedule();
    }

    public void addSteadySchedulePhase(PhasingSchedule newPhase)
    {
        algorithm.addSteadySchedulePhase(newPhase);
    }

    public void advanceChildInitSchedule(StreamInterface child)
    {
        algorithm.advanceChildInitSchedule(child, 1);
    }

    public void advanceChildInitSchedule(
        StreamInterface child,
        int numStages)
    {
        algorithm.advanceChildInitSchedule(child, numStages);
    }

    public void advanceChildSteadySchedule(StreamInterface child)
    {
        algorithm.advanceChildSteadySchedule(child, 1);
    }

    public void advanceChildSteadySchedule(
        StreamInterface child,
        int numPhases)
    {
        algorithm.advanceChildSteadySchedule(child, numPhases);
    }

    public PhasingSchedule getChildInitStage(
        StreamInterface child,
        int nStage)
    {
        return algorithm.getChildInitStage(child, nStage);
    }

    public PhasingSchedule getChildNextInitStage(StreamInterface child)
    {
        return algorithm.getChildInitStage(child, 0);
    }
    public PhasingSchedule getChildSteadyPhase(
        StreamInterface child,
        int nPhase)
    {
        return algorithm.getChildSteadyPhase(child, nPhase);
    }

    public PhasingSchedule getChildNextSteadyPhase(StreamInterface child)
    {
        return algorithm.getChildSteadyPhase(child, 0);
    }

    // These functions implement wrappers for StreamAlgorithmWithSnJ
    // I have to use this stupid style of coding to accomodate
    // Java with its lack of multiple inheritance

    public void advanceSplitSchedule(int numPhases)
    {
        algorithm.advanceSplitSchedule(numPhases);
    }

    public void advanceSplitSchedule()
    {
        algorithm.advanceSplitSchedule(1);
    }

    public void advanceJoinSchedule(int numPhases)
    {
        algorithm.advanceJoinSchedule(numPhases);
    }

    public void advanceJoinSchedule()
    {
        algorithm.advanceJoinSchedule(1);
    }

    public PhasingSchedule getSplitSteadyPhase(int nPhase)
    {
        return algorithm.getSplitSteadyPhase(nPhase);
    }
    
    public PhasingSchedule getNextSplitSteadyPhase ()
    {
        return getSplitSteadyPhase (0);
    }

    public PhasingSchedule getJoinSteadyPhase(int nPhase)
    {
        return algorithm.getJoinSteadyPhase(nPhase);
    }
    
    public PhasingSchedule getNextJoinSteadyPhase ()
    {
        return getJoinSteadyPhase (0);
    }

    public SplitFlow getSplitSteadyPhaseFlow (int nPhase)
    {
        return algorithm.getSplitSteadyPhaseFlow (nPhase);
    }
    
    public SplitFlow getNextSplitSteadyPhaseFlow ()
    {
        return getSplitSteadyPhaseFlow (0);
    }
    
    public JoinFlow getJoinSteadyPhaseFlow (int nPhase)
    {
        return algorithm.getJoinSteadyPhaseFlow (nPhase);
    }

    public JoinFlow getNextJoinSteadyPhaseFlow ()
    {
        return getJoinSteadyPhaseFlow (0);
    }
}
