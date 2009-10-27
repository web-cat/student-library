package student.web;

// -------------------------------------------------------------------------
/**
 *  This class represents a main program that takes a command-line
 *  argument that names a {@link WebBotTask}; it creates an instance
 *  of the named task and then runs it.  If multiple command-line
 *  arguments are given, they are all interpreted as {@link WebBotTask}
 *  names and are created and executed in turn.
 *  <p>
 *  As an example, to run a <code>RobotTask</code> called
 *  <i><code>MyRobotTest</code></i>, use the following command:
 *  </p>
 *  <pre>
 *    java -cp C:/BlueJ/lib/userlib/student.jar student.RunRobotTask MyRobotTest
 *  </pre>
 *  <p>
 *  Note that this example presumes that <code>student.jar</code> is
 *  located at the specified path (based on a default BlueJ installation
 *  using the course-provided installer) and <code>MyRobotTest.class</code>
 *  is in the current directory.
 *  Use relative or absolute path names to refer to these files if they are
 *  somewhere else.
 *  </p>
 *
 *  @version 2003.08.20
 *  @author Stephen Edwards
 */
public class RunWebBotTask
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Create a new RunRobotTask object.
     */
    public RunWebBotTask()
    {
        // Nothing to do
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * This main method takes the name of a RobotTask class as an
     * argument, and it executes the corresponding task.  At least one
     * argument is required.  If multiple arguments are given, they are
     * all interpreted as RobotTask class names and all are executed in
     * the order specified.
     *
     * @param args the argument list from the command line
     */
    public static void main( String[] args )
    {
        if ( args == null || args.length == 0 )
        {
            System.out.println(
            "Please provide the name of a RobotTask class to execute." );
        }
        else
        {
            ClassLoader loader = RunWebBotTask.class.getClassLoader();
            for ( int i = 0; i < args.length ; i ++  )
            {
                try
                {
                    // Create an instance of the named task
                    WebBotTask task =
                    (WebBotTask)loader.loadClass( args[i] ).newInstance();
                    // Now run it
                    task.task();
                }
                catch ( Exception e )
                {
                    System.out.println( "Exception loading RobotTask '"
                            + args[i] + "':" );
                    e.printStackTrace();
                }
            }
        }
    }

}
