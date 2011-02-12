package student.web.internal.converters;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import student.web.internal.Snapshot;

/**
 * Converts a java.util.Map to XML, specifying an 'entry' element with 'key' and
 * 'value' children.
 * <p>
 * Note: 'key' and 'value' is not the name of the generated tag. The children
 * are serialized as normal elements and the implementation expects them in the
 * order 'key'/'value'.
 * </p>
 * <p>
 * Supports java.util.HashMap, java.util.Hashtable and java.util.LinkedHashMap.
 * </p>
 * 
 * @author Joe Walnes
 */
public class MapConverter extends AbstractCollectionConverter {

//	Snapshot newSnapshot;
//	Snapshot oldSnapshot;

	public MapConverter(Mapper mapper) {
		super(mapper);
	}

//	public void setupSnapshots(Snapshot newSnap,
//			Snapshot oldSnap) {
//		newSnapshot = newSnap;
//		oldSnapshot = oldSnap;
//	}

	public boolean canConvert(Class type) {
		if(type == null)
			return false;
		return type.equals(HashMap.class) || type.equals(Hashtable.class)
				|| type.getName().equals("java.util.LinkedHashMap")
				|| type.getName().equals("sun.font.AttributeMap") // Used by
																	// java.awt.Font
																	// in JDK 6
		;
	}

	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
//		Map map = (Map) source;

//		UUID oldId = Snapshot.findId(map, newSnapshot, oldSnapshot);
		UUID id = Snapshot.lookupId(source,true);
		writer.addAttribute(XMLConstants.ID_ATTRIBUTE, id.toString());
		@SuppressWarnings("unchecked")
		Map<Object,Object> updatedMap = Snapshot.generateUpdatedMap((Map<Object,Object>)source);
		((Map)source).clear();
		((Map)source).putAll(updatedMap);
		for (Iterator<Entry<Object,Object>> iterator = updatedMap.entrySet().iterator(); iterator.hasNext();) {
			Entry<Object,Object> entry = (Entry<Object,Object>) iterator.next();
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, mapper()
					.serializedClass(Map.Entry.class), Map.Entry.class);

			writeItem(entry.getKey(), context, writer);
			writeItem(entry.getValue(), context, writer);

			writer.endNode();
		}
	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		Map map = (Map) createCollection(context.getRequiredType());
		UUID id= UUID.fromString(reader.getAttribute(XMLConstants.ID_ATTRIBUTE));
		Snapshot.resolveObject(id, map, null);
//		newSnapshot.addResolvedObject(id, map);
		populateMap(reader, context, map);
		return map;
	}

	protected void populateMap(HierarchicalStreamReader reader,
			UnmarshallingContext context, Map map) {
		while (reader.hasMoreChildren()) {
			reader.moveDown();

			reader.moveDown();
			Object key = readItem(reader, context, map);
			reader.moveUp();

			reader.moveDown();
			Object value = readItem(reader, context, map);
			reader.moveUp();

			map.put(key, value);

			reader.moveUp();
		}
	}

}
