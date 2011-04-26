package student.web.internal.tests.support;

public class ComplexClass  extends PlainClass implements Comparable{
	public String complexStuff = "!!!!";

	public int compareTo(Object o) {
		return 0;
	}
}
