/*
 * ==========================================================================*\
 * | $Id: LocalApplicationSupportStrategy.java,v 1.3 2011/01/16 20:33:00
 * mwoodsvt Exp $
 * |*-------------------------------------------------------------------------*|
 * | Copyright (C) 2009-2010 Virginia Tech | | This file is part of the
 * Student-Library. | | The Student-Library is free software; you can
 * redistribute it and/or | modify it under the terms of the GNU Lesser General
 * Public License as | published by the Free Software Foundation; either version
 * 3 of the | License, or (at your option) any later version. | | The
 * Student-Library is distributed in the hope that it will be useful, | but
 * WITHOUT ANY WARRANTY; without even the implied warranty of | MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the | GNU Lesser General Public
 * License for more details. | | You should have received a copy of the GNU
 * Lesser General Public License | along with the Student-Library; if not, see
 * <http://www.gnu.org/licenses/>.
 * \*==========================================================================
 */

package student.web.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.JVM;

import student.web.internal.PersistentStorageManager.StoredObject;


// -------------------------------------------------------------------------
/**
 * Defines the support methods needed to implement server-specific storage
 * features for an {@link student.web.Application} that is running as a plain
 * old Java program, rather than in the context of a web server application.
 * 
 * @author Stephen Edwards
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class LocalApplicationSupportStrategy
                implements
                ApplicationSupportStrategy
{
    // ~ Instance/static variables .............................................
    private static ThreadLocal<Map<String, Object>> localSessionMap = new ThreadLocal<Map<String, Object>>()
    {
        public Map<String, Object> initialValue()
        {
            return null;
        }
    };

    private Map<String, Object> sessionValues;

    private String path = "index.zhtml";


    // ~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Create a new object.
     */
    public LocalApplicationSupportStrategy()
    {
        if ( localSessionMap.get() == null )
        {
            sessionValues = new HashMap<String, Object>();
            localSessionMap.set( sessionValues );
        }
        else
        {
            sessionValues = localSessionMap.get();
        }
    }


    // ~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     * Get a parameter passed to this page in the query part of the URL.
     * 
     * @param name
     *            The name of the parameter to retrieve
     * @return The parameter's value on the current page, or null if there is
     *         none.
     */
    public String getPageParameter( String name )
    {
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Get a parameter stored in the current session.
     * 
     * @param name
     *            The name of the parameter to retrieve
     * @return The parameter's value in the current session, or null if there is
     *         none.
     */
    public Object getSessionParameter( String name )
    {
        return sessionValues.get( name );
    }


    // ----------------------------------------------------------
    /**
     * Remove a parameter stored in the current session, if it exists.
     * 
     * @param name
     *            The name of the parameter to remove
     * @return The removed value, if the parameter existed, or null if there is
     *         no value to remove.
     */
    public Object removeSessionParameter( String name )
    {
        return sessionValues.remove( name );
    }


    // ----------------------------------------------------------
    /**
     * Store a value in the current session. If a value already exists for the
     * given name, it is replaced.
     * 
     * @param name
     *            The name of the parameter to store
     * @param value
     *            The value to store
     * @return The previous value in the current session associated with the
     *         given name, if there is one, or null otherwise.
     */
    public Object setSessionParameter( String name, Object value )
    {
        return sessionValues.put( name, value );
    }


    // ----------------------------------------------------------
    /**
     * Cause the web application to show a different web page in the user's web
     * browser.
     * 
     * @param url
     *            The new web page to show in the user's browser
     */
    public void showWebPage( String url )
    {
        if ( url != null && url.length() > 0 )
        {
            System.out.println( "web application switches to page: " + url );
            path = url;
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the name of the current ZHTML file, such as "index.zhtml" or
     * "lab02.zhtml".
     * 
     * @return The name of the current ZHTML file, without any directory
     *         component, or "" if there is none.
     */
    public String getCurrentPageName()
    {
        String result = getCurrentPagePath();
        int pos = result.lastIndexOf( '/' );
        if ( pos >= 0 )
        {
            result = result.substring( 0, pos );
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the relative path name of the current ZHTML file, such as
     * "/Fall09/mypid/index.zhtml" or "/Fall09/mypid/lab02/lab02.zhtml".
     * 
     * @return The name path to the current ZHTML file, or "" if there is none.
     */
    public String getCurrentPagePath()
    {
        return path;
    }


    public Map<String, StoredObject> getPersistentCache( String cacheId )
    {
        Object cache = this.getSessionParameter(cacheId);
        if(cache == null)
            return null;
        return (Map<String,StoredObject>)cache;
    }


    public Map<String,StoredObject> initPersistentCache(
        String cacheId )
    {
        Map<String,StoredObject> cache = new HashMap<String,StoredObject>();
        this.setSessionParameter( cacheId, cache );
        return cache;
    }
    public Object resolveAlias(Object name)
    {
        //Not supported
        return null;
    }


    public Object getAlias( Object value )
    {
        //Not supported
        return null;
    }
    public ReflectionProvider getReflectionProvider()
    {
        //Nothing special needed, let xstream pick the best one.
        return (new JVM()).bestReflectionProvider();
    }


    public InputStream getObjectSource( File src )
    {
        if(src.exists())
        {
            try
            {
                return new FileInputStream(src);
            }
            catch ( FileNotFoundException e )
            {
                //Just return null so that we mark the file as to not be loaded
            }
        }
        return null;
    }


    public OutputStream getObjectOutput( File dest )
    {
        try
        {
            return new FileOutputStream(dest);
        }
        catch ( FileNotFoundException e )
        {
            return null;
        }
    }

    public File getPersistentFile( String subDir )
    {
        return new File(subDir);
    }
    public File getPersistentFile( File baseDir, String subDir )
    {
        return new File(baseDir, subDir);
    }


    public File getPersistentBase()
    {
        return new File("data");
    }

    private static final String _SERVER_SESSION_MAP = "_server_session_map";
    public Map<String, Object> getSessionPersistentMap()
    {
        Map<String,Object> self = (Map<String, Object>)getSessionParameter( _SERVER_SESSION_MAP );
        if ( self == null )
        {
            self = new HashMap<String, Object>();
            setSessionParameter( _SERVER_SESSION_MAP, self );
        }
        return self;
    }
}
