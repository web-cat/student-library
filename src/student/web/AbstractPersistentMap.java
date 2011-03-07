package student.web;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.AbstractMap.SimpleEntry;

import student.web.internal.ApplicationSupportStrategy;
import student.web.internal.LocalityService;
import student.web.internal.ObjectFieldExtractor;
import student.web.internal.PersistentStorageManager;
import student.web.internal.ReadOnlySet;
import student.web.internal.PersistentStorageManager.StoredObject;


/**
 *
 * This is an abstract map representing all of the general functionality for
 * maps that create projections on a particular persistence layer. Note that
 * some of these functions return the results from the entire persistence layer
 * without respect to the generic type of this map. However, some of the
 * operations do respect the generic type of the implementing map. To implement
 * this map, the client only needs to implement a constructor that informs the
 * super class of the generic type (to prevent type erasure) and the directory
 * to treat as the base directory in the persistence store. The directory is non
 * absolute and is based on the configured datastore directory.
 *
 * @author mjw87
 *
 * @param <T>
 */
public abstract class AbstractPersistentMap<T>
    implements PersistentMap<T>
{
//    /**
//     * Seperator character for keywords within a persisted object id. This is
//     * used for conversion to a remote map.
//     */
//    protected static final String SEPARATOR = "-.-";

//    /**
//     * Timestamp of the last time the keyset of the persistence library was
//     * retrieved.
//     */
//    private long idSetTimestamp = 0L;

    /**
     * The class type this map is projecting onto the persistence store.
     */
    protected Class<T> typeAware;

//    /**
//     * The latest obtained keyset from the persistence store.
//     */
//    protected HashSet<String> idSet = new HashSet<String>();
//    private ReadOnlyHashSet<String> snapshotIds;
//    private long snapshotTimestamp = 0L;

    /**
     * The cached context map for objects retrieved from the store. It is used
     * to reconsititue objects.
     */
    private Map<String, PersistentStorageManager.StoredObject> context;

    /**
     * An extractor object for extracting fields from objects.
     */
    private ObjectFieldExtractor extractor = new ObjectFieldExtractor();

    /**
     * The instance of the persistence store this Map is projecting on.
     */
    private PersistentStorageManager PSM;

    private ApplicationSupportStrategy support;


    protected abstract String getCacheId();


    protected AbstractPersistentMap( String directoryName )
    {

        PSM = PersistentStorageManager.getInstance( directoryName );
        support = LocalityService.getSupportStrategy();
        if ( support.getSessionParameter( getCacheId() ) != null )
        {
            context = (Map<String, PersistentStorageManager.StoredObject>)support.getSessionParameter( getCacheId() );
        }
        else
        {
            context = new HashMap<String, PersistentStorageManager.StoredObject>();
            support.setSessionParameter( getCacheId(), context );
        }
    }


    public T remove( Object key )
    {
        assert key instanceof String : "Persistence maps only allows for keys of type String";
        String objectId = (String)key;
        assert key != null : "An key cannot be null";
        assert objectId.length() > 0 : "An key cannot be an empty string";
        T previousValue = getPrevious( (String)key );
        removePersistentObject( objectId );
        return previousValue;

    }


    public T put( String key, T value )
    {
        assert key != null : "An objectId cannot be null";
        assert key.length() > 0 : "An objectId cannot be an empty string";
        assert !( value instanceof Class ) : "The object to store cannot "
            + "be a class; perhaps you wanted "
            + "to provide an instance of this class instead?";
        T previousValue = getPrevious( key );
        setPersistentObject( key, value );
        return previousValue;
    }


    private T getPrevious( String key )
    {
        PersistentStorageManager.StoredObject cached = context.get( key );
        T previousValue = null;
        if ( cached != null )
        {
            try
            {
                previousValue = (T)cached.value();
            }
            catch ( ClassCastException e )
            {
                // Cant cast!
                ;
            }
        }

        if ( cached == null )
        {
            PersistentStorageManager.StoredObject previous = PSM.getPersistentObject( key,
                typeAware.getClassLoader() );
            if ( previous != null )
            {
                if ( previous.value().getClass().equals( typeAware ) )
                {
                    previousValue = (T)previous.value();
                }
            }
        }
        return previousValue;
    }


    public void putAll( Map<? extends String, ? extends T> externalMap )
    {
        for ( Map.Entry<? extends String, ? extends T> entry : externalMap.entrySet() )
        {
            setPersistentObject( entry.getKey(), entry.getValue() );
        }
    }


    public T get( Object key )
    {
        assert key instanceof String : "Persistence maps only allows for keys of type String";
        String objectId = (String)key;
        assert key != null : "An objectId cannot be null";
        assert objectId.length() > 0 : "An objectId cannot be an empty string";
        T foundObject = getPersistentObject( objectId );
        if ( context.get( key ) != null
            && PSM.hasFieldSetChanged( (String)key, context.get( key )
                .timestamp() ) )
        {
            PSM.refreshPersistentObject( (String)key,
                context.get( key ),
                typeAware.getClassLoader() );
        }
        return foundObject;
    }


    //
    public boolean containsKey( Object key )
    {
        assert key instanceof String : "Persistence maps only allows for keys of type String";
        String objectId = (String)key;
        // TODO: Bug passing loader should be fixed
        return PSM.hasFieldSetFor( objectId, null );
    }


    public int size()
    {
        return PSM.getAllIds().size();
    }


    public boolean isEmpty()
    {

        return PSM.getAllIds().isEmpty();

    }


    //
    public boolean containsValue( Object value )
    {
        for ( String id : PSM.getAllIds() )
        {
            T persistedObject = getPersistentObject( id );
            // Just incase the store shifted under us
            if ( persistedObject != null && persistedObject.equals( value ) )
            {
                return true;
            }
        }
        return false;

    }


    //
    public void clear()
    {

        context.clear();
        for ( String id : PSM.getAllIds() )
        {
            removePersistentObject( id );
        }
        PSM.flushCache();
    }


    public Set<String> keySet()
    {
        return PSM.getAllIds();

    }


    public Collection<T> values()
    {
        Set<T> valueSet = new HashSet<T>();
        for ( String key : PSM.getAllIds() )
        {
            T lookup = getPersistentObject( key );
            // Just incase the persistence store moved under us
            if ( lookup != null )
                valueSet.add( lookup );
        }
        return valueSet;

    }


    public Set<Entry<String, T>> entrySet()
    {

        HashSet<Entry<String, T>> valueSet = new HashSet<Entry<String, T>>();

        for ( String id : PSM.getAllIds() )
        {
            T lookup = getPersistentObject( id );
            // Just incase the persistence store moved under us
            if ( lookup != null )
            {
                valueSet.add( new SimpleEntry<String, T>( id, lookup ) );
            }
        }
        return new ReadOnlySet<Entry<String, T>>(valueSet);
    }


    protected T getPersistentObject( String objectId )
    {
        T result = null;
        PersistentStorageManager.StoredObject latest = context.get( objectId );
        if ( latest != null
            && !PSM.hasFieldSetChanged( objectId, latest.timestamp() ) )
        {
            if ( latest.value().getClass().equals( typeAware ) )
            {
                result = returnAsType(typeAware, latest.value());
            }
        }
        else
        {
            ClassLoader loader = typeAware.getClassLoader();
            if ( loader == null )
            {
                loader = this.getClass().getClassLoader();
            }
            latest = PSM.getPersistentObject( objectId, loader );
            context.put( objectId, latest );
            if ( latest != null )
            {
                result = returnAsType( typeAware, latest.value() );
                if ( result != latest.value() )
                {
                    latest.setValue( result );
                }
            }
        }
        return result;
    }


    @SuppressWarnings("unchecked")
    private <V> V returnAsType( Class<V> t, Object value )
    {
        if ( value == null )
        {
            return null;
        }
        if ( value instanceof TreeMap && !TreeMap.class.isAssignableFrom( t ) )
        {
            value = extractor.fieldMapToObject( t, (Map<String, Object>)value );
        }
        if ( t.isAssignableFrom( value.getClass() ) )
        {
            return (V)value;
        }

        return null;
    }


    protected <ObjectType> void setPersistentObject(
        String objectId,
        ObjectType object )
    {
        try
        {
            PersistentStorageManager.StoredObject latest = context.get( objectId );
            if ( latest != null )
            {
                latest.setValue( object );
                ClassLoader loader = object.getClass().getClassLoader();
                if ( loader == null )
                    loader = this.getClass().getClassLoader();
                PSM.storePersistentObjectChanges( objectId, latest, loader );
            }
            else
            {
                latest = PSM.storePersistentObject( objectId, object );
                context.put( objectId, latest );
            }
        }
        catch ( RuntimeException e )
        {
            PSM.removeFieldSet( objectId );
            throw e;
        }
    }


    protected void removePersistentObject( String objectId )
    {
        PSM.removeFieldSet( objectId );
    }

}
