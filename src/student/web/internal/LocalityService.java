package student.web.internal;

import student.testingsupport.SystemIOUtilities;


public abstract class LocalityService
{
    public static ApplicationSupportStrategy getSupportStrategy()
    {
        ApplicationSupportStrategy support = null;
        if ( SystemIOUtilities.isOnServer() )
        {
            try
            {
                Class<?> strategyClass = Class.forName( "student.web.internal.ServerApplicationSupportStrategy" );
                support = (ApplicationSupportStrategy)strategyClass.newInstance();
            }
            catch ( Exception e )
            {
                System.out.println( "Error initializing application support strategy" );
                e.printStackTrace();
            }
        }
        if ( support == null )
        {
            support = new LocalApplicationSupportStrategy();
        }
        return support;
    }
}
