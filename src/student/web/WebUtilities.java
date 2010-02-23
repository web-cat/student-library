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

package student.web;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

//-------------------------------------------------------------------------
/**
 *  This class provides static utility methods that streamline some
 *  web-related operations.
 *
 *  @author  Stephen Edwards
 *  @author Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public class WebUtilities
{
    //~ Instance/static variables .............................................

    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new WebUtilities object.
     */
    private WebUtilities()
    {
        // Nothing to do
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Encodes a string for use in a URL.
     * This operation wraps the behavior of
     * {@link URLEncoder#encode(String,String)}, using a UTF-8 encoding and
     * turning any exceptions into RuntimeExceptions.
     *
     * @param content The string to encode
     * @return The URL-encoded version of the parameter
     */
    public static String urlEncode(String content)
    {
        String result = content;
        try
        {
            result = URLEncoder.encode(content, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Decodes a string extracted from a URL parameter.
     * This operation wraps the behavior of
     * {@link URLDecoder#decode(String,String)}, using a UTF-8 encoding and
     * turning any exceptions into RuntimeExceptions.
     *
     * @param content The string to decode
     * @return The URL-decoded version of the parameter
     */
    public static String urlDecode(String content)
    {
        String result = content;
        try
        {
            result = URLDecoder.decode(content, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Creates a URL object from a string, without throwing declared
     * exceptions.  This wrapper simplifies creation of URL objects in
     * student code by eliminating the need for explicit try/catch blocks
     * if you just want to create a new URL object.
     *
     * @param url The url as a string
     * @return The new URL object for the given address
     */
    public static URL urlFor(String url)
    {
        URL result = null;
        try
        {
            result = new URL(url);
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
        return result;
    }
}
