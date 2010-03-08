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

package student.web.internal;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.TreeMapConverter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

//-------------------------------------------------------------------------
/**
 *  A custom XStream converter class that stores each object as a map from
 *  field names to field values.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class FlexibleFieldSetConverter
    extends ReflectionConverter
{
    private TreeMapConverter mapConverter;
    private ReflectionProvider pjProvider = null;
    private Map<Object, Map<String, Object>> snapshots;
    private Map<Object, Map<String, Object>> oldSnapshots;


    // ----------------------------------------------------------
    public FlexibleFieldSetConverter(Mapper mapper, ReflectionProvider rp)
    {
        super(mapper, rp);
        mapConverter = new TreeMapConverter(mapper);
        if (!(reflectionProvider instanceof PureJavaReflectionProvider))
        {
            pjProvider = new PureJavaReflectionProvider();
        }
        clearSnapshots();
    }


    // ----------------------------------------------------------
    public Map<Object, Map<String, Object>> getSnapshots()
    {
        return snapshots;
    }


    // ----------------------------------------------------------
    public void setSnapshots(Map<Object, Map<String, Object>> newSnapshots)
    {
        snapshots = newSnapshots;
    }


    // ----------------------------------------------------------
    public void setOldSnapshots(
        Map<Object, Map<String, Object>> formerSnapshots)
    {
        oldSnapshots = formerSnapshots;
    }


    // ----------------------------------------------------------
    public void clearSnapshots()
    {
        snapshots = new HashMap<Object, Map<String, Object>>();
    }


    // ----------------------------------------------------------
    public void marshal(
        Object source,
        HierarchicalStreamWriter writer,
        MarshallingContext context)
    {
        if (source instanceof student.web.Application)
        {
            throw new IllegalArgumentException(
                "You cannot store an object that contains a reference "
                + "to your application");
        }
        writer.addAttribute("fieldset", "true");
        Map<String, Object> fields = objectToFieldMap(source);
        if (oldSnapshots != null && oldSnapshots.containsKey(source))
        {
            fields = difference(oldSnapshots.get(source), fields);
        }
        Map<String, Object> toStore = fields;
//        System.out.println("Field changes for object " + source);
//        System.out.println("    " + fields);
        String path = getPath(writer);
        if (snapshots != null && path != null && snapshots.containsKey(path))
        {
            toStore = new TreeMap<String, Object>(snapshots.get(path));
            toStore.putAll(fields);
        }
        mapConverter.marshal(toStore, writer, context);
    }


    // ----------------------------------------------------------
    public Object unmarshal(
        HierarchicalStreamReader reader,
        UnmarshallingContext context)
    {
        Object result = null;
        String path = getPath(reader);
        Map<String, Object> fields = null;
        if (reader.getAttribute("fieldset") != null)
        {
            System.out.println("attempting to unmarshal [fieldset] " + path);
            @SuppressWarnings("unchecked")
            Map<String, Object> realFields = (Map<String, Object>)
                mapConverter.unmarshal(reader, context);
            fields = realFields;
        }
        else
        {
            System.out.println("attempting to unmarshal " + path);
            result = super.unmarshal(reader, context);
            if (result instanceof TreeMap
                && !TreeMap.class.isAssignableFrom(context.getRequiredType()))
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> realFields = (Map<String, Object>)result;
                fields = realFields;
            }
        }

        if (fields != null)
        {
            result = reflectionProvider.newInstance(context.getRequiredType());
            if (!restoreObjectFromFieldMap(result, fields) &&
                pjProvider != null)
            {
                // If some fields weren't initialized, then try to create
                // an object using a default constructor, if possible
                try
                {
                    Object newResult =
                        pjProvider.newInstance(context.getRequiredType());
                    restoreObjectFromFieldMap(newResult, fields);
                    result = newResult;
                }
                catch (Exception e)
                {
                    // if we can't create it, there's no default constructor
                }
            }
            if (result != null)
            {
                snapshots.put(result, fields);
                if (path != null)
                {
                    snapshots.put(path, fields);
                }
            }
        }

        return result;
    }


    // ----------------------------------------------------------
    /**
     * Convert an object to a map of field name/value pairs.
     * @param object The object to convert
     * @return The object's field values in map form
     */
    private Map<String, Object> objectToFieldMap(Object object)
    {
        final TreeMap<String, Object> result = new TreeMap<String, Object>();

        reflectionProvider.visitSerializableFields(
            object,
            new ReflectionProvider.Visitor()
            {
                @SuppressWarnings("unchecked")
                public void visit(
                    String fieldName,
                    Class type,
                    Class definedIn,
                    Object value)
                {
                    result.put(fieldName, value);
                }
            });

        return result;
    }


    private static class BooleanWrapper { boolean value; }
    // ----------------------------------------------------------
    /**
     * Convert an object to a map of field name/value pairs.
     * @param object The object to convert
     * @param fields The field values to restore from
     * @return True if all fields in the object were present in the field set
     */
    private boolean restoreObjectFromFieldMap(
        Object object, final Map<String, Object> fields)
    {
        final Object result = object;
        final BooleanWrapper allFound = new BooleanWrapper();
        allFound.value = true;
        reflectionProvider.visitSerializableFields(
            result,
            new ReflectionProvider.Visitor()
            {
                @SuppressWarnings("unchecked")
                public void visit(
                    String fieldName,
                    Class type,
                    Class definedIn,
                    Object value)
                {
                    if (fields.containsKey(fieldName))
                    {
                        reflectionProvider.writeField(
                            result,
                            fieldName,
                            fields.get(fieldName),
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
    /**
     * Compute the difference between two field sets.  This is not the same
     * as "set difference".  Instead, it is really the "changes" map minus
     * any entries that duplicate those in the "original".  In other words,
     * what entries in "changes" map to different values than the same
     * entries in "original"?
     * @param original The base field set to compare against
     * @param changes The second (modified) field set
     * @return A field set map that contains the values defined
     * in changes that are either not present in or are different than
     * in the original.
     */
    private static Map<String, Object> difference(
        Map<String, Object> original,
        Map<String, Object> changes)
    {
        Map<String, Object> differences = new TreeMap<String, Object>();
        for (String key : changes.keySet())
        {
            Object value = changes.get(key);
            if ((value == null && original.get(key) != null)
                || (value != null
                    && !(isPrimitiveValue(value)
                         && value.equals(original.get(key)))))
            {
                differences.put(key, value);
            }
        }
        return differences;
    }


    // ----------------------------------------------------------
    private static boolean isPrimitiveValue(Object value)
    {
        return value instanceof String
            || value instanceof Number
            || value instanceof Boolean
            || value instanceof Character;
    }


    // ----------------------------------------------------------
    private String getPath(Object readerOrWriter)
    {
        String path = null;
        try
        {
            path = ((PathTracker)reflectionProvider.getField(
                readerOrWriter.getClass(), "pathTracker")
                    .get(readerOrWriter)).getPath().toString();
        }
        catch (Exception e)
        {
            System.out.println("cannot access pathTracker");
            e.printStackTrace();
        }
        return path;
    }


    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    public boolean canConvert(Class type)
    {
        return true;
    }

}
