package student.web.internal.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.commons.io.FileUtils;

import student.web.SharedPersistenceMap;
import student.web.internal.PersistentStorageManager;
import student.web.internal.PersistentStorageManager.StoredObject;
import student.web.internal.tests.support.*;

public class SharedPersistenceMapTest {
	private static final String TEST_ELEMENT = "TestElement";
	Stub stub = new Stub();

	private static class Stub {
		public Object toPersist = "Foo";

		public boolean equals(Object o) {
			if (o instanceof Stub) {
				if (((Stub) o).toPersist.equals(toPersist))
					return true;
			}
			return false;
		}

		public int hashCode() {
			return toPersist.hashCode();

		}
	}

	public void restoreTestData(String fileName, String oldFileName)
			throws IOException {
		// FileUtils.copyFileToDirectory(new File("data/test/"+fileName), new
		// File("data/shared"));
		FileUtils.copyFile(new File("data/test/" + oldFileName), new File(
				"data/shared/" + fileName));

	}

	SharedPersistenceMap<Stub> localAppStore;

	@Before
	public void setupLocalAppStore() throws InterruptedException {
		localAppStore = new SharedPersistenceMap<Stub>(Stub.class);
		localAppStore.clear();
	}

	@After
	public void clearLocalAppStore() {
		localAppStore.clear();
		assertTrue(localAppStore.isEmpty());
		assertEquals(localAppStore.size(), 0);
		assertEquals(localAppStore.keySet().size(), 0);
		assertEquals(localAppStore.values().size(), 0);
		assertEquals(localAppStore.entrySet().size(), 0);
	}

	@Test
	public void testApplicationPersistenceMap() {
		assertNotNull(localAppStore);
	}

	@Test
	public void testSize() {
		localAppStore.put(TEST_ELEMENT, stub);
		assertEquals(localAppStore.size(), 1);
		assertTrue(localAppStore.keySet().contains(TEST_ELEMENT));
		assertEquals(localAppStore.entrySet().size(), 1);
	}

	@Test
	public void testIsEmpty() {
		localAppStore.put(TEST_ELEMENT, stub);
		assertFalse(localAppStore.isEmpty());
	}

	@Test
	public void testContainsKey() {
		assertFalse(localAppStore.containsKey(TEST_ELEMENT));
		localAppStore.put(TEST_ELEMENT, stub);
		assertTrue(localAppStore.containsKey(TEST_ELEMENT));
	}

	@Test
	public void testContainsValue() {
		localAppStore.put(TEST_ELEMENT, stub);
		assertTrue(localAppStore.containsValue(stub));

	}

	@Test
	public void testGet() {
		localAppStore.put(TEST_ELEMENT, stub);
		Stub localStub = localAppStore.get(TEST_ELEMENT);
		assertEquals(stub, localStub);
	}

	@Test
	public void testPut() {
		localAppStore.put(TEST_ELEMENT, stub);
		assertTrue(localAppStore.containsKey(TEST_ELEMENT));
		assertTrue(localAppStore.containsValue(stub));
	}

	@Test
	public void testRemove() {
		localAppStore.put(TEST_ELEMENT, stub);
		localAppStore.remove(TEST_ELEMENT);
		assertFalse(localAppStore.containsKey(TEST_ELEMENT));
		assertFalse(localAppStore.containsValue(stub));
		assertEquals(localAppStore.size(), 0);
	}

	@Test
	public void testPutAll() {
		Map<String, Stub> toInsert = new HashMap<String, Stub>();
		toInsert.put("test1", stub);
		toInsert.put("test2", stub);
		toInsert.put("test3", stub);

		localAppStore.putAll(toInsert);
		assertEquals(localAppStore.size(), 3);
	}

	@Test
	public void testKeySet() {
		localAppStore.put("test1", stub);
		localAppStore.put("test2", stub);
		localAppStore.put("test2", stub);
		assertEquals(localAppStore.keySet().size(), 2);
		assertTrue(localAppStore.containsKey("test1"));
		assertTrue(localAppStore.containsKey("test2"));

	}

	@Test
	public void testValues() {
		localAppStore.put("test1", stub);
		localAppStore.put("test2", stub);
		localAppStore.put("test2", stub);
		assertEquals(localAppStore.keySet().size(), 2);
		assertTrue(localAppStore.containsValue(stub));
		assertTrue(localAppStore.values().contains(stub));
		assertEquals(localAppStore.values().size(), 1);

	}

	@Test
	public void testEntrySet() {
		localAppStore.put("test1", stub);
		localAppStore.put("test2", stub);
		for (Entry<String, Stub> entry : localAppStore.entrySet()) {
			assertTrue(entry.getKey().startsWith("test"));
			assertEquals(entry.getValue(), stub);
		}
		assertEquals(localAppStore.entrySet().size(), 2);

	}

	private static class Stub2 {
		public Object toPersist = "Bar";

		public boolean equals(Object o) {
			if (o instanceof Stub) {
				if (((Stub) o).toPersist.equals(toPersist))
					return true;
			}
			return false;
		}

		public int hashCode() {
			return toPersist.hashCode();

		}
	}

	private Stub2 stub2 = new Stub2();

	@Test
	public void testMultipleMaps() {
		SharedPersistenceMap<Stub2> stub2Map = new SharedPersistenceMap<Stub2>(
				Stub2.class);
		stub2Map.put("test1", stub2);
		localAppStore.put("test2", stub);
		assertEquals(localAppStore.size(), 2);
		assertEquals(stub2Map.size(), 2);
		assertEquals(localAppStore.get("test1"), null);
		assertEquals(stub2Map.get("test2"), null);

	}

	@Test
	public void testMultipleSnapshotKeySet() {
		SharedPersistenceMap<Stub2> stub2Map = new SharedPersistenceMap<Stub2>(
				Stub2.class);
		stub2Map.put("test1", stub2);
		localAppStore.put("test2", stub);
		Set<String> keyset1 = localAppStore.keySet();
		Set<String> keyset2 = stub2Map.keySet();
		localAppStore.clear();
		stub2Map.clear();
		assertEquals(2, keyset1.size());
		assertEquals(keyset2.size(), 2);
		assertEquals(localAppStore.size(), 0);
		assertEquals(stub2Map.size(), 0);
		assertEquals(localAppStore.keySet().size(), 0);
		assertEquals(stub2Map.keySet().size(), 0);
	}

	private class Inner {
	}

	@Test
	public void testInnerClass() {
		SharedPersistenceMap<Inner> persistMap = new SharedPersistenceMap<Inner>(
				Inner.class);
		try {
			persistMap.put("invalid", new Inner());
		} catch (IllegalArgumentException e) {
			assertTrue(true);
			return;
		}
		assertFalse(false);
	}

	@Test
	public void addEntryToTopOnCommit() {
		try {
			restoreTestData("testClass-010.dataxml", "testClass-010.dataxml");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		SharedPersistenceMap<DataStructClass> persistMap = new SharedPersistenceMap<DataStructClass>(
				DataStructClass.class);
		DataStructClass dataStruct;
		dataStruct = persistMap.get("testClass");
		dataStruct.remove(1);
		dataStruct.add(new InnerClass("testadded", 666));
		try {
			restoreTestData("testClass-010.dataxml",
					"testClass-010.dataxml.simChange");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		persistMap.put("testClass", dataStruct);
		// dataStruct = persistMap.get("testClass");
		assertEquals(4, dataStruct.internalDataStruct.size());
		assertEquals("syncAdd", dataStruct.internalDataStruct.get(0).getData0());
		assertEquals("First", dataStruct.internalDataStruct.get(1).getData0());
		assertEquals("3", dataStruct.internalDataStruct.get(2).getData0());
		assertEquals("testadded", dataStruct.internalDataStruct.get(3)
				.getData0());
		assertEquals("foo", dataStruct.internalDataStruct2.get(0).getData0());

	}

	@Test
	public void addEntryInMiddleOnCommit() {
		try {
			restoreTestData("testClass-010.dataxml", "testClass-010.dataxml");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		SharedPersistenceMap<DataStructClass> persistMap = new SharedPersistenceMap<DataStructClass>(
				DataStructClass.class);
		DataStructClass dataStruct;
		dataStruct = persistMap.get("testClass");
		dataStruct.remove(1);
		dataStruct.add(new InnerClass("testadded", 666));
		try {
			restoreTestData("testClass-010.dataxml",
					"testClass-010.dataxml.simChange1");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		persistMap.put("testClass", dataStruct);
		dataStruct = persistMap.get("testClass");
		assertEquals(4, dataStruct.internalDataStruct.size());
		assertEquals("syncAdd", dataStruct.internalDataStruct.get(1).getData0());
		assertEquals("0", dataStruct.internalDataStruct.get(0).getData0());
		assertEquals("3", dataStruct.internalDataStruct.get(2).getData0());
		assertEquals("testadded", dataStruct.internalDataStruct.get(3)
				.getData0());
		assertEquals("foo", dataStruct.internalDataStruct2.get(0).getData0());

	}

	@Test
	public void addEntryToBottomOnCommit() {

		try {
			restoreTestData("testClass-010.dataxml", "testClass-010.dataxml");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		SharedPersistenceMap<DataStructClass> persistMap = new SharedPersistenceMap<DataStructClass>(
				DataStructClass.class);
		DataStructClass dataStruct;
		dataStruct = persistMap.get("testClass");
		dataStruct.remove(1);
		dataStruct.add(new InnerClass("testadded", 666));
		try {
			restoreTestData("testClass-010.dataxml",
					"testClass-010.dataxml.simChange3");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		persistMap.put("testClass", dataStruct);
		dataStruct = persistMap.get("testClass");
		assertEquals(5, dataStruct.internalDataStruct.size());
		assertEquals("0", dataStruct.internalDataStruct.get(0).getData0());
		assertEquals("3", dataStruct.internalDataStruct.get(1).getData0());
		assertEquals("testadded", dataStruct.internalDataStruct.get(4)
				.getData0());
		assertEquals("syncAdd1", dataStruct.internalDataStruct.get(2)
				.getData0());
		assertEquals("syncAdd2", dataStruct.internalDataStruct.get(3)
				.getData0());
		assertEquals("foo", dataStruct.internalDataStruct2.get(0).getData0());

	}

	@Test
	public void addEntryInMiddleMultiOnCommit() {
		try {
			restoreTestData("testClass-010.dataxml", "testClass-010.dataxml");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		SharedPersistenceMap<DataStructClass> persistMap = new SharedPersistenceMap<DataStructClass>(
				DataStructClass.class);
		DataStructClass dataStruct;
		dataStruct = persistMap.get("testClass");
		dataStruct.remove(1);
		dataStruct.add(new InnerClass("testadded", 666));
		try {
			restoreTestData("testClass-010.dataxml",
					"testClass-010.dataxml.simChange2");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		persistMap.put("testClass", dataStruct);
		dataStruct = persistMap.get("testClass");
		assertEquals(5, dataStruct.internalDataStruct.size());
		assertEquals("syncAdd1", dataStruct.internalDataStruct.get(1)
				.getData0());
		assertEquals("syncAdd2", dataStruct.internalDataStruct.get(2)
				.getData0());
		assertEquals("0", dataStruct.internalDataStruct.get(0).getData0());
		assertEquals("3", dataStruct.internalDataStruct.get(3).getData0());
		assertEquals("testadded", dataStruct.internalDataStruct.get(4)
				.getData0());
		assertEquals("foo", dataStruct.internalDataStruct2.get(0).getData0());

	}

	@Test
	public void mixupOrderOnCommit() {
		try {
			restoreTestData("testClass-010.dataxml", "testClass-010.dataxml");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		SharedPersistenceMap<DataStructClass> persistMap = new SharedPersistenceMap<DataStructClass>(
				DataStructClass.class);
		DataStructClass dataStruct;
		dataStruct = persistMap.get("testClass");
		dataStruct.remove(1);
		dataStruct.add(new InnerClass("testadded", 666));
		try {
			restoreTestData("testClass-010.dataxml",
					"testClass-010.dataxml.simChange4");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		persistMap.put("testClass", dataStruct);
		dataStruct = persistMap.get("testClass");
		// assertEquals(5, dataStruct.internalDataStruct.size());
		// assertEquals("syncAdd1", dataStruct.internalDataStruct.get(1)
		// .getData0());
		// assertEquals("syncAdd2", dataStruct.internalDataStruct.get(2)
		// .getData0());
		// assertEquals("0", dataStruct.internalDataStruct.get(0).getData0());
		// assertEquals("3", dataStruct.internalDataStruct.get(3).getData0());
		// assertEquals("testadded", dataStruct.internalDataStruct.get(4)
		// .getData0());
		// assertEquals("foo",
		// dataStruct.internalDataStruct2.get(0).getData0());

	}

	@Test
	public void testComplexPlainClass() {
		SharedPersistenceMap<PlainClass> persistMap = new SharedPersistenceMap<PlainClass>(
				PlainClass.class);
		persistMap.put("test", (PlainClass) new ComplexClass());
		assertTrue(persistMap.containsKey("test"));
		SharedPersistenceMap<ComplexClass> complexPersistMap = new SharedPersistenceMap<ComplexClass>(
				ComplexClass.class);
		assertTrue(complexPersistMap.containsKey("test"));
		assertNotNull(complexPersistMap.get("test"));
		assertEquals(null, persistMap.get("test"));
		assertTrue(persistMap.containsKey("test"));
	}

	@Test
	public void testDataStructureClass() {
		SharedPersistenceMap<DataStructureTestClass> persistMap = new SharedPersistenceMap<DataStructureTestClass>(
				DataStructureTestClass.class);
		persistMap.put("blah", new DataStructureTestClass());
	}

	@Test
	public void arrayCollectionConverter() {
		try {
			restoreTestData("arrayTest-020.dataxml", "arrayTest-020.dataxml");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		SharedPersistenceMap<ArrayTestClass> persistMap = new SharedPersistenceMap<ArrayTestClass>(
				ArrayTestClass.class);
		ArrayTestClass temp = persistMap.get("arrayTest");
		assertEquals(10, temp.internalArray.length);
	}

	@Test
	public void arrayCollectionConverterCombinePersisted() {
		try {
			restoreTestData("arrayTest-020.dataxml", "arrayTest-020.dataxml");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		SharedPersistenceMap<ArrayTestClass> persistMap = new SharedPersistenceMap<ArrayTestClass>(
				ArrayTestClass.class);
		ArrayTestClass temp = persistMap.get("arrayTest");
		assertEquals(10, temp.internalArray.length);
		try {
			restoreTestData("arrayTest-020.dataxml",
					"arrayTest-020.dataxml.changes");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		persistMap.put("arrayTest", temp);
	}

	@Test
	public void persistMap() {
		SharedPersistenceMap<MapTestClass> persistMap = new SharedPersistenceMap<MapTestClass>(
				MapTestClass.class);
		persistMap.put("mapTest", new MapTestClass());
		try {
			restoreTestData("mapTest-80.dataxml", "mapTest-80.dataxml");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		MapTestClass mtc = persistMap.get("mapTest");
		try {
			restoreTestData("mapTest-80.dataxml", "mapTest-80.dataxml.changes");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		persistMap.put("mapTest", mtc);
	}

	@Test
	public void getPreviousNoneREMOVE() {
		SharedPersistenceMap<ComplexClass> persistMap = new SharedPersistenceMap<ComplexClass>(
				ComplexClass.class);
		ComplexClass old = persistMap.remove("Not there!");
		assertEquals(null, old);
	}

	@Test
	public void getPreviousExistsREMOVE() {
		SharedPersistenceMap<ComplexClass> persistMap = new SharedPersistenceMap<ComplexClass>(
				ComplexClass.class);
		ComplexClass limbo = new ComplexClass();
		ComplexClass old = persistMap.put("previous", limbo);
		assertEquals(null, old);
		old = persistMap.remove("previous");
		assertEquals(limbo, old);
	}

	@Test
	public void getPreviousNonePUT() {
		SharedPersistenceMap<ComplexClass> persistMap = new SharedPersistenceMap<ComplexClass>(
				ComplexClass.class);
		ComplexClass old = persistMap.put("putting", new ComplexClass());
		assertEquals(null, old);
	}

	@Test
	public void getPreviousExistsPUT() {
		SharedPersistenceMap<ComplexClass> persistMap = new SharedPersistenceMap<ComplexClass>(
				ComplexClass.class);
		ComplexClass limbo = new ComplexClass();
		ComplexClass old = persistMap.put("previous", limbo);
		assertEquals(null, old);
		ComplexClass newClass = new ComplexClass();
		old = persistMap.put("previous", newClass);
		assertEquals(limbo, old);
		assertFalse(limbo == newClass);
	}

	@Test
	public void testCacheGet() {
		SharedPersistenceMap<ComplexClass> persistMap = new SharedPersistenceMap<ComplexClass>(
				ComplexClass.class);
		persistMap.put("test", new ComplexClass());
		ComplexClass object1 = persistMap.get("test");
		ComplexClass object2 = persistMap.get("test");
		assertEquals(object1, object2);
	}

	@Test
	public void testCacheUpdated() {
		SharedPersistenceMap<ComplexClass> persistMap = new SharedPersistenceMap<ComplexClass>(
				ComplexClass.class);
		ComplexClass squirrel = persistMap.get("cachingUpdate");
		try {
			restoreTestData("cachingUpdate-0800.dataxml",
					"cachingUpdate-0800.dataxml");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		ComplexClass old = persistMap.get("cachingUpdate");
		// assertEquals(squirrel, old);
		assertEquals(null, squirrel);
		assertEquals("This isnt complex :-(", old.complexStuff);
	}

	@Test
	public void testCacheNotCompatable() {
		SharedPersistenceMap<PlainClass> persistMap = new SharedPersistenceMap<PlainClass>(
				PlainClass.class);
		persistMap.put("testClass", new PlainClass());
		try {
			restoreTestData("testClass-010.dataxml", "testClass-010.dataxml");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		PlainClass old = persistMap.get("testClass");
		// assertEquals(null,old);
	}

	@Test
	public void testComplexCacheUpdated() {
		SharedPersistenceMap<DataStructClass> persistMap = new SharedPersistenceMap<DataStructClass>(
				DataStructClass.class);
		DataStructClass squirrel = new DataStructClass();
		persistMap.put("testClass", squirrel);
		try {
			restoreTestData("testClass-010.dataxml", "testClass-010.dataxml");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		// List squirreledList = squirrel.internalDataStruct;
		DataStructClass old = persistMap.get("testClass");
		assertEquals(squirrel, old);
		// assertEquals("This isnt complex :-(",squirrel.internalDataStruct.size());
		// assertEquals("This isnt complex :-(",old.complexStuff);
	}

	@Test
	public void testNoChange() {
		SharedPersistenceMap<DataStructClass> persistMap = new SharedPersistenceMap<DataStructClass>(
				DataStructClass.class);
		DataStructClass squirrel = new DataStructClass();
		persistMap.put("testClass", squirrel);
		// List squirreledList = squirrel.internalDataStruct;
		DataStructClass old = persistMap.get("testClass");
		DataStructClass getAgain = persistMap.get("testClass");
		assertEquals(getAgain.internalDataStruct, old.internalDataStruct);
	}

	@Test
	public void testReferentialIdentity() {
		SharedPersistenceMap<DataStructClass> persistMap = new SharedPersistenceMap<DataStructClass>(
				DataStructClass.class);
		DataStructClass squirrel = new DataStructClass();
		persistMap.get("testClass");
		persistMap.put("testClass", squirrel);
		DataStructClass old = persistMap.get("testClass");
		assertEquals(squirrel, old);
		assertEquals(squirrel.internalDataStruct, old.internalDataStruct);
		assertEquals(squirrel.internalDataStruct2, old.internalDataStruct2);
		DataStructClass newObj = new DataStructClass();
		persistMap.put("testClass", newObj);
		DataStructClass cachedClass = persistMap.get("testClass");
		assertEquals(newObj, cachedClass);
		assertEquals(0, newObj.internalDataStruct.size());
	}

	@Test
	public void testRefIdExisting() {
		SharedPersistenceMap<DataStructClass> persistMap = new SharedPersistenceMap<DataStructClass>(
				DataStructClass.class);
		try {
			restoreTestData("testClass-010.dataxml", "testClass-010.dataxml");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		DataStructClass old = persistMap.get("testClass");
		assertEquals(3, old.internalDataStruct.size());
		persistMap.put("testClass", old);
		assertEquals(3, old.internalDataStruct.size());
		DataStructClass newClass = persistMap.get("testClass");
		assertEquals(3, old.internalDataStruct.size());
		assertEquals(newClass, old);
		old.internalDataStruct.get(0).data0 = "changedInTest";
		newClass = persistMap.get("testClass");
		assertEquals("changedInTest", newClass.internalDataStruct.get(0).data0);
		persistMap.put("testClass", newClass);
		assertEquals("changedInTest", newClass.internalDataStruct.get(0).data0);
		newClass = persistMap.get("testClass");
		assertEquals("changedInTest", newClass.internalDataStruct.get(0).data0);
		DataStructClass newObj = new DataStructClass();
		persistMap.put("testClass", newObj);
		DataStructClass afterPersist = persistMap.get("testClass");
		assertEquals(0, afterPersist.internalDataStruct.size());

	}

}
