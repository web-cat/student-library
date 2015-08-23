/**
 *
 */
package org.junit.internal.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

public class AllDefaultPossibilitiesBuilder extends RunnerBuilder {
	private final boolean fCanUseSuiteMethod;

    private static final String PROPERTY_PREFIX =
        AllDefaultPossibilitiesBuilder.class.getName();
    private static final String ENABLE_TESTER =
        PROPERTY_PREFIX + ".enableTester";
    private static final String DISABLE_MIX_RUNNER =
        PROPERTY_PREFIX + ".disableMixRunner";

    public AllDefaultPossibilitiesBuilder(boolean canUseSuiteMethod) {
		fCanUseSuiteMethod= canUseSuiteMethod;
	}

	@Override
	public Runner runnerForClass(Class<?> testClass) throws Throwable {
		List<RunnerBuilder> builders = new ArrayList<RunnerBuilder>(10);
		builders.add(ignoredBuilder());
		builders.add(annotatedBuilder());
		builders.add(suiteMethodBuilder());
		if (System.getProperty(ENABLE_TESTER, "true")
		    .matches("(?i)true|yes|on|[1-9][0-9]*"))
		{
		    // Add support for NU tester + MixRunner
		    builders.add(testerBuilder());
		}
		else
		{
		    // Original behavior
            builders.add(junit3Builder());
            builders.add(junit4Builder());
		}

		for (RunnerBuilder each : builders) {
			Runner runner= each.safeRunnerForClass(testClass);
			if (runner != null)
				return runner;
		}
		return null;
	}

    protected RunnerBuilder testerBuilder() {
        return new RunnerBuilder() {
            @Override
            public Runner runnerForClass(Class<?> testClass) throws Throwable {
//              return new BlockJUnit4ClassRunner(testClass);
                return new student.testingsupport.junit4
                    .JUnit4TesterRunner(testClass);
            }
        };
    }

	protected JUnit4Builder junit4Builder() {
		return new JUnit4Builder();
	}

	protected JUnit3Builder junit3Builder() {
		return new JUnit3Builder();
	}

	protected AnnotatedBuilder annotatedBuilder() {
		return new AnnotatedBuilder(this);
	}

	protected IgnoredBuilder ignoredBuilder() {
		return new IgnoredBuilder();
	}

	protected RunnerBuilder suiteMethodBuilder() {
		if (fCanUseSuiteMethod)
			return new SuiteMethodBuilder();
		return new NullBuilder();
	}
}