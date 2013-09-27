/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2007-2010 Virginia Tech
 |
 |  This file is part of the Student-Library.
 |
 |  The Student-Library is free software; you can redistribute it and/or
 |  modify it under the terms of the GNU Lesser General Public License as
 |  published by the Free Software Foundation; either version 3 of the
 |  License, or (at your option) any later version.
 |
 |  The Student-Library is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU Lesser General Public License for more details.
 |
 |  You should have received a copy of the GNU Lesser General Public License
 |  along with the Student-Library; if not, see <http://www.gnu.org/licenses/>.
\*==========================================================================*/

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
 *  student.Version: package student, v4.13 $Date$
 *  </pre>
 *
 *  @author Stephen Edwards
 *  @version 4.10
 *  @author Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public class Version
{
    //~ Instance/static variables .............................................

    // These fields are used for overridable world startup
    private static final String version = "v4.13 $Date$";


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
