package student.web.internal;

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
import java.util.TreeMap;
import java.util.WeakHashMap;
import student.web.WebUtilities;
import com.thoughtworks.xstream.XStream;

//-------------------------------------------------------------------------
/**
 *  Manages a persistent collection of data stored in files, where the data
 *  is in the form of field/value maps.
 *
 *  @author  Stephen Edwards
 *  @version 2009.09.13
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
//    private XStream xstream;
    private Map<ClassLoader, XStream> xstream =
        new WeakHashMap<ClassLoader, XStream>(256);
    private Map<ClassLoader, MRUMap<String, Map<String, Object>>> cache =
        new WeakHashMap<ClassLoader, MRUMap<String, Map<String, Object>>>(256);
    private Map<String, Set<ClassLoader>> idToClassLoader =
        new HashMap<String, Set<ClassLoader>>();

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
    public synchronized MRUMap.ValueWithTimestamp<Map<String, Object>>
    getFieldSet(String id, ClassLoader loader)
    {
//        System.out.println("==> getFieldSet(" + id + ", " + loader + ")");
        id = sanitizeId(id);
        return getFieldSetForSanitizedId(id, loader);
    }


    // ----------------------------------------------------------
    public synchronized boolean fieldSetHasChanged(
        String id, long timestamp, ClassLoader loader)
    {
//        System.out.println("==> fieldSetHasChanged("
//            + id + ", " + timestamp + ", " + loader + ")");
        id = sanitizeId(id);
        MRUMap<String, Map<String, Object>> fieldsetCache = cache.get(loader);
        if (fieldsetCache == null) return true;
        long current = fieldsetCache.getTimestampFor(id);
        return timestamp != current;
    }


    // ----------------------------------------------------------
    public synchronized void storeChangedFields(
        String id, Map<String, Object> fields, ClassLoader loader)
    {
//        System.out.println("==> storeChangedFields("
//            + id + ", " + fields + ", " + loader + ")");
        id = sanitizeId(id);
        Set<ClassLoader> listeners = idToClassLoader.get(id);
        if (listeners != null)
        {
            // Clear any stored cache values for any other classloaders
            for (ClassLoader other : listeners)
            {
                if (other != loader)
                {
                    MRUMap<String, Map<String, Object>> fieldsetCache =
                        cache.get(loader);
                    if (fieldsetCache != null)
                    {
                        fieldsetCache.remove(id);
                    }
                }
            }
            listeners.clear();
            listeners.add(loader);
        }

        MRUMap.ValueWithTimestamp<Map<String, Object>> old =
            getFieldSetForSanitizedId(id, loader);

        Map<String, Object> value = null;
        if (old != null)
        {
            value = old.value;
        }
        if (value == null)
        {
            value = new TreeMap<String, Object>();
        }
        value.putAll(fields);
        MRUMap<String, Map<String, Object>> fieldsetCache = cache.get(loader);
        if (fieldsetCache == null)
        {
            fieldsetCache = new MRUMap<String, Map<String, Object>>(10000, 0);
            cache.put(loader, fieldsetCache);
        }
        fieldsetCache.put(id, value);

        try
        {
            if (!baseDir.exists())
            {
                baseDir.mkdirs();
            }
            File dest = new File(baseDir, id + EXT);
            PrintWriter out = new PrintWriter(dest);
            getXStreamFor(loader).toXML(value, out);
            out.close();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }


    // ----------------------------------------------------------
    public synchronized boolean hasFieldSetFor(String id, ClassLoader loader)
    {
//        System.out.println("==> hasFieldSetFor(" + id + ", " + loader + ")");
        id = sanitizeId(id);
        // First, check the cache
        MRUMap<String, Map<String, Object>> fieldsetCache =
            cache.get(loader);
        if (fieldsetCache != null && fieldsetCache.get(id) != null)
        {
            return true;
        }

        // Otherwise, check the file system
        File dest = new File(baseDir, id + EXT);
        return dest.exists();
    }


    // ----------------------------------------------------------
    public synchronized void removeFieldSet(String id)
    {
//        System.out.println("==> removeFieldSet(" + id + ")");
        id = sanitizeId(id);
        Set<ClassLoader> listeners = idToClassLoader.get(id);
        if (listeners != null)
        {
            for (ClassLoader other : listeners)
            {
                MRUMap<String, Map<String, Object>> fieldsetCache =
                    cache.get(other);
                if (fieldsetCache != null)
                {
                    fieldsetCache.remove(id);
                }
            }
            listeners.clear();
        }
        File dest = new File(baseDir, id + EXT);
        if (dest.exists())
        {
            dest.delete();
        }
    }


    // ----------------------------------------------------------
    public synchronized void flushCache()
    {
        cache.clear();
        idToClassLoader.clear();
        idCache.clear();
        xstream.clear();
    }


    // ----------------------------------------------------------
    public synchronized void flushClassCacheFor(ClassLoader loader)
    {
//        System.out.println("==> flushClassCacheFor(" + loader + ")");
        xstream.remove(loader);
        MRUMap<String, Map<String, Object>> fieldsetCache = cache.get(loader);
        if (fieldsetCache != null)
        {
            for (String id : fieldsetCache.keySet())
            {
                Set<ClassLoader> listeners = idToClassLoader.get(id);
                if (listeners != null)
                {
                    listeners.remove(loader);
                }
            }
            fieldsetCache.clear();
            cache.remove(loader);
        }
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
                    marker += 1 << (i % 8);
                }
                if (i % 8 == 7)
                {
                    result += Integer.toHexString(marker);
                    marker = 0;
                }
            }
            if (length % 8 > 0)
            {
                result += Integer.toHexString(marker);
            }
            idCache.put(id, result);
            idToClassLoader.put(result, new HashSet<ClassLoader>());
        }
        return result;
    }


    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    private MRUMap.ValueWithTimestamp<Map<String, Object>>
    getFieldSetForSanitizedId(String id, ClassLoader loader)
    {
//        System.out.println("==> getFieldSetForSanitizedId("
//            + id + ", " + loader + ") --------------------");
//        System.out.println("  idCache = " + idCache);
//        System.out.println("  idToClassLoader = " + idToClassLoader);
//        System.out.println("  cache = " + cache + "\n");
        MRUMap<String, Map<String, Object>> fieldsetCache = cache.get(loader);
        if (fieldsetCache == null)
        {
//            System.out.println("  initializing fieldset cache for " + loader);
            fieldsetCache = new MRUMap<String, Map<String, Object>>(10000, 0);
            cache.put(loader, fieldsetCache);
        }
        MRUMap.ValueWithTimestamp<Map<String, Object>> result =
            fieldsetCache.getTimestampedValue(id);
        if (result == null || result.value == null)
        {
//            System.out.println("  no cached value found");
            Map<String, Object> fields = null;

            try
            {
                if (baseDir.exists())
                {
                    File src = new File(baseDir, id + EXT);
                    if (src.exists())
                    {
                        FileInputStream in = new FileInputStream(src);
                        fields = (Map<String, Object>)getXStreamFor(loader)
                            .fromXML(in);
                        in.close();
                    }
                }
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }

            fieldsetCache.put(id, fields);
            result = fieldsetCache.getTimestampedValue(id);
            idToClassLoader.get(id).add(loader);
        }
//        System.out.println("  fieldset = " + result.value);
//        System.out.println("\n  idCache = " + idCache);
//        System.out.println("  idToClassLoader = " + idToClassLoader);
//        System.out.println("  cache = " + cache);
        return result;
    }


    // ----------------------------------------------------------
//    private XStream privxstream;
    private XStream getXStreamFor(final ClassLoader loader)
    {
        XStream result = xstream.get(loader);
        if (result == null)
        {
            result = (XStream)AccessController.doPrivileged(
                new PrivilegedAction<Object>() {
                    public Object run()
                    {
                        XStream xs = new XStream();
                        xs.setClassLoader(loader);
                        return xs;
                    }
                });
            xstream.put(loader, result);
//            System.out.println("Creating new xstream " + result + " for "
//                + "loader " + loader);
        }

//        if (privxstream == null)
//        {
//            privxstream = (XStream)AccessController.doPrivileged(
//                new PrivilegedAction<Object>() {
//                    public Object run()
//                    {
//                        XStream xs = new XStream();
//                        return xs;
//                    }
//                });
//        }

//        System.out.println("Found xstream " + result + " for loader " + loader);
        return result;
    }


}
