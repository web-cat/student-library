package student.web.internal;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Field;

public class FlexibleXStream
    extends XStream
{
    public FlexibleXStream()
    {
        super();
    }

    @Override
    protected void setupAliases()
    {
        // XStream doesn't provide an easy way to hook into the
        // built-in mapper creation sequence.  Also, the method that
        // builds the default sequence is private, so it can't be easily
        // overloaded.
        //
        // Adding extra steps in a subclass constructor also doesn't work,
        // since later actions--specifically, converter setup--uses the
        // generated mapper before you have a chance to modify it.
        //
        // setupAliases() can be overridden, however, it is is called
        // immediately after the default mapper is set up, so this
        // implementation has nothing to do with aliasing and is just being
        // exploited to inject a change to the default mapper sequence.
        //
        // It's either that, or reproduce the full default mapper sequence
        // and pass it into the constructor instead.

        Mapper mapper = new FlexibleMapper(getMapper());

        try
        {
            Field mapperField = XStream.class.getDeclaredField("mapper");

            mapperField.setAccessible(true);
            mapperField.set(this, mapper);
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }

        super.setupAliases();
    }

}
