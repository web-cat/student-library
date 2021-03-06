/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2007-2012 Virginia Tech
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.AssertionFailedError;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import student.testingsupport.junit4.JUnit4TesterRunner;
import student.testingsupport.MutableStringBufferInputStream;
import student.testingsupport.PrintStreamWithHistory;
import student.testingsupport.PrintWriterWithHistory;
import student.testingsupport.StringNormalizer;
import student.testingsupport.SystemIOUtilities;

//-------------------------------------------------------------------------
/**
 *  This class provides some customized behavior beyond the basics of
 *  {@link junit.framework.TestCase} to support testing of I/O driven
 *  programs and flexible/fuzzy comparison of strings.  In most cases, it
 *  can be used as a completely transparent drop-in replacement for its
 *  parent class.
 *  <p>
 *  Fuzzy string comparisons in this class default to standard rules in
 *  {@link StringNormalizer} (excluding the OPT_* rules).  You can use
 *  the {@link #stringNormalizer()} method to access the normalizer and
 *  set your own comparison options, however.
 *  </p>
 *
 *  @author  Stephen Edwards
 *  @author Last changed by $Author$
 *  @version $Revision$, $Date$
 */
@org.junit.runner.RunWith(JUnit4TesterRunner.class)
public class TestCase
    extends junit.framework.TestCase
{
    //~ Instance/static variables .............................................

    // These don't use the names "in" or "out" to provide better error
    // messages if students type those method names and accidentally leave
    // off the parentheses.
    private PrintWriterWithHistory tcOut = null;
    private Scanner                tcIn  = null;
    private MutableStringBufferInputStream tcInBuf = null;
    private static StringNormalizer       sn = new StringNormalizer(true);

    // Used for communicating with assertTrue() and assertFalse().  Ideally,
    // they should be instance vars, but assertTrue() and assertFalse()
    // have to be static so these messages must be too.
    private static String predicateReturnsTrueReason;
    private static String predicateReturnsFalseReason;

    private static Boolean trimStackTraces;
    private static boolean normalizeLineEndings = true;
    private static boolean useFuzzyStringComparisons = false;


    //~ Public classes used inside this class .................................

    // ----------------------------------------------------------
    /**
     * A custom error class that represents a mistake in calling the wrong
     * assert method for a given situation.
     */
    public static class AssertMethodCalledIncorrectly
        extends AssertionError
    {
        private static final long serialVersionUID = 6737326499160903149L;

        AssertMethodCalledIncorrectly(String message)
        {
            super(message);
        }
    }


    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new TestCase object.
     */
    public TestCase()
    {
        super();
        // Re-initialize this, since it was made static--we don't want
        // state from earlier test cases messing things up here.
        sn = new StringNormalizer(true);
        normalizeLineEndings = true;
        useFuzzyStringComparisons = false;
    }


    // ----------------------------------------------------------
    /**
     * Creates a new TestCase object.
     * @param name The name of this test case
     */
    public TestCase(String name)
    {
        super(name);
        // Re-initialize this, since it was made static--we don't want
        // state from earlier test cases messing things up here.
        sn = new StringNormalizer(true);
        normalizeLineEndings = true;
        useFuzzyStringComparisons = false;
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Sets up the fixture, for example, open a network connection. This
     * method is called before each test in this class is executed.
     */
    @Override
    protected void setUp()
        throws Exception
    {
        // Included only for Javadoc purposes--implementation adds nothing.
        super.setUp();
    }


    // ----------------------------------------------------------
    /**
     * Tears down the fixture, for example, close a network connection.
     * This method is called after each test in this class is executed.
     */
    @Override
    protected void tearDown()
        throws Exception
    {
        // Included only for Javadoc purposes--implementation adds nothing.
        super.tearDown();
    }


    // ----------------------------------------------------------
    /**
     * An internal helper that ensures input/output buffering is in place
     * before each test case.
     */
    protected void instrumentIO()
    {
        // Clear out all the stream history stuff
        tcIn = null;
        tcOut = null;
        tcInBuf = null;

        // Make sure these are history-wrapped
        SystemIOUtilities.out();
        SystemIOUtilities.err();

        // First, make sure the original System.in gets captured, so it
        // can be restored later
        SystemIOUtilities.replaceSystemInContents(null);

        // The previous line actually replaced System.in, but now we'll
        // "replace the replacement" with one that uses fail() if it
        // has no contents.
        System.setIn(new MutableStringBufferInputStream((String)null)
        {
            protected void handleMissingContents()
            {
                fail("Attempt to access System.in before its contents "
                    + "have been set");
            }
        });
    }


    // ----------------------------------------------------------
    /**
     * An internal helper that resets all of the input/output buffering
     * after each test case.
     */
    protected void resetIO()
    {
        // Clear out all the stream history stuff
        tcIn = null;
        tcOut = null;
        tcInBuf = null;

        resetSystemIO();
    }


    // ----------------------------------------------------------
    /**
     * Get an output stream suitable for use in test cases.  You can pass
     * this output stream to your own methods, and later call its
     * {@link student.testingsupport.PrintWriterWithHistory#getHistory()}
     * method to extract all the output in the form of a string.  The contents
     * of this stream get cleared for every test case.
     * @return a {@link java.io.PrintWriter} suitable for use in test cases
     */
    public PrintWriterWithHistory out()
    {
        if (tcOut == null)
        {
            tcOut = new PrintWriterWithHistory();
        }
        return tcOut;
    }


    // ----------------------------------------------------------
    /**
     * Get a version of {@link System#out} that records its history
     * so you can compare against it later.  The history of this
     * stream gets cleared for every test case.
     * @return a {@link java.io.PrintStream} connected to System.out that
     * is suitable for use in test cases
     */
    public PrintStreamWithHistory systemOut()
    {
        return SystemIOUtilities.out();
    }


    // ----------------------------------------------------------
    /**
     * Get a version of {@link System#err} that records its history
     * so you can compare against it later.  The history of this
     * stream gets cleared for every test case.
     * @return a {@link java.io.PrintStream} connected to System.err that
     * is suitable for use in test cases
     */
    public PrintStreamWithHistory systemErr()
    {
        return SystemIOUtilities.err();
    }


    // ----------------------------------------------------------
    /**
     * Get an input stream containing the contents that you specify.
     * Set the contents by calling {@link #setIn(String...)} or
     * {@link #setIn(Scanner)} to set the contents before you begin
     * using this stream.  This stream gets reset for every test case.
     * @return a {@link Scanner} containing any contents you
     * have specified.
     */
    public Scanner in()
    {
        if (tcIn == null)
        {
            setIn((String)null);
        }
        return tcIn;
    }


    // ----------------------------------------------------------
    /**
     * Set the contents of this test case's input stream, which can then
     * be retrieved using {@link #in()}.
     * @param contents The contents to use for the stream, which replace
     * any that were there before.  The contents can be provided as a
     * single string, or as a series of strings that should be merged
     * using {@link #multiline(String...)}.  Line terminator sequences
     * in the contents will be normalized to unix-style line endings.
     */
    public void setIn(String... contents)
    {
        String input = (contents.length == 1)
            ? normEndings(contents[0])
            : multiline(contents);
        if (tcInBuf == null)
        {
            tcInBuf = new MutableStringBufferInputStream(input)
            {
                protected void handleMissingContents()
                {
                    fail("Attempt to access built-in test case Scanner "
                        + "in() before its contents have been set");
                }
            };
        }
        else
        {
            tcInBuf.resetContents(input);
        }
        // Note that this doesn't reset the existing scanner if any
        // code still has a reference to it.
        tcIn = new Scanner(tcInBuf);
    }


    // ----------------------------------------------------------
    /**
     * Set the contents of this test case's input stream, which can then
     * be retrieved using {@link #in()}.
     * @param contents The contents to use for the stream, which replace
     * any that were there before.
     */
    public void setIn(Scanner contents)
    {
        tcIn = contents;
        tcInBuf = null;
    }


    // ----------------------------------------------------------
    /**
     * Set the contents of {@link System#in} for this test case.
     * @param contents The contents to use for the stream, which replace
     * any that were there before.  The contents can be provided as a
     * single string, or as a series of strings that should be merged
     * using {@link #multiline(String...)}.  Line terminator sequences
     * in the contents will be normalized to unix-style line endings.
     */
    public void setSystemIn(String... contents)
    {
        String input = (contents.length == 1)
            ? normEndings(contents[0])
            : multiline(contents);
        SystemIOUtilities.replaceSystemInContents(input);
    }


    // ----------------------------------------------------------
    /**
     * Combine a list of strings provided as arguments into a single
     * string, following each "line" with a line terminator.  This
     * gives the same effect as if println() were called on each argument,
     * collecting the results into a single string.  Note that line
     * terminators will be normalized to unix-style line terminators,
     * if desired.
     *
     * @param lines The series of lines to be combined (a line terminator
     *              will be added after each one).
     * @return A single string representing the concatenation of all the
     *         provided lines.
     */
    public String multiline(String... lines)
    {
        String nl = java.security.AccessController
        .doPrivileged(new java.security.PrivilegedAction<String>()
        {
            public String run()
            {
                return System.getProperty("line.separator");
            }
        });
        StringBuilder builder = new StringBuilder(lines.length * 100);
        for (String line : lines)
        {
            builder.append(line);
            builder.append(nl);
        }
        return normEndings(builder.toString());
    }


    // ----------------------------------------------------------
    /**
     * Load the contents of a text file and return it as a string
     * (normalizing newlines to unix-style line ending conventions).
     *
     * @param file The file to read from.
     *
     * @return The contents of the specified file, as a single string.
     */
    public String fromFile(File file)
    {
        try
        {
            CharBuffer buffer = CharBuffer.allocate((int)file.length());
            new FileReader(file).read(buffer);
            return normEndings(buffer.toString());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }


    // ----------------------------------------------------------
    /**
     * Load the contents of a text file and return it as a string
     * (normalizing newlines to unix-style line ending conventions).
     *
     * @param file The name of the file to read from.
     *
     * @return The contents of the specified file, as a single string.
     */
    public String fromFile(String file)
    {
        if (file != null && file.matches("([a-zA-Z]:)?[\\/].*"))
        {
            throw new IllegalArgumentException(
                "You may not use absolute file or path names in your tests, "
                + "since absolute names are machine-specific.");
        }
        return fromFile(new File(file));
    }


    // ----------------------------------------------------------
    /**
     * Access the string normalizer that this test case uses in
     * fuzzy string comparisons.  You can set your preferences for
     * fuzzy string comparisons using this object's methods.  These settings
     * are persistent from test case method to test case method, so it is
     * sufficient to set them in your test class constructor if you want
     * to use the same settings for all of your test case methods.
     * @return the string normalizer
     * @see #assertFuzzyEquals(String, String)
     * @see StringNormalizer
     * @see student.testingsupport.StringNormalizer#addStandardRules()
     */
    protected static StringNormalizer stringNormalizer()
    {
        return sn;
    }


    // ----------------------------------------------------------
    /**
     * Control whether strings manipulated in this test case will have
     * their line terminator sequences automatically normalized to unix-style
     * line terminators (default is true).
     *
     * @param value True if line endings should be automatically converted
     *              to unix-style, or false if they should be left alone.
     */
    public static void setNormalizeLineEndings(boolean value)
    {
        normalizeLineEndings = value;
    }


    // ----------------------------------------------------------
    /**
     * Control whether assertEquals() on strings will use fuzzy comparisons
     * (using the {@link #stringNormalizer()} to ignore things like
     * punctuation differences, capitalization differences, etc.--see the
     * StringNormalizer documentation for how to control what differences
     * are ignored).  The default is false, meaning that fuzzy comparisons
     * must be explicitly requested using one of the "fuzzy"-named methods.
     * Calling this method with the parameter true will cause conventional
     * assertEquals() calls to use fuzzy string normalization in comparisons
     * as well, making assertEquals() and similar methods behave the same
     * as assertFuzzyEquals().
     *
     * @param value True if string comparisons should be fuzzy in
     *              assertEquals().
     */
    public static void setUseFuzzyStringComparisons(boolean value)
    {
        useFuzzyStringComparisons = value;
    }


    // ----------------------------------------------------------
    /**
     * Asserts that two Strings are equal, respecting preferences for what
     * differences matter.  This method mirrors the static
     * {@link junit.framework.TestCase#assertEquals(String,String)}
     * method, augmenting its behavior with the ability to make "fuzzy"
     * string comparisons that ignore things like differences in spacing,
     * punctuation, or capitalization.  Use
     * {@link #stringNormalizer()} to access and modify the
     * {@link StringNormalizer} object's preferences for comparing
     * strings.
     * @param expected The expected value
     * @param actual   The value to test
     */
    public static void assertFuzzyEquals(String expected, String actual)
    {
        assertFuzzyEquals(null, expected, actual);
    }


    // ----------------------------------------------------------
    /**
     * Asserts that two Strings are equal, respecting preferences for what
     * differences matter.  This method mirrors the static
     * {@link junit.framework.TestCase#assertEquals(String,String)}
     * method, augmenting its behavior with the ability to make "fuzzy"
     * string comparisons that ignore things like differences in spacing,
     * punctuation, or capitalization.  Use
     * {@link #stringNormalizer()} to access and modify the
     * {@link StringNormalizer} object's preferences for comparing
     * strings.
     * @param message  The message to use for a failed assertion
     * @param expected The expected value
     * @param actual   The value to test
     */
    public static void assertFuzzyEquals(
        String message, String expected, String actual)
    {
        if (message != null)
        {
            message += " (after normalizing strings)";
        }
        try
        {
            assertEquals(
                message, stringNormalizer().normalize(normEndings(expected)),
                stringNormalizer().normalize(normEndings(actual)));
        }
        catch (AssertionFailedError e)
        {
            trimStack(e);
            throw e;
        }
    }


    // ----------------------------------------------------------
    /**
     * Asserts that a condition is true. If it isn't, it throws an
     * AssertionFailedError with the given message.  This is a
     * special version of
     * {@link junit.framework.TestCase#assertTrue(String,boolean)}
     * that issues special diagnostics when the assertion fails, if
     * the given condition supports it.
     * @param message   The message to use for a failed assertion
     * @param condition The condition to check
     */
    public static void assertTrue(String message, boolean condition)
    {
        String falseReason = predicateReturnsFalseReason;
        predicateReturnsFalseReason = null;
        predicateReturnsTrueReason = null;
        if (falseReason == null)
        {
            falseReason = "expected: <true> but was: <false>";
        }
        if (falseReason != null)
        {
            if (message == null)
            {
                message = falseReason;
            }
            else
            {
                message += " " + falseReason;
            }
        }
        try
        {
            junit.framework.TestCase.assertTrue(message, condition);
        }
        catch (AssertionFailedError e)
        {
            trimStack(e);
            throw e;
        }
    }


    // ----------------------------------------------------------
    /**
     * Asserts that a condition is true. If it isn't, it throws an
     * AssertionFailedError with the given message.  This is a
     * special version of
     * {@link junit.framework.TestCase#assertTrue(boolean)}
     * that issues special diagnostics when the assertion fails, if
     * the given condition supports it.
     * @param condition The condition to check
     */
    public static void assertTrue(boolean condition)
    {
        assertTrue(null, condition);
    }


    // ----------------------------------------------------------
    /**
     * Asserts that a condition is false. If it isn't, it throws an
     * AssertionFailedError with the given message.  This is a
     * special version of
     * {@link junit.framework.TestCase#assertFalse(String,boolean)}
     * that issues special diagnostics when the assertion fails, if
     * the given condition supports it.
     * @param message   The message to use for a failed assertion
     * @param condition The condition to check
     */
    public static void assertFalse(String message, boolean condition)
    {
        String trueReason = predicateReturnsTrueReason;
        predicateReturnsFalseReason = null;
        predicateReturnsTrueReason = null;
        if (trueReason == null)
        {
            trueReason = "expected: <false> but was: <true>";
        }
        if (trueReason != null)
        {
            if (message == null)
            {
                message = trueReason;
            }
            else
            {
                message += " " + trueReason;
            }
        }
        try
        {
            junit.framework.TestCase.assertFalse(message, condition);
        }
        catch (AssertionFailedError e)
        {
            trimStack(e);
            throw e;
        }
    }


    // ----------------------------------------------------------
    /**
     * Asserts that a condition is false. If it isn't, it throws an
     * AssertionFailedError with the given message.  This is a
     * special version of
     * {@link junit.framework.TestCase#assertFalse(boolean)}
     * that issues special diagnostics when the assertion fails, if
     * the given condition supports it.
     * @param condition The condition to check
     */
    public static void assertFalse(boolean condition)
    {
        assertFalse(null, condition);
    }


    // ----------------------------------------------------------
    /**
     * There is no assertion to compare primitive values using assertSame(),
     * but autoboxing will allow you to compare them as objects, which is
     * never desired.  This overloaded method flags the problem as a test
     * case failure rather than letting it go undiagnosed.
     * @param message  The message to use if the assertion fails
     * @param expected The expected value
     * @param actual The actual value
     */
    public static void assertSame(
        String message, Number expected, Number actual)
    {
        String expectedType =
            expected.getClass().getSimpleName().toLowerCase();
        String actualType =
            actual.getClass().getSimpleName().toLowerCase();
        String expectedArticle =
            expectedType.startsWith("i") ? "an" : "a";
        String actualArticle =
            actualType.startsWith("i") ? "an" : "a";

        String msg = "Your test case calls assertSame() with "
            + expectedArticle + " " + expectedType
            + " and "
            + actualArticle + " " + actualType
            + ", but assertSame() compares two objects for identity--that is, "
            + "it checks whether two references refer to the same, identical "
            + "object.  This question does not make sense for primitive data "
            + "values, so use assertEquals() instead.";
        assertMethodCalledIncorrectly(msg);
    }


    // ----------------------------------------------------------
    /**
     * There is no assertion to compare primitive values using assertSame(),
     * but autoboxing will allow you to compare them as objects, which is
     * never desired.  This overloaded method flags the problem as a test
     * case failure rather than letting it go undiagnosed.
     * @param expected The expected value
     * @param actual The actual value
     */
    public static void assertSame(Number expected, Number actual)
    {
        assertSame(null, expected, actual);
    }


    // ----------------------------------------------------------
    /**
     * There is no assertion to compare ints with floats or doubles, or
     * to compare two floats or two doubles without specifying a tolerance,
     * but autoboxing will allow you to compare them as objects, which is
     * never desired.  This overloaded method flags the problem as a test
     * case failure rather than letting it go undiagnosed.
     * @param message  The message to use if the assertion fails
     * @param expected The expected value
     * @param actual The actual value
     */
    public static void assertEquals(
        String message, Number expected, Number actual)
    {
        if (expected == null
            || actual == null
            || (expected.getClass().equals(actual.getClass())
                && !Double.class.equals(expected)
                && !Double.class.equals(actual)
                && !Float.class.equals(expected)
                && !Float.class.equals(actual)))
        {
            junit.framework.TestCase.assertEquals(message, expected, actual);
        }
        String expectedType =
            expected.getClass().getSimpleName().toLowerCase();
        String actualType =
            actual.getClass().getSimpleName().toLowerCase();
        String expectedArticle =
            expectedType.startsWith("i") ? "an" : "a";
        String actualArticle =
            actualType.startsWith("i") ? "an" : "a";

        String msg = "Your test case calls assertEquals() with "
            + expectedArticle + " " + expectedType
            + " and "
            + actualArticle + " " + actualType
            + ", but comparing them this way may give incorrect results.";
        if (!expected.getClass().equals(actual.getClass()))
        {
            msg += " Instead, use a type cast to convert one of the "
                + "arguments so you are comparing values of the same type.";
        }
        if (expected instanceof Float || expected instanceof Double
            || actual instanceof Float || actual instanceof Double)
        {
            msg += " Don't forget that comparing two floating point "
                + "values requires a third argument indicating how close they "
                + "have to be to be considered equal.";
        }
        assertMethodCalledIncorrectly(msg);
    }


    // ----------------------------------------------------------
    /**
     * There is no assertion to compare ints with floats or doubles, or
     * to compare two floats or two doubles without specifying a tolerance,
     * but autoboxing will allow you to compare them as objects, which is
     * never desired.  This overloaded method flags the problem as a test
     * case failure rather than letting it go undiagnosed.
     * @param expected The expected value
     * @param actual The actual value
     */
    public static void assertEquals(Number expected, Number actual)
    {
        assertEquals(null, expected, actual);
    }


    // ----------------------------------------------------------
    /**
     * Asserts that two Strings are equal.
     * @param message  The message to use if the assertion fails
     * @param expected The expected value
     * @param actual The actual value
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(
        String message, String expected, String actual)
    {
        if (useFuzzyStringComparisons)
        {
            assertFuzzyEquals(message, expected, actual);
        }

        if (message == null)
        {
            message = "";
        }
        junit.framework.Assert.assertEquals(
            message, normEndings(expected), normEndings(actual));
    }


    // ----------------------------------------------------------
    /**
     * Asserts that two Strings are equal.
     * @param expected The expected value
     * @param actual The actual value
     */
    public static void assertEquals(String expected, String actual)
    {
        assertEquals("", expected, actual);
    }


    // ----------------------------------------------------------
    /**
     * Takes a string and, if it is too long, shortens it by replacing the
     * middle with an ellipsis.  For example, calling <code>compact("hello
     * there", 6, 3)</code> will return "hel...ere".
     * @param content The string to shorten
     * @param threshold Strings longer than this will be compacted, while
     *        strings less than or equal to this limit will be returned
     *        unchanged
     * @param prefixLen How many characters at the front and back of the
     *        string to keep.  This number must be less than or equal to half
     *        the threshold
     * @return The shortened version of the string
     */
    public static String compact(String content, int threshold, int prefixLen)
    {
        if (content != null && content.length() > threshold)
        {
            assert prefixLen < (threshold + 1) / 2;
            return content.substring(0, prefixLen) + "..."
                + content.substring(content.length() - prefixLen);
        }
        else
        {
            return content;
        }
    }


    // ----------------------------------------------------------
    /**
     * Takes a string and, if it is too long, shortens it by replacing the
     * middle with an ellipsis.
     * @param content The string to shorten
     * @return The shortened version of the string
     */
    public static String compact(String content)
    {
        return compact(content, 15, 5);
    }


    // ----------------------------------------------------------
    /**
     * Determines whether two Strings are equal.  This method is identical
     * to {@link String#equals(Object)}, but is provided for symmetry with
     * the other comparison predicates provided in this class.  For
     * assertion writing, remember that
     * {@link junit.framework.TestCase#assertEquals(String,String)} will
     * produce more useful information on failure, however.
     * @param left  The first string to compare
     * @param right The second string to compare
     * @return True if the strings are equal
     */
    public boolean equals(String left, String right)
    {
        boolean result = left == right;
        left = normEndings(left);
        right = normEndings(right);
        if (left != null && right != null)
        {
            result = left.equals(right);
        }
        if (result)
        {
            predicateReturnsTrueReason =
                "<" + compact(left) + "> was the same as:<"
                + compact(right) + ">";
        }
        else
        {
            String msg =
                (new junit.framework.ComparisonFailure(null, left, right))
                    .getMessage();
            if (msg.startsWith("null "))
            {
                msg = msg.substring("null ".length());
            }
            predicateReturnsFalseReason = msg;
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Determines whether two Strings are equal, respecting preferences for
     * what differences matter.  This method mirrors
     * {@link #equals(String,String)}, augmenting its behavior with the
     * ability to make "fuzzy" string comparisons that ignore things like
     * differences in spacing, punctuation, or capitalization.  It is also
     * identical to {@link #assertFuzzyEquals(String,String)}, except that it
     * returns the boolean result of the comparison instead of making a
     * test case assertion.  Use
     * {@link #stringNormalizer()} to access and modify the
     * {@link StringNormalizer} object's preferences for comparing
     * strings.  For assertion writing, remember that
     * {@link #assertFuzzyEquals(String,String)} will
     * produce more useful information on failure, however.
     * @param left  The first string to compare
     * @param right The second string to compare
     * @return True if the strings are equal
     */
    public boolean fuzzyEquals(String left, String right)
    {
        return equals(stringNormalizer().normalize(left),
            stringNormalizer().normalize(right));
    }


    // ----------------------------------------------------------
    /**
     * Determines whether a String exactly matches an expected regular
     * expression.  A null for the actual value is treated the same as an
     * empty string for the purposes of matching.  The regular expression
     * must match the full string (all characters taken together).  To
     * match a substring, use {@link #containsRegex(String,String...)}
     * instead.
     * <p>
     * Note that this predicate uses the opposite parameter ordering
     * from JUnit assertions: The value to test is the <b>first</b>
     * parameter, and the expected pattern is the <b>second</b>.
     * </p>
     * @param actual   The value to test
     * @param expected The expected value (interpreted as a regular
     *                 expression {@link Pattern})
     * @return True if the actual matches the expected pattern
     */
    public boolean equalsRegex(String actual, String expected)
    {
        return equalsRegex(actual, Pattern.compile(expected));
    }


    // ----------------------------------------------------------
    /**
     * Determines whether a String exactly matches an expected regular
     * expression.  A null for the actual value is treated the same as an
     * empty string for the purposes of matching.  The regular expression
     * must match the full string (all characters taken together).  To
     * match a substring, use {@link #containsRegex(String,Pattern...)}
     * instead.
     * <p>
     * Note that this predicate uses the opposite parameter ordering
     * from JUnit assertions: The value to test is the <b>first</b>
     * parameter, and the expected pattern is the <b>second</b>.
     * </p>
     * @param actual   The value to test
     * @param expected The expected value
     * @return True if the actual matches the expected pattern
     */
    public boolean equalsRegex(String actual, Pattern expected)
    {
        if (actual == null)
        {
            actual = "";
        }
        actual = normEndings(actual);
        boolean result = expected.matcher(actual).matches();
        if (result)
        {
            predicateReturnsTrueReason =
                "<" + compact(actual) + "> matches regex:<"
                + compact(expected.toString(), 25, 10) + ">";
        }
        else
        {
            predicateReturnsFalseReason =
                "<" + compact(actual) + "> does not match regex:<"
                + compact(expected.toString(), 25, 10) + ">";
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Determines whether a String exactly matches an expected regular
     * expression, respecting preferences for what differences matter.
     * A null for the actual value is treated the same as an empty string
     * for the purposes of matching.  The regular expression must match
     * the full string (all characters taken together).  To match a substring,
     * use {@link #fuzzyContainsRegex(String,String...)} instead.
     * <p>
     * Note that this predicate uses the opposite parameter ordering
     * from JUnit assertions: The value to test is the <b>first</b>
     * parameter, and the expected pattern is the <b>second</b>.
     * </p>
     * <p>Use
     * {@link #stringNormalizer()} to access and modify the
     * {@link StringNormalizer} object's preferences for comparing
     * strings.</p>
     * @param actual   The value to test
     * @param expected The expected value (interpreted as a regular
     *                 expression {@link Pattern})
     * @return True if the actual matches the expected pattern
     */
    public boolean fuzzyEqualsRegex(String actual, String expected)
    {
        return fuzzyEqualsRegex(actual, Pattern.compile(expected));
    }


    // ----------------------------------------------------------
    /**
     * Determines whether a String exactly matches an expected regular
     * expression, respecting preferences for what differences matter.
     * A null for the actual value is treated the same as an empty string
     * for the purposes of matching.  The regular expression must match
     * the full string (all characters taken together).  To match a substring,
     * use {@link #fuzzyContainsRegex(String,String...)} instead.
     * <p>Use
     * {@link #stringNormalizer()} to access and modify the
     * {@link StringNormalizer} object's preferences for comparing
     * strings.</p>
     * @param actual   The value to test
     * @param expected The expected value
     * @return True if the actual matches the expected pattern
     */
    public boolean fuzzyEqualsRegex(String actual, Pattern expected)
    {
        return equalsRegex(
            stringNormalizer().normalize(actual), expected);
    }


    // ----------------------------------------------------------
    /**
     * Determine whether one String contains a sequence of other substrings
     * in order.  In addition to the string to search, you can provide an
     * arbitrary number of additional parameters to search for.  If you only
     * provide one substring, this method behaves the same as
     * {@link String#contains(CharSequence)}.  If you provide more than
     * one substring, it looks for each such element in turn
     * in the larger string, making sure they are all found in the proper order
     * (each substring must strictly follow the previous one, although there
     * can be any amount of intervening characters between any two substrings
     * in the array).  If the larger string is null, this method returns
     * false (since it can contain nothing).
     * <p>
     * Note that this predicate uses the opposite parameter ordering
     * from JUnit assertions: The value to test is the <b>first</b>
     * parameter, and the expected substrings are the <b>second</b>.
     * </p>
     * @param largerString The target to look in
     * @param substrings   One or more substrings to look for (in order)
     * @return True if the largerString contains all of the specified
     * substrings in order.
     */
    public boolean contains(String largerString, String ... substrings)
    {
        largerString = normEndings(largerString);
        int pos = (largerString == null) ? -1 : 0;
        for (int i = 0; i < substrings.length  &&  pos >= 0; i++)
        {
            substrings[i] = normEndings(substrings[i]);
            pos = largerString.indexOf(substrings[i], pos);
            if (pos >= 0)
            {
                pos += substrings[i].length();
            }
            else
            {
                predicateReturnsFalseReason =
                    "<" + compact(largerString) + "> does not contain:<"
                    + compact(substrings[i], 25, 10) + ">";
                if (substrings.length > 1)
                {
                    predicateReturnsFalseReason += "(substring " + i + ")";
                }
                break;
            }
        }
        if (pos >= 0)
        {
            predicateReturnsTrueReason =
                "<" + compact(largerString) + "> contains:";
            for (int i = 0; i < substrings.length; i++)
            {
                if (i > 0)
                {
                    predicateReturnsTrueReason += ", ";
                }
                predicateReturnsTrueReason +=
                    "<" + compact(substrings[i], 25, 10) + ">";
            }
            return true;
        }
        else
        {
            return false;
        }
    }


    // ----------------------------------------------------------
    /**
     * Determine whether one String contains a sequence of other substrings
     * in order, respecting preferences for what differences matter.  It
     * looks for each of the specified substrings in turn
     * in the larger string, making sure they are all found in the proper order
     * (each substring must strictly follow the previous one, although there
     * can be any amount of intervening characters between any two substrings
     * in the array).  If the larger string is null, this method returns
     * false (since it can contain nothing).
     * <p>This method makes "fuzzy" string comparisons that ignore things
     * like differences in spacing, punctuation, or capitalization.  Use
     * {@link #stringNormalizer()} to access and modify the
     * {@link StringNormalizer} object's preferences for comparing
     * strings.
     * </p>
     * <p>
     * Note that this predicate uses the opposite parameter ordering
     * from JUnit assertions: The value to test is the <b>first</b>
     * parameter, and the expected substrings are the <b>second</b>.
     * </p>
     * @param largerString The target to look in
     * @param substrings   The substrings to look for (in order)
     * @return True if the largerString contains all of the specified
     * substrings in order.
     */
    public boolean fuzzyContains(String largerString, String ... substrings)
    {
        // Normalized the array of expected substrings
        String[] normalizedSubstrings = new String[substrings.length];
        for (int i = 0; i < substrings.length; i++)
        {
            normalizedSubstrings[i] =
                stringNormalizer().normalize(substrings[i]);
        }

        // Now call the regular version on the normalized args
        return contains(
            stringNormalizer().normalize(largerString), normalizedSubstrings);
    }


    // ----------------------------------------------------------
    /**
     * Determine whether one String contains a sequence of other substrings
     * in order, where the expected substrings are specified as a regular
     * expressions.  It looks for each of the specified regular expressions
     * in turn in the larger string, making sure they are all found in the
     * proper order (each substring must strictly follow the previous one,
     * although there can be any amount of intervening characters between
     * any two substrings in the array).  If the larger string is null, this
     * method returns false (since it can contain nothing).
     * <p>
     * Note that this predicate uses the opposite parameter ordering
     * from JUnit assertions: The value to test is the <b>first</b>
     * parameter, and the expected substrings are the <b>second</b>.
     * </p>
     * @param largerString The target to look in
     * @param substrings   A sequence of expected substrings (interpreted as
     *                     regular expression {@link Pattern}s), which must
     *                     occur in the same order in the larger string
     * @return True if the largerString contains all of the specified
     * regular expressions in order.
     */
    public boolean containsRegex(String largerString, String ... substrings)
    {
        Pattern[] patterns = new Pattern[substrings.length];
        for (int i = 0; i < substrings.length; i++)
        {
            patterns[i] = Pattern.compile(substrings[i]);
        }
        return containsRegex(largerString, patterns);
    }


    // ----------------------------------------------------------
    /**
     * Determine whether one String contains a sequence of other substrings
     * in order, where the expected substrings are specified as a regular
     * expressions.  It looks for each of the specified regular expressions
     * in turn in the larger string, making sure they are all found in the
     * proper order (each substring must strictly follow the previous one,
     * although there can be any amount of intervening characters between
     * any two substrings in the array).  If the larger string is null, this
     * method returns false (since it can contain nothing).
     * @param largerString The target to look in
     * @param substrings   A sequence of expected regular expressions, which
     *                     must occur in the same order in the larger string
     * @return True if the largerString contains all of the specified
     * regular expressions in order.
     */
    public boolean containsRegex(String largerString, Pattern ... substrings)
    {
        largerString = normEndings(largerString);
        boolean result = true;
        int pos = 0;
        for (int i = 0; i < substrings.length; i++)
        {
            Matcher matcher = substrings[i].matcher(largerString);
            result = matcher.find(pos);
            if (!result)
            {
                predicateReturnsFalseReason =
                    "<" + compact(largerString) + "> does not contain regex:<"
                    + compact(substrings[i].toString(), 25, 10) + ">";
                if (substrings.length > 1)
                {
                    predicateReturnsFalseReason += "(pattern " + i + ")";
                }
                break;
            }
            pos = matcher.end();
        }
        if (result)
        {
            predicateReturnsTrueReason =
                "<" + compact(largerString) + "> contains regexes:";
            for (int i = 0; i < substrings.length; i++)
            {
                if (i > 0)
                {
                    predicateReturnsTrueReason += ", ";
                }
                predicateReturnsTrueReason +=
                    "<" + compact(substrings[i].toString(), 25, 10) + ">";
            }
            return true;
        }
        else
        {
            return false;
        }
    }


    // ----------------------------------------------------------
    /**
     * Determine whether one String contains a sequence of other substrings
     * in order, where the expected substrings are specified as regular
     * expressions, and respecting preferences for what differences matter.
     * This method behaves just like {@link #fuzzyContains(String,String...)},
     * except that the second argument is interpreted as an array of regular
     * expressions.  String normalization rules are only appled to the
     * larger string, not to the regular expressions.
     * @param largerString The target to look in
     * @param substrings   An array of expected substrings (interpreted as
     *                     regular expression {@link Pattern}s), which must
     *                     occur in the same order in the larger string
     * @return True if the largerString contains all of the specified
     * substrings in order.
     */
    public boolean fuzzyContainsRegex(
        String largerString, String ... substrings)
    {
        Pattern[] patterns = new Pattern[substrings.length];
        for (int i = 0; i < substrings.length; i++)
        {
            patterns[i] = Pattern.compile(substrings[i]);
        }
        return fuzzyContainsRegex(largerString, patterns);
    }


    // ----------------------------------------------------------
    /**
     * Determine whether one String contains a sequence of other substrings
     * in order, where the expected substrings are specified as regular
     * expressions, and respecting preferences for what differences matter.
     * This method behaves just like {@link #fuzzyContains(String,String...)},
     * except that the second argument is interpreted as an array of regular
     * expressions.  String normalization rules are only appled to the
     * larger string, not to the regular expressions.
     * @param largerString The target to look in
     * @param substrings   An array of expected substrings, which must
     *                     occur in the same order in the larger string
     * @return True if the largerString contains all of the specified
     * substrings in order.
     */
    public boolean fuzzyContainsRegex(
        String largerString, Pattern ... substrings)
    {
        return containsRegex(
            stringNormalizer().normalize(largerString), substrings);
    }


    // ----------------------------------------------------------
    /**
     * Resets IO and output reasons, then instruments IO for next method.
     */
    @Before
    public void prepareIO()
    {
        resetIO();
        predicateReturnsTrueReason = null;
        predicateReturnsFalseReason = null;
        instrumentIO();
    }


    // ----------------------------------------------------------
    /**
     * An internal helper that resets the system part of the input/output
     * buffering after each test case.
     */
    @AfterClass
    public static void resetSystemIO()
    {
        // Make sure these are history-wrapped
        SystemIOUtilities.out().clearHistory();
        SystemIOUtilities.err().clearHistory();
        SystemIOUtilities.restoreSystemIn();
    }


    // ----------------------------------------------------------
    /**
     * This method is for internal use only and should not be called
     * by other code.  It is used to install System.exit() prevention
     * control.
     */
    @BeforeClass
    public static void installExitHandler()
    {
        student.testingsupport.ExitPreventingSecurityManager.install();
    }


    // ----------------------------------------------------------
    /**
     * This method is for internal use only and should not be called
     * by other code.  It is used to remove System.exit() prevention
     * control after the test class has been completed.
     */
    @AfterClass
    public static void uninstallExitHandler()
    {
        student.testingsupport.ExitPreventingSecurityManager.uninstall();
    }


    //-----------------------------------------------------------------
    /**
     * Throws a ReflectionError with the given message.
     * @param message The message will be used to create ReflectionError
     */
    private static void assertMethodCalledIncorrectly(String message)
    {
        AssertMethodCalledIncorrectly error =
            new AssertMethodCalledIncorrectly(message);
        trimStack(error);
        throw error;
    }


    // ----------------------------------------------------------
    private static String normEndings(String text)
    {
        return (text != null && normalizeLineEndings)
          ? Assert.normalizeLineEndings(text)
          : text;
    }


    // ----------------------------------------------------------
    private static void trimStack(Throwable t)
    {
        if (trimStackTraces == null)
        {
            try
            {
                String setting =
                    System.getProperty("student.TestCase.trimStackTraces");
                if (setting == null)
                {
                    trimStackTraces = true;
                }
                else
                {
                    setting = setting.toLowerCase().trim();
                    trimStackTraces = "yes".equals(setting)
                        || "true".equals(setting)
                        || "on".equals(setting)
                        || "1".equals(setting);
                }
            }
            catch (Exception e)
            {
                trimStackTraces = true;
            }
        }

        if (!trimStackTraces)
        {
            return;
        }

        StackTraceElement[] oldTrace = t.getStackTrace();
        int pos1 = 0;
        while (pos1 < oldTrace.length
            && (oldTrace[pos1].getClassName().equals("junit.framework.Assert")))
        {
            ++pos1;
        }
        int pos2 = pos1;
        while (pos2 < oldTrace.length
            && (oldTrace[pos2].getClassName().equals("student.TestCase")))
        {
            ++pos2;
        }

        // It would be good to strip out a top-level stack trace element for
        // student.TestCase.runBare(), which will come soon

        if (pos2 > pos1 && pos2 < oldTrace.length - 1)
        {
            StackTraceElement[] newTrace =
                new StackTraceElement[oldTrace.length - (pos2 - pos1)];
            if (pos1 > 0)
            {
                System.arraycopy(oldTrace, 0, newTrace, 0, pos1);
            }
            System.arraycopy(
                oldTrace, pos2, newTrace, pos1, newTrace.length - pos1);
            t.setStackTrace(newTrace);
        }
    }
}
