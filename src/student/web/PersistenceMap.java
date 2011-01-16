package student.web;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public interface PersistenceMap<T> extends java.util.Map<String, T>{
	/**
	 * Remove an Object from the persistence store. First this method attempts
	 * to retrieve a value currently associated with this key. Then object is
	 * removed from the persistence store. If the key value mapping has been
	 * successfully removed, the object previously associated with the mapping
	 * is returned.
	 * 
	 * @param key
	 *            The key associated with the value that is being removed. The
	 *            key must be a non-null string with a length greater than 0.
	 * 
	 * @return Returns the removed object or null if no previous association
	 *         existed.
	 * 
	 */
	public T remove(Object key);
	/**
	 * Creates an association in the map from a key to a value. First, the Map
	 * is checked for an existing object associated with the key and caches it.
	 * After caching a previous value, the previous association is overwritten
	 * with the new key value entry.
	 * 
	 * @param key
	 *            The non-null key with a string length greater than 0
	 * @param value
	 *            The object to be associated with the key in the map. This
	 *            object may not be of type Class.
	 * 
	 * @return The original value associated with the key is returned or null if
	 *         no previous association existed.
	 * 
	 */
	public T put(String key, T value);
	/**
	 * Iterate over every entry within the external map and add the key value
	 * pair to the persistence store.
	 * 
	 * @param externalMap
	 *            The external map of keys to values that will be added to the
	 *            persistence store.
	 * 
	 */
	public void putAll(Map<? extends String, ? extends T> externalMap);
	/**
	 * Returns the value to which the specified key is mapped. If a mapping
	 * exists but it is not the same type as the object stored in the
	 * persistence store then null may be returned. Use
	 * {@link #containsKey(Object) containsKey} to determine if a mapping
	 * exists.
	 * 
	 * @param key
	 *            The key to look up an associated value for. This key must be a
	 *            non-null String with length greater then zero.
	 * 
	 * @return The value mapped to the key. This may be null if no mapping
	 *         exists or the mapping does not match the generic type of this
	 *         map.
	 * 
	 */
	public T get(Object key);
	/**
	 * Check the persistence map for an association between the key and a value.
	 * 
	 * @param key
	 *            The key to check for a value mapping.
	 * 
	 * @return true if the map contains a mapping from this key to a value.
	 * 
	 */
	public boolean containsKey(Object key);
	/**
	 * Calculate the total number of elements stored in the Persistence layer.
	 * This calculation disregards the generic type of this persistence map.
	 * Therefore, of the key value mappings will return null when accessed.
	 * 
	 * @return The total number of persisted objects in this persistence layer.
	 * 
	 */
	public int size();
	/**
	 * Checks if any key to value mappings exist in the persistence map. This
	 * does not respect the generic type of the class. Therfore, some of these
	 * mappings may be null.
	 * 
	 * @return True if there are no persisted objects in this persistence layer.
	 * 
	 */
	public boolean isEmpty();
	/**
	 * Returns true if the value esists in the map. This value could be mapped
	 * to multiple keys.
	 * 
	 * @param value
	 *            the value whos presence in the map is being tested.
	 * 
	 * @return true if the map contains a mapping from one or more keys to this
	 *         value.
	 */
	public boolean containsValue(Object value);
	/**
	 * Clear the contents of the persistence layer. This operation does not
	 * respect the generic type of this map. Therefore, all key value mappings
	 * are removed from the persistence layer.
	 */
	public void clear();
	/**
	 * Returns the set of all possible keys for this persistence layer. This
	 * function does not respect the generic type of this map. Therefore, this
	 * keyset contains keys for every object regardless of type. This means some
	 * of the key value mappings may be null.
	 * 
	 * @return the set of all possible keys for this persistence layer.
	 */
	public Set<String> keySet();
	/**
	 * Returns a collection representing all of the values stored in this Map.
	 * This collection is not backed by the actual persistence layer. Changes to
	 * this collectio nare independent of the actual persistence layer.
	 * 
	 * @return a collection representing all of values stored in the map.
	 */
	public Collection<T> values();
	/**
	 * Returns a set of all entries in the Map. these entries are NOT backed by
	 * the persistence layer. Therefore, changes to this set and it's contents
	 * are not reflected in the actual persistence layer.
	 * 
	 * @return the set of entries within the map.
	 * 
	 */
	public Set<Entry<String, T>> entrySet();
}
