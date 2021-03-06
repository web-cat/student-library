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

package student.testingsupport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

//-------------------------------------------------------------------------
/**
 *  An enhanced version of {@link PrintStream} that provides for a history
 *  recall function and some other features making I/O testing a bit
 *  easier to perform.  See the documentation for {@link PrintStream} for
 *  more thorough details on what methods are provided.
 *
 *  @author  Stephen Edwards
 *  @author Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public class PrintStreamWithHistory
    extends PrintStream
{
    //~ Instance/static variables .............................................

    private StringWriter history = new StringWriter();
    private String historyAsString = null;
    private boolean normalizeLineEndings = true;


    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Create a new print stream.  This stream will not flush automatically.
     *
     * @param  out        The output stream to which values and objects will be
     *                    printed
     *
     * @see java.io.PrintWriter#PrintWriter(java.io.OutputStream)
     */
    public PrintStreamWithHistory(OutputStream out)
    {
        super(out, false);
    }


    // ----------------------------------------------------------
    /**
     * Create a new print stream.
     *
     * @param  out        The output stream to which values and objects will be
     *                    printed
     * @param  autoFlush  A boolean; if true, the output buffer will be flushed
     *                    whenever a byte array is written, one of the
     *                    <code>println</code> methods is invoked, or a newline
     *                    character or byte (<code>'\n'</code>) is written
     *
     * @see java.io.PrintWriter#PrintWriter(java.io.OutputStream, boolean)
     */
    public PrintStreamWithHistory(OutputStream out, boolean autoFlush)
    {
        super(out, autoFlush);
    }


    // ----------------------------------------------------------
    /**
     * Create a new print stream.
     *
     * @param  out        The output stream to which values and objects will be
     *                    printed
     * @param  autoFlush  A boolean; if true, the output buffer will be flushed
     *                    whenever a byte array is written, one of the
     *                    <code>println</code> methods is invoked, or a newline
     *                    character or byte (<code>'\n'</code>) is written
     * @param  encoding   The name of a supported
     *                    <a href="../lang/package-summary.html#charenc">
     *                    character encoding</a>
     *
     * @exception  UnsupportedEncodingException
     *             If the named encoding is not supported
     */
    public PrintStreamWithHistory(
        OutputStream out, boolean autoFlush, String encoding)
        throws UnsupportedEncodingException
    {
        super(out, autoFlush, encoding);
    }


    // ----------------------------------------------------------
    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file name.  This convenience constructor creates
     * the necessary intermediate {@link java.io.OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the
     * {@linkplain java.nio.charset.Charset#defaultCharset default charset}
     * for this instance of the Java virtual machine.
     *
     * @param  fileName
     *         The name of the file to use as the destination of this print
     *         stream.  If the file exists, then it will be truncated to
     *         zero size; otherwise, a new file will be created.  The output
     *         will be written to the file and is buffered.
     *
     * @throws  FileNotFoundException
     *          If the given file object does not denote an existing, writable
     *          regular file and a new regular file of that name cannot be
     *          created, or if some other error occurs while opening or
     *          creating the file
     *
     * @throws  SecurityException
     *          If a security manager is present and {@link
     *          SecurityManager#checkWrite checkWrite(fileName)} denies write
     *          access to the file
     *
     * @since  1.5
     */
    public PrintStreamWithHistory(String fileName)
        throws FileNotFoundException
    {
        super(fileName);
    }


    // ----------------------------------------------------------
    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file name and charset.  This convenience constructor creates
     * the necessary intermediate {@link java.io.OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the provided
     * charset.
     *
     * @param  fileName
     *         The name of the file to use as the destination of this print
     *         stream.  If the file exists, then it will be truncated to
     *         zero size; otherwise, a new file will be created.  The output
     *         will be written to the file and is buffered.
     *
     * @param  csn
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @throws  FileNotFoundException
     *          If the given file object does not denote an existing, writable
     *          regular file and a new regular file of that name cannot be
     *          created, or if some other error occurs while opening or
     *          creating the file
     *
     * @throws  SecurityException
     *          If a security manager is present and {@link
     *          SecurityManager#checkWrite checkWrite(fileName)} denies write
     *          access to the file
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     *
     * @since  1.5
     */
    public PrintStreamWithHistory(String fileName, String csn)
        throws FileNotFoundException, UnsupportedEncodingException
    {
        super(fileName, csn);
    }


    // ----------------------------------------------------------
    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file.  This convenience constructor creates the necessary
     * intermediate {@link java.io.OutputStreamWriter OutputStreamWriter},
     * which will encode characters using the {@linkplain
     * java.nio.charset.Charset#defaultCharset default charset} for this
     * instance of the Java virtual machine.
     *
     * @param  file
     *         The file to use as the destination of this print stream.  If the
     *         file exists, then it will be truncated to zero size; otherwise,
     *         a new file will be created.  The output will be written to the
     *         file and is buffered.
     *
     * @throws  FileNotFoundException
     *          If the given file object does not denote an existing, writable
     *          regular file and a new regular file of that name cannot be
     *          created, or if some other error occurs while opening or
     *          creating the file
     *
     * @throws  SecurityException
     *          If a security manager is present and {@link
     *          SecurityManager#checkWrite checkWrite(file.getPath())}
     *          denies write access to the file
     *
     * @since  1.5
     */
    public PrintStreamWithHistory(File file)
        throws FileNotFoundException
    {
        super(file);
    }


    // ----------------------------------------------------------
    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file and charset.  This convenience constructor creates
     * the necessary intermediate {@link java.io.OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the provided
     * charset.
     *
     * @param  file
     *         The file to use as the destination of this print stream.  If the
     *         file exists, then it will be truncated to zero size; otherwise,
     *         a new file will be created.  The output will be written to the
     *         file and is buffered.
     *
     * @param  csn
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @throws  FileNotFoundException
     *          If the given file object does not denote an existing, writable
     *          regular file and a new regular file of that name cannot be
     *          created, or if some other error occurs while opening or
     *          creating the file
     *
     * @throws  SecurityException
     *          If a security manager is presentand {@link
     *          SecurityManager#checkWrite checkWrite(file.getPath())}
     *          denies write access to the file
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     *
     * @since  1.5
     */
    public PrintStreamWithHistory(File file, String csn)
        throws FileNotFoundException, UnsupportedEncodingException
    {
        super(file, csn);
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Retrieve the text history of what has been sent to this PrintStream.
     * This will include all text printed through this object.  The
     * {@link #clearHistory()} method resets the history to be empty, just
     * as when the object was first created.  Note that newline characters
     * in the history are always represented by '\n', regardless of what
     * value the system <code>line.separator</code> property has.
     * @return all the text sent to this PrintStream
     */
    public String getHistory()
    {
        synchronized (history)
        {
            if (historyAsString == null)
            {
                historyAsString = history.toString();
                if (normalizeLineEndings)
                {
                    historyAsString =
                        student.Assert.normalizeLineEndings(historyAsString);
                }
            }
            return historyAsString;
        }
    }


    // ----------------------------------------------------------
    /**
     * Reset this object's history to be empty, just as when the object was
     * first created.  You can access the history using {@link #getHistory()}.
     */
    public void clearHistory()
    {
        synchronized (history)
        {
            historyAsString = null;
            getHistoryBuffer().setLength(0);
        }
    }


    // ----------------------------------------------------------
    /**
     * Set whether this object's history will have normalized unix-style
     * line endings, or the raw line endings generated by printing.  The
     * default for this is true, so history values will automatically be
     * normalized unless this method is used to change that behavior.
     *
     * @param value If true, the retrieved history will have all line
     *              endings automatically standardized to unix-style
     *              ("\n") line endings; if fase, the retrieved history
     *              will be presented without any modifications, using
     *              whatever (platform-specific) line endings were printed
     *              to this object.
     */
    public void setNormalizeLineEndings(boolean value)
    {
        normalizeLineEndings = value;
    }


    // ----------------------------------------------------------
    /**
     * Returns whether this object's history has normalized unix-style
     * line endings, or the raw line endings generated by printing.  The
     * default behavior is to normalize line endings.
     *
     * @return True if line endings will be normalized in the history.
     */
    public boolean getNormalizeLineEndings()
    {
        return normalizeLineEndings;
    }


    // ----------------------------------------------------------
    /**
     * Write the specified byte to this stream.  If the byte is a newline and
     * automatic flushing is enabled then the <code>flush</code> method will be
     * invoked.
     *
     * <p> Note that the byte is written as given; to write a character that
     * will be translated according to the platform's default character
     * encoding, use the <code>print(char)</code> or <code>println(char)</code>
     * methods.
     *
     * @param  b  The byte to be written
     * @see #print(char)
     * @see #println(char)
     */
    public void write(int b)
    {
        synchronized (history)
        {
            super.write(b);
            history.write(b);
            historyAsString = null;
        }
    }


    // ----------------------------------------------------------
    /**
     * Write <code>len</code> bytes from the specified byte array starting at
     * offset <code>off</code> to this stream.  If automatic flushing is
     * enabled then the <code>flush</code> method will be invoked.
     *
     * <p> Note that the bytes will be written as given; to write characters
     * that will be translated according to the platform's default character
     * encoding, use the <code>print(char)</code> or <code>println(char)</code>
     * methods.
     *
     * @param  buf   A byte array
     * @param  off   Offset from which to start taking bytes
     * @param  len   Number of bytes to write
     */
    public void write(byte buf[], int off, int len)
    {
        synchronized (history)
        {
            super.write(buf, off, len);
            history.write(new String(buf, off, len));
            historyAsString = null;
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the StringBuffer object used to store this object's text
     * history.
     * @return The history as a string buffer
     */
    private StringBuffer getHistoryBuffer()
    {
        synchronized (history)
        {
            return history.getBuffer();
        }
    }

}
