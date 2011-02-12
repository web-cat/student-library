package student.web.internal.tests.support;

import java.util.HashMap;
import java.util.Map;

public class MapTestClass {
	Map<Object, Object> toPersist = new HashMap<Object, Object>();
	public MapTestClass()
	{
		toPersist.put("hi", "hello");
		toPersist.put(new InnerClass("0",0), new InnerClass("1",1));
	}
}
