package student.web.internal.converters;

public class Alias
{

    private String key;


    // Needed for persistence stuff DONT DELETE
    public Alias()
    {

    }


    public Alias( String key )
    {
        this.key = key;
    }


    public String getKey()
    {
        return key;
    }


    public Object _get_value_( String fieldName )
    {
        if ( fieldName.equals( "key" ) )
        {
            return key;
        }
        return null;
    }


    public void _write_field_( String fieldName, Object value )
    {
        if ( fieldName.equals( "key" ) )
            key = (String)value;
    }
}
