package student.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;






public class SharedPersistenceMapTest {
	private static final String TEST_ELEMENT = "TestElement";
	Stub stub = new Stub();
	private static class Stub
	{
		public Object toPersist = "Foo";
		public boolean equals(Object o)
		{
			if(o instanceof Stub)
			{
				if(((Stub)o).toPersist.equals(toPersist))
					return true;
			}
			return false;
		}
		public int hashCode()
		{
			return toPersist.hashCode();
			
		}
	}
	SharedPersistenceMap<Stub> localAppStore;
	@Before
	public void setupLocalAppStore() throws InterruptedException
	{
		localAppStore = new SharedPersistenceMap<Stub>(Stub.class);
	}
	@After
	public void clearLocalAppStore()
	{
		localAppStore.clearLocal();
		assertTrue(localAppStore.isEmpty());
		assertEquals(localAppStore.size(),0);
		assertEquals(localAppStore.keySet().size(),0);
		assertEquals(localAppStore.values().size(),0);
		assertEquals(localAppStore.entrySet().size(),0);
	}
	@Test
	public void testApplicationPersistenceMap()
	{
		   assertNotNull(localAppStore);
	}

	@Test
	public void testSize()
	{
		localAppStore.put(TEST_ELEMENT, stub);
		assertEquals(localAppStore.size(),1);
		assertTrue(localAppStore.keySet().contains(TEST_ELEMENT));
		assertEquals(localAppStore.entrySet().size(),1);
	}

	@Test
	public void testIsEmpty()
	{
		localAppStore.put(TEST_ELEMENT, stub);
		assertFalse(localAppStore.isEmpty());
	}

	@Test
	public void testContainsKey()
	{
		assertFalse(localAppStore.containsKey(TEST_ELEMENT));
		localAppStore.put(TEST_ELEMENT, stub);
		assertTrue(localAppStore.containsKey(TEST_ELEMENT));
	}

	@Test
	public void testContainsValue()
	{
		localAppStore.put(TEST_ELEMENT, stub);
		assertTrue(localAppStore.containsValue(stub));

	}

	@Test
	public void testGet()
	{
		localAppStore.put(TEST_ELEMENT, stub);
		Stub localStub = localAppStore.get(TEST_ELEMENT);
		assertEquals(stub,localStub);
	}

	@Test
	public void testPut()
	{
		localAppStore.put(TEST_ELEMENT, stub);
		assertTrue(localAppStore.containsKey(TEST_ELEMENT));
		assertTrue(localAppStore.containsValue(stub));
	}

	@Test
	public void testRemove()
	{
		localAppStore.put(TEST_ELEMENT, stub);
		localAppStore.remove(TEST_ELEMENT);
		assertFalse(localAppStore.containsKey(TEST_ELEMENT));
		assertFalse(localAppStore.containsValue(stub));
		assertEquals(localAppStore.size(),0);
	}

	@Test
	public void testPutAll()
	{
		Map<String,Stub> toInsert = new HashMap<String, Stub>();
		toInsert.put("test1",stub);
		toInsert.put("test2", stub);
		toInsert.put("test3", stub);
		
		localAppStore.putAll(toInsert);
		assertEquals(localAppStore.size(),3);
	}


	@Test
	public void testKeySet()
	{
		localAppStore.put("test1", stub);
		localAppStore.put("test2", stub);
		localAppStore.put("test2", stub);
		assertEquals(localAppStore.keySet().size(),2);
		assertTrue(localAppStore.containsKey("test1"));
		assertTrue(localAppStore.containsKey("test2"));
		
	}

	@Test
	public void testValues()
	{
		localAppStore.put("test1", stub);
		localAppStore.put("test2", stub);
		localAppStore.put("test2", stub);
		assertEquals(localAppStore.keySet().size(),2);
		assertTrue(localAppStore.containsValue(stub));
		assertTrue(localAppStore.values().contains(stub));
		assertEquals(localAppStore.values().size(),1);

	}

	@Test
	public void testEntrySet()
	{
		localAppStore.put("test1", stub);
		localAppStore.put("test2", stub);
		for(Entry<String,Stub> entry : localAppStore.entrySet())
		{
			assertTrue(entry.getKey().startsWith("test"));
			assertEquals(entry.getValue(),stub);
		}
		assertEquals(localAppStore.entrySet().size(),2);
		
	}
	private static class Stub2
	{
		public Object toPersist = "Bar";
		public boolean equals(Object o)
		{
			if(o instanceof Stub)
			{
				if(((Stub)o).toPersist.equals(toPersist))
					return true;
			}
			return false;
		}
		public int hashCode()
		{
			return toPersist.hashCode();
			
		}
	}
	private Stub2 stub2 = new Stub2();
	@Test
	public void testMultipleMaps()
	{
		SharedPersistenceMap<Stub2> stub2Map = new SharedPersistenceMap<Stub2>(Stub2.class);
		stub2Map.put("test1", stub2);
		localAppStore.put("test2", stub);
		assertEquals(localAppStore.size(),2);
		assertEquals(stub2Map.size(),2);
		assertEquals(localAppStore.get("test1"),null);
		assertEquals(stub2Map.get("test2"),null);
		
		
	}
	@Test
	public void testMultipleSnapshotKeySet()
	{
		SharedPersistenceMap<Stub2> stub2Map = new SharedPersistenceMap<Stub2>(Stub2.class);
		stub2Map.put("test1", stub2);
		localAppStore.put("test2", stub);
		Set<String> keyset1 = localAppStore.keySet();
		Set<String> keyset2 = stub2Map.keySet();
		localAppStore.clearLocal();
		stub2Map.clearLocal();
		assertEquals(keyset1.size(),2);
		assertEquals(keyset2.size(),2);
		assertEquals(localAppStore.size(),0);
		assertEquals(stub2Map.size(),0);
		assertEquals(localAppStore.keySet().size(),0);
		assertEquals(stub2Map.keySet().size(),0);
	}
}
