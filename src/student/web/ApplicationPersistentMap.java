package student.web;

import java.io.File;
import student.web.internal.AbstractPersistentMap;

//-------------------------------------------------------------------------
/**
 *  This class represents a {@link PersistentMap} whose contents are
 *  <b>shared</b> only by separate executions of a single program.  In
 *  the constructor, you provide a unique "application identifier" as a
 *  string, and the persistent values stored in this map will only be
 *  accessible to other <code>ApplicationPersistentMap</code> instances
 *  that were created with the same application identifier.
 *  <p>
 *  In other respects, an <code>ApplicationPersistentMap</code> works
 *  just like a {@link SharedPersistentMap}.  Read the documentation for
 *  that class, and for the {@link PersistentMap} interface, to learn how
 *  to use this class.
 *  </p>
 *
 *  @param <T> The type of objects to store in the map
 *
 *  @author  Mike Woods
 *  @author Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public class ApplicationPersistentMap<T>
    extends AbstractPersistentMap<T>
{
    //~ Instance/static variables .............................................

	private static final String APP = "app";


    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Create a new map that associates keys with values of the specified
     * class.  Initially, the map will contain all of the key/value
     * associations that have been previously stored in other
     * instances of this class using the same application identifier,.
     * <p>
     * Typical usage: if you want an application persistent map that stores
     * objects of class <code>Widget</code>, then:
     * </p>
     * <pre>
     * ApplicationPersistentMap<Widget> map =
     *     new ApplicationPersistentMap<Widget>(Widget.class);
     * </pre>
     *
     * @param applicationIdentifier A non-null, non-empty string used to
     *                      uniquely identify the application using this
     *                      map.  All instances created with the same
     *                      application identifier will "share" the same set
     *                      of key/value mappings.
     * @param genericClass  The {@link Class} object that represents the
     *                      generic type <code>T</code>, the type of values
     *                      stored in this map.
     */
	public ApplicationPersistentMap(
	    String applicationIdentifier, Class<T> genericClass)
	{
		super(APP + File.separator + checkId(applicationIdentifier));
		typeAware = genericClass;
	}

	private static final String checkId(String applicationIdentifier)
	{
        assert applicationIdentifier != null
            : "The applicationIdentifier passed into the "
            + "ApplicationPersistentMap constructor cannot be null";
        assert applicationIdentifier.length() > 0
            : "The applicationIdentifier passed into the "
            + "ApplicationPersistentMap constructor cannot be an empty string";
        return applicationIdentifier;
	}
}