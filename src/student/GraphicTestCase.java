package student;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;

import junit.extensions.abbot.ComponentTestFixture;
import junit.framework.Assert;
import student.TestCase;
import student.GUITestCase.EventDispatchException;
import student.testingsupport.GObjectFilter;
import student.testingsupport.GUIFilter;
import abbot.finder.BasicFinder;
import abbot.finder.ComponentFinder;
import abbot.finder.ComponentNotFoundException;
import abbot.finder.Hierarchy;
import abbot.finder.Matcher;
import abbot.finder.MultipleComponentsFoundException;
import abbot.finder.TestHierarchy;
import abbot.tester.ComponentTester;
import abbot.util.AWTFixtureHelper;
import acm.graphics.GCanvas;
import acm.graphics.GObject;
import acm.program.GraphicsProgram;

public class GraphicTestCase extends GUITestCase
{
    public final GObjectFilter.Operator where = GObjectFilter.ClientImports.where;
	/**
	 * Helper method to retrieve the GCanvas object associated
	 * with the {@link GraphicsProgram} being tested.
	 * 
	 * @return the GCanvas containing all of the GraphicsProgram's 
	 * GObjects                        
	 */
	private GCanvas getCanvas()
	{
	    GCanvas gc = getComponent(GCanvas.class);
	    return gc;
	}
	
	/**
     * Look up a GObject in the GUI being tested by specifying its class.
     * This method expects the given class to identify a unique component,
     * meaning that there should only be one instance of the given class
     * in the entire GUI. 
     * <p>
     * If no matching GObject exists, the test case will fail with an
     * appropriate message.  If more than one matching component exists,
     * the test case will fail with an appropriate message.
     * @param <T>  This method is a template method, and the type T used for
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the GObject you wish to retrieve, and
     *             also the way you specify the return type of this method.
     * @return The single component of the desired type that was found
     *         (otherwise, a test case failure results).
     * @see #getFirstGObjectMatching(Class)
     * @see #getAllGObjectsMatching(Class)
     */
	public <T extends GObject> T getGObject(Class<T> type)
    {
        return (T)getGObject(where.typeIs(type));
    }
	
	/**
     * Look up a GObject in the GUI being tested by specifying its class
     * and a {@link GObjectFilter}.
     * This method expects exactly one component to match your criteria.
     * If no matching component exists, the test case will fail with an
     * appropriate message.  If more than one matching component exists,
     * the test case will fail with an appropriate message.
     * test case failure results).
     * @param <T>  This method is a template method, and the type T used for
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the component you wish to retrieve, and
     *             also the way you specify the return type of this method.
     * @param filter The search criteria.
     * @return The single component matching the criteria specified
     *         (otherwise, a test case failure results).
     * @see #getFirstGObjectMatching(Class,GObjectFilter)
     * @see #getAllGObjectsMatching(Class,GObjectFilter)
     */
	public <T extends GObject> T getGObject(Class<T> type, GObjectFilter filter)
    {
        return (T)getGObject(filter.and.typeIs(type));
    }
	
	/**
     * Look up a component in the GUI being tested, using a filter to
     * specify which component you want.  This method is more general
     * than {@link #getGObject(Class,GObjectFilter)}, since no class needs to be
     * specified, but that also means the return type is less specific
     * (it is always <code>GObject</code>).
     * This method expects the given filter
     * to identify a unique component.  If no matching component exists,
     * the test case will fail with an appropriate message.  If more than
     * one matching component exists, the test case will fail with an
     * appropriate message.
     * @param filter The search criteria.
     * @return The single component matching the provided filter (otherwise, a
     *         test case failure results).
     * @see #getFirstGObjectMatching(GObjectFilter)
     * @see #getAllGObjectsMatching(GObjectFilter)
     */
	public GObject getGObject(GObjectFilter filter)
    {
        IllegalStateException ise;

        GObject[] gobjs = getAllGObjectsMatching(filter);

        if(gobjs.length == 0)
        {
            Assert.fail("Cannot find GObject matching: " + filter);
        }
        else if(gobjs.length > 1)
        {
            Assert.fail("Found " + gobjs.length + "components matching: " + filter);
        }
        return gobjs[0];               
    }
	
	/**
     * Look up a component in the GUI being tested by specifying its class.
     * This method expects the given class to identify at least one such
     * component.  If no matching component exists, the test case will fail
     * with an appropriate message.  If more than one matching component
     * exists, the first one found will be returned (although client code
     * should not expect a specific search order).
     * @param <T>  This method is a template method, and the type T used for
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the component you wish to retrieve, and
     *             also the way you specify the return type of this method.
     * @return The first component of the desired type that was found
     *         (a test case failure results if there are none).
     * @see #getGObject(Class)
     * @see #getAllGObjectsMatching(Class)
     */
	public <T extends GObject> T getFirstGObjectMatching(Class<T> type)
    {
        T result = (T)getFirstGObjectMatching(where.typeIs(type));
        return result;
    }
	
	 /**
     * Look up a component in the GUI being tested by specifying its class
     * and a {@link GObjectFilter}.
     * This method expects the given criteria to identify at least one such
     * component.  If no matching component exists, the test case will fail
     * with an appropriate message.  If more than one matching component
     * exists, the first one found will be returned (although client code
     * should not expect a specific search order).
     * @param <T>  This method is a template method, and the type T used for
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the component you wish to retrieve, and
     *             also the way you specify the return type of this method.
     * @param filter The search criteria.
     * @return The first component that was found matching the criteria
     *         specified (a test case failure results if there are none).
     * @see #getGObject(Class,GObjectFilter)
     * @see #getAllGObjectsMatching(Class,GObjectFilter)
     */
	public <T extends GObject> T getFirstGObjectMatching(Class<T> type, GObjectFilter filter)
    {
        return (T)getFirstGObjectMatching(filter.and.typeIs(type));
    }
	
	/**
     * Look up a component in the GUI being tested by specifying
     * a {@link GObjectFilter}.  This method is more general
     * than {@link #getFirstGObjectMatching(Class,GObjectFilter)}, since no
     * class needs to be specified, but that also means the return type
     * is less specific (it is always <code>GObject</code>).
     * This method expects the given criteria to identify at least one such
     * component.  If no matching component exists, the test case will fail
     * with an appropriate message.  If more than one matching component
     * exists, the first one found will be returned (although client code
     * should not expect a specific search order).
     * @param filter The search criteria.
     * @return The first component that was found matching the criteria
     *         specified (a test case failure results if there are none).
     * @see #getGObject(GObjectFilter)
     * @see #getAllGObjectsMatching(GObjectFilter)
     */
    public GObject getFirstGObjectMatching(GObjectFilter filter)
    {
        Iterator iter = getCanvas().iterator();
        while(iter.hasNext())
        {    
            GObject gobj = (GObject)iter.next();
            if(filter.test(gobj))
                return gobj;
        }
        fail("Cannot find GObject matching: " + filter);
        return null;
    }
    
    /**
     * Look up all components in the GUI being tested by specifying their
     * class.  All matching objects are returned in a list.
     * @param <T>  This method is a template method, and the type T used as
     *             the <code>List</code> element type in
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the components you wish to retrieve,
     *             and also the way you specify the type of elements in
     *             the list returned by this method.
     * @return A list of all components of the desired type that were found.
     *         This will be an empty list (not null) if no matching components
     *         are found.
     * @see #getGObject(Class)
     * @see #getFirstGObjectMatching(Class)
     */
    public <T extends GObject> T[] getAllGObjectsMatching(Class<T> type)
    {
        return getAllGObjectsMatching(type, where.typeIs(type));
    }
	
    /**
     * Look up all components in the GUI being tested by specifying their
     * class and a {@link GObjectFilter}.  All matching objects are returned in
     * a list.
     * @param <T>  This method is a template method, and the type T used as
     *             the <code>List</code> element type in
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the components you wish to retrieve,
     *             and also the way you specify the type of elements in
     *             the list returned by this method.
     * @param filter The search criteria.
     * @return A list of all components found matching the criteria specified.
     *         This will be an empty list (not null) if no matching components
     *         are found.
     * @see #getGObject(Class,GObjectFilter)
     * @see #getAllGObjectsMatching(Class,GObjectFilter)
     */
    public <T extends GObject> T[] getAllGObjectsMatching(Class<T> type, GObjectFilter filter)
    {
        GObject[] gobjs = getAllGObjectsMatching(filter.and.typeIs(type));

        T[] tobjs = (T[]) Array.newInstance(type, gobjs.length);
        for (int i = 0; i < gobjs.length; i++)
        {
            tobjs[i] = (T) gobjs[i];
        }

        return tobjs;
    }
    
    /**
     * Look up all components in the GUI being tested by specifying
     * a {@link GObjectFilter}.
     * All matching objects are returned in a list.
     * This method is more general than
     * {@link #getAllGObjectsMatching(Class,GObjectFilter)}, since no
     * class needs to be specified, but that also means the return type
     * is less specific (it is always <code>List&lt;GObject&gt;</code>).
     * @param filter The search criteria.
     * @return A list of all components found matching the criteria specified.
     *         This will be an empty list (not null) if no matching components
     *         are found.
     * @see #getGObject(GObjectFilter)
     * @see #getAllGObjectsMatching(GObjectFilter)
     */
    public GObject[] getAllGObjectsMatching(GObjectFilter filter)
    {
        ArrayList<GObject> matches = new ArrayList<GObject>();
        Iterator<?> iter = getCanvas().iterator();
        while (iter.hasNext())
        {
            GObject gobj = (GObject)iter.next();
            if(filter.test(gobj))
            {
                matches.add(gobj);
            }
        }
        GObject[] gobjs = new GObject[matches.size()];
        int i = 0;
        for(GObject gobj : matches)
        {
            gobjs[i] = gobj;
            i++;
        }
        return gobjs;
    }	    
}
