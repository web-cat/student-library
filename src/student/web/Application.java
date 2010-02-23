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

package student.web;

// Critical
// TODO: add support for pumping changed values back into objects
// TODO: build a PersistentMap that can be used as a simple file database

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import student.testingsupport.SystemIOUtilities;
import student.web.internal.ApplicationSupportStrategy;
import student.web.internal.LocalApplicationSupportStrategy;
import student.web.internal.ObjectFieldExtractor;
import student.web.internal.PersistentStorageManager;

//-------------------------------------------------------------------------
/**
 *  An abstract base class that represents a web application that uses
 *  a zhtml interface.  To use this class, create a concrete subclass that,
 *  at a minimum, defines its own unique application identifier and
 *  defines its own {@link #login(String, String)} method.
 *  <p>
 *  When users interact with an instance of this class, they go through
 *  the following series of steps over time:
 *  </p>
 *  <ul>
 *  <li><p>The user will login, with user name and password (this begins a
 *  session).
 *  This should result in a call to {@link #login(String, String)}, which
 *  must be defined in a subclass.  As part of this method, the subclass
 *  should validate the user name and password, and normally will look up
 *  some kind of user account object.  The subclass <code>login()</code>
 *  method should then call {@link #setCurrentUser(Object)}.</p></li>
 *  <li><p>The user will interact with multiple pages of the application,
 *  performing whatever tasks they want.  During this time,
 *  {@link #hasCurrentUser()} returns true.  Any object values stored
 *  with the user's session will be available until the user logs out.
 *  The application can use {@link #getCurrentUser(Class)} to retrieve the
 *  current user.</p></li>
 *  <li>The user will logout  (this ends a session).  This should result in
 *  a call to {@link #logout()}, which will terminate the user's session.
 *  Afterward, {@link #hasCurrentUser()} will return false and
 *  {@link #getCurrentUser(Class)} will return null.</p></li>
 *  </ul>
 *  <p>
 *  This Application base class provides a mechanism for applications to
 *  store information persistently.  Three separate levels of storage
 *  are provided.</p>
 *  <p>First, session-level values can be stored while a user
 *  is logged in.  These values are only available to pages of the
 *  current application, and only for the current user's session.  They
 *  will disappear once the user logs out.</p>
 *  <p>Second, application-level values can be stored.  These values are
 *  persistent, and can be recalled at any time by the application, regardless
 *  of which user is logged in (or even if no user is logged in).  This is
 *  the place to store information that is only used by this specific web
 *  application, and no others.</p>
 *  <p>Third, shared values can be stored.  These values are persistent, and
 *  can be recalled at any time by any web application running on the same
 *  web server, regardless of which user is logged in, or which application
 *  was used to store the information.  This is the place to store information
 *  that should be available to all web applications.</p>
 *  <p>For all three levels of storage, applications can store any
 *  user-provided data.  Storing a value involves calling the appropriate
 *  setter for the level and providing a unique identifier.  For example,
 *  if you were storing a Department object named "ISE":</p>
 *  <pre>
 *  myApp.setApplicationObject(myDepartment.name(), myDepartment);
 *  </pre>
 *  <p>To retrieve the value, call the corresponding getter and provide
 *  the same unique identifier.  Since any kind of value can be stored, you
 *  must also tell the getter what kind of value you are retrieving (what
 *  Class it belongs to):</p>
 *  <pre>
 *  myApp.getApplicationObject("ISE", Department.class);
 *  </pre>
 *  <p>The second parameter is always the class of the object you intend
 *  to retrieve, with ".class" added on the end.</p>
 *  <p>This same pattern is used in several other places in the Application
 *  class.  Whenever the subclass has a choice of how to represent information,
 *  the corresponding getter will take an extra parameter used to specify
 *  which Class the resulting value should belong to.</p>
 *
 *  @author  Stephen Edwards
 *  @author Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public abstract class Application
{
    //~ Instance/static variables .............................................

    private static final String APP_STORE = "app-store";
    private static final String USER = "app-user";
    private static final String SEPARATOR = "-.-";

    private String id;
    private ApplicationSupportStrategy support;
    private Map<String, Object> sessionValues;
    private Map<String, PersistentStorageManager.StoredObject> context =
        new HashMap<String, PersistentStorageManager.StoredObject>();
    private ClassLoader mostRecent = this.getClass().getClassLoader();
    private Set<String> rawIdSet = null;
    private Set<String> thisAppIdSet = null;
    private Set<String> sharedIdSet = null;
    private long idSetTimestamp = 0L;
    private ObjectFieldExtractor extractor = new ObjectFieldExtractor();


    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new Application.
     * @param identifier A unique name identifying this application.  No other
     *        applications on the web server should use this same identifier.
     *        Identifiers cannot be null or empty.
     */
    @SuppressWarnings("unchecked")
    public Application(String identifier)
    {
        assert identifier != null : "The application identifier cannot be null";
        assert identifier.length() > 0 :
            "The application identifier cannot be an empty string";
//        System.out.println("Creating application " + identifier + ": " + this);
        id = identifier;
        if (SystemIOUtilities.isOnServer())
        {
            try
            {
                Class<?> strategyClass = Class.forName(
                    "student.web.internal.ServerApplicationSupportStrategy");
                support = (ApplicationSupportStrategy)
                    strategyClass.newInstance();
            }
            catch (Exception e)
            {
                System.out.println(
                    "Error initializing application support strategy");
                e.printStackTrace();
            }
        }
        if (support == null)
        {
            support = new LocalApplicationSupportStrategy();
        }
        Object appStore =
            support.getSessionParameter(id + SEPARATOR + APP_STORE);
        if (appStore == null)
        {
            // Use a synchronized map, since multiple concurrent requests
            // might be made involving the same session (say, from one
            // user's multiple browser tabs).
            sessionValues =
                Collections.synchronizedMap(new HashMap<String, Object>());
            support.setSessionParameter(
                id + SEPARATOR + APP_STORE, sessionValues);
//            System.out.println("  creating new session store");
        }
        else
        {
            sessionValues = (Map<String, Object>)appStore;
//            System.out.println(
//                "  reusing existing session store: " + sessionValues);
        }
    }


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     *  Get this application's identifier.
     *  @return This application's identifier
     */
    public String getIdentifier()
    {
        return id;
    }


    // ----------------------------------------------------------
    /**
     *  Login to this web application.  This method must be implemented
     *  in a subclass, and should call {@link #setCurrentUser(Object)} if
     *  it succeeds.  All account existence checking and password
     *  validation must be performed in the subclass implementation of this
     *  method.
     *  @param userName The user who is logging in.  The user name
     *                  cannot be null or empty.
     *  @param password The password entered by the user, which must
     *                  be checked by any implementation of this method.
     *  @return True if login was successful,
     */
    public abstract boolean login(String userName, String password);


    // ----------------------------------------------------------
    /**
     *  Set the current user for this web application.  This method <b>does
     *  not do any security checking</b>--it simply stores the provided object
     *  as the "current user".  It is intended to be used by subclasses inside
     *  an implementation of {@link #login(String, String)}.  Such a subclass
     *  should provide its own security checks in its <code>login()</code>
     *  method before calling <code>setCurrentUser()</code>.
     *  <p>
     *  [This method should be protected instead of public, since only
     *  subclasses should use it.]
     *  </p>
     *  @param userAccount An application-specific object that represents
     *         the current user's account.  This base class makes no
     *         assumptions about the user account object.  Instead, an
     *         appropriate concrete subclass of Application must provide
     *         any meaningful interaction with the user account object.
     */
    public void setCurrentUser(Object userAccount)
    {
        if (user() != null && !user().equals(userAccount))
        {
            logout();
        }
        if (userAccount != null)
        {
            setUser(userAccount);
        }
    }


    // ----------------------------------------------------------
    /**
     *  Retrieve the current user, which was passed into
     *  {@link #setCurrentUser(Object)}.
     *  @param <UserType>  This method is a template method, and the type
     *                     <code>UserType</code> used for the return value is
     *                     implicitly deduced from the provided argument
     *                     <code>userType</code>.
     *  @param userType    The type (class) of the current user object, and
     *                     also the way you specify the return type of this
     *                     method.
     *  @return The object representing the current user, or null if the
     *          user has not logged in.
     */
    public <UserType> UserType getCurrentUser(Class<UserType> userType)
    {
        return returnAsType(userType, user());
    }


    // ----------------------------------------------------------
    /**
     *  Determine if there is a user currently logged in.  Use
     *  {@link #login(String, String)} to login.
     *  @return True if the user has logged in, and false otherwise.
     */
    public boolean hasCurrentUser()
    {
        return user() != null;
    }


    // ----------------------------------------------------------
    /**
     *  End a user session.  This method clears all session objects,
     *  including the current user.  If this method is overridden
     *  in a subclass, do not forget to call <code>super.logout()</code>!
     */
    public void logout()
    {
        support.removeSessionParameter(USER);
        sessionValues.clear();
        context.clear();
    }


    // ----------------------------------------------------------
    /**
     *  Retrieve an object that is shared among all the applications on
     *  this web server.  Note that if you retrieve an object and then
     *  make changes to it, you <b>must</b> use
     *  {@link #setSharedObject(String, Object)} to store the modified
     *  object back, or the changes will be lost.
     *  @param <ObjectType>  This method is a template method, and the type
     *                  <code>ObjectType</code> used for the return value is
     *                  implicitly deduced from the provided argument
     *                  <code>objectType</code>.
     *  @param objectId The object's unique identifier.  Identifiers must
     *                  be unique across all web applications on this server.
     *                  Identifiers cannot be null or empty.
     *  @param objectType  The type (class) of the object that is being
     *                  retrieved, and also the way you specify the return
     *                  type of this method.  For example,
     *                  if the object you are retrieving is a string, then use
     *                  <code>String.class</code> for this parameter.  If the
     *                  object being requested in instead a user profile
     *                  object, use <code>UserProfile.class</code> as the
     *                  value for this parameter instead.
     *  @return The requested object, or null if none is found.  Unexpected
     *          results may occur if the specified <code>ObjectType</code>
     *          is incorrect.
     */
    public <ObjectType> ObjectType getSharedObject(
        String objectId, Class<ObjectType> objectType)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        return getPersistentObject(objectId, objectType);
    }


    // ----------------------------------------------------------
    /**
     *  Store an object that is shared among all the applications on
     *  this web server.  Once stored, objects can then be retrieved by
     *  any applications hosted on the same web server.
     *  @param <ObjectType>  This method is a template method, and the type
     *                  <code>ObjectType</code> used for the second
     *                  parameter is implicitly deduced from the provided
     *                  argument <code>object</code>.
     *  @param objectId The object's unique identifier.  Identifiers must
     *                  be unique across all web applications on this server.
     *                  Identifiers cannot be null or empty.
     *  @param object   The object to store.
     */
    public <ObjectType> void setSharedObject(
        String objectId, ObjectType object)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        assert !(object instanceof Class) :
            "The object to store cannot be a class; perhaps you wanted "
            + "to provide an instance of this class instead?";
        setPersistentObject(objectId, object);
    }


    // ----------------------------------------------------------
    /**
     *  Find out if an object is stored for a given identifier value.
     *  @param objectId The object's unique identifier.
     *  @return True if a shared object is stored using the given identifier,
     *  or false otherwise.
     */
    public boolean hasSharedObject(String objectId)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        return hasPersistentObject(objectId);
    }


    // ----------------------------------------------------------
    /**
     *  Remove (or erase) a shared object if it is present, so that it is no
     *  longer accessible to any web applications.
     *  @param objectId The object's unique identifier.
     */
    public void removeSharedObject(String objectId)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        removePersistentObject(objectId);
    }


    // ----------------------------------------------------------
    /**
     *  Just like {@link #getSharedObject(String, Class)}, except that
     *  it forces the object to be reloaded from persistent storage.
     *  @param <ObjectType>  This method is a template method, and the type
     *                  <code>ObjectType</code> used for the return value is
     *                  implicitly deduced from the provided argument
     *                  <code>object</code>.
     *  @param objectId The object's unique identifier.  Identifiers must
     *                  be unique across all web applications on this server.
     *                  Identifiers cannot be null or empty.
     *  @param object   The object to reload, previously retrieved using
     *                  {@link #getSharedObject(String, Class)}.
     *  @return The object passed in, after it has been reloaded.
     */
    public <ObjectType> ObjectType reloadSharedObject(
        String objectId, ObjectType object)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        assert !(object instanceof Class) :
            "The object to reload cannot be a class; perhaps you wanted "
            + "to provide an instance of this class instead?";
        return reloadPersistentObject(objectId, object);
    }


    // ----------------------------------------------------------
    /**
     * Get a set of all used object ids.  The set may be quite large,
     * and may take some time to produce.
     * @return A set of all ids for which shared objects are stored.
     */
    public Set<String> getAllSharedObjectIds()
    {
        ensureIdSetsAreCurrent();
        return sharedIdSet;
    }


    // ----------------------------------------------------------
    /**
     *  Retrieve an object that is available only to this web application.
     *  Note that if you retrieve an object and then
     *  make changes to it, you <b>must</b> use
     *  {@link #setApplicationObject(String, Object)} to store the modified
     *  object back, or the changes will be lost.
     *  @param <ObjectType>  This method is a template method, and the type
     *                  <code>ObjectType</code> used for the return value is
     *                  implicitly deduced from the provided argument
     *                  <code>objectType</code>.
     *  @param objectId The object's unique identifier.  Identifiers must
     *                  be unique within this web application.
     *                  Identifiers cannot be null or empty.
     *  @param objectType  The type (class) of the object that is being
     *                  retrieved, and also the way you specify the return
     *                  type of this method.  For example,
     *                  if the object you are retrieving is a string, then use
     *                  <code>String.class</code> for this parameter.  If the
     *                  object being requested in instead a user profile
     *                  object, use <code>UserProfile.class</code> as the
     *                  value for this parameter instead.
     *  @return The requested object, or null if none is found.  Unexpected
     *          results may occur if the specified <code>ObjectType</code>
     *          is incorrect.
     */
    public <ObjectType> ObjectType getApplicationObject(
        String objectId, Class<ObjectType> objectType)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        return getPersistentObject(id + SEPARATOR + objectId, objectType);
    }


    // ----------------------------------------------------------
    /**
     *  Store an object that will be available only to this application.
     *  Once stored, objects can then be retrieved by any pages belonging
     *  to this web application.
     *  @param <ObjectType>  This method is a template method, and the type
     *                  <code>ObjectType</code> used for the second
     *                  parameter is implicitly deduced from the provided
     *                  argument <code>object</code>.
     *  @param objectId The object's unique identifier.  Identifiers must
     *                  be unique within this web application.
     *                  Identifiers cannot be null or empty.
     *  @param object   The object to store.
     */
    public <ObjectType> void setApplicationObject(
        String objectId, ObjectType object)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        assert !(object instanceof Class) :
            "The object to store cannot be a class; perhaps you wanted "
            + "to provide an instance of this class instead?";
        setPersistentObject(id + SEPARATOR + objectId, object);
    }


    // ----------------------------------------------------------
    /**
     *  Find out if an object is stored for a given identifier value.
     *  @param objectId The object's unique identifier.
     *  @return True if an object is stored for this web application using
     *  the given identifier, or false otherwise.
     */
    public boolean hasApplicationObject(String objectId)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        return hasPersistentObject(id + SEPARATOR + objectId);
    }


    // ----------------------------------------------------------
    /**
     *  Remove (or erase) an object if it is present, so that it is no longer
     *  accessible to this web application.
     *  @param objectId The object's unique identifier.
     */
    public void removeApplicationObject(String objectId)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        removePersistentObject(id + SEPARATOR + objectId);
    }


    // ----------------------------------------------------------
    /**
     *  Just like {@link #getApplicationObject(String, Class)}, except that
     *  it forces the object to be reloaded from persistent storage.
     *  @param <ObjectType>  This method is a template method, and the type
     *                  <code>ObjectType</code> used for the return value is
     *                  implicitly deduced from the provided argument
     *                  <code>object</code>.
     *  @param objectId The object's unique identifier.  Identifiers must
     *                  be unique within this web application.
     *                  Identifiers cannot be null or empty.
     *  @param object   The object to reload, previously retrieved using
     *                  {@link #getApplicationObject(String, Class)}.
     *  @return The object passed in, after it has been reloaded.
     */
    public <ObjectType> ObjectType reloadApplicationObject(
        String objectId, ObjectType object)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        assert !(object instanceof Class) :
            "The object to reload cannot be a class; perhaps you wanted "
            + "to provide an instance of this class instead?";
        return reloadApplicationObject(id + SEPARATOR + objectId, object);
    }


    // ----------------------------------------------------------
    /**
     * Get a set of all used application-specific object ids.  The set may
     * take some time to produce.
     * @return A set of all ids for which application-specific objects are
     * stored.
     */
    public Set<String> getAllApplicationObjectIds()
    {
        ensureIdSetsAreCurrent();
        return thisAppIdSet;
    }


    // ----------------------------------------------------------
    /**
     *  Retrieve an object that is available only within the current
     *  user's session.  A "session" encompasses one user's interactions with
     *  the various pages of this web application, roughly from when they
     *  login until they logout--or until they are automatically logged out
     *  for not interacting with the application for a sufficiently long period
     *  of time.  Separate users have distinct sessions, so objects saved
     *  with one user's session are only available to that user, and only
     *  until they logout.
     *  <p>
     *  Note that if you retrieve an object and then
     *  make changes to it, you <b>must</b> use
     *  {@link #setSessionObject(String, Object)} to store the modified
     *  object back, or the changes will be lost.
     *  </p>
     *  @param <ObjectType>  This method is a template method, and the type
     *                  <code>ObjectType</code> used for the return value is
     *                  implicitly deduced from the provided argument
     *                  <code>objectType</code>.
     *  @param objectId The object's unique identifier.  Identifiers must
     *                  be unique within this web application.
     *                  Identifiers cannot be null or empty.
     *  @param objectType  The type (class) of the object that is being
     *                  retrieved, and also the way you specify the return
     *                  type of this method.  For example,
     *                  if the object you are retrieving is a string, then use
     *                  <code>String.class</code> for this parameter.  If the
     *                  object being requested in instead a user profile
     *                  object, use <code>UserProfile.class</code> as the
     *                  value for this parameter instead.
     *  @return The requested object, or null if none is found.  Unexpected
     *          results may occur if the specified <code>ObjectType</code>
     *          is incorrect.
     */
    public <ObjectType> ObjectType getSessionObject(
        String objectId, Class<ObjectType> objectType)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        Object result = sessionValues.get(objectId);
        if (result != null)
        {
            // If a student has multiple versions of the same app for
            // earlier/later programming assignments, they likely have
            // the same id.  However, they almost certainly live in
            // different class loaders.  If the user has session data
            // from an older login session from one version of an app,
            // and then visits a page from another version of the same
            // app that uses a different class loader, we need to
            // detect the fault and silently reset the session.
            if (!objectType.isAssignableFrom(result.getClass()))
            {
                // There was a type mismatch
                if (objectType.getName().equals(result.getClass().getName()))
                {
                    // Same type names, but not compatible!
                    // reset the session data and return null instead
                    result = null;
                    sessionValues.clear();
                }
            }
        }
        return returnAsType(objectType, result);
    }


    // ----------------------------------------------------------
    /**
     *  Store an object that will be available only to the current user in
     *  their current session.  Once stored, objects can then be retrieved
     *  by any pages belonging to this web application that the current
     *  user visits before they logout.
     *  @param <ObjectType>  This method is a template method, and the type
     *                  <code>ObjectType</code> used for the second
     *                  parameter is implicitly deduced from the provided
     *                  argument <code>object</code>.
     *  @param objectId The object's unique identifier.  Identifiers must
     *                  be unique within this web application.
     *                  Identifiers cannot be null and empty.
     *  @param object   The object to store.
     */
    public <ObjectType> void setSessionObject(
        String objectId, ObjectType object)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        assert !(object instanceof Class) :
            "The object to store cannot be a class; perhaps you wanted "
            + "to provide an instance of this class instead?";
        sessionValues.put(objectId, object);
        support.setSessionParameter(
            id + SEPARATOR + APP_STORE, sessionValues);
    }


    // ----------------------------------------------------------
    /**
     *  Find out if an object is stored for a given identifier value.
     *  @param objectId The object's unique identifier.
     *  @return True if an object is stored for the current user's session
     *  using the given identifier, or false otherwise.
     */
    public boolean hasSessionObject(String objectId)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        return sessionValues.containsKey(objectId);
    }


    // ----------------------------------------------------------
    /**
     *  Remove (or erase) an object if it is present, so that it is no longer
     *  accessible to this web application within the current user's session.
     *  @param objectId The object's unique identifier.
     */
    public void removeSessionObject(String objectId)
    {
        assert objectId != null : "An objectId cannot be null";
        assert objectId.length() > 0 :
            "An objectId cannot be an empty string";
        sessionValues.remove(objectId);
        support.setSessionParameter(
            id + SEPARATOR + APP_STORE, sessionValues);
    }


    // ----------------------------------------------------------
    /**
     * Get a set of all used session object ids.
     * @return A set of all ids for which session-specific objects are
     * stored.
     */
    public Set<String> getSessionObjectIds()
    {
        return sessionValues.keySet();
    }


    // ----------------------------------------------------------
    /**
     *  Cause the web application to show a different web page in the
     *  user's web browser.
     *  @param url The new web page to show in the user's browser
     */
    public void showWebPage(String url)
    {
        support.showWebPage(url);
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
        return support.getCurrentPageName();
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
        return support.getCurrentPagePath();
    }


    // ----------------------------------------------------------
    /**
     *  Get a parameter passed to this page in the query part of the URL.
     *  @param name The name of the parameter to retrieve
     *  @return The parameter's value on the current page, or null if
     *  there is none.
     */
    public String getPageParameter(String name)
    {
        return support.getPageParameter(name);
    }


    //~ Private Methods .......................................................

    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    private <T> T returnAsType(Class<T> t, Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof TreeMap
            && !TreeMap.class.isAssignableFrom(t))
        {
            value = extractor.fieldMapToObject(t, (Map<String, Object>)value);
        }

        assert t.isAssignableFrom(value.getClass())
            : "Cannot return object \"" + value + "\" of type "
              + value.getClass() + " when a " + t + " value is requested";
        return (T)value;
    }


    // ----------------------------------------------------------
    private Object user()
    {
        return sessionValues.get(USER);
    }


    // ----------------------------------------------------------
    private void setUser(Object user)
    {
        sessionValues.put(USER, user);
        support.setSessionParameter(
            id + SEPARATOR + APP_STORE, sessionValues);
    }


    // ----------------------------------------------------------
    private <ObjectType> ObjectType getPersistentObject(
        String objectId, Class<ObjectType> objectType)
    {
        ObjectType result = null;
//        System.out.println("looking up " + objectId);
        PersistentStorageManager.StoredObject latest = context.get(objectId);
        if (latest == null)
        {
            mostRecent = objectType.getClassLoader();
            latest = PersistentStorageManager.getInstance()
                .getPersistentObject(objectId, mostRecent);
//          System.out.println("  loaded from persistent storage: fields =\n\t"
//          + fieldSet.value);
            context.put(objectId, latest);
        }
//        System.out.println("  result = " + result);
        if (latest != null)
        {
            result = returnAsType(objectType, latest.value());
            if (result != latest.value())
            {
                latest.setValue(result);
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    private <ObjectType> void setPersistentObject(
        String objectId, ObjectType object)
    {
        PersistentStorageManager.StoredObject latest = context.get(objectId);
        if (latest != null)
        {
            latest.setValue(object);
            PersistentStorageManager.getInstance()
                .storePersistentObjectChanges(
                    objectId,
                    latest,
                    object.getClass().getClassLoader());
        }
        else
        {
            latest = PersistentStorageManager.getInstance()
                .storePersistentObject(objectId, object);
            context.put(objectId, latest);
        }
    }


    // ----------------------------------------------------------
    private boolean hasPersistentObject(String objectId)
    {
        return PersistentStorageManager.getInstance().hasFieldSetFor(
            objectId, mostRecent);
    }


    // ----------------------------------------------------------
    private void removePersistentObject(String objectId)
    {
        PersistentStorageManager.getInstance().removeFieldSet(
            objectId);
    }


    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    private <ObjectType> ObjectType reloadPersistentObject(
        String objectId, ObjectType object)
    {
        if (false)
        {
            context.remove(objectId);
            return (ObjectType)getPersistentObject(objectId, object.getClass());
        }

        ObjectType result = object;
        PersistentStorageManager.StoredObject latest = context.get(objectId);
        if (latest == null)
        {
            result = (ObjectType)getPersistentObject(
                objectId, object.getClass());
        }
        else
        {
            // Remove from cache
            context.remove(objectId);
            // Reload
            getPersistentObject(objectId, object.getClass());
            // grab newer copy
            PersistentStorageManager.StoredObject newest =
                context.get(objectId);
            Object original = latest.value();
            Map<String, Object> fields =
                extractor.objectToFieldMap(newest.value());

            // Reload the existing objects
            extractor.restoreObjectFromFieldMap(object, fields);
            if (object != original)
            {
                extractor.restoreObjectFromFieldMap(original, fields);
            }

            // Now, replace the "new" loaded copy with the freshly reloaded
            // original
            Map<String, Object> snapshot = newest.fieldset().get(newest.value());
            newest.fieldset().remove(newest.value());
            newest.setValue(original);
            newest.fieldset().put(original, snapshot);
        }
        return result;
    }


    // ----------------------------------------------------------
    private void ensureIdSetsAreCurrent()
    {
        PersistentStorageManager PSM = PersistentStorageManager.getInstance();
        if (rawIdSet == null || PSM.idSetHasChangedSince(idSetTimestamp))
        {
            rawIdSet = PSM.getAllIds();
            idSetTimestamp = System.currentTimeMillis();
            String prefix = id + SEPARATOR;
            thisAppIdSet = new HashSet<String>(rawIdSet.size() / 10 + 1);
            sharedIdSet = new HashSet<String>(rawIdSet.size());
            for (String id : rawIdSet)
            {
                if (id.startsWith(prefix))
                {
                    thisAppIdSet.add(id.substring(prefix.length()));
                }
                else if (!id.contains(SEPARATOR))
                {
                    sharedIdSet.add(id);
                }
            }
        }
    }
}
