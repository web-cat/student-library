package student.web.internal.converters;

import student.web.ApplicationPersistentMap;
import student.web.SessionPersistentMap;
import student.web.SharedPersistentMap;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;


public class PersistentMapConverter implements Converter
{

    public boolean canConvert( Class type )
    {
        if ( type.equals( SharedPersistentMap.class )
            || type.equals( ApplicationPersistentMap.class )
            || type.equals( SessionPersistentMap.class ) )
            return true;
        return false;
    }


    public void marshal(
        Object source,
        HierarchicalStreamWriter writer,
        MarshallingContext context )
    {
        throw new IllegalArgumentException( "You cannot store an object that contains a reference to a "
            + source.getClass().getSimpleName() );
    }


    public Object unmarshal(
        HierarchicalStreamReader reader,
        UnmarshallingContext context )
    {
        throw new IllegalArgumentException( "You cannot store an object that contains a reference to a PersistentMap" );
    }

}
