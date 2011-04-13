package student.testingsupport.junit4;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * Custom rule for managing a test class so as to allow time for well-behaved
 * methods but still cut off methods that run longer than expected or are
 * nonterminating. <br>
 * <br>
 * The problem this rule solves is where an automatic grading server must grade
 * student code based on professor-written tests. Most of the student code may
 * work properly, while some of it fails to run within a reasonable time frame.
 * It would slow down the entire server if methods were not given a time limit
 * for execution, and there may be many other students. Hard limits on the
 * entire test class could be adopted, but this may confuse students and would
 * not help pinpoint problem areas. A per-method limit could be adopted, but it
 * would have to be generous enough to handle all proper methods. This would be
 * problematic if there were a large number of nonterminating methods.<br>
 * <br>
 * The solution arrived upon is a sort of adaptive timeout, where the test class
 * is given an initial (generous) ceiling for each method. It does not cause
 * slowdowns to have a large timeout, unless there are nonterminating methods.
 * If a nonterminating method is detected, the ceiling is ramped down swiftly,
 * choking out other nonterminating methods, and, if enough occur, even correct
 * methods. This approach saves server time while still giving all tests a
 * chance to run. <br>
 * <br>
 * A running "ceiling" is applied as the timeout for the next method. This
 * ceiling may be adjusted upward to a maximum in the case of methods that run
 * close to the ceiling, or downward to a minimum if methods repeatedly time
 * out.
 *
 * @author Craig Estep
 */
public class AdaptiveTimeout implements MethodRule {

	private int ceiling;
	private int maximum;
	private int minimum;

	private double threshold;
	private double rampup;
	private double rampdown;

	private int nonterminating;

	private long start;
	private long last;

	private ArrayList<String> methodLogs;

	/**
	 * Writes out statistics from the run test, as they stand, to the given
	 * file. Statistics are written in CSV format, according to: <br>
	 * <br>
	 * <b>ClassName,MethodName,DidPreviousTerminate,PreviousRuntime,Minimum,
	 * Ceiling,Maximum</b> <br>
	 * <br>
	 * If the file does not exist, it is created and these values are written to
	 * the first line before statistics are written.
	 *
	 * @param testname
	 *            the name of the test class to write to the file.
	 */
	public void appendStatsToFile(String filename) {
		try {
			String s = "";
			File f = new File(filename);
			if (!f.exists()) {
				f.createNewFile();
				s += "ClassName,MethodName,PreviousTerminating,PreviousRuntime"
						+ ",Minimum,Ceiling,Maximum\n";
			}
			BufferedWriter writer = new BufferedWriter(new FileWriter(f, true));
			while (!methodLogs.isEmpty()) {
				s += methodLogs.remove(0);
			}

			writer.append(s);
			writer.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Default constructor assigns the following values: <br>
	 * <br>
	 * ceiling = 10000ms <br>
	 * maximum = 20000ms <br>
	 * minimum = 250ms <br>
	 * threshold = 0.8 (80%) <br>
	 * rampup = 1.4 <br>
	 * rampdown = 0.5
	 *
	 * @param the
	 *            name of the class to log.
	 */
	public AdaptiveTimeout() {
		this(10000, 20000, 250, 0.8, 1.4, 0.5);
	}

	/**
	 * Creates a timeout with the given options.
	 *
	 * @param <T>
	 *
	 * @param ceiling
	 *            the initial ceiling (in milliseconds).
	 * @param maximum
	 *            the maximum ceiling (in milliseconds).
	 * @param minimum
	 *            the minimum ceiling (in milliseconds).
	 * @param threshold
	 *            the % of ceiling such that if a test runs longer than this
	 *            percentage but still shorter than the ceiling, the ceiling is
	 *            increased according to the ramping up strategy.
	 * @param rampup
	 *            the value that is used to calculate a new higher ceiling, up
	 *            to the maximum. NewCeiling = OldCeiling * rampup.
	 * @param rampdown
	 *            the value that is used to calculate the new ceiling after a
	 *            timeout occurs. NewCeiling = OldCeiling * rampdown.
	 */
	public <T> AdaptiveTimeout(int ceiling, int maximum, int minimum,
			double threshold, double rampup, double rampdown) {
		this.ceiling = ceiling;
		this.maximum = maximum;
		this.minimum = minimum;

		this.threshold = threshold;
		this.rampup = rampup;
		this.rampdown = rampdown;

		nonterminating = 0;

		start = System.currentTimeMillis();
		last = start;

		methodLogs = new ArrayList<String>();
	}

	/**
	 * Adjusts the current ceiling based on the last method, and applies the
	 * ceiling to the next method to run.
	 */
	public Statement apply(
	    Statement base, FrameworkMethod method, Object target)
	{
		long curr = System.currentTimeMillis();

		boolean t = true;

		int diff = 0;

		if (last != start) {
			diff = (int) (curr - last);
			if (diff > ceiling) {
				t = false;
				nonterminating++;
				if (nonterminating >= 2) {
					if ((ceiling * rampdown) < minimum)
						ceiling = minimum;
					else
						ceiling = (int) (ceiling * rampdown);
				}
			} else if (diff > ceiling * threshold) {
				if ((ceiling * rampup) > maximum)
					ceiling = maximum;
				else
					ceiling = (int) (ceiling * rampup);
			}
		}

		methodLogs.add(method.getMethod().getDeclaringClass().getSimpleName()
				+ "," + method.getName() + "," + t + "," + diff + "," + minimum
				+ "," + ceiling + "," + maximum + "\n");

		last = curr;
		return new FailOnTimeout(base, ceiling);
	}
}