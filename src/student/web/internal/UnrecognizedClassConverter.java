package student.web.internal;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class UnrecognizedClassConverter
    implements Converter
{

    public UnrecognizedClassConverter()
    {
        // nothing to do
    }


    public void marshal(
        Object source,
        HierarchicalStreamWriter writer,
        MarshallingContext context)
    {
        // do nothing
    }


    public Object unmarshal(
        HierarchicalStreamReader reader,
        UnmarshallingContext context)
    {
        // Treat instances if this class as if they were null
        return null;
    }


    @SuppressWarnings("unchecked")
    public boolean canConvert(Class type)
    {
        return UnrecognizedClass.class.equals(type);
    }

}
