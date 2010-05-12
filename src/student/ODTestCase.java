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

import objectdraw.DrawableInterface;
import objectdraw.DrawableIterator;
import objectdraw.DrawingCanvas;
import objectdraw.JDrawingCanvas;

import junit.extensions.abbot.ComponentTestFixture;
import junit.framework.Assert;
import student.TestCase;
import student.GUITestCase.EventDispatchException;
import student.testingsupport.GUIFilter;
import student.testingsupport.ODFilter;
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
import acm.program.GraphicsProgram;

public class ODTestCase extends GUITestCase
{
    public final ODFilter.Operator where = ODFilter.ClientImports.where;
    /**
     * Helper method to retrieve the JDrawingCanvas object associated
     * with the {@link WindowController} being tested.
     * 
     * @return the JDrawingCanvas containing all of the WindowController's 
     * drawable objects                        
     */
    private DrawingCanvas getCanvas()
    {
        DrawingCanvas dc = getComponent(JDrawingCanvas.class);
        return dc;
    }
    
    /**
     * Look up a DrawableInterface object in the GUI being tested by specifying 
     * its class.
     * This method expects the given class to identify a unique DrawableInterface,
     * meaning that there should only be one instance of the given class
     * in the entire GUI. 
     * <p>
     * If no matching DrawableInterfaces exists, the test case will fail with an
     * appropriate message.  If more than one matching DrawableInterface exists,
     * the test case will fail with an appropriate message.
     * @param <T>  This method is a template method, and the type T used for
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the DrawableInterface you wish to retrieve, and
     *             also the way you specify the return type of this method.
     * @return The single DrawableInterface of the desired type that was found
     *         (otherwise, a test case failure results).
     * @see #getFirstDrawabletMatching(Class)
     * @see #getAllDrawablesMatching(Class)
     */
    public <T extends DrawableInterface> T getDrawable(Class<T> type)
    {
        return (T)getDrawable(where.typeIs(type));
    }
    
    /**
     * Look up a DrawableInterface in the GUI being tested by specifying its class
     * and a {@link ODFilter}.
     * This method expects exactly one DrawableInterface to match your criteria.
     * If no matching DrawableInterface exists, the test case will fail with an
     * appropriate message.  If more than one matching DrawableInterface exists,
     * the test case will fail with an appropriate message.
     * test case failure results).
     * @param <T>  This method is a template method, and the type T used for
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the DrawableInterface you wish to retrieve, and
     *             also the way you specify the return type of this method.
     * @param filter The search criteria.
     * @return The single DrawableInterface matching the criteria specified
     *         (otherwise, a test case failure results).
     * @see #getFirstDrawableMatching(Class,ODFilter)
     * @see #getAllDrawablesMatching(Class,ODFilter)
     */
    public <T extends DrawableInterface> T getDrawable(Class<T> type, ODFilter filter)
    {
        return (T)getDrawable(filter.and.typeIs(type));
    }
    
    /**
     * Look up a DrawableInterface in the GUI being tested, using a filter to
     * specify which component you want.  This method is more general
     * than {@link #getDrawable(Class, ODFilter)}, since no class needs to be
     * specified, but that also means the return type is less specific
     * (it is always <code>DrawableInterface</code>).
     * This method expects the given filter
     * to identify a unique DrawableInterface.  If no matching DrawableInterface exists,
     * the test case will fail with an appropriate message.  If more than
     * one matching DrawableInterface exists, the test case will fail with an
     * appropriate message.
     * @param filter The search criteria.
     * @return The single DrawableInterface matching the provided filter (otherwise, a
     *         test case failure results).
     * @see #getFirstDrawableMatching(ODFilter)
     * @see #getAllDrawablesMatching(ODFilter)
     */
    public DrawableInterface getDrawable(ODFilter filter)
    {
        IllegalStateException ise;

        DrawableInterface[] dis = getAllDrawablesMatching(filter);

        if(dis.length == 0)
        {
            Assert.fail("Cannot find objectdraw shape matching: " + filter);
        }
        else if(dis.length > 1)
        {
            Assert.fail("Found " + dis.length + "objectdraw shapes matching: " + filter);
        }
        return dis[0];               
    }
    
    /**
     * Look up a DrawableInterface in the GUI being tested by specifying its class.
     * This method expects the given class to identify at least one such
     * DrawableInterface.  If no matching DrawableInterface exists, the test case will fail
     * with an appropriate message.  If more than one matching DrawableInterface
     * exists, the first one found will be returned (although client code
     * should not expect a specific search order).
     * @param <T>  This method is a template method, and the type T used for
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the DrawableInterface you wish to retrieve, and
     *             also the way you specify the return type of this method.
     * @return The first DrawableInterface of the desired type that was found
     *         (a test case failure results if there are none).
     * @see #getDrawable(Class)
     * @see #getAllDrawablesMatching(Class)
     */
    public <T extends DrawableInterface> T getFirstDrawableMatching(Class<T> type)
    {
        T result = (T)getFirstDrawableMatching(where.typeIs(type));
        return result;
    }
    
     /**
     * Look up a DrawableInterface in the GUI being tested by specifying its class
     * and an {@link ODFilter}.
     * This method expects the given criteria to identify at least one such
     * DrawableInterface.  If no matching DrawableInterface exists, the test case will fail
     * with an appropriate message.  If more than one matching DrawableInterface
     * exists, the first one found will be returned (although client code
     * should not expect a specific search order).
     * @param <T>  This method is a template method, and the type T used for
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the DrawableInterface you wish to retrieve, and
     *             also the way you specify the return type of this method.
     * @param filter The search criteria.
     * @return The first DrawableInterface that was found matching the criteria
     *         specified (a test case failure results if there are none).
     * @see #getDrawable(Class,ODFilter)
     * @see #getAllDrawablesMatching(Class,ODFilter)
     */
    public <T extends DrawableInterface> T getFirstDrawableMatching(Class<T> type, ODFilter filter)
    {
        return (T)getFirstDrawableMatching(filter.and.typeIs(type));
    }
    
    /**
     * Look up a DrawableInterface in the GUI being tested by specifying
     * an {@link ODFilter}.  This method is more general
     * than {@link #getFirstDrawableMatching(Class,ODFilter)}, since no
     * class needs to be specified, but that also means the return type
     * is less specific (it is always <code>DrawableInterface</code>).
     * This method expects the given criteria to identify at least one such
     * DrawableInterface.  If no matching DrawableInterface exists, the test case will fail
     * with an appropriate message.  If more than one matching DrawableInterface
     * exists, the first one found will be returned (although client code
     * should not expect a specific search order).
     * @param filter The search criteria.
     * @return The first DrawableInterface that was found matching the criteria
     *         specified (a test case failure results if there are none).
     * @see #getDrawable(ODFilter)
     * @see #getAllDrawablesMatching(ODFilter)
     */
    public DrawableInterface getFirstDrawableMatching(ODFilter filter)
    {
        DrawableIterator iter = getCanvas().getDrawableIterator();
        while(iter.hasNext())
        {    
            DrawableInterface di = (DrawableInterface)iter.next();
            if(filter.test(di))
                return di;
        }
        fail("Cannot find objectdraw shape matching: " + filter);
        return null;
    }
    
    /**
     * Look up all DrawableInterfaces in the GUI being tested by specifying their
     * class.  All matching objects are returned in a list.
     * @param <T>  This method is a template method, and the type T used as
     *             the <code>List</code> element type in
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the DrawableInterface you wish to retrieve,
     *             and also the way you specify the type of elements in
     *             the list returned by this method.
     * @return A list of all DrawableInterfaces of the desired type that were found.
     *         This will be an empty list (not null) if no matching components
     *         are found.
     * @see #getDrawable(Class)
     * @see #getFirstDrawableMatching(Class)
     */
    public <T extends DrawableInterface> T[] getAllDrawablesMatching(Class<T> type)
    {
        return getAllDrawablesMatching(type, where.typeIs(type));
    }
    
    /**
     * Look up all DrawableInterfaces in the GUI being tested by specifying their
     * class and an {@link ODFilter}.  All matching objects are returned in
     * a list.
     * @param <T>  This method is a template method, and the type T used as
     *             the <code>List</code> element type in
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the DrawableInterfaces you wish to retrieve,
     *             and also the way you specify the type of elements in
     *             the list returned by this method.
     * @param filter The search criteria.
     * @return A list of all DrawableInterfaces found matching the criteria specified.
     *         This will be an empty list (not null) if no matching DrawableInterfaces
     *         are found.
     * @see #getDrawable(Class,ODFilter)
     * @see #getAllDrawablesMatching(Class,ODFilter)
     */
    public <T extends DrawableInterface> T[] getAllDrawablesMatching(Class<T> type, ODFilter filter)
    {
        DrawableInterface[] dis = getAllDrawablesMatching(filter.and.typeIs(type));

        T[] tobjs = (T[]) Array.newInstance(type, dis.length);
        for (int i = 0; i < dis.length; i++)
        {
            tobjs[i] = (T) dis[i];
        }

        return tobjs;
    }
    
    /**
     * Look up all DrawableInterfaces in the GUI being tested by specifying
     * a {@link ODFilter}.
     * All matching objects are returned in a list.
     * This method is more general than
     * {@link #getAllDrawablesMatching(Class,ODFilter)}, since no
     * class needs to be specified, but that also means the return type
     * is less specific (it is always <code>DrawableInterface</code>).
     * @param filter The search criteria.
     * @return A list of all DrawableInterfaces found matching the criteria specified.
     *         This will be an empty list (not null) if no matching DrawableInterfaces
     *         are found.
     * @see #getDrawable(ODFilter)
     * @see #getAllDrawablesMatching(ODFilter)
     */
    public DrawableInterface[] getAllDrawablesMatching(ODFilter filter)
    {
        ArrayList<DrawableInterface> matches = new ArrayList<DrawableInterface>();
        DrawableIterator iter = getCanvas().getDrawableIterator();
        while(iter.hasNext())
        {    
            DrawableInterface di = (DrawableInterface)iter.next();
            if(filter.test(di))
                matches.add(di);
        }
        
        DrawableInterface[] dis = new DrawableInterface[matches.size()];
        int i = 0;
        for(DrawableInterface di: matches)
        {
            dis[i] = di;
            i++;
        }
        return dis;
    }       
}
