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
	// private long idSetTimestamp = 0L;
	/**
	 * The class type this map is projecting onto the persistence store.
	 */
	protected Class<T> typeAware;
	/**
	 * The latest obtained keyset from the persistence store.
	 */
	// protected HashSet<String> idSet = new HashSet<String>();
	// private ReadOnlyHashSet<String> snapshotIds;
	// private long snapshotTimestamp = 0L;
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

	public T remove(Object key) {
		assert key instanceof String : "Persistence maps only allows for keys of type String";
		String objectId = (String) key;
		assert key != null : "An key cannot be null";
		assert objectId.length() > 0 : "An key cannot be an empty string";
		T previousValue = getPrevious((String) key);
		removePersistentObject(objectId);
		return previousValue;

	}

	public T put(String key, T value) {
		assert key != null : "An objectId cannot be null";
		assert key.length() > 0 : "An objectId cannot be an empty string";
		assert !(value instanceof Class) : "The object to store cannot "
				+ "be a class; perhaps you wanted "
				+ "to provide an instance of this class instead?";
		T previousValue = getPrevious(key);
		setPersistentObject(key, value);
		return previousValue;
	}

	private T getPrevious(String key) {
		PersistentStorageManager.StoredObject cached = context.get(key);
		T previousValue = null;
		if (cached != null) {
			if (cached.value().getClass().equals(typeAware)) {
				previousValue = (T) cached.value();
			}
		}

		if (cached == null) {
			PersistentStorageManager.StoredObject previous = PSM
					.getPersistentObject(key, typeAware.getClassLoader());
			if (previous != null) {
				if (previous.value().getClass().equals(typeAware)) {
					previousValue = (T) previous.value();
				}
			}
		}
		return previousValue;
	}

	public void putAll(Map<? extends String, ? extends T> externalMap) {
		for (Map.Entry<? extends String, ? extends T> entry : externalMap
				.entrySet()) {
			setPersistentObject(entry.getKey(), entry.getValue());
		}
	}

	public T get(Object key) {
		assert key instanceof String : "Persistence maps only allows for keys of type String";
		String objectId = (String) key;
		assert key != null : "An objectId cannot be null";
		assert objectId.length() > 0 : "An objectId cannot be an empty string";
		T foundObject = getPersistentObject(objectId);
		if (context.get(key) != null
				&& PSM.hasFieldSetChanged((String) key, context.get(key)
						.timestamp())) {
			PSM.refreshPersistentObject((String) key, context.get(key),
					typeAware.getClassLoader());
		}
		return foundObject;
	}

	//
	public boolean containsKey(Object key) {
		assert key instanceof String : "Persistence maps only allows for keys of type String";
		String objectId = (String) key;
		// TODO: Bug passing loader should be fixed
		return PSM.hasFieldSetFor(objectId, null);
	}

	public int size() {
		return PSM.getAllIds().size();
	}

	public boolean isEmpty() {

		return PSM.getAllIds().isEmpty();

	}

	//
	public boolean containsValue(Object value) {
		for (String id : PSM.getAllIds()) {
			T persistedObject = getPersistentObject(id);
			// Just incase the store shifted under us
			if (persistedObject != null && persistedObject.equals(value)) {
				return true;
			}
		}
		return false;

	}

	//
	public void clear() {

		context.clear();
		for (String id : PSM.getAllIds()) {
			removePersistentObject(id);
		}
		PSM.flushCache();
	}

	public Set<String> keySet() {
		return PSM.getAllIds();

	}

	public Collection<T> values() {
		Set<T> valueSet = new HashSet<T>();
		for (String key : PSM.getAllIds()) {
			T lookup = getPersistentObject(key);
			// Just incase the persistence store moved under us
			if (lookup != null)
				valueSet.add(lookup);
		}
		return valueSet;

	}

	public Set<Entry<String, T>> entrySet() {

		ReadOnlyHashSet<Entry<String, T>> valueSet = new ReadOnlyHashSet<Entry<String, T>>();

		for (String id : PSM.getAllIds()) {
			T lookup = getPersistentObject(id);
			// Just incase the persistence store moved under us
			if (lookup != null)
				valueSet.addLocal(new SimpleEntry<String, T>(id, lookup));
		}
		return valueSet;

	}

	protected T getPersistentObject(String objectId) {
		T result = null;
		PersistentStorageManager.StoredObject latest = context.get(objectId);
		if (latest != null
				&& !PSM.hasFieldSetChanged(objectId, latest.timestamp())) {
			if (latest.value().getClass().equals(typeAware))
				return (T) latest.value();
			return null;
		}
		ClassLoader loader = typeAware.getClassLoader();
		latest = PSM.getPersistentObject(objectId, loader);
		context.put(objectId, latest);
		if (latest != null) {
			result = returnAsType(typeAware, latest.value());
			if (result != latest.value()) {
				latest.setValue(result);
			}
			return result;
		}
		return null;
	}

	// private Object refreshPersistentObject(String objectId, T object,
	// ClassLoader loader, boolean force)
	// {
	// PersistentStorageManager.StoredObject latest = context.get(objectId);
	// if (!PSM.hasFieldSetChanged(objectId, latest.timestamp())) {
	// return object;
	// }
	// PersistentStorageManager.StoredObject newest =
	// PSM.getPersistentObject(objectId, typeAware.getClassLoader());
	// if(newest == null)
	// {
	// return object;
	// }
	// UUID oldId = latest.fieldset().lookupId(object);
	// if(newest.fieldset().getFieldSet(oldId) == null)
	// {
	// context.put(objectId, newest);
	// return newest.value();
	// }
	// refreshPersistentObject0(object, loader, latest, newest);
	// return object;
	// }
	// private void refreshPersistentObject0(Object object, ClassLoader loader,
	// StoredObject latest, StoredObject newest)
	// {
	// if(object instanceof Collection)
	// {
	// for(Object entry : (Collection)object)
	// {
	// refreshPersistentObject0(entry,loader,latest,newest);
	// }
	// return;
	// }
	// if(object instanceof Map)
	// {
	// return;
	// }
	// UUID oldId = latest.fieldset().lookupId(object);
	// Map<String, Object> localFields = extractor.objectToFieldMap(object);
	// Map<String, Object> newFields = newest.fieldset().getFieldSet(oldId);
	// Map<String, Object> fields = Snapshot.updateFieldSet(object, newFields,
	// newest.fieldset(),latest.fieldset());
	// for(Entry<String,Object> field : localFields.entrySet())
	// {
	// if(latest.fieldset().lookupId(field.getValue())!= null)
	// {
	// fields.remove(field.getKey());
	// refreshPersistentObject0(field.getValue(),loader,latest,newest);
	//
	// }
	// }
	// Map<String,Object> updateSet = new HashMap<String,Object>();
	// for(Entry<String,Object> field : fields.entrySet())
	// {
	// Object localValue = localFields.get(field.getKey());
	// if(localFields.containsKey(field.getKey()) &&
	// !localValue.equals(field.getValue()))
	// {
	// updateSet.put(field.getKey(), field.getValue());
	// }
	// }
	// localFields.putAll(updateSet);
	// extractor.restoreObjectFromFieldMap(object, localFields);
	//
	// }
	// public T reloadPersistentObject(String objectId, T object) {
	// T result = object;
	// // Remove from cache
	// context.remove(objectId);
	// // Reload
	// PersistentStorageManager.StoredObject newest = PSM.getPersistentObject(
	// objectId, typeAware.getClassLoader());
	// // Object original = latest.value();
	// if (newest.value().getClass().equals(typeAware)) {
	// Map<String, Object> fields = extractor.objectToFieldMap(newest
	// .value());
	// extractor.restoreObjectFromFieldMap(object, fields);
	//
	// } else {
	// result = null;
	// }
	//
	// // Reload the existing objects
	// // if (object != original) {
	// // extractor.restoreObjectFromFieldMap(original, fields);
	// // }
	// newest.setValue(object);
	// context.put(objectId, newest);
	// // Now, replace the "new" loaded copy with the freshly reloaded
	// // original
	// // Map<String, Object> snapshot = newest.fieldset().get(newest.value());
	// // newest.fieldset().remove(newest.value());
	// // newest.setValue(object);
	// // newest.fieldset().put(original, snapshot);
	//
	// return result;
	// }

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
	}

	protected void removePersistentObject(String objectId) {
		PSM.removeFieldSet(objectId);
	}

}