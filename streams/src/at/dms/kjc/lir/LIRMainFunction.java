package at.dms.kjc.lir;

import java.util.LinkedList;

import at.dms.kjc.JStatement;
import at.dms.kjc.SLIRVisitor;

/**
 * This represents the contents of the main function in the low-level
 * program.  It calls the init function of the top-level stream,
 * performs some initial execution code (e.g. for the initailization
 * schedule), and should look something like this:
 *
 * HelloWorld6_data *test = malloc(sizeof(HelloWorld6_data));
 * test->c = create_context(test);
 * HelloWorld6_init(test, NULL);
 *
 * streamit_run(test->c);  <-- includes "initial execution code"
 */
public class LIRMainFunction extends LIRNode {

    /**
	 * 
	 */
	private static final long serialVersionUID = -6956339628837207230L;

	/**
     * The name of the type of the struct required by the toplevel
     * init function.  
     */
    private String typeName;
    
    /**
     * The toplevel init function.
     */
    private LIRFunctionPointer init;

    /**
     * A list of statements to run between top-level stream
     * initialization and stead-state running.  This list should
     * include the execution of the initialization schedule for the
     * stream.
     */
    private LinkedList<JStatement> initStatements;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.  Keeps a clone of <initStatements>.
     */
    public LIRMainFunction(String typeName,
                           LIRFunctionPointer init,
                           LinkedList<JStatement> initStatements) {
        // stream context is null since we're at the toplevel
        super(null);
        this.init = init;
        this.initStatements = (LinkedList<JStatement>)initStatements.clone();
        this.typeName = typeName;
    }

    @Override
	public void accept(SLIRVisitor v) {
        v.visitMainFunction(this, 
                            this.typeName, 
                            this.init,
                            this.initStatements);
    }
}
