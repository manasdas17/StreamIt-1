package at.dms.kjc;

import at.dms.util.*;
import at.dms.kjc.*;

import java.security.Permission;
import java.util.*;
import java.lang.reflect.*;
import java.io.IOException;

public class CloneGenerator {

    /** header for cloning methods in files */
    private static final String HEADER = "/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */";
    /** footer for cloning methods in files */
    private static final String FOOTER = "/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */";

    // disable access control when this class is running
    static {
	System.setSecurityManager(new SecurityManager() {
		public void checkPermission(Permission perm) {}
	    });
    }

    /**
     * Call as with argument "replace" to do replacement in actual
     * java code.  NOTE that this should be run from
     * $STREAMIT_HOME/compiler directory to do the right thing for
     * replacement.
     */
    public static void main(String[] args) {
	boolean replacing = (args.length>0 && args[0].equals("replace"));
	// go through all classes
	for (int i=0; i<classes.length; i++) {
	    // get cloning code
	    String code = generateCloneMethods(classes[i]);
	    // if replacing, then do replacement in java files;
	    // otherwise just print to screen
	    if (replacing) {
		doReplace(classes[i], HEADER + "\n\n" + code + "\n" + FOOTER);
	    } else {
		System.out.println("Methods for " + classes[i] + " -----------------------------------------");
		System.out.println(code);
	    }
	}
    }

    private static String generateCloneMethods(String className) {
	Class c = null;
	try {
	    c = Class.forName(className);
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	return generateClone(c) + "\n" + generateCloneInto(c);
    }

    /**
     * Generates a suitable deepClone() method for class <c>
     */
    private static String generateClone(Class c) {
	String className = c.getName();
	StringBuffer sb = new StringBuffer();
	sb.append("/** Returns a deep clone of this object. */\n");
	if (Modifier.isAbstract(c.getModifiers())) {
	    // if this is an abstract class, then deepClone method
	    // should be empty.  Don't make it abstract because we
	    // might not want all the children to have to implement if
	    // (if there are no references to them.)
	    sb.append("public Object deepClone() { at.dms.util.Utils.fail(\"Error in auto-generated cloning methods - deepClone was called on an abstract class.\"); return null; }\n");
	} else {
	    // otherwise, should define contents of class
	    sb.append("public Object deepClone() {\n");
	    sb.append("  " + className + " other = new " + className + "();\n");
	    sb.append("  at.dms.kjc.AutoCloner.register(this, other);\n");
	    sb.append("  deepCloneInto(other);\n");
	    sb.append("  return other;\n");
	    sb.append("}\n");
	}
	return sb.toString();
    }

    /**
     * Generates a suitable deepCloneInto(<c.getName> other) method
     * for class <className>, which copies all the fields over.
     */
    private static String generateCloneInto(Class c) {
	StringBuffer sb = new StringBuffer();
	// ignore interfaces
	if (c.isInterface()) { 
	    sb.append("INTERFACE - ABORTING\n");
	    return sb.toString();
	}
	sb.append("/** Clones all fields of this into <other> */\n");
	sb.append("protected void deepCloneInto(" + c.getName() + " other) {\n");
	// if there's a superclass, then call deepClone on super.
	if (c.getSuperclass()!=null && !c.getSuperclass().getName().equals("java.lang.Object")) {
	    if (!inTargetClasses(c.getSuperclass().getName())) {
		System.err.println("WARNING:  Generating call to undefined method " + c.getSuperclass().getName() + ".deepCloneInto as super of " + c.getName() +
				   "\n  This is because " + c.getSuperclass().getName() + " is not in target classes.");
	    }
	    sb.append("  super.deepCloneInto(other);\n");
	}
	// copy all fields over, calling clone on them only if they are DeepCloneable
	Field[] field = c.getDeclaredFields();
	for (int i=0; i<field.length; i++) {
	    // get the value for the field
	    field[i].setAccessible(true);
	    // get the name, type of field
	    String name = field[i].getName();
	    Class type = field[i].getType();
	    if (Modifier.isStatic(field[i].getModifiers())) {
		// do nothing for static fields
	    } else if (type.isPrimitive()) {
		// for primitives, copy them straight over
		sb.append("  other." + name + " = this." + name + ";\n");
	    } else if (name.equals("serializationHandle")) {
		// for now, ignore serialization handles.  To be
		// changed once we get complete new cloning framework.
		sb.append("  other." + name + " = this." + name + ";\n");
	    } else {
		// otherwise call toplevel cloning method
		sb.append("  other." + name + " = (" + type.getName() + ")at.dms.kjc.AutoCloner.cloneToplevel(this." + name + ");\n");
	    }
	}
	sb.append("}\n");
	return sb.toString();
    }

    /**
     * Replaces cloning methods (if they exit) in given class name.
     * Otherwise, adds them to the class.
     */
    private static void doReplace(String className, String newCode) {
	System.err.print("Working on " + className + ".");
	// get name of class
	String filename = "./" + className.replace('.', '/') + ".java";
	// load it in as a string
	StringBuffer contents = null;
	try {
	    contents = Utils.readFile(filename);
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	// first look for header, footer
	String contentsStr = contents.toString();
	int start = contentsStr.indexOf(HEADER);
	int end = contentsStr.indexOf(FOOTER);
	// build up the newContents below
	String newContents = null;
	if (start!=-1 && end!=-1) {
	    System.err.print("\tReplacing old code...");
	    // make sure we only have one of them
	    int nextHeader = contentsStr.indexOf(HEADER, start+1);
	    int nextFooter = contentsStr.indexOf(FOOTER, end+1);
	    Utils.assert(nextHeader==-1 && nextFooter==-1, "Found multiple headers/footers per file -- can't deal with this.");
	    // replace old cloner with new
	    String oldCode = contentsStr.substring(start, end+FOOTER.length());
	    newContents = Utils.replaceAll(contentsStr, oldCode, newCode);
	} else {
	    System.err.print("\tAdding new code...");
	    // otherwise, we need to add the code fresh.  Do this at
	    // the end of the class -- find end of class by counting {
	    // and }
	    int braces = 0;
	    int pos = -1;
	    boolean started = false;
	    do {
		pos++;
		if (contentsStr.charAt(pos)=='{') {
		    started = true;
		    braces++;
		}
		if (contentsStr.charAt(pos)=='}') {
		    braces--;
		}
	    } while (!(started && braces==0));
	    // at this point, charAt(pos) points to the last closing
	    // brace of the first class defined in the file.  Insert
	    // the new code at this position.  Also surround with
	    // newlines since we'd like some space on the first
	    // addition.
	    newContents = contents.insert(pos, "\n" + newCode + "\n").toString();
	}
	// write new contents to disk
	try {
	    Utils.writeFile(filename, newContents);
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	System.err.println(" done.");
    }

    /**
     * For testing.
     */
    private static final String[] classes1 = {
	"at.dms.kjc.JLocalVariable"
    };

    /**
     * Whether or not <className> is a class we're generating cloning code for.
     */
    public static boolean inTargetClasses(String className) {
	for (int i=0; i<classes.length; i++) {
	    if (classes[i].equals(className)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Names of classes to insert cloning stuff on.
     */
    private static final String[] classes = {
	//"at.dms.kjc.AttributeVisitor", -- interface
	//"at.dms.kjc.BytecodeOptimizer", -- shouldn't have references to this
	"at.dms.kjc.CArrayType",
	"at.dms.kjc.CBadClass",
	"at.dms.kjc.CBinaryClass",
	"at.dms.kjc.CBinaryField",
	"at.dms.kjc.CBinaryMethod",
	"at.dms.kjc.CBitType",
	"at.dms.kjc.CBlockContext",
	//"at.dms.kjc.CBlockError",  -- ignore the error classes
	"at.dms.kjc.CBodyContext",
	"at.dms.kjc.CBooleanType",
	"at.dms.kjc.CByteType",
	"at.dms.kjc.CCatchContext",
	"at.dms.kjc.CCharType",
	"at.dms.kjc.CClass",
	"at.dms.kjc.CClassContext",
	"at.dms.kjc.CClassNameType",
	"at.dms.kjc.CClassType",
	"at.dms.kjc.CCompilationUnit",
	"at.dms.kjc.CCompilationUnitContext",
	"at.dms.kjc.CConstructorContext",
	"at.dms.kjc.CContext",
	"at.dms.kjc.CDoubleType",
	"at.dms.kjc.CExpressionContext",
	//"at.dms.kjc.CExpressionError", -- ignore the error classes
	"at.dms.kjc.CField",
	"at.dms.kjc.CFloatType",
	"at.dms.kjc.CInitializerContext",
	"at.dms.kjc.CIntType",
	"at.dms.kjc.CInterfaceContext",
	"at.dms.kjc.CLabeledContext",
	//"at.dms.kjc.CLineError", -- ignore the error classes
	"at.dms.kjc.CLongType",
	"at.dms.kjc.CLoopContext",
	"at.dms.kjc.CMember",
	"at.dms.kjc.CMethod",
	"at.dms.kjc.CMethodContext",
	//"at.dms.kjc.CMethodNotFoundError", -- ignore the error classes
	"at.dms.kjc.CModifier",
	"at.dms.kjc.CNullType",
	"at.dms.kjc.CNumericType",
	"at.dms.kjc.CParseClassContext",
	"at.dms.kjc.CParseCompilationUnitContext",
	"at.dms.kjc.CShortType",
	"at.dms.kjc.CSimpleBodyContext",
	"at.dms.kjc.CSourceClass",
	"at.dms.kjc.CSourceField",
	"at.dms.kjc.CSourceMethod",
	"at.dms.kjc.CStdType",
	"at.dms.kjc.CSwitchBodyContext",
	"at.dms.kjc.CSwitchGroupContext",
	"at.dms.kjc.CThrowableInfo",
	"at.dms.kjc.CTopLevel",
	"at.dms.kjc.CTryContext",
	"at.dms.kjc.CTryFinallyContext",
	"at.dms.kjc.CType",
	"at.dms.kjc.CVariableInfo",
	"at.dms.kjc.CVoidType",
	"at.dms.kjc.CodeLabel",
	"at.dms.kjc.CodeSequence",
	//"at.dms.kjc.Constants", -- interface
	"at.dms.kjc.DefaultFilter",
	//"at.dms.kjc.EmptyAttributeVisitor", -- shouldn't be fields of this type
	//"at.dms.kjc.Finalizable", -- interface
	"at.dms.kjc.JAddExpression",
	"at.dms.kjc.JArrayAccessExpression",
	"at.dms.kjc.JArrayInitializer",
	"at.dms.kjc.JArrayLengthExpression",
	"at.dms.kjc.JAssignmentExpression",
	"at.dms.kjc.JBinaryArithmeticExpression",
	"at.dms.kjc.JBinaryExpression",
	"at.dms.kjc.JBitwiseComplementExpression",
	"at.dms.kjc.JBitwiseExpression",
	"at.dms.kjc.JBlock",
	"at.dms.kjc.JBooleanLiteral",
	"at.dms.kjc.JBreakStatement",
	"at.dms.kjc.JByteLiteral",
	"at.dms.kjc.JCastExpression",
	"at.dms.kjc.JCatchClause",
	"at.dms.kjc.JCharLiteral",
	"at.dms.kjc.JCheckedExpression",
	"at.dms.kjc.JClassBlock",
	"at.dms.kjc.JClassDeclaration",
	"at.dms.kjc.JClassExpression",
	"at.dms.kjc.JClassFieldDeclarator",
	"at.dms.kjc.JClassImport",
	"at.dms.kjc.JCompilationUnit",
	"at.dms.kjc.JCompoundAssignmentExpression",
	"at.dms.kjc.JCompoundStatement",
	"at.dms.kjc.JConditionalAndExpression",
	"at.dms.kjc.JConditionalExpression",
	"at.dms.kjc.JConditionalOrExpression",
	"at.dms.kjc.JConstructorBlock",
	"at.dms.kjc.JConstructorCall",
	"at.dms.kjc.JConstructorDeclaration",
	"at.dms.kjc.JContinueStatement",
	"at.dms.kjc.JDivideExpression",
	"at.dms.kjc.JDoStatement",
	"at.dms.kjc.JDoubleLiteral",
	"at.dms.kjc.JEmptyStatement",
	"at.dms.kjc.JEqualityExpression",
	"at.dms.kjc.JExpression",
	"at.dms.kjc.JExpressionListStatement",
	"at.dms.kjc.JExpressionStatement",
	"at.dms.kjc.JFieldAccessExpression",
	"at.dms.kjc.JFieldDeclaration",
	"at.dms.kjc.JFloatLiteral",
	"at.dms.kjc.JForStatement",
	"at.dms.kjc.JFormalParameter",
	"at.dms.kjc.JGeneratedLocalVariable",
	"at.dms.kjc.JIfStatement",
	"at.dms.kjc.JInitializerDeclaration",
	"at.dms.kjc.JInstanceofExpression",
	"at.dms.kjc.JIntLiteral",
	"at.dms.kjc.JInterfaceDeclaration",
	"at.dms.kjc.JLabeledStatement",
	"at.dms.kjc.JLiteral",
	"at.dms.kjc.JLocalVariable",
	"at.dms.kjc.JLocalVariableExpression",
	"at.dms.kjc.JLogicalComplementExpression",
	"at.dms.kjc.JLongLiteral",
	"at.dms.kjc.JLoopStatement",
	"at.dms.kjc.JMemberDeclaration",
	"at.dms.kjc.JMethodCallExpression",
	"at.dms.kjc.JMethodDeclaration",
	"at.dms.kjc.JMinusExpression",
	"at.dms.kjc.JModuloExpression",
	"at.dms.kjc.JMultExpression",
	"at.dms.kjc.JNameExpression",
	"at.dms.kjc.JNewArrayExpression",
	"at.dms.kjc.JNullLiteral",
	"at.dms.kjc.JOuterLocalVariableExpression",
	"at.dms.kjc.JPackageImport",
	"at.dms.kjc.JPackageName",
	"at.dms.kjc.JParenthesedExpression",
	"at.dms.kjc.JPhylum",
	"at.dms.kjc.JPostfixExpression",
	"at.dms.kjc.JPrefixExpression",
	"at.dms.kjc.JQualifiedAnonymousCreation",
	"at.dms.kjc.JQualifiedInstanceCreation",
	"at.dms.kjc.JRelationalExpression",
	"at.dms.kjc.JReturnStatement",
	"at.dms.kjc.JShiftExpression",
	"at.dms.kjc.JShortLiteral",
	"at.dms.kjc.JStatement",
	"at.dms.kjc.JStringLiteral",
	"at.dms.kjc.JSuperExpression",
	"at.dms.kjc.JSwitchGroup",
	"at.dms.kjc.JSwitchLabel",
	"at.dms.kjc.JSwitchStatement",
	"at.dms.kjc.JSynchronizedStatement",
	"at.dms.kjc.JThisExpression",
	"at.dms.kjc.JThrowStatement",
	"at.dms.kjc.JTryCatchStatement",
	"at.dms.kjc.JTryFinallyStatement",
	"at.dms.kjc.JTypeDeclaration",
	"at.dms.kjc.JTypeDeclarationStatement",
	"at.dms.kjc.JTypeNameExpression",
	"at.dms.kjc.JUnaryExpression",
	"at.dms.kjc.JUnaryMinusExpression",
	"at.dms.kjc.JUnaryPlusExpression",
	"at.dms.kjc.JUnaryPromote",
	"at.dms.kjc.JUnqualifiedAnonymousCreation",
	"at.dms.kjc.JUnqualifiedInstanceCreation",
	"at.dms.kjc.JVariableDeclarationStatement",
	"at.dms.kjc.JVariableDefinition",
	"at.dms.kjc.JWhileStatement",
	//"at.dms.kjc.KjcEmptyVisitor", -- shouldn't have references to this
	//"at.dms.kjc.KjcMessages", -- interface
	"at.dms.kjc.KjcPrettyPrinter",
	//"at.dms.kjc.KjcVisitor", -- interface
	//"at.dms.kjc.Kopi2SIR", -- shouldn't have references to this
	//"at.dms.kjc.Main", -- shouldn't have references to this
	"at.dms.kjc.MethodSignatureParser",
	//"at.dms.kjc.ObjectDeepCloner", -- shouldn't have references to this
	//"at.dms.kjc.ReplacingVisitor", -- shouldn't have references to this
	//"at.dms.kjc.SLIRAttributeVisitor", -- interface
	//"at.dms.kjc.SLIREmptyAttributeVisitor", -- shouldn't have refs
	//"at.dms.kjc.SLIREmptyVisitor",  -- shouldn't have refs
	//"at.dms.kjc.SLIRReplacingVisitor",  -- shouldn't have refs
	//"at.dms.kjc.SLIRVisitor", -- interface
	//"at.dms.kjc.StreaMITMain", -- shouldn't have references to this
	//"at.dms.kjc.StreamItDot", -- shouldn't have references to this
	//"at.dms.kjc.TestK2S", -- shouldn't have references to this
	// "at.dms.kjc.KjcOptions", -- shouldn't have references to this
	//"at.dms.kjc.KjcTokenTypes",
	//"at.dms.kjc.KjcScanner", -- shouldn't have references to this
	//"at.dms.kjc.KjcParser", -- don't want to descend into antlr
	"at.dms.util.Utils",
	//"at.dms.compiler.WarningFilter", -- interface
	"at.dms.compiler.Phylum",
	"at.dms.compiler.TokenReference",
	"at.dms.compiler.Compiler",
	"at.dms.compiler.TabbedPrintWriter",
	//"at.dms.compiler.PositionedError", -- ignore the error classes
	"at.dms.compiler.JavadocComment",
	"at.dms.compiler.JavaStyleComment",
	"at.dms.classfile.AbstractInstructionAccessor",
	//"at.dms.util.FormattedException",
	"at.dms.util.Message",
	"at.dms.util.MessageDescription"
	//"at.dms.util.Options" -- shouldn't have references to this
	//"at.dms.kjc.SimpleDot",  -- don't do this because it has lots of open, close braces
	//"at.dms.kjc.CloneGenerator", -- don't do this because it has lots of open, close braces
    };
}
