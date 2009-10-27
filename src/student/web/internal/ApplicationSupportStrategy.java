package student.web.internal;

//-------------------------------------------------------------------------
/**
 *  Defines the support methods needed to implement server-specific
 *  storage features for an {@link student.web.Application}.
 *
 *  @author  Stephen Edwards
 *  @version 2009.09.14
 */
public interface ApplicationSupportStrategy
{
    // ----------------------------------------------------------
    /**
     *  Cause the web application to show a different web page in the
     *  user's web browser.
     *  @param url The new web page to show in the user's browser
     */
    void showWebPage(String url);


    // ----------------------------------------------------------
    /**
     *  Retrieve the name of the current ZHTML file, such as
     *  "index.zhtml" or "lab02.zhtml".
     *  @return The name of the current ZHTML file, without any
     *  directory component, or "" if there is none.
     */
    String getCurrentPageName();


    // ----------------------------------------------------------
    /**
     *  Retrieve the relative path name of the current ZHTML file, such
     *  as "/Fall09/mypid/index.zhtml" or "/Fall09/mypid/lab02/lab02.zhtml".
     *  @return The name path to the current ZHTML file,
     *  or "" if there is none.
     */
    String getCurrentPagePath();


    // ----------------------------------------------------------
    /**
     *  Get a parameter passed to this page in the query part of the URL.
     *  @param name The name of the parameter to retrieve
     *  @return The parameter's value on the current page, or null if
     *  there is none.
     */
    String getPageParameter(String name);


    // ----------------------------------------------------------
    /**
     *  Get a parameter stored in the current session.
     *  @param name The name of the parameter to retrieve
     *  @return The parameter's value in the current session, or null if
     *  there is none.
     */
    Object getSessionParameter(String name);


    // ----------------------------------------------------------
    /**
     *  Store a value in the current session.  If a value already exists
     *  for the given name, it is replaced.
     *  @param name The name of the parameter to store
     *  @param value The value to store
     *  @return The previous value in the current session associated with
     *  the given name, if there is one, or null otherwise.
     */
    Object setSessionParameter(String name, Object value);


    // ----------------------------------------------------------
    /**
     *  Remove a parameter stored in the current session, if it exists.
     *  @param name The name of the parameter to remove
     *  @return The removed value, if the parameter existed, or null if
     *  there is no value to remove.
     */
    Object removeSessionParameter(String name);
}
