package streamit;

import java.lang.reflect.*;
import java.util.*;

// an operator takes N inputs and produces N outputs.
// Never explicitly instantiated
public class Operator extends DestroyedClass
{
    ParameterContainer initParams;
    boolean initialized = false;

    public Operator(float x1, float y1, int z1)
    {
        initParams = new ParameterContainer ("float-float-int")
            .add("x1", x1)
            .add("y1", y1)
            .add("z1", z1);
    }

    public Operator(int a, float b)
    {
        initParams = new ParameterContainer("int-float")
            .add("a", a)
            .add("b", b);
    }

    public Operator(float a, float b, float c)
    {
        initParams = new ParameterContainer("float-float-float")
            .add("a", a)
            .add("b", b)
            .add("c", c);
    }

    public Operator(float x3, float y3, int z3, int a3)
    {
        initParams = new ParameterContainer ("float-float-int-int")
            .add("x3", x3)
            .add("y3", y3)
            .add("z3", z3)
            .add("a3", a3);
    }

    public Operator(int a, int b, float c, int d, float e)
    {
        initParams = new ParameterContainer ("int-int-float-int-float")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e);
    }

    public Operator(int a, int b, int c, float d, float e)
    {
        initParams = new ParameterContainer ("int-int-int-float-float")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e);
    }

    public Operator(int a, int b, int c, float d, int e)
    {
        initParams = new ParameterContainer ("int-int-int-float-int")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e);
    }

    public Operator(float x2, float y2, float z2, int a2, float b2)
    {
        initParams = new ParameterContainer ("float-float-float-int-float")
            .add("x2", x2)
            .add("y2", y2)
            .add("z2", z2)
            .add("a2", a2)
            .add("b2", b2);
    }

    public Operator(float x2, float y2, float z2, int a2)
    {
        initParams = new ParameterContainer ("float-float-float-int")
            .add("x2", x2)
            .add("y2", y2)
            .add("z2", z2)
            .add("a2", a2);
    }

    public Operator()
    {
        initParams = new ParameterContainer ("");
    }

    public Operator(int n)
    {
        initParams = new ParameterContainer ("int").add ("n", n);
    }

    public Operator(char c)
    {
        initParams = new ParameterContainer ("char").add ("c", c);
    }

    public Operator (int x, int y)
    {
        initParams = new ParameterContainer ("int-int").add ("x", x).add ("y", y);
    }

    public Operator (int x, int y, int z)
    {
        initParams = new ParameterContainer ("int-int-int").add ("x", x).add ("y", y).add("z", z);
    }

    public Operator (int x, int y, int z,
                     int a, float b)
    {
        initParams = new ParameterContainer ("int-int-int-int-float").add ("x", x).add ("y", y).add("z", z).add ("a", a).add ("b", b);
    }

    public Operator (int x, int y, int z,
                     int a, int b, int c)
    {
        initParams = new ParameterContainer ("int-int-int-int-int-int").add ("x", x).add ("y", y).add("z", z).add ("a", a).add ("b", b).add("c", c);
    }

    public Operator(float f)
    {
        initParams = new ParameterContainer ("float").add ("f", f);
    }

    public Operator(String str)
    {
        initParams = new ParameterContainer ("String").add ("str", str);
    }

    public Operator(ParameterContainer params)
    {
        initParams = new ParameterContainer ("ParameterContainer").add ("params", params);
    }

    // INIT FUNCTIONS ---------------------------------------------------------------------

    void invalidInitError ()
    {
        ERROR ("You didn't provide a valid init function in class " + getClass ().getName () + ".\nFilters now need init functions, even if they're empty.\nPerhaps you're passing parameters that don't have a valid prototype yet?\nCheck streams/docs/implementation-notes/library-init-functions.txt for instructions on adding new signatures to init functions.");
    }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, float z, int a, float b) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, float z, int a) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, int z) { invalidInitError (); }

    public void init(float a, float b, float c) { invalidInitError(); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, float c, int d, float e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float d, float e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, float b) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float d, int e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, int z, int a) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init() { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int n) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z, int a, float b) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z, int a, int b, int c) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float f) { invalidInitError (); }

    // initialization functions, to be over-ridden
    public void init(char c) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(String str) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(ParameterContainer params) { invalidInitError (); }

    // initIO initializes all input/output channels
    // as required
    final public void initIO ()
    {
        // You must not provide an initIO function
        // to initialize
        ASSERT (false);
    }

    public static MessageStub MESSAGE_STUB;

    // initialize the MESSAGE_STUB
    static {
        MESSAGE_STUB = MessageStub.STUB;
    }

    // allSinks is used for scheduling the streaming graph
    public static LinkedList allSinks;
    public static LinkedList allFilters;
    public static HashSet fullChannels;

    static {
        allSinks = new LinkedList ();
        allFilters = new LinkedList ();
        fullChannels = new HashSet ();
    }

    void addSink ()
    {
        allSinks.add (this);
    }

    void addFilter ()
    {
        allFilters.add (this);
    }

    void runSinks ()
    {
        ListIterator iter;

        if (!allSinks.isEmpty ())
            iter = allSinks.listIterator ();
        else
            iter = allFilters.listIterator ();

        // go over all the sinks
        while (iter.hasNext ())
        {
            Operator sink;
            sink = (Operator) iter.next ();
            ASSERT (sink != null);

            // do bunch of work
            int i;
            for (i = 0; i < 10; i++)
            {
                sink.work ();
            }
        }
    }

    void drainChannels ()
    {
        while (!fullChannels.isEmpty ())
        {
                // empty any full channels:
            Iterator fullChannel;
                fullChannel = fullChannels.iterator ();

            Channel ch = (Channel) fullChannel.next ();
            ASSERT (ch != null);

            ch.getSink ().work ();
             }
    }


    void addFullChannel (Channel channel)
    {
        fullChannels.add (channel);
    }

    void removeFullChannel (Channel channel)
        {
                fullChannels.remove (channel);
        }

    public static void passOneData (Channel from, Channel to)
    {
        Class type = from.getType ();
        SASSERT (type == to.getType ());

        if (type == Integer.TYPE)
        {
            to.pushInt (from.popInt ());
        } else
        if (type == Short.TYPE)
        {
            to.pushShort (from.popShort ());
        } else
        if (type == Character.TYPE)
        {
            to.pushChar (from.popChar ());
        } else
        if (type == Float.TYPE)
        {
            to.pushFloat (from.popFloat ());
        } else
        if (type == Double.TYPE)
        {
            to.pushDouble (from.popDouble ());
        } else {
            to.push (from.pop ());
        }
    }

    public static void duplicateOneData (Channel from, Channel [] to)
    {
        Class type = from.getType ();
        SASSERT (to != null && type == to[0].getType ());

        if (type == Integer.TYPE)
        {
            int data = from.popInt ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .pushInt (data);
            }
        } else
        if (type == Short.TYPE)
        {
            short data = from.popShort ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .pushShort (data);
            }
        } else
        if (type == Character.TYPE)
        {
            char data = from.popChar ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .pushChar (data);
            }
        } else
        if (type == Float.TYPE)
        {
            float data = from.popFloat ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .pushFloat (data);
            }
        } else
        if (type == Double.TYPE)
        {
            double data = from.popDouble ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .pushDouble (data);
            }
        } else {
            Object data = from.pop ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .push (data);
            }
        }
    }

    // send a message to a handler that returns <stub> within <delay>
    // units of my input/output (to be specified more clearly...)
    public void sendMessage(MessageStub stub, int delay) {}

    // send a message to a handler that returns <stub> at the earliest
    // convenient time.
    public void sendMessage(MessageStub stub) {}

    protected static class MessageStub
    {
        private static MessageStub STUB = new MessageStub();
        private MessageStub() {}
    }

    // a prototype work function
    void work () { }

    // this function will take care of all appropriate calls to init:
    void setupOperator ()
    {
        // don't re-initialize
        if (initialized) return;

        ASSERT (initParams != null);

        if(initParams.getParamName().equals("int-int-int-int-float"))
            init (initParams.getIntParam("x"),
                  initParams.getIntParam("y"),
                  initParams.getIntParam("z"),
                  initParams.getIntParam("a"),
                  initParams.getFloatParam("b"));
        else
        if(initParams.getParamName().equals("int-int-int-int-int-int"))
            init (initParams.getIntParam("x"),
                  initParams. getIntParam("y"),
                  initParams.getIntParam("z"),
                  initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"));
        else
        if(initParams.getParamName().equals("int-float"))
            init (initParams.getIntParam("a"),
                  initParams.getFloatParam("b"));
        else
        if(initParams.getParamName().equals("float-float-int"))
            init (initParams.getFloatParam("x1"),
                  initParams.getFloatParam("y1"),
                  initParams.getIntParam("z1"));
        else
        if(initParams.getParamName().equals("float-float-int-int"))
            init (initParams.getFloatParam("x3"),
                  initParams.getFloatParam("y3"),
                  initParams.getIntParam("z3"),
                  initParams.getIntParam("a3"));
        else
        if(initParams.getParamName().equals("float-float-float-int-float"))
            init (initParams.getFloatParam("x2"),
                  initParams.getFloatParam("y2"),
                  initParams.getFloatParam("z2"),
                  initParams.getIntParam("a2"),
                  initParams.getFloatParam("b2"));
        else
        if(initParams.getParamName().equals("int-int-float-int-float"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getFloatParam("c"),
                  initParams.getIntParam("d"),
                  initParams.getFloatParam("e"));
        else
        if(initParams.getParamName().equals("int-int-int-float-float"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"),
                  initParams.getFloatParam("d"),
                  initParams.getFloatParam("e"));
        else
        if(initParams.getParamName().equals("int-int-int-float-int"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"),
                  initParams.getFloatParam("d"),
                  initParams.getIntParam("e"));
        else
        if(initParams.getParamName().equals("float-float-float-int"))
            init (initParams.getFloatParam("x2"),
                  initParams.getFloatParam("y2"),
                  initParams.getFloatParam("z2"),
                  initParams.getIntParam("a2"));
        else

        if (initParams.getParamName ().equals("int-int")) init (initParams.getIntParam ("x"), initParams.getIntParam ("y")); else
        if (initParams.getParamName ().equals("int-int-int")) init (initParams.getIntParam ("x"), initParams.getIntParam ("y"), initParams.getIntParam ("z")); else
        if (initParams.getParamName ().equals("float-float-float")) init (initParams.getFloatParam ("a"), initParams.getFloatParam ("b"), initParams.getFloatParam ("c")); else
        if (initParams.getParamName ().equals("")) init (); else
        if (initParams.getParamName ().equals("int")) init (initParams.getIntParam ("n")); else
        if (initParams.getParamName ().equals("float")) init (initParams.getFloatParam ("f")); else
        if (initParams.getParamName ().equals("char")) init (initParams.getCharParam ("c")); else
        if (initParams.getParamName ().equals("String")) init (initParams.getStringParam ("str")); else
        if (initParams.getParamName ().equals("ParameterContainer")) init ((ParameterContainer) initParams.getObjParam ("params"));
        else {
            // labels don't match - print an error
            ERROR ("You didn't provide a correct if-else statement in setupOperator.\nPlease read streams/docs/implementation-notes/library-init-functions.txt for instructions.");
        }

        connectGraph ();
        initialized = true;
    }

    public void connectGraph ()
    {
        ASSERT (false);
    }

    // ------------------------------------------------------------------
    // ------------------ all graph related functions -------------------
    // ------------------------------------------------------------------

    // get an IO field (input or output)
    // returns null if none present
    Channel[] getIOFields (String fieldName)
    {
        ASSERT (fieldName == "input" || fieldName == "output");

        Channel fieldsInstance [] = null;

        try
        {
            Class thisClass = this.getClass ();
            ASSERT (thisClass != null);

            Field ioField;
            ioField  = thisClass.getField (fieldName);

            Object fieldValue = ioField.get (this);

            if (ioField.getType ().isArray ())
            {
                fieldsInstance = (Channel []) fieldValue;
            } else {
                fieldsInstance = new Channel [1];
                fieldsInstance [0] = (Channel) fieldValue;

                if (fieldsInstance [0] == null) fieldsInstance = null;
            }
        }
        catch (NoSuchFieldException noError)
        {
            // do not do anything here, this is NOT an error!
        }
        catch (Throwable error)
        {
            // this is all the other errors:
            error.getClass ();
            ASSERT (false);
        }

        return fieldsInstance;
    }

    Channel getIOField (String fieldName, int fieldIndex)
    {
        Channel field = null;

        {
            Channel fieldInstance[];
            fieldInstance = getIOFields (fieldName);

            if (fieldInstance != null)
            {
                ASSERT (fieldInstance.length > fieldIndex);
                field = fieldInstance [fieldIndex];
            }
        }

        return field;
    }

    void setIOField (String fieldName, int fieldIndex, Channel newChannel)
    {
        ASSERT (fieldName == "input" || fieldName == "output");

        Channel fieldsInstance [];

        try
        {
            Class thisClass = this.getClass ();
            ASSERT (thisClass != null);

            Field ioField;
            ioField  = thisClass.getField (fieldName);

            if (ioField.getType () == newChannel.getClass ())
            {
                ASSERT (fieldIndex == 0);
                ioField.set (this, newChannel);
            } else {
                fieldsInstance = (Channel []) ioField.get (this);
                ASSERT (fieldsInstance != null);
                ASSERT (fieldsInstance.length > fieldIndex);

                fieldsInstance [fieldIndex] = newChannel;
            }

        }
        catch (Throwable error)
        {
            // this is all the other errors:
            ASSERT (false);
        }
    }
}
