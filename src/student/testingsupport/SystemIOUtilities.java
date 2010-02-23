package student.testingsupport;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Stack;

//-------------------------------------------------------------------------
/**
 *  A utility class that provides functions for replacing {@link System}
 *  I/O streams with helpful alternative implementations to make some
 *  testing jobs easier.  This class is really for use by infrastructure
 *  and support code, and students should never need to use it directly.
 *
 *  <p>Since this class provides only static methods, clients should not
 *  create an instance.  As a result, it provides no public constructors.</p>
 *
 *  @author  Stephen Edwards
 *  @author Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public class SystemIOUtilities
{
    //~ Instance/static variables .............................................

    private static Stack<PrintStream> originalOut = new Stack<PrintStream>();
    private static Stack<PrintStream> originalErr = new Stack<PrintStream>();
    private static InputStream originalIn;


    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Since this class provides only static methods, clients should not create
     * an instance.
     */
    private SystemIOUtilities()
    {
        // nothing to do
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Get a "wrapped" version of {@link System#out} that provides
     * history recording functions.
     * @return a version of System.out that provides history features
     */
    public static PrintStreamWithHistory out()
    {
        assertNotOnServer();
        PrintStreamWithHistory out = null;
        if (System.out instanceof PrintStreamWithHistory)
        {
            out = (PrintStreamWithHistory)System.out;
        }
        else
        {
            out = new PrintStreamWithHistory(System.out);
            originalOut.push(System.out);
            System.setOut(out);
        }
        return out;
    }


    // ----------------------------------------------------------
    /**
     * "Unwrap" {@link System#out} by removing any history recording
     * wrapper, and return it to its original state.
     */
    public static void restoreSystemOut()
    {
        assertNotOnServer();
        if (originalOut.size() > 0)
        {
            System.setOut(originalOut.pop());
        }
    }


    // ----------------------------------------------------------
    /**
     * Get a "wrapped" version of {@link System#err} that provides
     * history recording functions.
     * @return a version of System.err that provides history features
     */
    public static PrintStreamWithHistory err()
    {
        assertNotOnServer();
        PrintStreamWithHistory err = null;
        if (System.err instanceof PrintStreamWithHistory)
        {
            err = (PrintStreamWithHistory)System.err;
        }
        else
        {
            err = new PrintStreamWithHistory(System.err);
            originalErr.push(System.err);
            System.setErr(err);
        }
        return err;
    }


    // ----------------------------------------------------------
    /**
     * "Unwrap" {@link System#err} by removing any history recording
     * wrapper, and return it to its original state.
     */
    public static void restoreSystemErr()
    {
        assertNotOnServer();
        if (originalErr.size() > 0)
        {
            System.setErr(originalErr.pop());
        }
    }


    // ----------------------------------------------------------
    /**
     * Replace {@link System#in} with the contents of the given string.
     * @param contents The content to read from
     */
    public static void replaceSystemInContents(String contents)
    {
        assertNotOnServer();
        if (System.in instanceof MutableStringBufferInputStream)
        {
            ((MutableStringBufferInputStream)System.in)
                .resetContents(contents);
        }
        else
        {
            originalIn = System.in;
            MutableStringBufferInputStream newIn =
                new MutableStringBufferInputStream(contents);
            newIn.setName("System.in");
            System.setIn(newIn);
        }
    }


    // ----------------------------------------------------------
    /**
     * Restore {@link System#in} to its original value.
     */
    public static void restoreSystemIn()
    {
        assertNotOnServer();
        if (originalIn != null)
        {
            System.setIn(originalIn);
            originalIn = null;
        }
    }


    // ----------------------------------------------------------
    /**
     * Checks to see if the calling program is running under the Apache
     * Tomcat servlet container.
     * @return True if running as a servlet
     */
    public static boolean isOnServer()
    {
        boolean inServlet = false;
        try
        {
            if (SystemIOUtilities.class.getClassLoader()
                    .loadClass("student.web.internal.Interpreter") != null)
            {
                inServlet = true;
            }
        }
        catch (ClassNotFoundException e)
        {
            // If that class isn't around, then we're not running under
            // the ZK servlet engine, so assume tweaking System.in/out
            // is OK.
            inServlet = false;
        }
        return inServlet;
    }


    // ----------------------------------------------------------
    /**
     * Checks to see if the calling program is running under the Apache
     * Tomcat servlet container.  When running in such an environment,
     * some behaviors should be avoided.  For example, it is not appropriate
     * to modify globally shared resources like those in the class System.
     */
    public static void assertNotOnServer()
    {
        assert !isOnServer()
            : "This method cannot be executed while running on the server.";
    }
}
