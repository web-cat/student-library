package student.testingsupport.junit4;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.internal.runners.model.MultipleFailureException;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * A custom JUnit runner which uses reflection to run both JUnit3 as well as
 * JUnit4 tests. The usefulness of this is that it can be used with a
 * {@code @RunWith} annotation in a parent class, and the resulting subclasses
 * can be written as if they are JUnit3 tests, but advanced users can use
 * annotations as well, and any functionality dictated by the superclass, for
 * instance {@code @Rule} annotations, will be applied to the children as well.
 *
 * It also looks for JUnit3 setUp() and tearDown() methods and performs them as
 * if they are JUnit4 {@code @Before}s and {@code @After}s.
 *
 * @author Craig Estep
 */
public class MixRunner extends BlockJUnit4ClassRunner {

	private boolean junit3beforesAdded = false;
	private boolean junit3methodsAdded = false;
	private boolean junit3aftersAdded = false;

	/**
	 * Creates a JUnitMixRunner to run {@code klass}
	 *
	 * @throws InitializationError
	 *             if the test class is malformed.
	 */
	public MixRunner(Class<?> klass) throws InitializationError {
		super(klass);
	}

	/**
	 * Returns a {@link Statement}: run all non-overridden {@code @Before}
	 * methods on this class and superclasses, as well as any JUnit3 setUp
	 * methods, before running {@code next}; if any throws an Exception, stop
	 * execution and pass the exception on.
	 *
	 * Note that in BlockJUnit4ClassRunner this method is deprecated.
	 */
	@Override
	protected Statement withBefores(FrameworkMethod method, Object target,
			Statement statement) {
		List<FrameworkMethod> befores = getTestClass().getAnnotatedMethods(
				Before.class);

		if (!junit3beforesAdded) {
			Method[] methods = getTestClass().getJavaClass().getMethods();
			for (Method m : methods) {
				if (m.getName().equals("setUp")) {
					FrameworkMethod fm = new FrameworkMethod(m);
					befores.add(fm);
				}
			}
			junit3aftersAdded = true;
		}

		return befores.isEmpty() ? statement : new RunBefores(statement,
				befores, target);
	}

	/**
	 * Returns a {@link Statement}: run all non-overridden {@code @After}
	 * methods, as well as any JUnit3 tearDown methods, on this class and
	 * superclasses before running {@code next}; all After methods are always
	 * executed: exceptions thrown by previous steps are combined, if necessary,
	 * with exceptions from After methods into a
	 * {@link MultipleFailureException}.
	 *
	 * Note that in BlockJUnit4ClassRunner this method is deprecated.
	 */
	@Override
	protected Statement withAfters(FrameworkMethod method, Object target,
			Statement statement) {
		List<FrameworkMethod> afters = getTestClass().getAnnotatedMethods(
				After.class);

		if (!junit3aftersAdded) {
			Method[] methods = getTestClass().getJavaClass().getMethods();
			for (Method m : methods) {
				if (m.getName().equals("tearDown")) {
					FrameworkMethod fm = new FrameworkMethod(m);
					afters.add(fm);
				}
			}
			junit3aftersAdded = true;
		}

		return afters.isEmpty() ? statement : new RunAfters(statement, afters,
				target);
	}

	/**
	 * Gathers all JUnit4 and JUnit3 test methods from this class and its
	 * superclasses.
	 *
	 * @return the list of test methods to run.
	 */
	@Override
	protected List<FrameworkMethod> getChildren() {
		List<FrameworkMethod> children = super.computeTestMethods();

		if (!junit3methodsAdded) {
			Method[] methods = getTestClass().getJavaClass().getMethods();
			for (Method method : methods) {
				FrameworkMethod fm = new FrameworkMethod(method);
				if (method.getName().startsWith("test")
						&& !children.contains(fm)) {
					children.add(fm);
				}
			}
			junit3methodsAdded = true;
		}

		return children;
	}
}
