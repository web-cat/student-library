/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2007-2010 Virginia Tech
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

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import student.web.WebUtilities;

//-------------------------------------------------------------------------
/**
 *  Manages a persistent collection of data stored in files, where the data
 *  is in the form of field/value maps.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public class PersistentStorageManager
{
    //~ Instance/static variables .............................................

    private static PersistentStorageManager PSM =
        new PersistentStorageManager();
    private static final String EXT = ".dataxml";

    private File baseDir = new File("data");
    private MRUMap<String, String> idCache =
        new MRUMap<String, String>(10000, 0);
    private MRUMap<String, String> idReverseCache =
        new MRUMap<String, String>(10000, 0);
    private Map<ClassLoader, XStreamBundle> xstream =
        new WeakHashMap<ClassLoader, XStreamBundle>(256);
    private Set<String> usedIds = null;
    private long usedIdsTimestamp = 0L;


    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * This is a singleton class.
     */
    private PersistentStorageManager()
    {
        // Nothing to do
    }


    // ----------------------------------------------------------
    /**
     * Get the singleton instance of this class.
     * @return The singleton instance of this class
     */
    public static PersistentStorageManager getInstance()
    {
        return PSM;
    }


    // ----------------------------------------------------------
    /**
     * Get the singleton instance of this class.
     * @return The singleton instance of this class
     */
    public static void setStorageLocation(File dir)
    {
        synchronized (PSM)
        {
            PSM.baseDir = dir;
        }
    }


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    public synchronized Set<String> getAllIds()
    {
        if (usedIds == null)
        {
            usedIds = new HashSet<String>(256);
            for (File file : baseDir.listFiles())
            {
                String name = file.getName();
                if (name.endsWith(EXT))
                {
                    // Strip the extension
                    name = name.substring(0, name.length() - EXT.length());
                    usedIds.add(unsanitizeId(name));
                }
            }
            usedIdsTimestamp = System.currentTimeMillis();
        }
        return usedIds;
    }


    // ----------------------------------------------------------
    public synchronized Set<String> getAllIdsContaining(
        String fragment, String after)
    {
        if (fragment == null || fragment.length() == 0)
        {
            return getAllIds();
        }
        else
        {
            fragment = fragment.toLowerCase();
            Set<String> result = new HashSet<String>(getAllIds().size());
            for (String id : getAllIds())
            {
                if (id.toLowerCase().contains(fragment))
                {
                    result.add(id);
                }
            }
            return result;
        }
    }


    // ----------------------------------------------------------
    public synchronized Set<String> getAllIdsNotContaining(String fragment)
    {
        if (fragment == null || fragment.length() == 0)
        {
            return getAllIds();
        }
        else
        {
            fragment = fragment.toLowerCase();
            Set<String> result = new HashSet<String>(getAllIds().size());
            for (String id : getAllIds())
            {
                if (!id.toLowerCase().contains(fragment))
                {
                    result.add(id);
                }
            }
            return result;
        }
    }


    // ----------------------------------------------------------
    public synchronized boolean idSetHasChangedSince(long time)
    {
        return usedIdsTimestamp == 0L || usedIdsTimestamp > time;
    }


    // ----------------------------------------------------------
    public synchronized StoredObject getPersistentObject(
        String id, ClassLoader loader)
    {
//        System.out.println("==> getFieldSet(" + id + ", " + loader + ")");
        String sanitizedId = sanitizeId(id);

        StoredObject result = null;

        try
        {
            if (baseDir.exists())
            {
                File src = new File(baseDir, sanitizedId + EXT);
                if (src.exists())
                {
                    final FileInputStream in = new FileInputStream(src);
                    final XStreamBundle bundle = getXStreamFor(loader);
                    bundle.converter.clearSnapshots();
                    try
                    {
                        Object object = AccessController.doPrivileged(
                            new PrivilegedAction<Object>() {
                                public Object run()
                                {
                                    return bundle.xstream.fromXML(in);
                                }
                            });
                        result = new StoredObject(
                            id,
                            sanitizedId,
                            object,
                            bundle.converter.getSnapshots(),
                            src.lastModified());
                    }
                    finally
                    {
                        in.close();
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

//        System.out.println("  fieldset = " + result.value);
//        System.out.println("\n  idCache = " + idCache);
//        System.out.println("  idToClassLoader = " + idToClassLoader);
//        System.out.println("  cache = " + cache);
        return result;
    }


    // ----------------------------------------------------------
    public synchronized boolean persistentObjectHasChanged(
        String id, long timestamp, ClassLoader loader)
    {
//        System.out.println("==> fieldSetHasChanged("
//            + id + ", " + timestamp + ", " + loader + ")");
        id = sanitizeId(id);

        File dest = new File(baseDir, id + EXT);
        return timestamp < dest.lastModified();
    }


    // ----------------------------------------------------------
    public synchronized StoredObject storePersistentObject(
        String id, Object object)
    {
        ClassLoader loader = object.getClass().getClassLoader();
        StoredObject stored = new StoredObject(
            id,
            sanitizeId(id),
            object,
            new HashMap<Object, Map<String, Object>>(),
            0L);
        storePersistentObjectChanges(id, stored, loader);
        return stored;
    }


    // ----------------------------------------------------------
    public synchronized void storePersistentObjectChanges(
        String id, final StoredObject object, ClassLoader loader)
    {
//        System.out.println("==> storeChangedFields("
//            + id + ", " + fields + ", " + loader + ")");
        String sanitizedId = sanitizeId(id);

        try
        {
            if (!baseDir.exists())
            {
                baseDir.mkdirs();
            }
            File dest = new File(baseDir, sanitizedId + EXT);
            final XStreamBundle bundle = getXStreamFor(loader);
            bundle.converter.clearSnapshots();
            if (dest.exists())
            {
                final FileInputStream in = new FileInputStream(dest);
                try
                {
                    AccessController.doPrivileged(
                        new PrivilegedAction<Object>() {
                            public Object run()
                            {
                                bundle.xstream.fromXML(in);
                                return null;
                            }
                        });
                }
                finally
                {
                    in.close();
                }
                // Leave the snapshots set in the converter
            }
            else
            {
                bundle.converter.clearSnapshots();
            }
            final PrintWriter out = new PrintWriter(dest);
            bundle.converter.setOldSnapshots(object.fieldset());
            AccessController.doPrivileged(
                new PrivilegedAction<Object>() {
                    public Object run()
                    {
                        bundle.xstream.toXML(object.value(), out);
                        return null;
                    }
                });
            out.close();
            if (usedIds != null && !usedIds.contains(id))
            {
                usedIdsTimestamp = System.currentTimeMillis();
                usedIds.add(id);
            }
            object.fieldset().clear();
            object.fieldset().putAll(bundle.converter.getSnapshots());
            object.timestamp = System.currentTimeMillis();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }


    // ----------------------------------------------------------
    public synchronized boolean hasFieldSetFor(String id, ClassLoader loader)
    {
//        System.out.println("==> hasFieldSetFor(" + id + ", " + loader + ")");
        id = sanitizeId(id);

        File dest = new File(baseDir, id + EXT);
        return dest.exists();
    }


    // ----------------------------------------------------------
    public synchronized void removeFieldSet(String id)
    {
//        System.out.println("==> removeFieldSet(" + id + ")");
        id = sanitizeId(id);

        File dest = new File(baseDir, id + EXT);
        if (dest.exists())
        {
            dest.delete();
        }
    }


    // ----------------------------------------------------------
    public synchronized void flushCache()
    {
        idCache.clear();
        xstream.clear();
    }


    // ----------------------------------------------------------
    public synchronized void flushClassCacheFor(ClassLoader loader)
    {
//        System.out.println("==> flushClassCacheFor(" + loader + ")");
        xstream.remove(loader);
    }


    // ----------------------------------------------------------
    public static class StoredObject
    {
        private StoredObject(
            String id,
            String sanitizedId,
            Object value,
            Map<Object, Map<String, Object>> fieldset,
            long timestamp)
        {
            this.id = id;
            this.sanitizedId = sanitizedId;
            this.value = value;
            this.fieldset = fieldset;
            this.timestamp = timestamp;
        }

        public String id() { return id; }
        public String sanitizedId() { return sanitizedId; }
        public Object value() { return value; }
        public Map<Object, Map<String, Object>> fieldset() { return fieldset; }
        public long timestamp() { return timestamp; }

        public void setValue(Object newValue) { value = newValue; }

        private String id;
        private String sanitizedId;
        private Object value;
        private Map<Object, Map<String, Object>> fieldset;
        private long timestamp;
    }


    //~ Private Methods .......................................................

    // ----------------------------------------------------------
    /**
     * Transforms an id into something safe to use as a file name.
     * First, it url-encodes the id.  Then it adds on a hex suffix that
     * encodes the capitalization of the id.  This second step is to
     * ensure that names that differ only by case still map to unique
     * file names, even on platforms that do not support case-sensitive
     * file names (like Windows).  Once converted, the results are
     * cached so that the conversion can be faster on subsequent calls
     * for the same id.
     * @param id The id to transform
     * @return A version of the id safe for use as a file name.
     */
    private String sanitizeId(String id)
    {
        String result = idCache.get(id);
        if (result == null)
        {
            result = WebUtilities.urlEncode(id) + "-";
            int length = id.length();
            int marker = 0;
            for (int i = 0; i < length; i++)
            {
                if (Character.isUpperCase(id.charAt(i)))
                {
                    marker += 1 << (i % 4);
                }
                if (i % 4 == 3)
                {
                    result += Integer.toHexString(marker);
                    marker = 0;
                }
            }
            if (length % 4 > 0)
            {
                result += Integer.toHexString(marker);
            }
            idCache.put(id, result);
            idReverseCache.put(result, id);
        }
        return result;
    }


    // ----------------------------------------------------------
    private String unsanitizeId(String id)
    {
        String result = idReverseCache.get(id);
        if (result == null)
        {
            String encodedBase = id;
            String caps = "";
            int pos = id.lastIndexOf('-');
            if (pos > 0)
            {
                encodedBase = id.substring(0, pos);
                caps = id.substring(pos + 1);
            }
            String unencoded = WebUtilities.urlDecode(encodedBase);

            result = "";
            pos = 0;
            int length = caps.length();
            for (int i = 0; i < length && pos < unencoded.length(); i++)
            {
                int digit = Integer.parseInt(caps.substring(i, i + 1), 16);
                for (int j = 0; j < 4 && pos < unencoded.length(); j++)
                {
                    if ((digit & (1 << j)) != 0)
                    {
                        result +=
                            Character.toUpperCase(unencoded.charAt(pos++));
                    }
                    else
                    {
                        result +=
                            Character.toLowerCase(unencoded.charAt(pos++));
                    }
                }
            }
            if (pos < unencoded.length())
            {
                result += unencoded.substring(pos);
            }
            idReverseCache.put(id, result);
            idCache.put(result, id);
        }
        return result;
    }


    // ----------------------------------------------------------
    private XStreamBundle getXStreamFor(final ClassLoader loader)
    {
        XStreamBundle result = xstream.get(loader);
        if (result == null)
        {
            result = AccessController.doPrivileged(
                new PrivilegedAction<XStreamBundle>() {
                    public XStreamBundle run()
                    {
                        return new XStreamBundle(loader);
                    }
                });
            xstream.put(loader, result);
        }
        return result;
    }


    // ----------------------------------------------------------
    private static class XStreamBundle
    {
        XStream xstream;
        FlexibleFieldSetConverter converter;

        public XStreamBundle(ClassLoader loader)
        {
            xstream = new FlexibleXStream();
            xstream.setClassLoader(loader);

            converter = new FlexibleFieldSetConverter(
                xstream.getMapper(), xstream.getReflectionProvider());
            xstream.registerConverter(converter, XStream.PRIORITY_VERY_LOW);

            UnrecognizedClassConverter ucc = new UnrecognizedClassConverter();
            xstream.registerConverter(ucc, XStream.PRIORITY_VERY_HIGH);
        }
    }
}
