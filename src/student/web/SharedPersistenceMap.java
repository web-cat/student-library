package student.web;

import student.web.internal.AbstractPersistenceStoreMap;

/**
 * This map accesses the shared persistence layer. This layer is shared between
 * all users running on the same hardware.This layer is identified by the unique
 * application id passed to it's constructor. It is important to note that some
 * of the operations on this map do not respect the generic type passed to it.
 * This means that operations like {@link AbstractPersistenceStoreMap.size}
 * return information based on all of the elements in the Application layer, not
 * just objects of the generic type. In addition, this map generally performs
 * type checking on all method calls to prevent using different key types or
 * value types to access the application persistence layer. <br/><br/>
 * Typical Usage: <br/><br/>
 * {@code SharedPersistenceMap<User> userAppMap = new SharedPersistenceMap<User>(UNIQUE_ID, User.class);}
 * <br/><br/>
 * Where User is the class being persisted and retrieved.
 * 
 * This map uses special logic to recreate objects that have been stored in the
 * persistence layer. The persistence layer will initially attempt to retrieve
 * the stored object from the shared persistence layer. The persistence layer
 * will then attempt to create a new object and fill it with the data in the
 * persistence store. This means by default, no constructors are called on the
 * class. If every field in the class definition is present in the persistence
 * layer then the recreated class is returned. If there are fields in the
 * generic type that are not present in the persisted object, then the
 * persistence layer will attempt to create an instance of the object using the
 * default constructor. After creating the object with the default constructor
 * it will populate the fields within the object. This new "special" object will
 * then be returned to the user.
 * 
 * @author mjw87
 * 
 * @param <T>
 */
public class SharedPersistenceMap<T> extends AbstractPersistenceStoreMap<T> {
	private static final String SHARED = "shared";

	public SharedPersistenceMap(Class<T> genericClass) {
		super(SHARED);
		typeAware = genericClass;
	}
	/**
	 * NOT SUPPORTED
	 */
	@Override
	public void clear()
	{
		throw new UnsupportedOperationException("You cannot clear the shared map.");
	}
	/**
	 * Testing purposes only
	 * 
	 */
	void clearLocal()
	{
		super.clear();
	}

}
