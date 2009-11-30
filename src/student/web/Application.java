package student.web;

// Critical
// TODO: add support for pumping changed values back into objects
// TODO: build a PersistentMap that can be used as a simple file database

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.sf.webcat.ReflectionSupport;
import net.sf.webcat.SystemIOUtilities;
import student.web.internal.ApplicationSupportStrategy;
import student.web.internal.LocalApplicationSupportStrategy;
import student.web.internal.MRUMap;
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
 *  @version 2009.09.11
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
    private Map<String, CachedObject> context =
        new HashMap<String, CachedObject>();
    private ObjectFieldExtractor extractor = new ObjectFieldExtractor();
    private ClassLoader mostRecent = this.getClass().getClassLoader();


    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new Application.
     * @param identifier A unique name identifying this application.  No other
     *        applications on the web server should use this same identifier.
     *        Identifiers cannot be null or empty.
     */
    public Application(String identifier)
    {
        assert identifier != null : "The application identifier cannot be null";
        assert identifier.length() > 0 :
            "The application identifier cannot be an empty string";
//        System.out.println("Creating application " + identifier + ": " + this);
        id = identifier;
        if (SystemIOUtilities.isOnServer())
        {
            support = (ApplicationSupportStrategy)ReflectionSupport.create(
                "student.web.internal.ServerApplicationSupportStrategy");
        }
        else
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
     *  @param userType The class used for user account objects
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
     *  Get this application's identifier.  If this method is overridden
     *  in a subclass, do not forget to call <code>super.logout()</code>!
     *  @return This application's identifier
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
     *  @param objectId The object's unique identifier.  Identifiers must
     *                  be unique across all web applications on this server.
     *                  Identifiers cannot be null or empty.
     *  @param objectType The class to which the object belongs.  For example,
     *                  if the object is a string, then use
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
     *  Retrieve an object that is available only to this web application.
     *  Note that if you retrieve an object and then
     *  make changes to it, you <b>must</b> use
     *  {@link #setApplicationObject(String, Object)} to store the modified
     *  object back, or the changes will be lost.
     *  @param objectId The object's unique identifier.  Identifiers must
     *                  be unique within this web application.
     *                  Identifiers cannot be null or empty.
     *  @param objectType The class to which the object belongs.  For example,
     *                  if the object is a string, then use
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
     *  @param objectId The object's unique identifier.  Identifiers must
     *                  be unique within this web application.
     *                  Identifiers cannot be null or empty.
     *  @param objectType The class to which the object belongs.  For example,
     *                  if the object is a string, then use
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
        CachedObject latest = context.get(objectId);
        if (latest != null)
        {
            result = returnAsType(objectType, latest.clientValue);
//            System.out.println("  found cached value");
        }
        else
        {
            mostRecent = objectType.getClassLoader();
            MRUMap.ValueWithTimestamp<Map<String, Object>> fieldSet =
                PersistentStorageManager.getInstance().getFieldSet(
                    objectId, mostRecent);
            if (fieldSet != null)
            {
                result =
                    extractor.fieldMapToObject(objectType, fieldSet.value);
                latest = new CachedObject(fieldSet, result);
                context.put(objectId, latest);
            }
//            System.out.println("  loaded from persistent storage: fields =\n\t"
//                + fieldSet.value);
        }
//        System.out.println("  result = " + result);
        return result;
    }


    // ----------------------------------------------------------
    private <ObjectType> void setPersistentObject(
        String objectId, ObjectType object)
    {
        CachedObject latest = context.get(objectId);
        Map<String, Object> fields = extractor.objectToFieldMap(object);
        if (latest != null)
        {
            Map<String, Object> diffs =
                extractor.difference(latest.vwt.value, fields);
            PersistentStorageManager.getInstance().storeChangedFields(
                objectId, diffs, object.getClass().getClassLoader());
        }
        else
        {
            PersistentStorageManager psm =
                PersistentStorageManager.getInstance();
            synchronized (psm)
            {
                psm.storeChangedFields(
                    objectId, fields, object.getClass().getClassLoader());
                latest = new CachedObject(psm.getFieldSet(
                    objectId, object.getClass().getClassLoader()), object);
                context.put(objectId, latest);
            }
        }
        latest.vwt.value = fields;
        latest.clientValue = object;
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
        ObjectType result = object;
        CachedObject latest = context.get(objectId);
        if (latest == null)
        {
            mostRecent = object.getClass().getClassLoader();
            return (ObjectType)getPersistentObject(objectId, object.getClass());
        }
        else
        {
            if (result == null)
            {
                result = (ObjectType)latest.clientValue;
            }
            mostRecent = object.getClass().getClassLoader();
            MRUMap.ValueWithTimestamp<Map<String, Object>> fieldSet =
                PersistentStorageManager.getInstance().getFieldSet(
                    objectId, mostRecent);
            if (fieldSet != null)
            {
                latest.vwt = fieldSet;
            }
            extractor.restoreObjectFromFieldMap(
                latest.clientValue, fieldSet.value);
            if (latest.clientValue != result)
            {
                extractor.restoreObjectFromFieldMap(result, fieldSet.value);
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    private static class CachedObject
    {
        public MRUMap.ValueWithTimestamp<Map<String, Object>> vwt;
        public Object clientValue;

        public CachedObject(
            MRUMap.ValueWithTimestamp<Map<String, Object>> vwt,
            Object clientValue)
        {
            this.vwt = vwt;
            this.clientValue = clientValue;
        }
    }
}
