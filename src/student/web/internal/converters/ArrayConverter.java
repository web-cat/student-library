package student.web.internal.converters;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import student.web.internal.Snapshot;

/**
 * Converts an array of objects or primitives to XML, using a nested child
 * element for each item.
 * 
 * @author Joe Walnes
 */
public class ArrayConverter extends AbstractCollectionConverter {

	Snapshot newSnapshot;
	Snapshot oldSnapshot;

	public ArrayConverter(Mapper mapper) {
		super(mapper);
	}

	public void setupSnapshots(Snapshot newSnap,
			Snapshot oldSnap) {
		newSnapshot = newSnap;
		oldSnapshot = oldSnap;
	}

	public boolean canConvert(Class type) {
		if(type == null)
			return false;
		return type.isArray();
	}

	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		int length = Array.getLength(source);
		UUID oldId = Snapshot.lookupId(source, true);

			Object[] finalItems = Snapshot.generateUpdatedArray(source);
//			for (Iterator iterator = finalItems.iterator(); iterator.hasNext();) {
//				Object item = iterator.next();
//				writeItem(item, context, writer);
//			}
		writer.addAttribute(XMLConstants.ID_ATTRIBUTE, oldId.toString());
//		for (int i = 0; i < length; i++) {
//			Object item = Array.get(finalItems, i);
//			writeItem(item, context, writer);
//		}
	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		// read the items from xml into a list
		List items = new ArrayList();
		UUID id = UUID.fromString(reader
				.getAttribute(XMLConstants.ID_ATTRIBUTE));
		while (reader.hasMoreChildren()) {
			reader.moveDown();
			Object item = readItem(reader, context, null); // TODO: arg, what
															// should replace
															// null?
			items.add(item);
			reader.moveUp();
		}
		// now convertAnother the list into an array
		// (this has to be done as a separate list as the array size is not
		// known until all items have been read)
		Object array = Array.newInstance(context.getRequiredType()
				.getComponentType(), items.size());
		int i = 0;
		for (Iterator iterator = items.iterator(); iterator.hasNext();) {
			Array.set(array, i++, iterator.next());
		}
		Snapshot.resolveObject(id, array,null);
		return array;
	}
}
