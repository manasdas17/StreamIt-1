package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.compiler.*;
import java.util.*;

/**
 * Provides a means for estimating the amount of work in a stream graph.
 */
class WorkEstimate {
    
    /**
     * Maps stream constructs to a WorkInfo node.
     */
    private HashMap workMap;

    /**
     * The total amount of work contained in the stream.
     */
    private int toplevelWork;

    private WorkEstimate() {
	this.workMap = new HashMap();
	this.toplevelWork = 0;
    }

    /**
     * Returns a work estimate of <str>
     */
    public static WorkEstimate getWorkEstimate(SIRStream str) {
	WorkEstimate result = new WorkEstimate();
	result.doit(str);
	return result;
    }

    /**
     * Returns a sorted worklist corresponding to a given work
     * estimate.  The entries will be sorted by increasing values of
     * work.  These entries will map FILTERS to work estimate, for all
     * filters in the graph.
     */
    public WorkList getSortedFilterWork() {
	return buildWorkList(workMap);
    }

    /**
     * Builds a worklist out of <map>
     */
    private WorkList buildWorkList(final HashMap map) {
	// first make a treemap to do the sorting for us
	TreeMap treeMap = new TreeMap(new Comparator() {
		public int compare(Object o1, Object o2) {
		    // assume these are map.entry's
		    int work1 = ((WorkInfo)map.get(o1)).totalWork();
		    int work2 = ((WorkInfo)map.get(o2)).totalWork();
		    if (o1==o2) {
			return 0;
		    } else if (work1 < work2) {
			return -1;
		    } else {
			return 1;
		    }
		}
	    });
	treeMap.putAll(map);
	// result is a work list based on the treemap
	WorkList result = new WorkList(treeMap.entrySet());
	// make sure our sizes our right - was a problem at one point
	// due to improper comparator function (should preserve .equals)
	Utils.assert(map.size()==treeMap.size(),
		     "map.size=" + map.size() + "; treemap.size=" + treeMap.size());
	Utils.assert(map.size()==result.size(),
		     "map.size=" + map.size() + "; result.size=" + result.size());
	return result;
    }

    /**
     * Returns a sorted worklist in increasing order of work for all
     * containers in this graph that contain at least a single filter.
     * The caller might then try fusing the first elements of this
     * list.
     */
    public WorkList getSortedContainerWork() {
	// make a hashmap mapping containers to work they contain
	HashMap containerMap = new HashMap();
	// fill up the map with the filter's work
	for (Iterator it = workMap.entrySet().iterator(); it.hasNext(); ) {
	    Map.Entry entry = (Map.Entry)it.next();
	    // get filter
	    SIRFilter filter = (SIRFilter)entry.getKey();
	    // get its parent
	    SIRContainer parent = filter.getParent();
	    // get work
	    int work = ((WorkInfo)entry.getValue()).totalWork();
	    // if it doesn't contain parent, add parent
	    if (!containerMap.containsKey(parent)) {
		containerMap.put(parent, new WorkInfo(work));
	    } else {
		// otherwise, just increment the work
		((WorkInfo)containerMap.get(parent)).incrementWork(work);
	    }
	}
	// return sorted list
	return buildWorkList(containerMap);
    }

    /**
     * Returns the work estimate for filter <obj>.  Requires that
     * <obj> was present in the original graph used to construct this.
     */
    public int getWork(SIRFilter obj) {
	return ((WorkInfo)workMap.get(obj)).totalWork();
    }

    /**
     * Returns the percent of the total stream's work that is consumed
     * by filter <obj>.  Returns this in the range of 0-100.
     */
    public float getPercentageWork(SIRFilter obj) {
	return 100 * 
	    ((float)((WorkInfo)workMap.get(obj)).totalWork()) / toplevelWork;
    }

    /**
     * prints work of all functions to system.err.
     */
    public void printWork() {
	System.err.println("\nWORK ESTIMATES:");
	for (Iterator it = workMap.keySet().iterator(); it.hasNext(); ) {
	    SIRFilter obj = (SIRFilter)it.next();
	    String objName = obj.toString();
	    System.err.print(objName);
	    for (int i=objName.length();  i<70; i++) {
		System.err.print(" ");
	    }
	    System.err.println("\t" + getWork(obj) + "\t" + "(" +
			       ((int)getPercentageWork(obj)) + "%)");
	}
	
    }

    /**
     * Estimates the work in <str>
     */
    private void doit(SIRStream str) {
	// get execution counts for filters in <str>
	HashMap executionCounts = SIRScheduler.getExecutionCounts(str)[1];

	// for each filter, build a work count
	for (Iterator it = executionCounts.keySet().iterator();
	     it.hasNext(); ){
	    SIROperator obj = (SIROperator)it.next();
	    if (obj instanceof SIRFilter) {
		int count = ((int[])executionCounts.get(obj))[0];
		int work = WorkVisitor.getWork((SIRFilter)obj);
		//System.out.println("Work:"+obj+" "+count+" "+work);
		workMap.put(obj, new WorkInfo(count*work));
		toplevelWork += count*work;
	    }
	}
    }
}

class WorkVisitor extends SLIREmptyVisitor implements WorkConstants {
    /**
     * An estimate of the amount of work found by this filter.
     */
    private int work;

    /**
     * The filter we're looking at.
     */
    private SIRFilter theFilter;

    private WorkVisitor() {
	this.work = 0;
    }

    /**
     * Returns estimate of work in <filter>
     */
    public static int getWork(SIRFilter filter) {
        if (!filter.needsWork ())
		return 0;
	WorkVisitor visitor = new WorkVisitor();
	if (filter.getWork()==null) {
	    System.err.println("this filter has null work function: " + filter);
	    return 0;
	}
        visitor.theFilter = filter;
	filter.getWork().accept(visitor);
	return visitor.work;
    }

    private JMethodDeclaration findMethod(String name) 
    {
        JMethodDeclaration[] methods = theFilter.getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            JMethodDeclaration method = methods[i];
            if (method.getName().equals(name))
                return method;
        }
        return null;
    }

    /* still need to handle:
    public void visitCastExpression(JCastExpression self,
				    JExpression expr,
				    CType type) {}

    public void visitUnaryPromoteExpression(JUnaryPromote self,
					    JExpression expr,
					    CType type) {}
    */

    /**
     * SIR NODES.
     */

    /**
     * Visits a peek expression.
     */
    public void visitPeekExpression(SIRPeekExpression self,
				    CType tapeType,
				    JExpression arg) {
	super.visitPeekExpression(self, tapeType, arg);
	work += PEEK;
    }

    /**
     * Visits a pop expression.
     */
    public void visitPopExpression(SIRPopExpression self,
				   CType tapeType) {
	super.visitPopExpression(self, tapeType);
	work += POP;
    }

    /**
     * Visits a print statement.
     */
    public void visitPrintStatement(SIRPrintStatement self,
				    JExpression arg) {
	super.visitPrintStatement(self, arg);
	work += PRINT;
    }

    /**
     * Visits a push expression.
     */
    public void visitPushExpression(SIRPushExpression self,
				    CType tapeType,
				    JExpression arg) {
	super.visitPushExpression(self, tapeType, arg);
	work += PUSH;
    }

    /*

    /**
     * KJC NODES.
     */

    /**
     * prints a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
				    JExpression cond,
				    JStatement body) {
	//System.err.println("WARNING:  Estimating work in loop, assume N=" +
	//LOOP_COUNT);
	int oldWork = work;
	super.visitWhileStatement(self, cond, body);
	int newWork = work;
	work = oldWork + LOOP_COUNT * (newWork - oldWork);
    }

    /**
     * prints a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
				     JExpression expr,
				     JSwitchGroup[] body) {
	super.visitSwitchStatement(self, expr, body);
	work += SWITCH;
    }

    /**
     * prints a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
				     JExpression expr) {
	super.visitReturnStatement(self, expr);
	work += RETURN;
    }

    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
				 JExpression cond,
				 JStatement thenClause,
				 JStatement elseClause) {
	super.visitIfStatement(self, cond, thenClause, elseClause);
	work += IF;
    }

    /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
				  JStatement init,
				  JExpression cond,
				  JStatement incr,
				  JStatement body) {
	if (init != null) {
	    init.accept(this);
	}
	// try to determine how many times the loop executes
	int loopCount = Unroller.getNumExecutions(init, cond, incr, body);
	if (loopCount==-1) {
	    //System.err.println("WARNING:  Estimating work in loop, assume N=" +
	    //LOOP_COUNT);
	    loopCount = LOOP_COUNT;
	}
	int oldWork = work;
	if (cond != null) {
	    cond.accept(this);
	}
	if (incr != null) {
	    incr.accept(this);
	}
	body.accept(this);
	int newWork = work;
	work = oldWork + loopCount * (newWork - oldWork);
    }

    /**
     * prints a do statement
     */
    public void visitDoStatement(JDoStatement self,
				 JExpression cond,
				 JStatement body) {
	//System.err.println("WARNING:  Estimating work in loop, assume N=" +
	//LOOP_COUNT);
	int oldWork = work;
	super.visitDoStatement(self, cond, body);
	int newWork = work;
	work = oldWork + LOOP_COUNT * (newWork - oldWork);
    }

    /**
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
				       String label) {
	super.visitContinueStatement(self, label);
	work += CONTINUE;
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
				    String label) {
	super.visitBreakStatement(self, label);
	work += BREAK;
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * Adds to work estimate an amount for an arithmetic op of type
     * expr.  Assumes <expr> is integral unless the type is explicitly
     * float or double.
     */
    private void countArithOp(JExpression expr) {
	if (expr.getType()==CStdType.Float ||
	    expr.getType()==CStdType.Double) {
	    int add=FLOAT_ARITH_OP;
	    if(expr instanceof JDivideExpression)
		add*=16;
	    work += add;
	} else {
	    work += INT_ARITH_OP;
	}
    }

    /**
     * prints an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
					 JExpression expr) {
	super.visitUnaryPlusExpression(self, expr);
	countArithOp(self);
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
					  JExpression expr) {
	super.visitUnaryMinusExpression(self, expr);
	countArithOp(self);
    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	super.visitBitwiseComplementExpression(self, expr);
	countArithOp(self);
    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	super.visitLogicalComplementExpression(self, expr);
	countArithOp(self);
    }

    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
				     int oper,
				     JExpression left,
				     JExpression right) {
	super.visitShiftExpression(self, oper, left, right);
	countArithOp(self);
    }

    /**
     * prints a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
					  int oper,
					  JExpression left,
					  JExpression right) {
	super.visitRelationalExpression(self, oper, left, right);
	countArithOp(self);
    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
				      int oper,
				      JExpression expr) {
	super.visitPrefixExpression(self, oper, expr);
	countArithOp(self);
    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
				       int oper,
				       JExpression expr) {
	super.visitPostfixExpression(self, oper, expr);
	countArithOp(self);
    }

    /**
     * prints a name expression
     */
    public void visitNameExpression(JNameExpression self,
				    JExpression prefix,
				    String ident) {
	super.visitNameExpression(self, prefix, ident);
	work += MEMORY_OP;
    }

    /**
     * prints an array allocator expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
				      String oper,
				      JExpression left,
				      JExpression right) {
	super.visitBinaryExpression(self, oper, left, right);
	countArithOp(self);
    }

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
					  JExpression prefix,
					  String ident,
					  JExpression[] args) {
	super.visitMethodCallExpression(self, prefix, ident, args);
        // Known values from profiling RAW code:
        if (ident.equals("acos")) work += 515;
        else if (ident.equals("acosh")) work += 665;
        else if (ident.equals("acosh")) work += 665;
        else if (ident.equals("asin")) work += 536;
        else if (ident.equals("asinh")) work += 578;
        else if (ident.equals("atan")) work += 195;
        else if (ident.equals("atan2")) work += 272;
        else if (ident.equals("atanh")) work += 304;
        else if (ident.equals("ceil")) work += 47;
        else if (ident.equals("cos")) work += 120;
        else if (ident.equals("cosh")) work += 368;
        else if (ident.equals("exp")) work += 162;
        else if (ident.equals("expm1")) work += 220;
        else if (ident.equals("floor")) work += 58;
        else if (ident.equals("fmod")) work += 147;
        else if (ident.equals("frexp")) work += 60;
        else if (ident.equals("log")) work += 146;
        else if (ident.equals("log10")) work += 212;
        else if (ident.equals("log1p")) work += 233;
        else if (ident.equals("modf")) work += 41;
        else if (ident.equals("pow")) work += 554;
        else if (ident.equals("sin")) work += 97;
        else if (ident.equals("sinh")) work += 303;
        else if (ident.equals("sqrt")) work += 297;
        else if (ident.equals("tan")) work += 224;
        else if (ident.equals("tanh")) work += 288;
        else
        {
            JMethodDeclaration target = findMethod(ident);
            if (target != null)
                target.accept(this);
            else
                work += METHOD_CALL;
        }
    }

    /**
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
					boolean equal,
					JExpression left,
					JExpression right) {
	super.visitEqualityExpression(self, equal, left, right);
	countArithOp(self);
    }

    /**
     * prints a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
					   JExpression cond,
					   JExpression left,
					   JExpression right) {
	super.visitConditionalExpression(self, cond, left, right);
	work += IF;
    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
						  int oper,
						  JExpression left,
						  JExpression right) {
	super.visitCompoundAssignmentExpression(self, oper, left, right);
	work += ASSIGN;
    }

    /**
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
				     JExpression left,
				     String ident) {
	super.visitFieldExpression(self, left, ident);
	work += MEMORY_OP;
    }

    /**
     * prints a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
				       int oper,
				       JExpression left,
				       JExpression right) {
	super.visitBitwiseExpression(self, oper, left, right);
	countArithOp(self);
    }

    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
					  JExpression left,
					  JExpression right) {
	super.visitAssignmentExpression(self, left, right);
	work += ASSIGN;
    }
}
