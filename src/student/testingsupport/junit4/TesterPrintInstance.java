package student.testingsupport.junit4;

import org.junit.runners.model.Statement;
import tester.Printer;

public class TesterPrintInstance extends Statement
{
    private Object example;

    public TesterPrintInstance(Object example)
	{
        this.example = example;
	}

	@Override
	public void evaluate() throws Throwable
	{
	    // pretty-print the 'Examples' class data when desired
	    System.out.println(example.getClass().getName() + ":");
	    System.out.println("---------------");
	    System.out.println(Printer.produceString(example));
	    System.out.println("---------------");
	}
}
