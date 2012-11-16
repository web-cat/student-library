package student.testingsupport.junit4;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class InvokeTesterMethod extends Statement
{
	private final FrameworkMethod testMethod;
	private final Object target;
	private final Object arg;

	public InvokeTesterMethod(
	    FrameworkMethod testMethod, Object target, Object arg)
	{
		this.testMethod = testMethod;
		this.target = target;
		this.arg = arg;
	}

	@Override
	public void evaluate() throws Throwable
	{
		testMethod.invokeExplosively(target, arg);
	}
}
