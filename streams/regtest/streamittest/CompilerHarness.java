/**
 * Provides Java interface to the main StreamIT compiler, allowing
 * for easy regression testing.
 * $Id: CompilerHarness.java,v 1.7 2002-11-05 22:54:45 aalamb Exp $
 **/
package streamittest;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.raw.*;

import java.io.*;

public class CompilerHarness extends Harness {
    // command lines    
    static final String GCC_COMMAND = "gcc";
    static final String JAVA_COMMAND = "java";

    // main compiler java class
    static final String JAVA_MAIN = "at.dms.kjc.Main";
    // java memory option
    static final String JAVA_OPTION_MEM = "-Xmx1700M";

    // location of streamit c library files
    static final String C_LIBRARY_PATH = "library/c/";
    static final String C_LIBRARY_FILES = C_LIBRARY_PATH + "stream*.c";
    
    /**
     * Run the streamit compiler with the options specified in the 
     * passed array. Returns true if compliation is successful
     * false otherwise.
     **/
    static boolean streamITCompile(String[] options,
				   String root,
				   String inFileName,
				   String outFileName) {

	// result of running the streamit compiler
	boolean compilerResult = false;

	// expand input streamit files
	String[] expandedFileNames = expandFileName(inFileName);

	// if no filenames returned, signal error via stderr and return false
	if (expandedFileNames.length < 1) {
	    ResultPrinter.printError(":filename " + inFileName +
				     " did not expand");
	    ResultPrinter.flushFileWriter();
	    return false;
	}
		
	
	// new array for options and for filename
	String[] cmdLineArgs = getJavaCommandArray(options, root, expandedFileNames);

	try {

	    // set up a java file i/o stream so we can save
	    // the output of the streamit compiler into a file (which we
	    // can then compile with gcc)
	    FileOutputStream fileOut = new FileOutputStream(outFileName);
	    
	    // execute natively
	    compilerResult = executeNative(cmdLineArgs, fileOut);

	    // close file descriptor
	    fileOut.close();
	    
	} catch (Exception e) {
	    ResultPrinter.printError("Caught exception compiling with streamit : " + e.getMessage());
	    e.printStackTrace();
	    return false;
	}

	return compilerResult;

    }

    /**
     * Run the gcc compiler (uniprocessor path)
     * to convert the source file to the exeFile.
     **/
    static boolean gccCompile(String sourceFileName,
			      String exeFileName) {
    
	// result of running gcc
	boolean gccResult = false;
	
	// try and compile the source file with gcc
	try {

	    gccResult = executeNative(getGccCommandArray(sourceFileName,
							 exeFileName));
	} catch (Exception e) {
	    ResultPrinter.printError("gcc execution caused exception (?): " + e);
	    e.printStackTrace();
	}

	return gccResult;

    }
    /**
     * run the make process to compile for raw.
     **/
    static boolean rawCompile(String rootPath,
			      String makefileName) {

	// result of running the raw compiler 
	boolean rawResult = false;
	
	// try and compile the source file with make
	try {
	    rawResult = executeNative(getRawCommandArray(rootPath,
							 makefileName));
	} catch (Exception e) {
	    ResultPrinter.printError("raw compliation caused exception (?): " + e);
	    e.printStackTrace();
	}


	return rawResult;
    }    

    /**
     * Get command line options for running the streamit compiler
     * with the specified options and the specified file names.
     * root path is needed to change dir so raw stuff ends up in the correct place
     **/
    public static String[] getJavaCommandArray(String[] options,
					       String root, 
					       String[] expandedFileNames) {
	// expand the filename that was passed in to multiple filenames
	// if that is necessary
	String[] cmdLineArgs = new String[3];

	cmdLineArgs[0] = "csh";
	cmdLineArgs[1] = "-c";
	cmdLineArgs[2] = ("cd " + root + ";" + // cd to the correct directory
			  JAVA_COMMAND + " " +
			  JAVA_OPTION_MEM + " " +
			  JAVA_MAIN + " " +
			  flattenCommandArray(options) +  // compiler options
			  flattenCommandArray(expandedFileNames));

	return cmdLineArgs;
    }
    

    /**
     * Set up an array of commands that will start up
     * gcc to compile inputFileName and write executable to exeFileName.
     **/
    public static String[] getGccCommandArray(String inputFileName,
					      String exeFileName) {

	String streamit_root = getStreamITRoot();
	// expand out the library files path
	String[] libFiles = expandFileName(streamit_root + C_LIBRARY_FILES);

	String[] opts = new String[(6 + // set up args
				    libFiles.length)];
	
	opts[0] = GCC_COMMAND;
	opts[1] = "-O2";
	opts[2] = "-lm";
	opts[3] = "-I" + streamit_root + C_LIBRARY_PATH;
	opts[4] = "-o" + exeFileName;
	opts[5] = inputFileName;
	
	// copy over the stream library files
	for (int i=0; i<libFiles.length; i++) {
	    opts[6+i] = libFiles[i];
	}

	return opts;
    }


    /**
     * Set up an array of commands to run the raw compiler makefile.
     **/
    public static String[] getRawCommandArray(String rootPath, String makefileName) {

	String opts[] = new String[(1 + // make
				    1 + // -C
				    1 + // dir
				    1 + // -f
				    1)]; // makefile
	opts[0] = "make";
	opts[1] = "-C";
	opts[2] = rootPath;
	opts[3] = "-f";
	opts[4] = makefileName;

	return opts;
    }
    

    /**
     * Print out the options in the passed array.
     **/
    public static void printOptions(String[] opts) {
	String s = "Compiling with options: ";
	for (int i=0; i<opts.length; i++) {
	    s += (opts[i] + " ");
	}
	ResultPrinter.printInfo(s);
    }

}
