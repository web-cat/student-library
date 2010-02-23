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

package student.web.internal;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Field;

//-------------------------------------------------------------------------
/**
 *  A custom subclass of {@link XStream} that allows for flexible/forgiving
 *  reconstruction of objects.  Missing classes don't cause errors--the
 *  corresponding objects are reconstituted as null.  Missing fields do
 *  not cause errors, either--they are simply ignored.
 *
 *  @author Stephen Edwards
 *  @author Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public class FlexibleXStream
    extends XStream
{
    // ----------------------------------------------------------
    /**
     * Create a new object.
     */
    public FlexibleXStream()
    {
        super();
    }


    // ----------------------------------------------------------
    /**
     * Redefined to inject a {@link FlexibleMapper} into this object's
     * mapper sequence.
     * <p>
     * XStream doesn't provide an easy way to hook into the
     * built-in mapper creation sequence.  Also, the method that
     * builds the default sequence is private, so it can't be easily
     * overloaded.
     * </p><p>
     * Adding extra steps in a subclass constructor also doesn't work,
     * since later actions--specifically, converter setup--uses the
     * generated mapper before you have a chance to modify it.
     * </p><p>
     * <code>setupAliases()</code> can be overridden, however.  It is
     * called immediately after the default mapper is set up, so this
     * implementation has nothing to do with aliasing and is just being
     * exploited to inject a change to the default mapper sequence.
     * </p><p>
     * It's either that, or reproduce the full default mapper sequence
     * and pass it into the constructor instead.
     * </p>
     */
    @Override
    protected void setupAliases()
    {
        Mapper mapper = new FlexibleMapper(getMapper());

        try
        {
            Field mapperField = XStream.class.getDeclaredField("mapper");

            mapperField.setAccessible(true);
            mapperField.set(this, mapper);
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }

        super.setupAliases();
    }

}
