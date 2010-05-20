/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2007-2010 Virginia Tech
 |
 |  This file is part of the Student-Library.
 |
 |  The Student-Library is free software; you can redistribute it and/or
 |  modify it under the terms of the GNU Lesser General Public License as
 |  published by the Free Software Foundation; either version 3 of the
 |  License, or (at your option) any later version.
 |
 |  The Student-Library is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU Lesser General Public License for more details.
 |
 |  You should have received a copy of the GNU Lesser General Public License
 |  along with the Student-Library; if not, see <http://www.gnu.org/licenses/>.
\*==========================================================================*/

package student.testingsupport;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import objectdraw.*;

//-------------------------------------------------------------------------
/**
 *  This class Represents a filter or query that can be used to describe
 *  a {@link DrawableInterface} when searching.  Note that the methods and fields
 *  in this class are designed specifically to support a natural, readable,
 *  boolean expression "mini-language" for use in describing a single
 *  {@link DrawableInterface} (or group of {@link DrawableInterface}s) by its (or their)
 *  properties.  As a result, it does violate some conventions regarding
 *  the use of public fields (although note that all here are immutable)
 *  and occasionally even the naming conventions for constants (e.g.,
 *  <code>where</code>).  However, breaking these conventions is necessary
 *  in this class in order to support the more natural syntax for filter
 *  expressions, and so was deemed a better design choice.
 *  <p>
 *  Client classes that wish to use these filters should add the
 *  following static import directive:
 *  </p>
 *  <pre>
 *  import static student.testingsupport.ODFilter.ClientImports.*;
 *  </pre>
 *  <p>
 *  Note that the {@link student.ODTestCase} class already re-exports the
 *  items defined in the {@link ClientImports} nested class, so OD test
 *  cases should <em>not</em> include the static import.
 *  </p>
 *  <p>
 *  The expressions that you can create with this class are designed to
 *  represent "filters" or boolean predicates that can be applied to a
 *  DrawableInterface, returning true if the component "matches" the filter or false
 *  if the component does not match.
 *  </p>
 *  <p>
 *  Often, a filter object is created solely for the purpose of passing
 *  the filter into some other operation, such as a search operation.  For
 *  example, the student.ODTestCase class provides a
 *  {@link student.ODTestCase#getDrawable(Class,ODFilter) getDrawable()}
 *  method that takes a filter as a parameter.  For the examples below, we
 *  will use <code>getDrawable()</code> as the context, specifying each
 *  filter as an argument value in a call to that method.
 *  </p>
 *  <p>
 *  The basic principles for using this class are as follows:
 *  </p>
 *  <ul>
 *  <li><p>Never try to create a ODFilter object directly.  Instead, always
 *         write something that looks like a boolean expression, and that
 *         starts with the operator <code>where</code>:</p>
 *  <pre>
 *  FilledRect rect = getDrawable(FilledRect.class, where.heightIs(10));
 *  </pre></li>
 *  <li><p>The basic properties you can check with filters include:
 *         <code>sizeIs</code>, <code>textIs()</code>,
 *         <code>locationIs()</code>, <code>hiddenIs()</code>,
 *         and <code>typeIs()</code>.  They are
 *         all used the same way:</p>
 *  <pre>
 *  FilledRect rect = getDrawable(FilledRect.class, where.widthIs(10);
 *  FramedOval oval = getDrawable(FramedOval.class, where.hiddenIs(true));
 *  </pre></li>
 *  <li><p>You can combine filters using logical "and" as necessary:</p>
 *  <pre>
 *  FilledRect rect = getDrawable(FilledRect.class,
 *      where.heightIs(10).and.widthIs(20).and.hiddenIs(false));
 *  </pre></li>
 *  <li><p>You can also use "or":</p>
 *  <pre>
 *  FilledRect rect = getDrawable(FilledRect.class,
 *      where.heightIs(10).or.widthIs(20).or.hiddenIs(false));
 *  </pre></li>
 *  <li><p>Operators like "and" and "or" are interpreted strictly left to
 *  right.  There is <b>no precedence</b>, because of the way Java interprets
 *  dot notation.</p>
 *  <pre>
 *  FilledRect rect = getDrawable(FilledRect.class,
 *      where.heightIs(10).or.widthIs(20).and.hiddenIs(false));
 *      // means ((height = 10 or width = 20) and hidden = false)
 *      // note that the left operator is always evaluated first!
 *  </pre>
 *  <p>If you want to force a different order of evaluation
 *  than strictly left-to-right, then use parentheses by writing the
 *  appropriate operator as <code>and()</code> or <code>or()</code>.  Just
 *  be sure to start the new expression inside the parentheses with
 *  <code>where</code>:</p>
 *  <pre>
 *  FilledRect rect = getDrawable(FilledRect.class,
 *      where.heightIs(10).or(where.widthIs(20).and.hiddenIs(false)));
 *      // now means (height = 10 or (width = 20 and hidden = false))
 *      // because of the extra parentheses used
 *  </pre></li>
 *  <li><p>Finally, you can even use "not" (logical negation), but it is
 *  called like a method, so parentheses (and thus a leading <code>where)
 *  are <em>always required</em> to make the intended extent of the negation
 *  clear:</p>
 *  <pre>
 *  FilledRect rect = getDrawable(FilledRect.class,
 *      where.heightIs(10).and.not(where.widthIs(20).or.hiddenIs(true)));
 *  </pre></li>
 *  </ul>
 *
 *  @author  Stephen Edwards
 *  @author Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public abstract class ODFilter
{
    //~ Instance/static variables .............................................

    private String description;


    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new filter object.  This constructor is not public, since
     * all filters are expected to be created using operators rather than
     * by calling new.
     * @param description A string description of this filter, used in
     *                    {@link #toString()}.
     */
    protected ODFilter(String description)
    {
        this.description = description;
    }


    //~ Public Fields .........................................................

    // These fields are public to afford a more natural syntax, although they
    // can never be manipulated since they are final and have no mutators.
    // They are instance fields instead of static fields, because that is
    // necessary for their semantics.

    // ----------------------------------------------------------
    /**
     * The "and" operator for combining filters, designed to be used in
     * expressions like <code>where.heightIs(10).and.hiddenIs(true)</code>.
     * This operator is implemented as a public field so that the simple
     * <code>.and.</code> notation can be used as a connective between
     * filters.  If you want to use parentheses for grouping to define
     * the right argument, see {@link #and(ODFilter)} instead.
     */
    public final BinaryOperator and = new BinaryOperator() {
        // ----------------------------------------------------------
        @Override
        protected boolean combine(boolean leftResult, boolean rightResult)
        {
            return leftResult && rightResult;
        }

        // ----------------------------------------------------------
        @Override
        protected String description(
            String leftDescription, String rightDescription)
        {
            return "(" + leftDescription + " AND " + rightDescription + ")";
        }

    };


    // ----------------------------------------------------------
    /**
     * The "or" operator for combining filters, designed to be used in
     * expressions like <code>where.nameIs("abc").or.nameIs("def")</code>.
     * This operator is implemented as a public field so that the simple
     * <code>.or.</code> notation can be used as a connective between
     * filters.  If you want to use parentheses for grouping to define
     * the right argument, see {@link #or(ODFilter)} instead.
     */
    public final BinaryOperator or = new BinaryOperator() {
        // ----------------------------------------------------------
        @Override
        protected boolean combine(boolean leftResult, boolean rightResult)
        {
            return leftResult || rightResult;
        }

        // ----------------------------------------------------------
        @Override
        protected String description(
            String leftDescription, String rightDescription)
        {
            return "(" + leftDescription + " OR " + rightDescription + ")";
        }

    };


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     *  This base class represents an operator used to create a query.
     *  As the base class for all operators, it defines the primitive
     *  query operations supported for all {@link DrawableInterface} objects,
     *  each of which can be combined using any operator.
     */
    public static abstract class Operator
    {
        // ----------------------------------------------------------
        /**
         * Create a filter that checks the text of a shape by calling the
         * shape's <code>getText()</code> method, if it has one.
         * @param text The text to look for
         * @return A new filter that succeeds only on shape where
         *         <code>getText()</code> is a valid method and 
         *         returns the specified text.
         */
        public ODFilter textIs(final String text)
        {
            return applySelfTo(ODFilter.textIs(text));
        }


        
        // ----------------------------------------------------------
        /**
         * Create a filter that checks the class of a shape.
         * @param aClass The required class to check for (any subclass will
         *               also match).
         * @return A new filter that only succeeds on instances of the
         *         given class.
         */
        public ODFilter typeIs(final Class<? extends DrawableInterface> aClass)
        {
            return applySelfTo(ODFilter.typeIs(aClass));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a shape's width.
         * @param value The width to look for.
         * @return A new filter that succeeds only on shapes that have
         *         the given width.
         */
        public ODFilter widthIs(final int value)
        {
            return applySelfTo(ODFilter.widthIs(value));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a shape's height.
         * @param value The height to look for.
         * @return A new filter that succeeds only on shapes that have
         *         the given height.
         */
        public ODFilter heightIs(final int value)
        {
            return applySelfTo(ODFilter.heightIs(value));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a shape's size.
         * @param width The required width.
         * @param height The required height.
         * @return A new filter that succeeds only on shapes that have
         *         the given size.
         */
        public ODFilter sizeIs(final int width, final int height)
        {
            return applySelfTo(ODFilter.sizeIs(width, height));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a shape's size.
         * @param maxWidth The required width.
         * @param maxHeight The required height.
         * @return A new filter that succeeds only on shapes that have
         *         the given size.
         */
        public ODFilter sizeIsWithin(final int maxWidth, final int maxHeight)
        {
            return applySelfTo(ODFilter.sizeIsWithin(maxWidth, maxHeight));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a shape's x-coordinate.
         * @param x The required x-coordinate
         * @return A new filter that succeeds only on shapes that have
         *         the given x-coordinate.
         */
        public ODFilter xLocationIs(final int x)
        {
            return applySelfTo(ODFilter.xLocationIs(x));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a shape's y-coordinate.
         * @param y The required y-coordinate
         * @return A new filter that succeeds only on shapes that have
         *         the given y-coordinate.
         */
        public ODFilter yLocationIs(final int y)
        {
            return applySelfTo(ODFilter.yLocationIs(y));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a shape's location
         * @param x The required x-coordinate
         * @param y The required y-coordinate
         * @return A new filter that succeeds only on shapes that have
         *         the given location.
         */
        public ODFilter locationIs(final int x, final int y)
        {
            return applySelfTo(ODFilter.locationIs(x, y));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks whether a shape's location (its
         * top left corner) lies within a specific rectangle.
         * @param region A rectangle defining a region 
         * @return A new filter that succeeds only on components that have
         *         a location within the given region.
         */
        public ODFilter isLocatedWithin(final Rectangle region)
        {
            return applySelfTo(ODFilter.isLocatedWithin(region));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks whether a shape's bounding box
         * lies within a specific rectangle--that is, whether the entire 
         * shape's area rather than just its top left corner, lies within 
         * the specified region.
         * @param region A rectangle defining a region 
         * @return A new filter that succeeds only on shapes that
         *         lie entirely within the given region, as determined by
         *         {@link Rectangle#contains(Rectangle)}.
         */
        public ODFilter isContainedWithin(final Rectangle region)
        {
            return applySelfTo(ODFilter.isContainedWithin(region));
        }
        
        // ----------------------------------------------------------
        /**
         * Create a filter that checks a shape's color
         * @param color the color to check for
         * @return A new filter that succeeds only on shapes that are
         *  colored color.
         */
        public ODFilter colorIs(final Color color)
        {
            return applySelfTo(ODFilter.colorIs(color));
        }
        
        // ----------------------------------------------------------
        /**
         * Create a filter that checks whether a shape is hidden
         * @param hidden a boolean indicating the hidden state to check for
         * @return A new filter that succeeds only if the shape's hidden value
         * matches hidden.
         */
        public ODFilter hiddenIs(final boolean hidden)
        {
            return applySelfTo(ODFilter.hiddenIs(hidden));
        }
        
        // ----------------------------------------------------------
        /**
         * Create a filter that checks whether a line starts at a given coordinate
         * @param x the required x-coordinate
         * @param y the required y-coordinate
         * @return A new filter that succeeds only lines that start at (x, y)
         */
        public ODFilter lineStartLocationIs(final int x, final int y)
        {
            return applySelfTo(ODFilter.lineStartPointIs(new Location(x, y)));
        }
        
        /**
         * Create a filter that checks whether a line starts at a given location
         * @param location the required Location
         * @return A new filter that succeeds only for lines that start at location
         */
        public ODFilter lineStartLocationIs(final Location location)
        {
            return applySelfTo(ODFilter.lineStartPointIs(location));
        }
        
        /**
         * Create a filter that checks whether a line ends at a given coordinate
         * @param x the required x-coordinate
         * @param y the required y-coordinate
         * @return A new filter that succeeds only for lines that end at (x, y)
         */
        public ODFilter lineEndLocationIs(final int x, final int y)
        {
            return applySelfTo(ODFilter.lineEndPointIs(new Location(x, y)));
        }
        
        /**
         * Create a filter that checks whether a line starts at a given location
         * @param location the required Location
         * @return A new filter that succeeds only lines that end at location.
         */
        public ODFilter lineEndLocationIs(final Location l)
        {
            return applySelfTo(ODFilter.lineEndPointIs(l));
        }
        
        


        // ----------------------------------------------------------
        /**
         * Concrete subclasses must override this to implement an
         * operation on the filter being passed in to transform it into
         * another filter.
         * @param otherFilter The argument to transform (second argument,
         *               for binary operators)
         * @return A new compound filter that includes the given argument
         *         as one subfilter, after applying this operator to it.
         */
        protected abstract ODFilter applySelfTo(final ODFilter otherFilter);

    }


    // ----------------------------------------------------------
    /**
     * A non-static subclass for binary operators that implicitly
     * captures the outer filter to which it belongs, using it as
     * the first/left argument to the operator.
     */
    public abstract class BinaryOperator
        extends Operator
    {
        // ----------------------------------------------------------
        /**
         * The "not" operator for negating an existing filter, when you
         * want to use parentheses to group its righthand argument.  This
         * method is designed to be used in expressions like
         * <code>where.nameIs("abc").and.not(enabledIs(true).or.hasFocusIs(true))</code>.
         * If you wish to use the <code>.not.</code> notation instead, leaving
         * off the parentheses, see {@link BinaryOperator#not}.
         *
         * @param otherFilter The filter to negate
         * @return A new filter that represents a combination of the left
         *         filter with "NOT otherFilter".
         */
        public ODFilter not(final ODFilter otherFilter)
        {
            return applySelfTo(primitiveNot(otherFilter));
        }


        // ----------------------------------------------------------
        /**
         * Implements a composite filter based on a binary operation,
         * where the "left"/"first" filter is the parent from which this
         * class was created, and the "right"/"second" filter is the
         * argument supplied to this operation.
         * @param otherFilter The argument to transform (second argument,
         *               for binary operators)
         * @return A new compound filter that represents a combination
         *         of the first and second filters.
         */
        @Override
        protected ODFilter applySelfTo(final ODFilter otherFilter)
        {
            return new ODFilter(description(
                ODFilter.this.toString(), otherFilter.toString()))
            {
                public boolean test(DrawableInterface shape)
                {
                    return combine(ODFilter.this.test(shape),
                        otherFilter.test(shape));
                }
            };
        }


        // ----------------------------------------------------------
        /**
         * Concrete subclasses must override this to implement the
         * appropriate logic for combining the results of the two filters
         * being combined.
         * @param leftResult The boolean result of the left filter
         * @param rightResult The boolean result of the right filter
         * @return The result of this combined filter.
         */
        protected abstract boolean combine(
            boolean leftResult, boolean rightResult);


        // ----------------------------------------------------------
        /**
         * Concrete subclasses must override this to implement the
         * appropriate logic for building a description of this filter
         * based on the descriptions of the two filters
         * being combined.
         * @param leftDescription The description of the left filter
         * @param rightDescription The description of the right filter
         * @return The description of this combined filter.
         */
        protected abstract String description(
            String leftDescription, String rightDescription);
    }


    // ----------------------------------------------------------
    /**
     * Get a string representation of this filter.
     * @return A string representation of this filter.
     */
    public String toString()
    {
        return description;
    }


    // ----------------------------------------------------------
    /**
     * The "and" operator for combining filters, when you want to use
     * parentheses to group its righthand argument.  This method is designed
     * to be used in expressions like
     * <code>where.nameIs("abc").and(enabledIs(true).or.hasFocusIs(true))</code>.
     * If you wish to use the <code>.and.</code> notation instead, leaving
     * off the parentheses, see {@link BinaryOperator#and(ODFilter)}.
     *
     * @param otherFilter  The second argument to "and".
     * @return A new filter object that represents "this AND otherFilter".
     */
    public final ODFilter and(final ODFilter otherFilter)
    {
        final ODFilter self = this;
        ODFilter gf =  new ODFilter("(" + this + " AND " + otherFilter + ")")
        {
            public boolean test(DrawableInterface shape)
            {
               return self.test(shape) && otherFilter.test(shape);
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    /**
     * The "or" operator for combining filters, when you want to use
     * parentheses to group its righthand argument.  This method is designed
     * to be used in expressions like
     * <code>where.heightIs(10).or(hiddenIs(true).and.widthIs(20))</code>.
     * If you wish to use the <code>.or.</code> notation instead, leaving
     * off the parentheses, see {@link BinaryOperator#or(ODFilter)}.
     *
     * @param otherFilter  The second argument to "or".
     * @return A new filter object that represents "this OR otherFilter".
     */
    public final ODFilter or(final ODFilter otherFilter)
    {
        final ODFilter self = this;
        ODFilter gf = new ODFilter("(" + this + " OR " + otherFilter + ")")
        {
            public boolean test(DrawableInterface shape)
            {
                return self.test(shape) || otherFilter.test(shape);
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    /**
     * Evaluate whether a DrawableInterface matches this filter.  This operation is
     * intended to be overridden by each subclass to implement the actual
     * check that a specific kind of filter performs.
     * @param shape The DrawableInterface to check
     * @return true if the component matches this filter
     */
    public abstract boolean test(DrawableInterface shape);


    // ----------------------------------------------------------
    /**
     * This class represents the "where" operator that is used to begin
     * a filter expression.  Client classes that wish to support filter
     * syntax should declare a final field (static or instance) like
     * this:
     * <pre>
     * public static final ODFilter.WhereOperator where =
     *     new ODFilter.WhereOperator();
     * </pre>
     */
    public static class ClientImports
    {
        // ----------------------------------------------------------
        /**
         * This object represents the "where" operator that is used to begin
         * a filter expression.
         */
        public static final Operator where = new Operator() {
            // ----------------------------------------------------------
            @Override
            protected ODFilter applySelfTo(ODFilter filter)
            {
                return filter;
            }
        };


        // ----------------------------------------------------------
        /**
         * The "not" operator for negating an existing filter, when the not
         * operation is at the very beginning of the expression.  This
         * method is designed to be used in expressions like
         * <code>not(where.enabledIs(true).or.hasFocusIs(true))</code>.
         *
         * @param otherFilter The filter to negate
         * @return A new filter that represents a combination of the left
         *         filter with "NOT otherFilter".
         */
        public static ODFilter not(final ODFilter otherFilter)
        {
            return primitiveNot(otherFilter);
        }
    }


    //~ Private Methods/Declarations ..........................................

    // ----------------------------------------------------------
    private static ODFilter textIs(final String text)
    {
        ODFilter gf = new ODFilter("text = \"" + text + "\"")
        {
            public boolean test(DrawableInterface shape)
            {
                Method m = null;
                try
                {
                    m = shape.getClass().getMethod("getText");
                    return ((String)m.invoke(shape)).equals(text);
                }
                catch ( Exception e )
                {
                    return false;
                }
            }
        };
        return gf;
    }





    // ----------------------------------------------------------
    private static final ODFilter widthIs(final int value)
    {
        ODFilter gf = new ODFilter("width = " + value)
        {
            public boolean test(DrawableInterface shape)
            {
                return (shape instanceof Drawable2DInterface) ?
                ((Drawable2DInterface)shape).getWidth() == value : 
                    false;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final ODFilter heightIs(final int value)
    {
        ODFilter gf = new ODFilter("height = " + value)
        {
            public boolean test(DrawableInterface shape)
            {
                return (shape instanceof Drawable2DInterface) ? 
                ((Drawable2DInterface)shape).getHeight() == value :
                    false;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final ODFilter sizeIs(final int width, final int height)
    {
        ODFilter gf = new ODFilter("size = (" + width + ", " + height + ")")
        {
            public boolean test(DrawableInterface shape)
            {
                return (shape instanceof Drawable2DInterface) ?
                    ((Drawable2DInterface)shape).getWidth() == width
                    && ((Drawable2DInterface)shape).getHeight() == height :
                        false;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final ODFilter sizeIsWithin(
        final int maxWidth, final int maxHeight)
    {
        ODFilter gf = new ODFilter(
            "sizeIsWithin(" + maxWidth + ", " + maxHeight + ")")
        {
            public boolean test(DrawableInterface shape)
            {
                return (shape instanceof Drawable2DInterface) ? 
                    ((Drawable2DInterface)shape).getWidth() <= maxWidth
                    && ((Drawable2DInterface)shape).getHeight() <= maxHeight :
                        false;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final ODFilter xLocationIs(final int value)
    {
        ODFilter gf = new ODFilter("xLocation = " + value)
        {
            public boolean test(DrawableInterface shape)
            {
                return(shape instanceof Drawable2DInterface) ? 
                    ((Drawable2DInterface)shape).getX() == value :
                        false;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final ODFilter yLocationIs(final int value)
    {
        ODFilter gf = new ODFilter("yLocation = " + value)
        {
            public boolean test(DrawableInterface shape)
            {
                return (shape instanceof Drawable2DInterface) ?
                    ((Drawable2DInterface)shape).getY() == value :
                        false;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final ODFilter locationIs(final int x, final int y)
    {
        ODFilter gf = new ODFilter("location = (" + x + ", " + y + ")")
        {
            public boolean test(DrawableInterface shape)
            {
                
                return (shape instanceof Drawable2DInterface) ?
                    ((Drawable2DInterface)shape).getX() == x 
                    && ((Drawable2DInterface)shape).getY() == y :
                        false;
               
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final ODFilter isLocatedWithin(final Rectangle region)
    {
        ODFilter gf = new ODFilter("isLocatedWithin(" + region + ")")
        {
            public boolean test(DrawableInterface shape)
            {
                return (shape instanceof Drawable2DInterface) ?
                    region.contains(((Drawable2DInterface)shape).getLocation().toPoint()) :
                        false;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final ODFilter isContainedWithin(final Rectangle region)
    {
        ODFilter gf = new ODFilter("isContainedWithin(" + region + ")")
        {
            public boolean test(DrawableInterface shape)
            {
                return (shape instanceof Drawable2DInterface) ?
                    region.contains(new Rectangle(
                        (int)((Drawable2DInterface)shape).getX(), 
                        (int)((Drawable2DInterface)shape).getY(), 
                        (int)((Drawable2DInterface)shape).getWidth(),
                        (int)((Drawable2DInterface)shape).getHeight())):
                            false;
            }
        };
        return gf;
    }
    
    private static final ODFilter hiddenIs(final boolean hidden)
    {
        ODFilter gf = new ODFilter("hidden is " + hidden + ".")
        {
            public boolean test(DrawableInterface shape)
            {
                return shape.isHidden() == hidden;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static ODFilter typeIs(final Class<? extends DrawableInterface> aClass)
    {
        ODFilter gf = new ODFilter("type = " + aClass.getSimpleName())
        {
            public boolean test(DrawableInterface shape)
            {
                return aClass.isAssignableFrom(shape.getClass());
            }
        };
        return gf;
    }
    
    private static ODFilter colorIs(final Color color)
    {
        ODFilter odf = new ODFilter("color = " + color)
        {
            public boolean test(DrawableInterface shape)
            {
                return shape.getColor() == color;
            }
        };
        return odf;
    }
    
    private static ODFilter lineStartPointIs(final Location l)
    {
        ODFilter odf = new ODFilter("line start point is (" 
            + l.getX() + ", " + l.getY() +")")
        {
            public boolean test(DrawableInterface shape)
            {
                return (shape instanceof Drawable1DInterface) ?
                    ((Drawable1DInterface)shape).getStart().equals(l) :
                    false;
            }
        };
        return odf;
    }
    
    private static ODFilter lineEndPointIs(final Location l)
    {
        ODFilter odf = new ODFilter("line end point is (" 
            + l.getX() + ", " + l.getY() +")")
        {
            public boolean test(DrawableInterface shape)
            {
                return (shape instanceof Drawable1DInterface) ?
                    ((Drawable1DInterface)shape).getEnd().equals(l) :
                        false;
            }
        };
        return odf;
    }


    // ----------------------------------------------------------
    private static final ODFilter primitiveNot(final ODFilter otherFilter)
    {
        return new ODFilter("(NOT " + otherFilter + ")")
        {
            public boolean test(DrawableInterface shape)
            {
                return !otherFilter.test(shape);
            }
        };
    }

}