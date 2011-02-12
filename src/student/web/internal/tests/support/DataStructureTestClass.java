package student.web.internal.tests.support;

import java.util.*;

public class DataStructureTestClass {
	private PlainClass[] blah = new PlainClass[10];
	private Map<String,Object> foo = new HashMap<String,Object>();
	private TreeMap treeMap = new TreeMap();
	private TreeSet treeSet = new TreeSet();
	private Properties prop = new Properties();
	public DataStructureTestClass()
	{
		foo.put("foo", new PlainClass());
		foo.put("bar", new PlainClass());
		prop.put("foo", new PlainClass());
		prop.put("bar", new PlainClass());
		treeMap.put("foo", new ComplexClass());
		treeMap.put("bar", new ComplexClass());
		treeSet.add(new ComplexClass());
		treeSet.add(new ComplexClass());
	}
}
