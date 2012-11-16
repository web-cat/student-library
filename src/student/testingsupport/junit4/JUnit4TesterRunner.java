/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2011 Virginia Tech
 |
 |  This file is part of the Student-Library.
 |
 |  The Student-Library is free software; you can redistribute it and/or
 |  modify it under the terms of the GNU Lesser General Public License as
 |  published by the Free Software Foundation; either version 3 of the
 |  License, or (at your option) any later version.
 |
 |  The Student-Library is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU Lesser General Public License for more details.
 |
 |  You should have received a copy of the GNU Lesser General Public License
 |  along with the Student-Library; if not, see <http://www.gnu.org/licenses/>.
\*==========================================================================*/

package student.testingsupport.junit4;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.model.MultipleFailureException;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.MethodRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import student.testingsupport.junit4.AdaptiveTimeout;
import student.testingsupport.junit4.RunTestMethodWrapper;
import tester.Printer;
import tester.TestMethod;
import tester.Tester;
import tester.junit.TesterMethodResult;
import tester.junit.TesterMethodResults;

//-------------------------------------------------------------------------
/**
 * A custom JUnit runner which uses reflection to run both JUnit3 as well as
 * JUnit4 tests. The usefulness of this is that it can be used with a
 * {@code @RunWith} annotation in a parent class, and the resulting subclasses
 * can be written as if they are JUnit3 tests, but advanced users can use
 * annotations as well, and any functionality dictated by the superclass, for
 * instance {@code @Rule} annotations, will be applied to the children as
 * well.
 *
 * It also looks for JUnit3 setUp() and tearDown() methods and performs them
 * as if they are JUnit4 {@code @Before}s and {@code @After}s.
 *
 * @author Craig Estep
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class JUnit4TesterRunner
    extends BlockJUnit4ClassRunner
{
    //~ Instance/static variables .............................................

	private List<FrameworkMethod> befores      = null;
	private boolean         junit3methodsAdded = false;
	private boolean         junit3aftersAdded  = false;
	private boolean         testerDetected     = false;
	private boolean         testerFull         = false;
	private boolean         testerPrintAll     = false;
	private Object          cachedTest         = null;
	private JUnit4Tester    cachedTester       = null;
	private FrameworkMethod testerMethodHook   = null;

	private static final String PROPERTY_PREFIX =
        JUnit4TesterRunner.class.getName();
    private static final String FORCE_ADAPTIVE_TIMEOUT =
        PROPERTY_PREFIX + ".force.AdaptiveTimeout";
    private static final String TESTER_FULL = PROPERTY_PREFIX + ".full";
    private static final String TESTER_PRINT_ALL =
        PROPERTY_PREFIX + ".printall";


	//~ Constructors ..........................................................

    // ----------------------------------------------------------
	/**
	 * Creates a JUnitMixRunner to run {@code klass}
	 *
	 * @param  klass The test class to run
	 * @throws InitializationError if the test class is malformed.
	 */
	public JUnit4TesterRunner(Class<?> klass)
	    throws InitializationError
	{
		super(klass);
	}


	//~ Methods ...............................................................

    // ----------------------------------------------------------
	/**
	 * Returns a {@link Statement}: run all non-overridden {@code @Before}
	 * methods on this class and superclasses, as well as any JUnit3 setUp
	 * methods, before running {@code next}; if any throws an Exception, stop
	 * execution and pass the exception on.
	 *
	 * Note that in BlockJUnit4ClassRunner this method is deprecated.
	 */
	@Override
	protected Statement withBefores(
	    FrameworkMethod method, Object target, Statement statement)
	{
		List<FrameworkMethod> annotatedBefores =
		    getTestClass().getAnnotatedMethods(Before.class);

		if (befores != annotatedBefores)
		{
            befores = annotatedBefores;
			Method[] methods =
			    getTestClass().getJavaClass().getDeclaredMethods();
			for (final Method m : methods)
			{
			    // Need to check for correct signature
			    // Need to ensure it isn't annotated as @Before already
				if (m.getName().equals("setUp")
                    && m.getAnnotation(Before.class) == null
				    && m.getParameterTypes().length == 0
				    && !Modifier.isPrivate(m.getModifiers()))
				{
				    ensureIsAccessible(m);
					FrameworkMethod fm = new FrameworkMethod(m);
					// add at the end, so it will be executed last, after
					// all other @Before methods
					befores.add(fm);
				}
			}
		}

		return befores.isEmpty()
		    ? statement
		    : new RunBefores(statement, befores, target);
	}


    // ----------------------------------------------------------
	/**
	 * Returns a {@link Statement}: run all non-overridden {@code @After}
	 * methods, as well as any JUnit3 tearDown methods, on this class and
	 * superclasses before running {@code next}; all After methods are always
	 * executed: exceptions thrown by previous steps are combined, if
	 * necessary, with exceptions from After methods into a
	 * {@link MultipleFailureException}.
	 *
	 * Note that in BlockJUnit4ClassRunner this method is deprecated.
	 */
	@Override
	protected Statement withAfters(
	    FrameworkMethod method, Object target, Statement statement)
	{
		List<FrameworkMethod> afters =
		    getTestClass().getAnnotatedMethods(After.class);

		if (!junit3aftersAdded)
		{
			Method[] methods = getTestClass().getJavaClass().getMethods();
			for (final Method m : methods)
			{
                // Need to check for correct signature
                // Need to ensure it isn't annotated as @After already
				if (m.getName().equals("tearDown")
                    && m.getAnnotation(After.class) == null
                    && m.getParameterTypes().length == 0
                    && !Modifier.isPrivate(m.getModifiers()))
				{
				    ensureIsAccessible(m);
					FrameworkMethod fm = new FrameworkMethod(m);
					// Add at position zero, so it will be executed first,
					// before all other @After methods
					afters.add(0, fm);
				}
			}
			junit3aftersAdded = true;
		}

		return afters.isEmpty()
		    ? statement
		    : new RunAfters(statement, afters, target);
	}


    // ----------------------------------------------------------
	public void testerBeforeHook()
	    throws Exception
	{
	    if (testerDetected)
	    {
	        testerFull = System.getProperty(
	            TESTER_FULL, "").matches("(?i)true|yes|on|[1-9][0-9]*");
	        testerPrintAll = System.getProperty(
                TESTER_PRINT_ALL, "")
                .matches("(?i)true|yes|on|[1-9][0-9]*");
	        getTester().prepareToRunAnyTests(
	            getTest(), testerFull, testerPrintAll);
	    }
	}


	// ----------------------------------------------------------
	@Override
	protected Statement withBeforeClasses(Statement statement)
	{
	    try
	    {
	        return new RunBefores(
	            super.withBeforeClasses(statement),
	            Arrays.asList(new FrameworkMethod(JUnit4TesterRunner.class
	                .getDeclaredMethod("testerBeforeHook"))),
	            this);
	    }
	    catch (Exception e)
	    {
	        throw new RuntimeException(e);
	    }
	}


    // ----------------------------------------------------------
	public void testerMethodHook(Method m)
	    throws Throwable
	{
        if (testerDetected)
        {
            getTester().runOneTestMethod(
                getTest(), m, testerFull, testerPrintAll);
        }
        else
        {
            new FrameworkMethod(m).invokeExplosively(getTest());
        }
	}


    // ----------------------------------------------------------
    private FrameworkMethod getTesterMethodHook()
    {
        if (testerMethodHook == null)
        {
            try
            {
                testerMethodHook = new FrameworkMethod(
                    JUnit4TesterRunner.class.getDeclaredMethod(
                        "testerMethodHook", Method.class));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        return testerMethodHook;
    }


    // ----------------------------------------------------------
    public void testerAfterHook()
        throws Exception
    {
        if (testerDetected)
        {
            getTester().finishRunningTests(testerFull, testerPrintAll);
        }
    }


    // ----------------------------------------------------------
	@Override
	protected Statement withAfterClasses(Statement statement)
	{
        try
        {
            return new RunAfters(
                super.withAfterClasses(statement),
                Arrays.asList(new FrameworkMethod(JUnit4TesterRunner.class
                    .getDeclaredMethod("testerAfterHook"))),
                this);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
	}


	// ----------------------------------------------------------
	/**
	 * Gathers all JUnit4 and JUnit3 test methods from this class and its
	 * superclasses.
	 *
	 * @return the list of test methods to run.
	 */
	@Override
	protected List<FrameworkMethod> getChildren()
	{
		List<FrameworkMethod> children = super.computeTestMethods();

		if (!junit3methodsAdded)
		{
			Method[] methods =
			    getTestClass().getJavaClass().getDeclaredMethods();
			for (final Method method : methods)
			{
				if (method.getName().startsWith("test")
				    && method.getParameterTypes().length == 0
				    && Modifier.isPublic(method.getModifiers()))
				{
				    // This is a JUnit3-style test method
                    FrameworkMethod fm = new FrameworkMethod(method);
                    // Make sure it isn't already in the list
                    if (!children.contains(fm))
                    {
                        children.add(fm);
                    }
				}
				else if (method.getAnnotation(TestMethod.class) != null
				    || (method.getName().startsWith("test")
				        && method.getParameterTypes().length == 1
				        && Tester.class.equals(method.getParameterTypes()[0])
				        && !Modifier.isPrivate(method.getModifiers())))
				{
				    // Then it is an NU Tester-style method
				    testerDetected = true;
				    ensureIsAccessible(method);
				    children.add(new FrameworkMethod(method));
				}
			}
			junit3methodsAdded = true;
		}

		return children;
	}


    // ----------------------------------------------------------
	/**
     * Adds to {@code errors} a throwable for each problem noted with the
     * test class (available from {@link #getTestClass()}).  Default
     * implementation adds an error for each method annotated with
     * {@code @BeforeClass} or {@code @AfterClass} that is not
     * {@code public static void} with no arguments.
     */
    protected void collectInitializationErrors(List<Throwable> errors)
    {
        super.collectInitializationErrors(errors);
        for (int i = 0; i < errors.size(); i++)
        {
            if (errors.get(i).getMessage().equals("No runnable methods"))
            {
                errors.remove(i);
                break;
            }
        }
    }


    // ----------------------------------------------------------
    @SuppressWarnings("deprecation")
    protected Statement methodBlock(FrameworkMethod method)
    {
        Object test;
        try
        {
            test = new ReflectiveCallable() {
                @Override
                protected Object runReflectiveCall() throws Throwable
                {
                    return getTest();
                }
            }.run();
        }
        catch (Throwable e)
        {
            return new Fail(e);
        }

        Statement statement = methodInvoker(method, test);
        statement = possiblyExpectingExceptions(method, test, statement);
        statement = withPotentialTimeout(method, test, statement);
        statement = withBefores(method, test, statement);
        statement = withAfters(method, test, statement);
        statement = withRules(method, test, statement);
        statement = new RunTestMethodWrapper(statement, test);
        return statement;
    }


    // ----------------------------------------------------------
    /**
     * This method was declared private in the parent class, when it should
     * have been protected (sigh)--it takes a {@link Statement}, and decorates
     * it with all the {@link MethodRule}s in the test class.
     * @param method The test method itself.
     * @param target The instance of the test class, on which the method will
     *               be called.
     * @param statement The decorated, executable representation of the method
     *               call that has all supplementary behaviors added on.
     * @return A new statement that represents the incoming statement with
     * any method rules added to it.
     */
    protected Statement withRules(
        FrameworkMethod method, Object target, Statement statement)
    {
        Statement result = statement;
        // Use system property to suppress forcing of adaptive timeouts
        boolean foundAdaptiveTimeout = !System.getProperty(
            FORCE_ADAPTIVE_TIMEOUT, "true")
            .matches("(?i)true|yes|on|[1-9][0-9]*");
        for (MethodRule each : getTestClass()
            .getAnnotatedFieldValues(target, Rule.class, MethodRule.class))
        {
            if (each instanceof AdaptiveTimeout)
            {
                foundAdaptiveTimeout = true;
            }
            result = each.apply(result, method, target);
        }

        // force adaptive timeout
        if (!foundAdaptiveTimeout)
        {
            result = new AdaptiveTimeout().apply(result, method, target);
        }
        return result;
    }


    // ----------------------------------------------------------
    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test)
    {
        Class<?>[] params = method.getMethod().getParameterTypes();
        if (params.length == 1 && Tester.class.equals(params[0]))
        {
            return new InvokeTesterMethod(
                getTesterMethodHook(), this, method.getMethod());
        }
        else
        {
            return super.methodInvoker(method, test);
        }
    }


    // ----------------------------------------------------------
    // Needed, since can't replace inherited runLeaf() method
    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier)
    {
        Description description = describeChild(method);
        if (method.getAnnotation(Ignore.class) != null)
        {
            notifier.fireTestIgnored(description);
        }
        else
        {
            // Only change
            runMyLeaf(methodBlock(method), description, notifier, method);
        }
    }


    // ----------------------------------------------------------
    // Can't replace inherited runLeaf() method!
    protected void runMyLeaf(Statement statement, Description description,
        RunNotifier notifier, FrameworkMethod method)
    {
        boolean wantsFinish = true;
        EachTestNotifier eachNotifier =
            new EachTestNotifier(notifier, description);
        eachNotifier.fireTestStarted();
        try
        {
            statement.evaluate();
        }
        catch (TesterMethodResults results)
        {
            // Decode bundled output from all the tester methods included
            for (TesterMethodResult result : results.getResults())
            {
                if (!wantsFinish)
                {
                    description = describeChild(method);
                    eachNotifier = new EachTestNotifier(notifier, description);
                    eachNotifier.fireTestStarted();
                }

                if (!result.didSucceed())
                {
                    StringBuilder builder = new StringBuilder();
                    result.appendDescriptionTo(builder);
                    Throwable resultEx = result.getException();
                    if (resultEx == null)
                    {
                        // should never happen
                        resultEx = new Exception(builder.toString());
                        resultEx.setStackTrace(new StackTraceElement[] {
                            new StackTraceElement(
                                result.getExampleClass().getName(),
                                result.getExampleMethod().getName(),
                                null,
                                -1)});
                    }
                    else if (resultEx instanceof tester.ErrorReport)
                    {
//                        resultEx = new ErrorReport(builder.toString());
                        resultEx = new AssertionError(builder.toString());
                        resultEx.setStackTrace(
                            result.getException().getStackTrace());
                    }
                    eachNotifier.addFailure(resultEx);
                }
                eachNotifier.fireTestFinished();
                wantsFinish = false;
            }
        }
        catch (AssumptionViolatedException e)
        {
            eachNotifier.addFailedAssumption(e);
        }
        catch (Throwable e)
        {
            eachNotifier.addFailure(e);
        }
        finally
        {
            if (wantsFinish)
            {
                eachNotifier.fireTestFinished();
            }
        }
    }


    // ----------------------------------------------------------
    protected Object getTest()
        throws Exception
    {
        if (testerDetected) // should only use one instance of test class
        {
            if (cachedTest == null)
            {
                cachedTest = getTestClass().getJavaClass()
                    .getDeclaredConstructor().newInstance();
            }
            return cachedTest;
        }
        else
        {
            return super.createTest();
        }
    }


    // ----------------------------------------------------------
    protected JUnit4Tester createTester()
    {
        return new JUnit4Tester();
    }


    // ----------------------------------------------------------
    protected JUnit4Tester getTester()
    {
        if (cachedTester == null)
        {
            cachedTester = createTester();
        }
        return cachedTester;
    }


    // ----------------------------------------------------------
    protected class JUnit4Tester extends Tester
    {
        // ----------------------------------------------------------
        public void prepareToRunAnyTests(
            Object f, boolean full, boolean printall)
        {
            this.numberOfTests = 0; // number of tests run
            this.failed = false; // any tests failed?

            System.out.println();
            System.out.println();
            System.out.println("-----------------------------------");
            System.out.println("Tests for the class: "
                + f.getClass().getName());
            System.out.println(this.version);
            // print the name of the 'Examples' class
            System.out.println("Tests defined in the class: "
                + f.getClass().getName() + ":");
            System.out.println("---------------------------");

            if (printall) {
              // pretty-print the 'Examples' class data when desired
              System.out.println(f.getClass().getName()+ ":");
              System.out.println("---------------");
              System.out.println(Printer.produceString(f));
              System.out.println("---------------");
            }
        }


        // ----------------------------------------------------------
        public void runOneTestMethod(
            Object f, Method testMethod, boolean full, boolean printall)
            throws Throwable
        {
            this.testResults = new ArrayList<TesterMethodResult>();
            if (testMethod != null) {
                try
                {
                    testMethod.invoke(f, this);
                }
                // catch all exceptions
                catch (Throwable e)
                {
                    if (e instanceof InvocationTargetException)
                    {
                        e = e.getCause();
                    }

                    failed = true;
                    report("An exception occurred while executing the "
                        + "method " + testMethod.getName() + "()",
                        false, null, e);
                }
                if (testResults.size() > 0)
                {
                    throw new TesterMethodResults(testResults);
                }
            }
        }


        // ----------------------------------------------------------
        public void finishRunningTests(boolean full, boolean printall)
        {
            if (full)
            {
                this.fullTestReport();
            }
            else
            {
                this.testReport();
            }
            done(failed);
        }
    }


    // ----------------------------------------------------------
    /**
     * Ensures that the given object is accessible by using setAccessible()
     * if necessary.
     *
     * This method is explicitly package-private, so that it is available
     * only in this package and not to client code outside this package.
     *
     * @param object The object to check/modify.
     */
    private void ensureIsAccessible(final AccessibleObject object)
    {
        if (!object.isAccessible())
        {
            // execute this as a privileged action, in case a restrictive
            // security policy is in place to limit the capabilities of
            // non-library code.
             AccessController.doPrivileged(
                 new PrivilegedAction<Object>()
                 {
                     public Object run()
                     {
                         object.setAccessible(true);
                         return null;
                     }
                 });
        }
    }
}
