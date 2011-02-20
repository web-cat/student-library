package student.web.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;


public class ReadOnlyHashSet<T> extends HashSet<T>
{
    /**
	 * 
	 */
    private static final long serialVersionUID = 3888304419328745503L;


    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * default initial capacity (16) and load factor (0.75).
     */
    public ReadOnlyHashSet()
    {
        super();
    }


    /**
     * Constructs a new set containing the elements in the specified collection.
     * The <tt>HashMap</tt> is created with default load factor (0.75) and an
     * initial capacity sufficient to contain the elements in the specified
     * collection.
     * 
     * @param c
     *            the collection whose elements are to be placed into this set
     * @throws NullPointerException
     *             if the specified collection is null
     */
    public ReadOnlyHashSet( Collection<? extends T> c )
    {
        super();
        addAllLocal( c );
    }


    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * the specified initial capacity and the specified load factor.
     * 
     * @param initialCapacity
     *            the initial capacity of the hash map
     * @param loadFactor
     *            the load factor of the hash map
     * @throws IllegalArgumentException
     *             if the initial capacity is less than zero, or if the load
     *             factor is nonpositive
     */
    public ReadOnlyHashSet( int initialCapacity, float loadFactor )
    {
        super( initialCapacity, loadFactor );
    }


    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * the specified initial capacity and default load factor (0.75).
     * 
     * @param initialCapacity
     *            the initial capacity of the hash table
     * @throws IllegalArgumentException
     *             if the initial capacity is less than zero
     */
    public ReadOnlyHashSet( int initialCapacity )
    {
        super( initialCapacity );
    }


    /**
     * Unsupported Operation
     */
    public boolean add( T e )
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Adds the specified element to this set if it is not already present. More
     * formally, adds the specified element <tt>e</tt> to this set if this set
     * contains no element <tt>e2</tt> such that
     * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>. If this
     * set already contains the element, the call leaves the set unchanged and
     * returns <tt>false</tt>.
     * 
     * @param e
     *            element to be added to this set
     * @return <tt>true</tt> if this set did not already contain the specified
     *         element
     */
    boolean addLocal( T e )
    {
        return super.add( e );
    }


    /**
     * Unsupported Operation
     */
    public boolean remove( Object o )
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Removes the specified element from this set if it is present. More
     * formally, removes an element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>, if this
     * set contains such an element. Returns <tt>true</tt> if this set contained
     * the element (or equivalently, if this set changed as a result of the
     * call). (This set will not contain the element once the call returns.)
     * 
     * @param o
     *            object to be removed from this set, if present
     * @return <tt>true</tt> if the set contained the specified element
     */
    boolean removeLocal( Object o )
    {
        return super.remove( o );
    }


    /**
     * Unsupported Operation
     */
    public void clear()
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Removes all of the elements from this set. The set will be empty after
     * this call returns.
     */
    void clearLocal()
    {
        super.clear();
    }


    /**
     * Unsupported Operation
     */
    public Object clone()
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Returns a shallow copy of this <tt>HashSet</tt> instance: the elements
     * themselves are not cloned.
     * 
     * @return a shallow copy of this set
     */
    Object cloneLocal()
    {
        return super.clone();
    }


    /**
     * Unsupported Operation
     */
    public boolean removeAll( Collection<?> c )
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Removes from this set all of its elements that are contained in the
     * specified collection (optional operation). If the specified collection is
     * also a set, this operation effectively modifies this set so that its
     * value is the <i>asymmetric set difference</i> of the two sets.
     * 
     * <p>
     * This implementation determines which is the smaller of this set and the
     * specified collection, by invoking the <tt>size</tt> method on each. If
     * this set has fewer elements, then the implementation iterates over this
     * set, checking each element returned by the iterator in turn to see if it
     * is contained in the specified collection. If it is so contained, it is
     * removed from this set with the iterator's <tt>remove</tt> method. If the
     * specified collection has fewer elements, then the implementation iterates
     * over the specified collection, removing from this set each element
     * returned by the iterator, using this set's <tt>remove</tt> method.
     * 
     * <p>
     * Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> if the iterator returned by the
     * <tt>iterator</tt> method does not implement the <tt>remove</tt> method.
     * 
     * @param c
     *            collection containing elements to be removed from this set
     * @return <tt>true</tt> if this set changed as a result of the call
     * @throws UnsupportedOperationException
     *             if the <tt>removeAll</tt> operation is not supported by this
     *             set
     * @throws ClassCastException
     *             if the class of an element of this set is incompatible with
     *             the specified collection (optional)
     * @throws NullPointerException
     *             if this set contains a null element and the specified
     *             collection does not permit null elements (optional), or if
     *             the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean removeAllLocal( Collection<?> c )
    {
        return super.removeAll( c );
    }


    /**
     * Unsupported Operation
     */
    public boolean retainAll( Collection<?> c )
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation iterates over this collection, checking each element
     * returned by the iterator in turn to see if it's contained in the
     * specified collection. If it's not so contained, it's removed from this
     * collection with the iterator's <tt>remove</tt> method.
     * 
     * <p>
     * Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> if the iterator returned by the
     * <tt>iterator</tt> method does not implement the <tt>remove</tt> method
     * and this collection contains one or more elements not present in the
     * specified collection.
     * 
     * @throws UnsupportedOperationException
     *             {@inheritDoc}
     * @throws ClassCastException
     *             {@inheritDoc}
     * @throws NullPointerException
     *             {@inheritDoc}
     * 
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean retainAllLocal( Collection<?> c )
    {
        return super.retainAll( c );
    }


    /**
     * Unsupported Operation
     */
    public boolean addAll( Collection<? extends T> c )
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation iterates over the specified collection, and adds each
     * object returned by the iterator to this collection, in turn.
     * 
     * <p>
     * Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> unless <tt>add</tt> is overridden
     * (assuming the specified collection is non-empty).
     * 
     * @throws UnsupportedOperationException
     *             {@inheritDoc}
     * @throws ClassCastException
     *             {@inheritDoc}
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws IllegalArgumentException
     *             {@inheritDoc}
     * @throws IllegalStateException
     *             {@inheritDoc}
     * 
     * @see #add(Object)
     */
    boolean addAllLocal( Collection<? extends T> c )
    {
        boolean modified = false;
        Iterator<? extends T> e = c.iterator();
        while ( e.hasNext() )
        {
            if ( addLocal( e.next() ) )
                modified = true;
        }
        return modified;
    }
}
