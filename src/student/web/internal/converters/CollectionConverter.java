package student.web.internal.converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.Vector;

import student.web.internal.Snapshot;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * Converts most common Collections (Lists and Sets) to XML, specifying a nested
 * element for each item.
 * <p/>
 * <p>
 * Supports java.util.ArrayList, java.util.HashSet, java.util.LinkedList,
 * java.util.Vector and java.util.LinkedHashSet.
 * </p>
 * 
 * @author Joe Walnes
 */
public class CollectionConverter extends AbstractCollectionConverter {

	private Snapshot newSnapshot;
	private Snapshot oldSnapshot;
	private ReflectionProvider rp;

	public CollectionConverter(Mapper mapper, ReflectionProvider rp) {
		super(mapper);
		this.rp = rp;
	}

	public boolean canConvert(Class type) {
		if(type == null)
			return false;
		return type.equals(ArrayList.class)
				|| type.equals(HashSet.class)
				|| type.equals(LinkedList.class)
				|| type.equals(Vector.class)
				|| (JVM.is14() && type.getName().equals(
						"java.util.LinkedHashSet"));
	}

	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
//		Collection collection = (Collection) source;
		UUID id = Snapshot.lookupId(source,true);

//		Collection newestVersion = (Collection) newSnapshot
//				.getObjectFromKey(oldId);

		Collection<Object> updatedCollection = Snapshot.generateUpdatedCollection((Collection)source);
		((Collection)source).clear();
		((Collection)source).addAll(updatedCollection);
//		Collection finalItems = Snapshot.combineCollections(collection, newestVersion,newSnapshot,oldSnapshot);
		writer.addAttribute(XMLConstants.ID_ATTRIBUTE, id.toString());
		for (Iterator iterator = updatedCollection.iterator(); iterator.hasNext();) {
			Object item = iterator.next();
			writeItem(item, context, writer);
		}
	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		Class<?> type = context.getRequiredType();
		UUID id = UUID.fromString(reader
				.getAttribute(XMLConstants.ID_ATTRIBUTE));
		@SuppressWarnings("unchecked")
		Collection<Object> collection = (Collection<Object>) createCollection(type);
//		newSnapshot.addResolvedObject(id, collection);
		populateCollection(reader, context, collection);
		Snapshot.resolveObject(id, collection, null);
		return collection;
	}

	protected void populateCollection(HierarchicalStreamReader reader,
			UnmarshallingContext context, Collection<Object> collection) {
		while (reader.hasMoreChildren()) {
			reader.moveDown();
			Object item = readItem(reader, context, collection);
			collection.add(item);
			reader.moveUp();
		}
	}

	public void setNewSnapshots(Snapshot newContext) {
		this.newSnapshot = newContext;

	}

	public void setOldSnapshots(Snapshot oldContext) {
		this.oldSnapshot = oldContext;
	}

//	private String getPath(Object readerOrWriter) {
//		String path = null;
//		try {
//			path = ((PathTracker) rp.getField(readerOrWriter.getClass(),
//					"pathTracker").get(readerOrWriter)).getPath().toString();
//		} catch (Exception e) {
//			System.out.println("cannot access pathTracker");
//			e.printStackTrace();
//		}
//		return path.intern();
//	}
}
