package student.web.internal.converters;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import student.web.internal.Snapshot;
import student.web.internal.PersistentStorageManager.FakePrintWriter;


/**
 * Converts an array of objects or primitives to XML, using a nested child
 * element for each item.
 * 
 * @author Joe Walnes
 */
public class ArrayConverter extends CollectionConverter
{

    public ArrayConverter( Mapper mapper )
    {
        super( mapper );
    }


    public boolean canConvert( Class type )
    {
        if ( type == null )
            return false;
        return type.isArray();
    }


    public void marshal(
        Object source,
        HierarchicalStreamWriter writer,
        MarshallingContext context )
    {
        Class<?> arrayElementType = source.getClass().getComponentType();
        UUID id = Snapshot.lookupId( source, false );
        List<Object> localCollection;
        if ( arrayElementType.equals( byte.class ) )
        {
            localCollection = new ArrayList( Arrays.asList( (byte[])source ) );
            super.marshal0( id, source, writer, context, localCollection );
        }
        else if ( arrayElementType.equals( short.class ) )
        {
            localCollection = new ArrayList( Arrays.asList( (short[])source ) );
            super.marshal0( id, source, writer, context, localCollection );
        }
        else if ( arrayElementType.equals( int.class ) )
        {
            localCollection = new ArrayList( Arrays.asList( (int[])source ) );
            super.marshal0( id, source, writer, context, localCollection );
        }
        else if ( arrayElementType.equals( long.class ) )
        {
            localCollection = new ArrayList( Arrays.asList( (long[])source ) );
            super.marshal0( id, source, writer, context, localCollection );
        }
        else if ( arrayElementType.equals( float.class ) )
        {
            localCollection = new ArrayList( Arrays.asList( (float[])source ) );
            super.marshal0( id, source, writer, context, localCollection );
        }
        else if ( arrayElementType.equals( double.class ) )
        {
            localCollection = new ArrayList( Arrays.asList( (double[])source ) );
            super.marshal0( id, source, writer, context, localCollection );
        }
        else if ( arrayElementType.equals( char.class ) )
        {
            localCollection = new ArrayList( Arrays.asList( (char[])source ) );
            super.marshal0( id, source, writer, context, localCollection );
        }
        else if ( arrayElementType.equals( boolean.class ) )
        {
            localCollection = new ArrayList( Arrays.asList( (boolean[])source ) );
            super.marshal0( id, source, writer, context, localCollection );
        }
        else
        {
            localCollection = new ArrayList( Arrays.asList( (Object[])source ) );
            super.marshal0( id, source, writer, context, localCollection );
        }
        for ( int i = 0; i < localCollection.size(); i++ )
        {
            Array.set( source, i, localCollection.get( i ) );
        }
        if ( !( writer instanceof FakePrintWriter ) )
        {
            Snapshot.getLocal().resolveObject( id, source, localCollection );
        }

    }


    public Object unmarshal(
        HierarchicalStreamReader reader,
        UnmarshallingContext context )
    {
        UUID idAttr = UUID.fromString( reader.getAttribute( XMLConstants.ID_ATTRIBUTE ) );
        Collection<Object> toPopulate = new ArrayList<Object>();
        super.populateCollection( reader, context, toPopulate, null );
        Object array = Array.newInstance( context.getRequiredType()
            .getComponentType(), toPopulate.size() );
        int i = 0;
        for ( Iterator iterator = toPopulate.iterator(); iterator.hasNext(); )
        {
            Array.set( array, i++, iterator.next() );
        }
        Snapshot.getLocal().resolveObject( idAttr, array, toPopulate );
        return array;
    }
}
