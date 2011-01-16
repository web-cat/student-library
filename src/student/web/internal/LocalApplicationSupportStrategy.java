/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2009-2010 Virginia Tech
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

package student.web.internal;

import java.util.HashMap;
import java.util.Map;

//-------------------------------------------------------------------------
/**
 *  Defines the support methods needed to implement server-specific
 *  storage features for an {@link student.web.Application} that is
 *  running as a plain old Java program, rather than in the context
 *  of a web server application.
 *
 *  @author  Stephen Edwards
 *  @author Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public class LocalApplicationSupportStrategy
    implements ApplicationSupportStrategy
{
    //~ Instance/static variables .............................................
	private static ThreadLocal<Map<String,Object>> localSessionMap = new ThreadLocal<Map<String,Object>>()
	{
		public Map<String, Object> initialValue()
		{
			return null;
		}
	};
	private Map<String, Object> sessionValues;
    private String path = "index.zhtml";


    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Create a new object.
     */
    public LocalApplicationSupportStrategy()
    {
    	if(localSessionMap.get() == null)
    	{
    		sessionValues = new HashMap<String,Object>();
    		localSessionMap.set(sessionValues);
    	}
    	else
    	{
    		sessionValues = localSessionMap.get();
    	}
    }


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     *  Get a parameter passed to this page in the query part of the URL.
     *  @param name The name of the parameter to retrieve
     *  @return The parameter's value on the current page, or null if
     *  there is none.
     */
    public String getPageParameter(String name)
    {
        return null;
    }


    // ----------------------------------------------------------
    /**
     *  Get a parameter stored in the current session.
     *  @param name The name of the parameter to retrieve
     *  @return The parameter's value in the current session, or null if
     *  there is none.
     */
    public Object getSessionParameter(String name)
    {
        return sessionValues.get(name);
    }


    // ----------------------------------------------------------
    /**
     *  Remove a parameter stored in the current session, if it exists.
     *  @param name The name of the parameter to remove
     *  @return The removed value, if the parameter existed, or null if
     *  there is no value to remove.
     */
    public Object removeSessionParameter(String name)
    {
        return sessionValues.remove(name);
    }


    // ----------------------------------------------------------
    /**
     *  Store a value in the current session.  If a value already exists
     *  for the given name, it is replaced.
     *  @param name The name of the parameter to store
     *  @param value The value to store
     *  @return The previous value in the current session associated with
     *  the given name, if there is one, or null otherwise.
     */
    public Object setSessionParameter(String name, Object value)
    {
        return sessionValues.put(name, value);
    }


    // ----------------------------------------------------------
    /**
     *  Cause the web application to show a different web page in the
     *  user's web browser.
     *  @param url The new web page to show in the user's browser
     */
    public void showWebPage(String url)
    {
        if (url != null && url.length() > 0)
        {
            System.out.println("web application switches to page: " + url);
            path = url;
        }
    }


    // ----------------------------------------------------------
    /**
     *  Retrieve the name of the current ZHTML file, such as
     *  "index.zhtml" or "lab02.zhtml".
     *  @return The name of the current ZHTML file, without any
     *  directory component, or "" if there is none.
     */
    public String getCurrentPageName()
    {
        String result = getCurrentPagePath();
        int pos = result.lastIndexOf('/');
        if (pos >= 0)
        {
            result = result.substring(0, pos);
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     *  Retrieve the relative path name of the current ZHTML file, such
     *  as "/Fall09/mypid/index.zhtml" or "/Fall09/mypid/lab02/lab02.zhtml".
     *  @return The name path to the current ZHTML file,
     *  or "" if there is none.
     */
    public String getCurrentPagePath()
    {
        return path;
    }
}
