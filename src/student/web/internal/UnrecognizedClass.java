package student.web.internal;

public class UnrecognizedClass
{
    private UnrecognizedClass()
    {
        // provided just to hide the constructor
    }

    public static UnrecognizedClass getInstance()
    {
        return INSTANCE;
    }

    private static final UnrecognizedClass INSTANCE = new UnrecognizedClass();
}
