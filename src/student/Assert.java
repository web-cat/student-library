/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2015 Virginia Tech
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

//-------------------------------------------------------------------------
/**
 *  This class is the student library equivalent of JUnit's Assert class.
 *  It provides customized assertion methods as a series of static methods.
 *  They can be imported statically into JUnit 4-style test cases by
 *  students.
 *
 *  @author  Stephen Edwards
 *  @author Last changed by $Author$
 *  @version $Revision$
 */
public class Assert
{
    //~ Fields ................................................................


    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * A private constructor ensures this library class cannot be
     * instantiated.
     */
    private Assert()
    {
        // intentionally blank
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Converts all line ending sequences in a given text string to
     * unix-style newlines (\n).  This is used in a number of assertions
     * to provide for platform-independent comparison of multi-line
     * strings.
     *
     * @param text The text to normalize.
     *
     * @return The value of text, with all line separator sequences replaced
     *         by "\n" (all line separators converted to unix-style
     *         line separators).
     */
    public static String normalizeLineEndings(String text)
    {
        String nl = java.security.AccessController
            .doPrivileged(new java.security.PrivilegedAction<String>()
            {
                public String run()
                {
                    return System.getProperty("line.separator");
                }
            });
        String result = text;

        // First, convert the platform's native newlines
        if (nl != null && !nl.equals("\n"))
        {
            result = result.replace(nl, "\n");
        }

        // Now, forcibly convert any Windows-style newlines, as well
        if (nl == null || !nl.equals("\r\n"))
        {
            result = result.replace("\r\n", "\n");
        }

        return result;
    }
}
