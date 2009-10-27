package student.web;

import java.net.*;
import java.io.UnsupportedEncodingException;

//-------------------------------------------------------------------------
/**
 *  This class provides static utility methods that streamline some
 *  web-related operations.
 *
 *  @author  Stephen Edwards
 *  @version 2007.10.11
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
