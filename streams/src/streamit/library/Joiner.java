package streamit;

import streamit.scheduler.SchedJoinType;

import java.util.*;

// many inputs, 1 output
public class Joiner extends Operator
{
    List srcs = new ArrayList ();
    int inputIndex = 0;

    public Channel input [] = null;
    public Channel output = null;

    public void initIO () { }
    public void init () { }

    void add (Stream s)
    {
        srcs.add (s);
    }

    public boolean isInputUsed (int index)
    {
        return true;
    }

    public void connectGraph ()
    {
        // do I even have anything to do?
        if (srcs.isEmpty ()) return;

        // yep, create an input array of appropriate size
        input = new Channel [srcs.size ()];

        // yep, go through my members and connect them all with
        // ChannelConnectFilter
        int inputIndx = 0;
        ListIterator iter = srcs.listIterator ();
        while (iter.hasNext ())
        {
            // connect the input streams:
            Stream s = (Stream) iter.next ();

            // it is possible for a stream to be null - if I'm doing a
            // weighted joiner and I really don't have the stream!
            if (s != null)
            {
                s.setupOperator ();

                // retrieve the output of this filter, which will be an
                // input to this joiner
                Channel channel = s.getIOField ("streamOutput");
                input [inputIndx] = channel;

                // if it is not a sink, make sure that it produces data
                // of the same kind as everything else in this Joiner
                if (channel != null)
                {
                    // handle input channel
                    if (output == null)
                    {
                        output = new Channel (channel);
                        output.setSource (this);
                    } else {
                        // check that the input types agree
                        ASSERT (channel.getType ().getName ().equals (output.getType ().getName ()));
                    }

                    // now connect the channel to me
                    channel.setSink (this);
                }
            }

            inputIndx ++;
        }
    }

    public void work ()
    {
        ASSERT (false);
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedJoinType getSchedType ()
    {
        // you must override this function!
        ASSERT (false);
        return null;
    }
}


