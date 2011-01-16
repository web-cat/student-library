package student.web.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.AbstractMap.SimpleEntry;

import student.web.PersistenceMap;

/**
 * 
 * This is an abstract map representing all of the general functionality for
 * maps that create projections on a particular persistence layer. Note that
 * some of these functions return the results from the entire persistence layer
 * without respect to the generic type of this map. However, some of the
 * operations do respect the generic type of the implementing map. To implement
 * this map, the client only needs to implement a constructor that informs the
 * super class of the generic type (to prevent type erasure) and the directory
 * to treat as the base directory in the persistence store. The directory is non
 * absolute and is based on the configured datastore directory.
 * 
 * @author mjw87
 * 
 * @param <T>
 */
public abstract class AbstractPersistenceStoreMap<T> implements
		PersistenceMap<T> {
	/**
	 * Seperator character for keywords within a persisted object id. This is
	 * used for conversion to a remote map.
	 */
	protected static final String SEPARATOR = "-.-";
	/**
	 * Timestamp of the last time the keyset of the persistence library was
	 * retrieved.
	 */
	private long idSetTimestamp = 0L;
	/**
	 * The class type this map is projecting onto the persistence store.
	 */
	protected Class<T> typeAware;
	/**
	 * The latest obtained keyset from the persistence store.
	 */
	protected HashSet<String> idSet = new HashSet<String>();
	private ReadOnlyHashSet<String> snapshotIds;
	private long snapshotTimestamp = 0L;
	/**
	 * The cached context map for objects retrieved from the store. It is used
	 * to reconsititue objects.
	 */
	private Map<String, PersistentStorageManager.StoredObject> context;
	/**
	 * An extractor object for extracting fields from objects.
	 */
	private ObjectFieldExtractor extractor = new ObjectFieldExtractor();
	/**
	 * The instance of the persistence store this Map is projecting on.
	 */
	private PersistentStorageManager PSM;
	private ApplicationSupportStrategy support;

	@SuppressWarnings("unchecked")
	protected AbstractPersistenceStoreMap(String directoryName) {

		PSM = PersistentStorageManager.getInstance(directoryName);
		support = LocalityService.getSupportStrategy();
		if (support.getSessionParameter("context-object") != null) {
			context = (Map<String, PersistentStorageManager.StoredObject>) support
					.getSessionParameter("context-object");
		} else {
			context = new HashMap<String, PersistentStorageManager.StoredObject>();
			support.setSessionParameter("context-object", context);
		}
	}

	@Override
	public T remove(Object key) {
		assert key instanceof String : "Persistence maps only allows for keys of type String";
		String objectId = (String) key;
		assert key != null : "An key cannot be null";
		assert objectId.length() > 0 : "An key cannot be an empty string";
		T cached = getPersistentObject(objectId, typeAware);
		if (cached != null) {
			removePersistentObject(objectId);
			if (idSet != null) {
				idSet.remove(objectId);
				this.idSetTimestamp = System.currentTimeMillis();
			}
		}
		return cached;

	}

	@Override
	public T put(String key, T value) {
		assert key != null : "An objectId cannot be null";
		assert key.length() > 0 : "An objectId cannot be an empty string";
		assert !(value instanceof Class) : "The object to store cannot "
				+ "be a class; perhaps you wanted "
				+ "to provide an instance of this class instead?";
		T cached = getPersistentObject(key, typeAware);
		setPersistentObject(key, value);
		return cached;
	}

	@Override
	public void putAll(Map<? extends String, ? extends T> externalMap) {
		for (Map.Entry<? extends String, ? extends T> entry : externalMap
				.entrySet()) {
			setPersistentObject(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public T get(Object key) {
		assert key instanceof String : "Persistence maps only allows for keys of type String";
		String objectId = (String) key;
		assert key != null : "An objectId cannot be null";
		assert objectId.length() > 0 : "An objectId cannot be an empty string";
		return getPersistentObject(objectId, typeAware);
	}

	@Override
	public boolean containsKey(Object key) {
		assert key instanceof String : "Persistence maps only allows for keys of type String";
		String objectId = (String) key;
		// TODO: Bug passing loader should be fixed
		return PSM.hasFieldSetFor(objectId, null);
	}

	@Override
	public int size() {

		ensureIdSetsAreCurrent();
		return idSet.size();

	}

	@Override
	public boolean isEmpty() {

		ensureIdSetsAreCurrent();
		return idSet.isEmpty();

	}

	@Override
	public boolean containsValue(Object value) {
		ensureIdSetsAreCurrent();

		for (String id : idSet) {
			T persistedObject = getPersistentObject(id, typeAware);
			// Just incase the store shifted under us
			if (persistedObject != null && persistedObject.equals(value)) {
				return true;
			}
		}
		return false;

	}

	@Override
	public void clear() {

		ensureIdSetsAreCurrent();
		for (String id : idSet) {
			removePersistentObject(id);
		}
		idSet.clear();
		this.idSetTimestamp = System.currentTimeMillis();

	}

	@Override
	public Set<String> keySet() {
		ensureIdSetsAreCurrent();
		// Set<String> ids = new HashSet<String>();
		// ids.addAll(idSet);
		snapshotIdSet();
		return this.snapshotIds;

	}

	@Override
	public Collection<T> values() {
		ensureIdSetsAreCurrent();
		Set<T> valueSet = new HashSet<T>();
		for (String key : idSet) {
			T lookup = getPersistentObject(key, typeAware);
			// Just incase the persistence store moved under us
			if (lookup != null)
				valueSet.add(lookup);
		}
		return valueSet;

	}

	@Override
	public Set<Entry<String, T>> entrySet() {

		ensureIdSetsAreCurrent();
		ReadOnlyHashSet<Entry<String, T>> valueSet = new ReadOnlyHashSet<Entry<String, T>>();

		for (String id : idSet) {
			T lookup = getPersistentObject(id, typeAware);
			// Just incase the persistence store moved under us
			if (lookup != null)
				valueSet.addLocal(new SimpleEntry<String, T>(id, lookup));
		}
		return valueSet;

	}

	// Private Methods.
	private void ensureIdSetsAreCurrent() {
		synchronized (idSet) {
			if (PSM.idSetHasChangedSince(idSetTimestamp)) {

				Set<String> rawIdSet = PSM.getAllIds();
				idSetTimestamp = System.currentTimeMillis();

				idSet = new HashSet<String>(rawIdSet.size() / 10 + 1);
				// sharedIdSet = new HashSet<String>(rawIdSet.size());
				for (String id : rawIdSet) {
					idSet.add(id);

				}
			}
		}

	}

	private void snapshotIdSet() {
		// == catches if the update happens too quick.
		if (snapshotTimestamp <= idSetTimestamp) {
			synchronized (idSet) {
				snapshotIds = new ReadOnlyHashSet<String>(idSet);
				snapshotTimestamp = System.currentTimeMillis();
			}
		}
	}

	protected <ObjectType> ObjectType getPersistentObject(String objectId,
			Class<ObjectType> objectType) {
		ObjectType result = null;
		PersistentStorageManager.StoredObject latest;// = context.get(objectId);
		ClassLoader loader = objectType.getClassLoader();
		// if (latest == null) {
		latest = PSM.getPersistentObject(objectId, loader);
		context.put(objectId, latest);
		if (latest != null) {
			result = returnAsType(objectType, latest.value());
			if (result != latest.value()) {
				latest.setValue(result);
			}
			return result;
		}
		// }
		return null;
		// if (latest == null) {
		// return null;
		// }
		// result = returnAsType(objectType,
		// reloadPersistentObject(objectId, latest,loader).value());
		// return result;
	}

	private <ObjectType> ObjectType reloadPersistentObject(String objectId,
			ObjectType object, ClassLoader loader) {
		ObjectType result = object;
		PersistentStorageManager.StoredObject latest = context.get(objectId);
		// Remove from cache
		context.remove(objectId);
		// Reload
		PersistentStorageManager.StoredObject newest = PSM.getPersistentObject(
				objectId, loader);
		Object original = latest.value();
		Map<String, Object> fields = extractor.objectToFieldMap(newest.value());

		// Reload the existing objects
		extractor.restoreObjectFromFieldMap(object, fields);
		// if (object != original) {
		// extractor.restoreObjectFromFieldMap(original, fields);
		// }

		// Now, replace the "new" loaded copy with the freshly reloaded
		// original
		Map<String, Object> snapshot = newest.fieldset().get(newest.value());
		newest.fieldset().remove(newest.value());
		newest.setValue(original);
		newest.fieldset().put(original, snapshot);

		return result;
	}

	@SuppressWarnings("unchecked")
	private <V> V returnAsType(Class<V> t, Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof TreeMap && !TreeMap.class.isAssignableFrom(t)) {
			value = extractor.fieldMapToObject(t, (Map<String, Object>) value);
		}
		if (t.isAssignableFrom(value.getClass())) {
			return (V) value;
		}

		return null;

		// assert t.isAssignableFrom(value.getClass()) :
		// "Cannot return object \""
		// + value + "\" of type " + value.getClass() + " when a " + t
		// + " value is requested";
		// return (V) value;
	}

	protected <ObjectType> void setPersistentObject(String objectId,
			ObjectType object) {
		try {
			PersistentStorageManager.StoredObject latest = context
					.get(objectId);
			if (latest != null) {
				latest.setValue(object);
				PSM.storePersistentObjectChanges(objectId, latest, object
						.getClass().getClassLoader());
			} else {
				latest = PSM.storePersistentObject(objectId, object);
				context.put(objectId, latest);
			}
		} catch (RuntimeException e) {
			PSM.removeFieldSet(objectId);
			throw e;
		}
		if (idSet != null) {
			idSet.add(objectId);
			this.idSetTimestamp = System.currentTimeMillis();
		}
	}

	protected void removePersistentObject(String objectId) {
		PSM.removeFieldSet(objectId);
	}

}
