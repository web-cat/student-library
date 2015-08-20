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

import java.io.InputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.collections.comparators.ComparatorChain;
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
	private AdaptiveTimeout adaptiveTimeout    = null;

	private static final String PROPERTY_PREFIX =
        JUnit4TesterRunner.class.getName();
    private static final String FORCE_ADAPTIVE_TIMEOUT =
        PROPERTY_PREFIX + ".force.AdaptiveTimeout";
    private static final String TESTER_FULL = PROPERTY_PREFIX + ".full";
    private static final String TESTER_PRINT_ALL =
        PROPERTY_PREFIX + ".printall";

    private static final FrameworkMethod OBJECT_HASH_CODE = objectHashCode();
    // Initialization for the value above
    private static FrameworkMethod objectHashCode()
    {
        try
        {
            return new FrameworkMethod(
                Object.class.getDeclaredMethod("hashCode"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }


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
    private Method find(Class<?> cls, String name, Class<?>... param_types)
    {
        while (cls != null)
        {
            try
            {
                return cls.getDeclaredMethod(name, param_types);
            }
            catch (NoSuchMethodException e)
            {
                // go around the loop again
            }
            cls = cls.getSuperclass();
        }
        return null;
    }


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
			Method setUp = find(getTestClass().getJavaClass(), "setUp");
			// Need to ensure it isn't annotated as @Before already
			if (setUp != null
			    && setUp.getAnnotation(Before.class) == null
			    && !Modifier.isPrivate(setUp.getModifiers()))
			{
				ensureIsAccessible(setUp);
				FrameworkMethod fm = new FrameworkMethod(setUp);
				// add at the end, so it will be executed last, after
				// all other @Before methods
				befores.add(fm);
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
			Method tearDown = find(getTestClass().getJavaClass(), "tearDown");
            // Need to ensure it isn't annotated as @After already
			if (tearDown != null
			    && tearDown.getAnnotation(After.class) == null
                && !Modifier.isPrivate(tearDown.getModifiers()))
			{
				ensureIsAccessible(tearDown);
				FrameworkMethod fm = new FrameworkMethod(tearDown);
				// Add at position zero, so it will be executed first,
				// before all other @After methods
				afters.add(0, fm);
			}
			if (!student.TestCase.class.isAssignableFrom(
			    getTestClass().getJavaClass()))
			{
			    try
			    {
			        afters.add(new FrameworkMethod(null)
			        {
			            @Override
			            public Object invokeExplosively(
			                Object target,
			                Object... params)
			                throws Throwable
			            {
			                if (adaptiveTimeout != null)
			                {
			                    adaptiveTimeout.logTestMethod(true);
			                }
			                return null;
			            }
			        });
			    }
			    catch (Exception e)
			    {
			        throw new RuntimeException(e);
			    }
	        }
			junit3aftersAdded = true;
		}

		return afters.isEmpty()
		    ? statement
		    : new RunAfters(statement, afters, target);
	}


    // ----------------------------------------------------------
	/**
	 * An internal hook that is executed automatically as a @BeforeClass
	 * action.  It provides pre-test features for the NU Tester, if it
	 * is detected for the current test, and also installs the
	 * {@link student.testingsupport.ExitPreventingSecurityManager} for all
	 * test classes.
	 * @throws Exception If {@link #getTest()} fails.
	 */
	public void testerBeforeHook()
	    throws Exception
	{
        if (!student.TestCase.class.isAssignableFrom(
            getTestClass().getJavaClass()))
        {
            student.testingsupport.ExitPreventingSecurityManager.install();
        }
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
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateConstructor(List<Throwable> errors)
	{
	    Class<?> testClass = getTestClass().getJavaClass();
	    if (junit.framework.TestCase.class.isAssignableFrom(testClass))
	    {
	        boolean found = false;
	        for (Constructor<?> ctor : testClass.getConstructors())
	        {
	            Class<?>[] params = ctor.getParameterTypes();
	            if (params.length == 0
	                || (params.length == 1
	                    && String.class.equals(params[0])))
	            {
	                found = true;
	            }
	        }
	        if (!found)
	        {
	            errors.add(new Exception("Test class does not have an "
	                + "appropriate public constructor."));
	        }
	    }
	    else
	    {
	        super.validateConstructor(errors);
	    }
	}


    // ----------------------------------------------------------
	@Override
	protected void validateOnlyOneConstructor(List<Throwable> errors)
	{
        Class<?> testClass = getTestClass().getJavaClass();
        if (!Modifier.isPublic(testClass.getModifiers())
            && !Modifier.isProtected(testClass.getModifiers())
            && !Modifier.isPrivate(testClass.getModifiers()))
        {
            int count = 0;
            for (Constructor<?> c : testClass.getDeclaredConstructors())
            {
                if (Modifier.isPublic(c.getModifiers())
                    ||
                    (!Modifier.isPublic(c.getModifiers())
                     && !Modifier.isProtected(c.getModifiers())
                     && !Modifier.isPrivate(c.getModifiers())))
                {
                    count++;
                }
            }
            if (count != 1)
            {
                errors.add(new Exception(
                    "Test class should have exactly one public constructor"));
            }
        }
        else
        {
            super.validateOnlyOneConstructor(errors);
        }
	}


    // ----------------------------------------------------------
	@Override
	protected void validateZeroArgConstructor(List<Throwable> errors)
	{
        Class<?> testClass = getTestClass().getJavaClass();
        if (!Modifier.isPublic(testClass.getModifiers())
            && !Modifier.isProtected(testClass.getModifiers())
            && !Modifier.isPrivate(testClass.getModifiers()))
        {
            for (Constructor<?> c : testClass.getDeclaredConstructors())
            {
                if (Modifier.isPublic(c.getModifiers())
                    ||
                    (!Modifier.isPublic(c.getModifiers())
                     && !Modifier.isProtected(c.getModifiers())
                     && !Modifier.isPrivate(c.getModifiers())))
                {
                    if (c.getParameterTypes().length > 0)
                    {
                        errors.add(new Exception("Test class should have "
                            + "exactly one public zero-argument constructor"));
                        break;
                    }
                }
            }
        }
        else
        {
            super.validateZeroArgConstructor(errors);
        }
	}


	// ----------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
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
    /**
     * An internal hook that is used to conditionally execute test
     * methods either the "normal" JUnit way, or using the NU Tester
     * infrastructure, if NU Tester use is detected in the test class.
     * @param m The test method to invoke.
     * @throws Throwable If any error arises invoking the test method.
     */
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
    /**
     * An internal hook that is executed automatically as an @AfterClass
     * action.  It provides post-test features for the NU Tester, if it
     * is detected for the current test, and also un-installs the
     * {@link student.testingsupport.ExitPreventingSecurityManager} for all
     * test classes.
     */
    public void testerAfterHook()
    {
        if (adaptiveTimeout != null)
        {
            adaptiveTimeout.appendStatsToFile();
        }
        if (!student.TestCase.class.isAssignableFrom(
            getTestClass().getJavaClass()))
        {
            student.testingsupport.ExitPreventingSecurityManager.uninstall();
        }
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
		    // First, check public methods
			Method[] methods =
			    getTestClass().getJavaClass().getMethods();
			for (final Method method : methods)
			{
				if (method.getName().startsWith("test")
				    && method.getParameterTypes().length == 0
				    && method.getAnnotation(org.junit.Test.class) == null)
				{
				    // This is a JUnit3-style test method
                    FrameworkMethod fm = new FrameworkMethod(method);
                    // Make sure it isn't already in the list
                    if (!children.contains(fm))
                    {
                        children.add(fm);
                    }
				}
				else if (method.getAnnotation(tester.TestMethod.class) != null
				    || (method.getName().startsWith("test")
				        && method.getParameterTypes().length == 1
				        && Tester.class.equals(method.getParameterTypes()[0])))
				{
				    // Then it is an NU Tester-style method
				    testerDetected = true;
				    children.add(new FrameworkMethod(method));
				}
			}

			// Now check for non-public tester-style methods
            methods = getTestClass().getJavaClass().getDeclaredMethods();
            // Note that the line above does not perform an inheritance-based
            // search and will only find methods declared in the most
            // specific subclass for this test class.
            for (final Method method : methods)
            {
                if (method.getAnnotation(tester.TestMethod.class) != null
                    || (method.getName().startsWith("test")
                        && method.getParameterTypes().length == 1
                        && Tester.class.equals(method.getParameterTypes()[0])
                        && !Modifier.isPublic(method.getModifiers())
                        && !Modifier.isPrivate(method.getModifiers())))
                {
                    // Then it is an NU Tester-style method
                    testerDetected = true;
                    ensureIsAccessible(method);
                    children.add(new FrameworkMethod(method));
                }
            }

            // If no tests, and it looks like a tester-style test, then
            // treat object construction (including initializing fields)
            // as a single test case itself.
			if (children.size() == 0
			    && !junit.framework.TestCase.class.isAssignableFrom(
			        getTestClass().getJavaClass()))
			{
			    children.add(OBJECT_HASH_CODE);
			    testerDetected = true;
			}
			junit3methodsAdded = true;

			Collections.sort(children, new MethodComparator());
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
            adaptiveTimeout = new AdaptiveTimeout();
            result = adaptiveTimeout.apply(result, method, target);
        }
        else
        {
            adaptiveTimeout = null;
        }
        return result;
    }


    // ----------------------------------------------------------
    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test)
    {
        if (method == OBJECT_HASH_CODE)
        {
            return new TesterPrintInstance(test);
        }

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
    /**
     * Runs a single test (replaces a method in the base class that is
     * declared private and can't be overridden).
     * @param statement   The test to run.
     * @param description A description of this test.
     * @param notifier    The notifier to use
     * @param method      The test method bundled in the statement.
     */
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
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private int ctor_time_limit = 10;
    private int ctor_timeout_count = 0;
    private Object getTest(Callable<Object> ctor)
        throws Exception
    {
        try
        {
            Future<Object> task = exec.submit(ctor);
            return task.get(ctor_time_limit, TimeUnit.SECONDS);
        }
        catch (TimeoutException e)
        {
            int limit = ctor_time_limit;
            ctor_timeout_count++;
            if (ctor_timeout_count > 1 && ctor_time_limit > 1)
            {
                // Ramp limit down, if necessary
                ctor_time_limit /= 2;
            }
            throw new AssertionError("constructor " + ctor
                + " took longer than " + limit
                + " second"
                + (limit == 1 ? "" : "s" )
                + " to execute.");
        }
        catch (ExecutionException e)
        {
            if (e.getCause() != null
                && e.getCause() instanceof Exception)
            {
                throw (Exception)e.getCause();
            }
            else
            {
                throw e;
            }
        }
    }


    // ----------------------------------------------------------
    private Object getTest(final Constructor<?> ctor, final Object... args)
        throws Exception
    {
        return getTest(new Callable<Object>() {
                public Object call()
                    throws Exception
                {
                    return ctor.newInstance(args);
                }
                @Override
                public String toString()
                {
                    return ctor == null ? "null" : ctor.toString();
                }
            });
    }


    // ----------------------------------------------------------
    /**
     * Get the test class for this runner, taking NU Tester considerations
     * into account.
     * @return The test object.
     * @throws Exception If any of the underlying JUnit methods fail.
     */
    protected Object getTest()
        throws Exception
    {
        if (testerDetected) // should only use one instance of test class
        {
            if (cachedTest == null)
            {
                Class<?> testClass = getTestClass().getJavaClass();
                if (junit.framework.TestCase.class.isAssignableFrom(testClass))
                {
                    try
                    {
                        cachedTest = getTest(
                            testClass.getConstructor(String.class),
                            testClass.getName());
                    }
                    catch (NoSuchMethodException e)
                    {
                        cachedTest = getTest(testClass.getConstructor());
                    }
                }
                else
                {
                    Constructor<?> c = testClass.getDeclaredConstructor();
                    ensureIsAccessible(c);
                    cachedTest = getTest(c);
                }
            }
            return cachedTest;
        }
        else
        {
            Class<?> testClass = getTestClass().getJavaClass();
            if (junit.framework.TestCase.class.isAssignableFrom(testClass))
            {
                try
                {
                    return getTest(testClass.getConstructor(String.class),
                        testClass.getName());
                }
                catch (NoSuchMethodException e)
                {
                    return getTest(testClass.getConstructor());
                }
            }
            else
            {
//                return getTest(new Callable<Object>() {
//                    public Object call()
//                        throws Exception
//                    {
                        return JUnit4TesterRunner.super.createTest();
//                    }
//                });
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * Factor method to create a new JUnit4Tester object.
     * @return a new Tester object for NU Tester-style use.
     */
    protected JUnit4Tester createTester()
    {
        return new JUnit4Tester();
    }


    // ----------------------------------------------------------
    /**
     * Get the JUnit4Tester object used to run NU Tester-style tests
     * (will call the factory method {@link #createTester()} to create
     * one, if needed, then cache it).
     * @return a new Tester object for NU Tester-style use.
     */
    protected JUnit4Tester getTester()
    {
        if (cachedTester == null)
        {
            cachedTester = createTester();
        }
        return cachedTester;
    }


    // ----------------------------------------------------------
    /**
     * A customized version of {@link Tester} that will run NU Tester-style
     * tests within JUnit 4's infrastructure.
     */
    protected class JUnit4Tester extends Tester
    {
        // ----------------------------------------------------------
        /**
         * Pre-test setup for running tests, which basically prints the
         * header information for this test object.
         * @param f    The object containing the tests to run.
         * @param full Unused.
         * @param printall Pretty-print the class data from the test class
         *                 object before any tests are run.
         */
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
        /**
         * Run a single NU Tester-style test method.
         * @param f          The object containing the tests to run.
         * @param testMethod The single test to run.
         * @param full       Unused.
         * @param printall   Unused.
         * @throws Throwable If a failure occurs.
         */
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
        /**
         * Post-test processing after running tests, which basically prints the
         * report of results.
         * @param full     Unused.
         * @param printall Unused.
         */
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


    // ----------------------------------------------------------
    private static class MethodComparator
        implements Comparator<FrameworkMethod>
    {
        private static final char[] METHOD_SEPARATORS = {1, 7};
        private static final Map<Class<?>, Integer> classDepth =
            new HashMap<Class<?>, Integer>();
        private static final Map<String, Integer> methodPosition =
            new HashMap<String, Integer>();


        // ----------------------------------------------------------
        public MethodComparator()
        {
            // No initialization needed
        }


        // ----------------------------------------------------------
        public int compare(FrameworkMethod o1, FrameworkMethod o2)
        {
            Method method1 = o1.getMethod();
            Method method2 = o2.getMethod();
            int left = classDepth(method1.getDeclaringClass());
            int right = classDepth(method2.getDeclaringClass());
            if (left == right)
            {
                left = methodPosition(method1);
                right = methodPosition(method2);
                if (left == right)
                {
                    int result =
                        method1.getName().compareTo(method2.getName());
                    if (result == 0)
                    {
                        result = method1.getDeclaringClass().getName()
                            .compareTo(method2.getDeclaringClass().getName());
                    }
                    return result;
                }
            }
            return left - right;
        }


        // ----------------------------------------------------------
        private int classDepth(Class<?> declaringClass)
        {
            Integer depth = classDepth.get(declaringClass);
            if (depth == null)
            {
                Class<?> s = declaringClass.getSuperclass();
                if (s == null || Object.class.equals(s))
                {
                    depth = 0;
                }
                else
                {
                    depth = classDepth(s) + 1;
                }
                classDepth.put(declaringClass, depth);
            }
            return depth;
        }


        // ----------------------------------------------------------
        private int methodPosition(Method method)
        {
            String methodFQ = method.getDeclaringClass().getName() + "."
                + method.getName();
            Integer pos = methodPosition.get(methodFQ);
            if (pos == null)
            {
                Class<?> c = method.getDeclaringClass();
                InputStream in = c.getResourceAsStream(
                    c.getName().replace('.', '/') + ".class");
                StringBuffer buff = new StringBuffer();
                try
                {
                    byte[] buf = new byte[4096];
                    int count = in.read(buf);
                    while (count > 0)
                    {
                        // Don't want any UTF-8 encoding errors trying
                        // to read in the bytecode as a string, since
                        // bytecode isn't UTF-8 compatible, so using
                        // Windows encoding instead, since it is 8-bit safe.
                        // However, this will "distort" UTF-8 names that
                        // use characters outside of ASCII, so we'll have
                        // to handle that later.
                        String contents =
                            new String(buf, 0, count, "Windows-1252");
                        buff.append(contents);
                        count = in.read(buf);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    try
                    {
                        in.close();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                String contents = buff.toString();
                for (Method m : c.getDeclaredMethods())
                {
                    String name = c.getName();
                    // Try munging the name from proper UTF-8 encoding to
                    // be re-interpreted/munged as Windows-1252.  This
                    // will mess up the raw chars in the string, but will
                    // force the string to match the munged contents
                    // read in using the incorrect Windows-1252 encoding
                    // during the file read process.
                    try
                    {
                        byte[] bytes = name.getBytes("UTF-8");
                        name = new String(bytes, "Windows-1252");
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    methodPosition.put(c.getName() + "." + m.getName(),
                        contents.indexOf(m.getName()));
                }
                pos = methodPosition.get(methodFQ);
                if (pos == null)
                {
                    pos = 0;
                }
            }
            return pos;
        }
    }
}
