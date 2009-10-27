package student.web;

//-------------------------------------------------------------------------
/**
 *  This class defines the common interface for all robot task objects.
 *  It defines two methods.  A concrete robot task class should define
 *  the {@link #task()} method to create and initialize the desired robot(s)
 *  and command them to behave.  Also, the concrete task class should
 *  define {@link #getRobot()} to return the robot that is used in the task.
 *  <p>
 *  A <code>WebBotTask</code> can be executed from the
 *  command-line using {@link RunWebBotTask}.
 *  </p>
 *  @version 2003.08.20
 *  @author Stephen Edwards
 */
public interface WebBotTask
{
    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * This method encapsulates a sequence of instructions for
     * creating and commanding one or more robots (see {@link WebBot}).
     * Define it in any concrete class that implements this interface.
     */
    public void task();


    // ----------------------------------------------------------
    /**
     * This method provides access to the robot that carries out this
     * task.  In a concrete subclass implementing this interface, declare
     * your own instance variable to hold your robot and define this
     * method to return it.
     * @return The robot used by this task.
     */
    public WebBot getRobot();

}
