package student;
import java.util.Iterator;


//-------------------------------------------------------------------------
/**
 *  This class is just an iterator wrapped inside an Iterable so that it
 *  can be used in for-each loops.
 *
 *  @param <T> The type of elements contained in this sequence
 *
 *  @author  Stephen Edwards
 *  @version 2007.07.26
 */
public class IterableSequence<T>
    implements Iterable<T>
{
    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new IterableSequence.
     * @param iterator The iterator to wrap
     */
    public IterableSequence(Iterator<T> iterator)
    {
        iter = iterator;
    }


    //~ Public methods ........................................................

    // ----------------------------------------------------------
    /**
     * Returns an iterator over a set of elements of type T.
     * @return an iterator
     */
    public Iterator<T> iterator()
    {
        return iter;
    }


    //~ Instance/static variables .............................................
    private Iterator<T> iter;
}
