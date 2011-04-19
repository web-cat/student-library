/*
 * ==========================================================================*\
 * | $Id$
 * |*-------------------------------------------------------------------------*|
 * | Copyright (C) 2007-2010 Virginia Tech | | This file is part of the
 * Student-Library. | | The Student-Library is free software; you can
 * redistribute it and/or | modify it under the terms of the GNU Lesser General
 * Public License as | published by the Free Software Foundation; either version
 * 3 of the | License, or (at your option) any later version. | | The
 * Student-Library is distributed in the hope that it will be useful, | but
 * WITHOUT ANY WARRANTY; without even the implied warranty of | MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the | GNU Lesser General Public
 * License for more details. | | You should have received a copy of the GNU
 * Lesser General Public License | along with the Student-Library; if not, see
 * <http://www.gnu.org/licenses/>.
 * \*==========================================================================
 */

package student.web.internal;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import student.web.WebUtilities;
import student.web.internal.converters.ArrayConverter;
import student.web.internal.converters.CollectionConverter;
import student.web.internal.converters.FlexibleFieldSetConverter;
import student.web.internal.converters.MapConverter;
import student.web.internal.converters.PersistentMapConverter;
import student.web.internal.converters.UnrecognizedClassConverter;


// -------------------------------------------------------------------------
/**
 * Manages a persistent collection of data stored in files, where the data is in
 * the form of field/value maps.
 *
 * @author Stephen Edwards
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class PersistentStorageManager
{
    // ~ Instance/static variables .............................................

    private static PersistentStorageManager PSM = new PersistentStorageManager();

    private static final String EXT = ".dataxml";

    private File baseDir = LocalityService.getSupportStrategy().getPersistentBase();

    private MRUMap<String, String> idCache = new MRUMap<String, String>( 10000,
        0 );

    private MRUMap<String, String> idReverseCache = new MRUMap<String, String>( 10000,
        0 );

    private Map<ClassLoader, XStreamBundle> xstream = new WeakHashMap<ClassLoader, XStreamBundle>( 256 );

    private Set<String> usedIds = null;

    private long usedIdsTimestamp = 0L;

    //This is used to allow other code to lock the persistence store without calling into it.
    private Lock pLock = new ReentrantLock();
    // ~ Constructor ...........................................................

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
     *
     * @return The singleton instance of this class
     */
    public static PersistentStorageManager getInstance()
    {
        return PSM;
    }


    public static PersistentStorageManager getInstance( String dirName )
    {
        PersistentStorageManager manager = new PersistentStorageManager();
        manager.baseDir = LocalityService.getSupportStrategy().getPersistentFile( PSM.baseDir.getPath()+"/"+ dirName );
        return manager;
    }


    // ----------------------------------------------------------
    /**
     * Get the singleton instance of this class.
     *
     * @return The singleton instance of this class
     */
    public static void setStorageLocation( File dir )
    {
        synchronized ( PSM )
        {
            PSM.baseDir = LocalityService.getSupportStrategy().getPersistentFile( dir.getPath() );
        }
    }


    // ~ Public Methods ........................................................

    // ----------------------------------------------------------
    public synchronized Set<String> getAllIds()
    {
        // if (usedIds == null)
        // {
        usedIds = new HashSet<String>( 256 );
        if(!baseDir.exists())
            baseDir.mkdir();
        if (baseDir.exists())
        {
            for ( File file : baseDir.listFiles() )
            {
                String name = file.getName();
                if ( name.endsWith( EXT ) )
                {
                    // Strip the extension
                    name = name.substring( 0, name.length() - EXT.length() );
                    usedIds.add( unsanitizeId( name ) );
                }
            }
        }
        usedIdsTimestamp = System.currentTimeMillis();
        // }
        return new HashSet<String>( usedIds );
    }


    // ----------------------------------------------------------
    public synchronized Set<String> getAllIdsContaining(
        String fragment,
        String after )
    {
        if ( fragment == null || fragment.length() == 0 )
        {
            return getAllIds();
        }
        else
        {
            fragment = fragment.toLowerCase();
            Set<String> result = new HashSet<String>( getAllIds().size() );
            for ( String id : getAllIds() )
            {
                if ( id.toLowerCase().contains( fragment ) )
                {
                    result.add( id );
                }
            }
            return result;
        }
    }


    // ----------------------------------------------------------
    public synchronized Set<String> getAllIdsNotContaining( String fragment )
    {
        if ( fragment == null || fragment.length() == 0 )
        {
            return getAllIds();
        }
        else
        {
            fragment = fragment.toLowerCase();
            Set<String> result = new HashSet<String>( getAllIds().size() );
            for ( String id : getAllIds() )
            {
                if ( !id.toLowerCase().contains( fragment ) )
                {
                    result.add( id );
                }
            }
            return result;
        }
    }


    // ----------------------------------------------------------
    public synchronized boolean idSetHasChangedSince( long time )
    {
        return usedIdsTimestamp == 0L || usedIdsTimestamp > time;
    }


    // ----------------------------------------------------------
    public synchronized StoredObject getPersistentObject(
        String id,
        ClassLoader loader )
    {
        StoredObject result = null;
        pLock.lock();
        try
        {
        try
        {
            result = getPersistentObjectHelper( id, loader);
        }
        finally
        {
            Snapshot.clearLocal();
        }
        }
        finally
        {
            pLock.unlock();
        }
        return result;
    }


    private StoredObject getPersistentObjectHelper(
        String id,
        ClassLoader loader)
    {
        StoredObject result = null;
        if ( baseDir.exists() )
        {
            String sanitizedId = sanitizeId( id );
            File src= LocalityService.getSupportStrategy().getPersistentFile( baseDir,sanitizedId + EXT);
            final InputStream in = LocalityService.getSupportStrategy().getObjectSource(src);
            if ( in != null )
            {
                try
                {
                    Object object = readObjectFromXML( in, loader, new Snapshot() );
                    result = new StoredObject( id,
                        sanitizedId,
                        object,
                        Snapshot.getLocal(),
                        src.lastModified() );
                }
                finally
                {
                    try
                    {
                    in.close();
                    }
                    catch(IOException e)
                    {
                        //Oh well
                    }
                }
            }
        }
        return result;
    }


    public Object readObjectFromXML(
        final InputStream in,
        final ClassLoader loader, Snapshot local)
    {
        Snapshot.setLocal( local );
        final XStreamBundle bundle = getXStreamFor( loader );
        Object object = AccessController.doPrivileged( new PrivilegedAction<Object>()
        {
            public Object run()
            {
                return bundle.xstream.fromXML( in );
            }
        } );
        return object;
    }


    // ----------------------------------------------------------
    public synchronized boolean persistentObjectHasChanged(
        String id,
        long timestamp,
        ClassLoader loader )
    {
        id = sanitizeId( id );

        File dest = LocalityService.getSupportStrategy().getPersistentFile( baseDir, id+EXT );
        return timestamp < dest.lastModified();
    }


    // ----------------------------------------------------------
    public synchronized StoredObject storePersistentObject(
        String id,
        Object object )
    {
        pLock.lock();
        StoredObject stored = null;
        try
        {
        ClassLoader loader = object.getClass().getClassLoader();
        stored = new StoredObject( id,
            sanitizeId( id ),
            object,
            Snapshot.getLocal(),
            0L );
        storePersistentObjectChanges( id, stored, loader );
        }
        finally
        {
            pLock.unlock();
        }
        return stored;
    }


    public synchronized void storePersistentObjectChanges(
        String id,
        final StoredObject object,
        ClassLoader loader )
    {
        pLock.lock();
        try
        {
        storePersistentObjectChanges( id, object, loader, null );
        }
        finally
        {
            pLock.unlock();
        }
    }


    public class FakePrintWriter extends Writer
    {

        @Override
        public void write( char[] cbuf, int off, int len ) throws IOException
        {
            // heh do nothing
        }


        @Override
        public void flush() throws IOException
        {
            // heh do nothing

        }


        @Override
        public void close() throws IOException
        {
            // heh do nothing
        }

    }


    public synchronized void refreshPersistentObject(
        String id,
        final StoredObject object,
        ClassLoader loader )
    {
        storePersistentObjectChanges( id, object, loader, new FakePrintWriter() );
    }


    // ----------------------------------------------------------
    public synchronized void storePersistentObjectChanges(
        String id,
        final StoredObject object,
        ClassLoader loader,
        final Writer replacementWriter )
    {
        pLock.lock();
        try
        {
        String sanitizedId = sanitizeId( id );
        Snapshot.setLocal( new Snapshot() );
            getPersistentObjectHelper( id, loader );
           
                // Leave the snapshots set in the converter
            File dest = LocalityService.getSupportStrategy().getPersistentFile( baseDir, sanitizedId + EXT );
            final PrintWriter out;
            if ( replacementWriter == null )
            {
                out = new PrintWriter( LocalityService.getSupportStrategy().getObjectOutput(dest) );
            }
            else
            {
                out = new PrintWriter( replacementWriter );
            }
            
            Snapshot local;
            if ( object.fieldset() == null )
            {
                local = new Snapshot();
            }
            else
            {
                local = object.fieldset();
            }
            writeObjectToXML( object.value(), out,loader,Snapshot.getLocal(), local );
            if ( usedIds != null && !usedIds.contains( id ) )
            {
                usedIdsTimestamp = System.currentTimeMillis();
                usedIds.add( id );
            }
            object.timestamp = System.currentTimeMillis();
        Snapshot.clearNewest();
        Snapshot.clearLocal();
        }
        finally
        {
            pLock.unlock();
        }
    }


    public void writeObjectToXML(
        final Object object,
        final Writer out,
        final ClassLoader loader,
        Snapshot newest,
        Snapshot local)
    {
        Snapshot.setNewest( newest );
        Snapshot.setLocal( local );
        final XStreamBundle bundle = getXStreamFor( loader );
        AccessController.doPrivileged( new PrivilegedAction<Object>()
        {
            public Object run()
            {
                bundle.xstream.toXML( object, out );
                return null;
            }
        } );

            try
            {
                out.close();
            }
            catch ( IOException e )
            {
                //Best attempt at close
            }
    }


    // ----------------------------------------------------------
    public synchronized boolean hasFieldSetFor( String id, ClassLoader loader )
    {
        id = sanitizeId( id );

        File dest = LocalityService.getSupportStrategy().getPersistentFile( baseDir,id + EXT );
        return dest.exists();
    }


    // ----------------------------------------------------------
    public synchronized void removeFieldSet( String id )
    {
        pLock.lock();
        try
        {
        idCache.remove( id );
        if ( usedIds != null )
        {
            usedIds.remove( id );
        }
        id = sanitizeId( id );

        File dest = LocalityService.getSupportStrategy().getPersistentFile( baseDir,id + EXT );
        if ( dest.exists() )
        {
            dest.delete();
        }
        }
        finally
        {
            pLock.unlock();
        }
    }
    public void lock()
    {
        pLock.lock();
    }
    public void unlock()
    {
        pLock.unlock();
    }

    // ----------------------------------------------------------
    public synchronized void flushCache()
    {
        idCache.clear();
        xstream.clear();
        usedIds.clear();
    }


    // ----------------------------------------------------------
    public synchronized void flushClassCacheFor( ClassLoader loader )
    {
        xstream.remove( loader );
    }


    // ----------------------------------------------------------
    public static class StoredObject
    {
        private StoredObject( String id, String sanitizedId, Object value,
        Snapshot fieldset, long timestamp )
        {
            this.id = id;
            this.sanitizedId = sanitizedId;
            this.value = value;
            this.fieldset = fieldset;
            this.timestamp = timestamp;
        }


        public String id()
        {
            return id;
        }


        public String sanitizedId()
        {
            return sanitizedId;
        }


        public Object value()
        {
            return value;
        }


        public Snapshot fieldset()
        {
            return fieldset;
        }


        public void setFieldset( Snapshot snaps )
        {
            fieldset = snaps;
        }


        public long timestamp()
        {
            return timestamp;
        }


        public void setValue( Object newValue )
        {
            value = newValue;
        }

        private String id;

        private String sanitizedId;

        private Object value;

        private Snapshot fieldset;

        private long timestamp;
    }


    // ~ Private Methods .......................................................

    // ----------------------------------------------------------
    /**
     * Transforms an id into something safe to use as a file name. First, it
     * url-encodes the id. Then it adds on a hex suffix that encodes the
     * capitalization of the id. This second step is to ensure that names that
     * differ only by case still map to unique file names, even on platforms
     * that do not support case-sensitive file names (like Windows). Once
     * converted, the results are cached so that the conversion can be faster on
     * subsequent calls for the same id.
     *
     * @param id
     *            The id to transform
     * @return A version of the id safe for use as a file name.
     */
    private String sanitizeId( String id )
    {
        String result = idCache.get( id );
        if ( result == null )
        {
            result = WebUtilities.urlEncode( id ) + "-";
            int length = id.length();
            int marker = 0;
            for ( int i = 0; i < length; i++ )
            {
                if ( Character.isUpperCase( id.charAt( i ) ) )
                {
                    marker += 1 << ( i % 4 );
                }
                if ( i % 4 == 3 )
                {
                    result += Integer.toHexString( marker );
                    marker = 0;
                }
            }
            if ( length % 4 > 0 )
            {
                result += Integer.toHexString( marker );
            }
            idCache.put( id, result );
            idReverseCache.put( result, id );
        }
        return result;
    }


    // ----------------------------------------------------------
    private String unsanitizeId( String id )
    {
        String result = idReverseCache.get( id );
        if ( result == null )
        {
            String encodedBase = id;
            String caps = "";
            int pos = id.lastIndexOf( '-' );
            if ( pos > 0 )
            {
                encodedBase = id.substring( 0, pos );
                caps = id.substring( pos + 1 );
            }
            String unencoded = WebUtilities.urlDecode( encodedBase );

            result = "";
            pos = 0;
            int length = caps.length();
            for ( int i = 0; i < length && pos < unencoded.length(); i++ )
            {
                int digit = Integer.parseInt( caps.substring( i, i + 1 ), 16 );
                for ( int j = 0; j < 4 && pos < unencoded.length(); j++ )
                {
                    if ( ( digit & ( 1 << j ) ) != 0 )
                    {
                        result += Character.toUpperCase( unencoded.charAt( pos++ ) );
                    }
                    else
                    {
                        result += Character.toLowerCase( unencoded.charAt( pos++ ) );
                    }
                }
            }
            if ( pos < unencoded.length() )
            {
                result += unencoded.substring( pos );
            }
            idReverseCache.put( id, result );
            idCache.put( result, id );
        }
        return result;
    }


    // ----------------------------------------------------------
    private XStreamBundle getXStreamFor( final ClassLoader loader )
    {
        XStreamBundle result = xstream.get( loader );
        if ( result == null )
        {
            result = AccessController.doPrivileged( new PrivilegedAction<XStreamBundle>()
            {
                public XStreamBundle run()
                {
                    return new XStreamBundle( loader );
                }
            } );
            xstream.put( loader, result );
        }
        return result;
    }


    // ----------------------------------------------------------
    private static class XStreamBundle
    {
        XStream xstream;

        private FlexibleFieldSetConverter fConverter;

        private CollectionConverter cConverter;

        private MapConverter mConverter;

        private ArrayConverter aConverter;


        public XStreamBundle( ClassLoader loader )
        {
            try
            {
                if ( System.getSecurityManager() != null )
                    System.getSecurityManager().checkCreateClassLoader();
                xstream = new FlexibleXStream(loader);
            }
            catch(AccessControlException e)
            {
                xstream = new FlexibleXStream(this.getClass().getClassLoader());
            }

            //Prevent Persistent Map from being converted.
            PersistentMapConverter pConverter = new PersistentMapConverter();
            xstream.registerConverter( pConverter, XStream.PRIORITY_VERY_HIGH );

            // flex field converter
            fConverter = new FlexibleFieldSetConverter( xstream.getMapper(),
                xstream.getReflectionProvider() );
            xstream.registerConverter( fConverter, XStream.PRIORITY_VERY_LOW );

            // Unrecognized Class Converter
            UnrecognizedClassConverter ucc = new UnrecognizedClassConverter(xstream.getMapper());
            xstream.registerConverter( ucc, XStream.PRIORITY_VERY_HIGH );

            // //Collection Converter
            cConverter = new CollectionConverter( xstream.getMapper() );
            xstream.registerConverter( cConverter, XStream.PRIORITY_VERY_HIGH );
            //
            // //Map Converter
            mConverter = new MapConverter( xstream.getMapper() );
            xstream.registerConverter( mConverter, XStream.PRIORITY_VERY_HIGH );
            //
            // //Array Converter
            aConverter = new ArrayConverter( xstream.getMapper() );
            xstream.registerConverter( aConverter, XStream.PRIORITY_VERY_HIGH );
        }
    }


    public boolean hasFieldSetChanged( String key, long timestamp )
    {
        String sanitized = sanitizeId( key );
        File persisted = LocalityService.getSupportStrategy().getPersistentFile( baseDir,sanitized + EXT );
        return persisted.exists() && persisted.lastModified() > timestamp;
    }
}
