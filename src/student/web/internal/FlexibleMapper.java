package student.web.internal;

import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;

public class FlexibleMapper
    extends MapperWrapper
{
    public FlexibleMapper(Mapper wrapped)
    {
        super(wrapped);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class realClass(String elementName)
    {
        try
        {
            return super.realClass(elementName);
        }
        catch (CannotResolveClassException e)
        {
            return UnrecognizedClass.class;
        }
    }
}
