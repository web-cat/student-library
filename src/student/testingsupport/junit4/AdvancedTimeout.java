package student.testingsupport.junit4;

import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.rules.*;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * A custom MethodRule, following the example of Timeout, that supports both a
 * per-method timeout, and a per-class timeout. The smallest of whichever of
 * these remains is used for the next test method.
 *
 * NOTE: You must declare an instance of this timeout as a static field in the
 * test class you want it to apply to.
 *
 * @author Craig Estep
 */
public class AdvancedTimeout implements MethodRule {

	private int classwide;
	private int method;

	private static boolean useClasswideTimeout;
	private static boolean useMethodTimeout;

	private static long beforeTime;
	private static long afterTime;

	/**
	 * Creates a new timeout with the specified timeout options. If a value
	 * entered is not positive, that timeout condition will be ignored. The
	 * method timeout is a flat number applied to every method. The classwide
	 * timeout is a countdown that prevents further tests from running once the
	 * specified limit has expired.
	 *
	 * @param classwide
	 *            controls the timeout for the test class applied to.
	 * @param method
	 *            controls timeout on an individual method.
	 */
	public AdvancedTimeout(int classwide, int method) {
		this.classwide = classwide;
		useClasswideTimeout = (classwide > 0);
		this.method = method;
		useMethodTimeout = (method > 0);
		beforeTime = System.currentTimeMillis();
	}

	/**
	 * Applies the strictest remaining timeout.
	 *
	 * @base the statement to apply the timeout to.
	 */
	public Statement apply(Statement base, FrameworkMethod fm, Object target) {
		if (useClasswideTimeout) {
			afterTime = System.currentTimeMillis();
			int diff = (int) (afterTime - beforeTime);
			classwide = classwide - diff;
			if (useMethodTimeout) {
				return new FailOnTimeout(base, Math.min(classwide, method));
			} else {
				return new FailOnTimeout(base, classwide);
			}
		} else {
			if (useMethodTimeout) {
				return new FailOnTimeout(base, method);
			} else {
				return base;
			}
		}
	}
}