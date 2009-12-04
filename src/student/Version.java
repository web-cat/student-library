package student;

// -------------------------------------------------------------------------
/**
 *  This class allows access to the package version information.
 *  The version identification can be retrieved using the {@link #version()}
 *  method.  From within BlueJ, you can type the following line into the
 *  code pad to get the version information:
 *  <pre>
 *  student.Version.printVersion();
 *  </pre>
 *  <p>Alternatively, you can run this class as a main program
 *  to print out the version information:</p>
 *  <pre>
 *  C:\&gt; java -jar C:/BlueJ/lib/userlib/student.jar
 *  </pre>
 *  <p>Either approach will generate this version information:</p>
 *  <pre>
 *  student.Version: package student, v3.07 2009-12-03
 *  </pre>
 *
 *  @version 2009.09.15
 *  @author Stephen Edwards
 */
public class Version
{
    //~ Instance/static variables .............................................

    // These fields are used for overridable world startup
    private static final String version = "v3.07 2009-12-03";


    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Create a new Version object.
     */
    public Version()
    {
        // nothing to do
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Retrieve the current version information for this class (and
     * this class jar/library) as a string.
     *
     * @return the version information as a string
     */
    public static String version()
    {
        return version;
    }


    // ----------------------------------------------------------
    /**
     * Prints out the version information on {@link System#out}.
     */
    public static void printVersion()
    {
        String className = Version.class.getName();
        int    lastDot   = className.lastIndexOf( '.' );
        System.out.print(  className );
        System.out.print( ": " );
        if ( lastDot >= 0 )
        {
            String pkgName = className.substring( 0, lastDot );
            System.out.print( "package " + pkgName + ", " );
        }
        System.out.println( version() );
    }


    // ----------------------------------------------------------
    /**
     * This main method simply prints out the version information on
     * {@link System#out}.
     * @param args The command line arguments, if any, are completely ignored
     */
    public static void main( String[] args )
    {
        printVersion();
    }

}
