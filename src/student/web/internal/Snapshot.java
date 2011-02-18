package student.web.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Vector;
import org.apache.commons.collections.BidiMap;
import org.incava.util.diff.Diff;
import org.incava.util.diff.Difference;

public class Snapshot {

	// <Object,UUID>
	BidiMap objToUuid = new DualBidiIdentityHashMap<Object, UUID>();
	Map<Object, Map<String, Object>> objToFieldSet = new IdentityHashMap<Object, Map<String, Object>>();

	private UUID findId(Object source) {
		return (UUID) objToUuid.get(source);
	}

	private Map<String, Object> getFieldSetFromObject(Object source) {
		return objToFieldSet.get(source);
	}

	private Map<String, Object> getFieldSetFromId(UUID id) {
		if (id == null)
			return null;
		Object source = objToUuid.getKey(id);
		return objToFieldSet.get(source);
	}

	private void resolveObject0(UUID id, Object result,
			Map<String, Object> fields) {
		objToUuid.put(result, id);
		objToFieldSet.put(result, fields);
	}

	private static Snapshot local = null;
	private static Snapshot newest = null;

	public static Snapshot getLocal() {
		return local;
	}

	public static void setLocal(Snapshot snapshot) {
		local = snapshot;
	}

	public static void clearLocal() {
		local = null;
	}

	public static void setNewest(Snapshot snap) {
		newest = snap;
	}

	public static Snapshot getNewest() {
		return newest;
	}

	public static void clearNewest() {
		newest = null;
	}

	/**
	 * This looks up the id for the source object using local context from when
	 * the object was retrieved or a snapshot tagged as the new version of an
	 * object.
	 *
	 * @param source
	 *            the object to find the id for
	 * @param generate
	 *            if you want a new id for the object to be generated.
	 * @return
	 */
	public static UUID lookupId(Object source, boolean generate) {
		UUID id = local.findId(source);
		if (id != null) {
			return id;
		}
		if (newest != null)
			id = newest.findId(source);
		if (generate) {
			id = UUID.randomUUID();
		}
		return id;
	}

	public static Map<String, Object> generateUpdatedFieldSet(Object source,
			Map<String, Object> fields) {
		if (newest == null) {
			return fields;
		}
		Map<String, Object> finalFieldSet = new TreeMap<String, Object>();
		Map<String, Object> localFieldSet = local.getFieldSetFromObject(source);
		Map<String, Object> newestFieldSet = newest.getFieldSetFromId(Snapshot
				.lookupId(source, false));
		if (localFieldSet == null && newestFieldSet == null)
			return fields;
		if (localFieldSet != null && newestFieldSet != null) {
			Map<String, Object> localChangedFields = difference(localFieldSet,
					fields);
			Map<String, Object> newestChangedFields = difference(localFieldSet,
					newestFieldSet);
			newestChangedFields.putAll(localChangedFields);
			finalFieldSet.putAll(localFieldSet);
			finalFieldSet.putAll(newestChangedFields);
		} else if (localFieldSet == null) {
			finalFieldSet.putAll(newestFieldSet);
		} else if (newestFieldSet == null) {
			finalFieldSet.putAll(localFieldSet);
		}
		return finalFieldSet;
	}

	/**
	 * Compute the difference between two field sets. This is not the same as
	 * "set difference". Instead, it is really the "changes" map minus any
	 * entries that duplicate those in the "original". In other words, what
	 * entries in "changes" map to different values than the same entries in
	 * "original"?
	 *
	 * @param original
	 *            The base field set to compare against
	 * @param changes
	 *            The second (modified) field set
	 * @return A field set map that contains the values defined in changes that
	 *         are either not present in or are different than in the original.
	 */
	public static Map<String, Object> difference(Map<String, Object> original,
			Map<String, Object> changes) {
		Map<String, Object> differences = new TreeMap<String, Object>();
		for (String key : changes.keySet()) {
			Object value = changes.get(key);
			if ((value == null && original.get(key) != null)
					|| (value != null && !(isPrimitiveValue(value) && value
							.equals(original.get(key))))) {
				differences.put(key, value);
			}
		}
		return differences;
	}

	private static boolean isPrimitiveValue(Object value) {
		return value instanceof String || value instanceof Number
				|| value instanceof Boolean || value instanceof Character;
	}

	public static void resolveObject(UUID id, Object result,
			Map<String, Object> fields) {
		local.resolveObject0(id, result, fields);
	}

	private static class SnapshotComparator implements Comparator {
		Snapshot newContext;
		Snapshot oldContext;

		public SnapshotComparator(Snapshot newContext, Snapshot oldContext) {
			this.newContext = newContext;
			this.oldContext = oldContext;
		}

		public int compare(Object o1, Object o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			UUID oldId = oldContext.findId(o1);
			UUID newId = newContext.findId(o2);
			if (oldId != null && oldId.equals(newId))
				return 0;
			return -1;

		}
	}

	public static Collection<Object> generateUpdatedCollection(
			Collection<Object> localCollection) {
		UUID id = local.findId(localCollection);
		@SuppressWarnings("unchecked")
		Collection<Object> newCollection = (Collection<Object>) newest.objToUuid
				.getKey(id);
		if (newCollection == null) {
			return new ArrayList(localCollection);
		}
		if (localCollection instanceof Vector) {
			localCollection = new ArrayList<Object>(localCollection);
			newCollection = new ArrayList<Object>(newCollection);
		}
		LinkedList<Object> result = new LinkedList<Object>();
		if (localCollection instanceof List) {
			List<Object> newList = (List<Object>) newCollection;
			List<Object> localList = (List<Object>) localCollection;
			@SuppressWarnings("unchecked")
			Diff<Object> diff = new Diff<Object>(localList, newList,
					new SnapshotComparator(newest, local));
			List<Difference> diffList = diff.diff();
			result.addAll(localCollection);
			List<Integer> reverseChange = new ArrayList<Integer>();
			int offset = 0;
			for (Difference dif : diffList) {
				int addStart = dif.getAddedStart() + offset;
				int addEnd = dif.getAddedEnd() + offset;
				int delStart = dif.getDeletedStart();
				int delEnd = dif.getDeletedEnd();
				if (delEnd == -1) {
					for (int i = addStart; i <= addEnd; i++) {
						UUID addedId = Snapshot.lookupId(newList.get(i), false);
						if (local.objToUuid.getKey(addedId) == null) {
							result.add(i, newList.get(i));

						} else {
							offset--;
						}
					}
				} else if (addEnd == -1) {
					offset++;
				} else {
					for (int i = addStart; i <= addEnd; i++) {
						result.add(i, newList.get(delStart + 1));
						delStart++;
					}
				}
			}
			return result;
		}
		// This is for unordered Collections
		for (Object item : newCollection) {
			if (local.findId(item) == null) {
				result.add(item);
			}
		}
		return result;
	}

	public static Map<Object, Object> generateUpdatedMap(
			Map<Object, Object> source) {
		Map resolvedMap = new HashMap();
		resolvedMap.putAll(source);
		UUID mapId = Snapshot.lookupId(source, false);
		Map newestMap = (Map) newest.objToUuid.getKey(mapId);
		if (newestMap != null) {
			for (Object newObject : newestMap.keySet()) {
				UUID id = newest.findId(newestMap.get(newObject));
				if (id != null && local.objToUuid.getKey(id) == null) {
					resolvedMap.put(newObject, newestMap.get(newObject));
				}
			}
		}
		return resolvedMap;
	}

	public static Object[] generateUpdatedArray(Object source) {
		Object[] localArray = (Object[]) source;
		Object[] finalArray = new Object[localArray.length];
		UUID newestArrayId = Snapshot.lookupId(source, false);
		Object newestObject = newest.objToUuid.getKey(newestArrayId);
		if (newestObject != null) {
			Object[] newestArray = (Object[]) newestObject;
			Diff<Object> diffComp = new Diff<Object>(localArray, newestArray,
					new SnapshotComparator(newest, local));
			List<Difference> diffList = diffComp.diff();
			for (int i = 0; i < localArray.length; i++)
				finalArray[i] = localArray[i];
			for (Difference diff : diffList) {
				int addStart = diff.getAddedStart();
				int addEnd = diff.getAddedEnd();
				int delStart = diff.getDeletedStart();
				int delEnd = diff.getDeletedEnd();
				if (addEnd != -1) {
					for (int i = addStart; i <= addEnd; i++) {
						Object item = newestArray[i];
						UUID newId = newest.findId(item);

						if (!local.objToUuid.containsValue(newId)) {
							// finalArray.remove(delStart);
							// finalArray.add(delStart, item);
							finalArray[delStart] = item;
							delStart++;
						}
					}
				} else {
					for (int i = delStart; i <= delEnd; i++) {
						Object item = newestArray[delStart];
						UUID newId = newest.findId(item);
						finalArray[addStart] = item;

					}
				}
			}
		}
		// } else {
		// for (int i = 0; i < localArray.length; i++)
		// finalArray.add(localArray[i]);
		// }
		return finalArray;
	}
}
