/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2009-2010 Virginia Tech
 |
 |  This file is part of the Student-Library.
 |
 |  The Student-Library is free software; you can redistribute it and/or
 |  modify it under the terms of the GNU Lesser General Public License as
 |  published by the Free Software Foundation; either version 3 of the
 |  License, or (at your option) any later version.
 |
 |  The Student-Library is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU Lesser General Public License for more details.
 |
 |  You should have received a copy of the GNU Lesser General Public License
 |  along with the Student-Library; if not, see <http://www.gnu.org/licenses/>.
\*==========================================================================*/

package student.web.internal.converters;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import student.web.internal.Snapshot;
import student.web.internal.TemplateManager;
import student.web.internal.TemplateManager.Template;
import student.web.internal.TemplateManager.Template.Field;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.TreeMapConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

//-------------------------------------------------------------------------
/**
 * A custom XStream converter class that stores each object as a map from field
 * names to field values.
 * 
 * @author Stephen Edwards
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class FlexibleFieldSetConverter extends ReflectionConverter
{
	private TreeMapConverter mapConverter;
//	Snapshot snapshots = new Snapshot();
//	Snapshot oldSnapshots;
	
	public class IllegalPersistException extends RuntimeException
	{

		/**
		 * 
		 */
		private static final long serialVersionUID = -6748655411127633818L;

		public IllegalPersistException(String message)
		{
			super(message);
		}

	}

	// ----------------------------------------------------------
	public FlexibleFieldSetConverter(Mapper mapper, ReflectionProvider rp)
	{
		super(mapper, rp);
		mapConverter = new TreeMapConverter(mapper);
//		clearSnapshots();
	}

//	// ----------------------------------------------------------
//	public Snapshot getSnapshots()
//	{
//		return snapshots;
//	}
//
//	// ----------------------------------------------------------
//	public void setOldSnapshots(Snapshot formerSnapshots)
//	{
//		oldSnapshots = formerSnapshots;
//	}
//
//	// ----------------------------------------------------------
//	public void clearSnapshots()
//	{
//		snapshots = new Snapshot();
//	}

	// ----------------------------------------------------------
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context)
	{
		if (source instanceof student.web.Application)
		{
			throw new IllegalArgumentException(
					"You cannot store an object that contains a reference " + "to your application");
		}
		writer.addAttribute(XMLConstants.FIELDSET_ATTRIBUTE, "true");
		Map<String, Object> fields = objectToFieldMap(source);
		//BUG: The persistence store will attempt to persist an inner class.  This is the same as storing an object that references the Application class
		if(fields.containsKey("this$0"))
		{
			throw new IllegalArgumentException("Sorry but the Persistence store does not support inner classes.  Please move your class to a seperate file.");
		}
		checkFields(source.getClass().getName(), fields);
		UUID id = Snapshot.lookupId(source,true);
		writer.addAttribute(XMLConstants.ID_ATTRIBUTE, id.toString());
		
		Map<String,Object> updatedFieldSets = Snapshot.generateUpdatedFieldSet(source,fields);
		restoreObjectFromFieldMap(source, updatedFieldSets);
//		Map<String,Object> toStore = Snapshot.updateFieldSet(source, fields, snapshots, oldSnapshots);
//		oldSnapshots.trackFieldset(id, toStore);
//		oldSnapshots.addResolvedObject(id, source);
		mapConverter.marshal(updatedFieldSets, writer, context);
	}

	private void checkFields(String source, Map<String, Object> fields)
	{
		TemplateManager tm = TemplateManager.getInstance();
		if (tm.isEnabled())
		{
			Template classTemplate = tm.getTemplate(source);
			if (classTemplate == null)
			{
				return;
			}
			
			//Check for every required template field.
			for(Field templateField : classTemplate.getFields())
			{
				if(!fields.containsKey(templateField.getName()))
				{
					if(!templateField.isNullable())
					{
						throw new IllegalPersistException("Your class must contain " +
								"the Field named \""+templateField.getName()+"\".  " +
								"Please add this to your class definition.");
					}
				}
			}
			//Check all present fields to ensure they are nullable
			for (String classField : fields.keySet())
			{
				Object value = fields.get(classField);
				if (value == null)
				{
					Field templateField = classTemplate.getField(classField);
					if (templateField != null)
					{
							if (!templateField.isNullable())
							{
								throw new IllegalPersistException("You cannot store the field "
									+ classField + " as null.  Make sure the field contains some data");
							}
					}
					else
					{
						Field defaultField = classTemplate.getDefault();
						if(defaultField.isNullable())
						{
							continue;
						}
						throw new IllegalPersistException("Your implementation of " + source
								+ "must contain a value for the field " + classField
								+ ".");
					}
				}
			}
		}

	}

	// ----------------------------------------------------------
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
	{
		Object result = null;
		Map<String, Object> fields = null;
		UUID id = null;
		if (reader.getAttribute(XMLConstants.FIELDSET_ATTRIBUTE) != null)
		{
			String objId = reader.getAttribute(XMLConstants.ID_ATTRIBUTE);
			id = UUID.fromString(objId);
			@SuppressWarnings("unchecked")
			Map<String, Object> realFields = (Map<String, Object>) mapConverter.unmarshal(reader,
					context);
			fields = realFields;
		}
		else
		{
			result = super.unmarshal(reader, context);
			if (result instanceof TreeMap
					&& !TreeMap.class.isAssignableFrom(context.getRequiredType()))
			{
				@SuppressWarnings("unchecked")
				Map<String, Object> realFields = (Map<String, Object>) result;
				fields = realFields;
			}
		}

		if (fields != null)
		{
			result = reflectionProvider.newInstance(context.getRequiredType());
			if (!restoreObjectFromFieldMap(result, fields) /*&& pjProvider != null*/)
			{
				try
				{
					Method initFieldsMethod = result.getClass().getMethod("initializeFields",
							(Class<?>) null);
					initFieldsMethod.invoke(result, (Object[]) null);
				}
				catch (Exception e)
				{
					try
					{
						Constructor<?> defaultConst = result.getClass().getConstructor();
						Object newResult = defaultConst.newInstance();
						restoreObjectFromFieldMap(newResult, fields);
						result = newResult;
						
					}
					catch (SecurityException e1)
					{
						System.err.println("You made your default constructor for "+result.getClass().getName() + " private.  If you would like it to be used" +
								" by the persistence library please make it public");
					}
					catch (NoSuchMethodException e2)
					{
						System.err
						.println("The shared object \""+result.getClass().getName()+"\" you have loaded did not "
								+ "contain all of the fields present in your original object.  "
								+ "We were also unable to find a method with the signature \"public "
								+ "void initializeFields()\" or the "
								+ "default constructor \"public "
								+ result.getClass().getName()
								+ "()\".  If you have made assumptions "
								+ "about the state of certain fields in your "
								+ result.getClass().getName()
								+ " Object please provide one of these methods to initialize the fields "
								+ "not present in the datastore.");
					} catch (Exception e3) {
						System.err.println("An exception occured when initializing your object with the default constructor.");
					}
					

				}
			}
			
			if (result != null)
			{
				Snapshot.resolveObject(id,result,fields);
//				snapshots.addResolvedObject(id, result);
//				snapshots.trackFieldset(id,fields);
			}
		}

		return result;
	}

	// ----------------------------------------------------------
	/**
	 * Convert an object to a map of field name/value pairs.
	 * 
	 * @param object
	 *            The object to convert
	 * @return The object's field values in map form
	 */
	private Map<String, Object> objectToFieldMap(Object object)
	{
		final TreeMap<String, Object> result = new TreeMap<String, Object>();

		reflectionProvider.visitSerializableFields(object, new ReflectionProvider.Visitor() {
			@SuppressWarnings("rawtypes")
			public void visit(String fieldName, Class type, Class definedIn, Object value)
			{
				result.put(fieldName, value);
			}
		});

		return result;
	}

	private static class BooleanWrapper
	{
		boolean value;
	}

	// ----------------------------------------------------------
	/**
	 * Convert an object to a map of field name/value pairs.
	 * 
	 * @param object
	 *            The object to convert
	 * @param fields
	 *            The field values to restore from
	 * @return True if all fields in the object were present in the field set
	 */
	private boolean restoreObjectFromFieldMap(Object object, final Map<String, Object> fields)
	{
		final Object result = object;
		final BooleanWrapper allFound = new BooleanWrapper();
		allFound.value = true;
		reflectionProvider.visitSerializableFields(result, new ReflectionProvider.Visitor() {
			@SuppressWarnings("rawtypes")
			public void visit(String fieldName, Class type, Class definedIn, Object value)
			{
				if (fields.containsKey(fieldName))
				{
					reflectionProvider.writeField(result, fieldName, fields.get(fieldName),
							definedIn);
				}
				else
				{
					allFound.value = false;
				}
			}
		});
		return allFound.value;
	}
	// ----------------------------------------------------------
	@SuppressWarnings("rawtypes")
	public boolean canConvert(Class type)
	{
		return true;
	}

}
