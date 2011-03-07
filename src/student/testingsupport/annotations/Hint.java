/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2011 Virginia Tech
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

package student.testingsupport.annotations;

import java.lang.annotation.*;

//-------------------------------------------------------------------------
/**
 *  Annotation used to provide explicit hint text for a test case method
 *  or class.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author$
 *  @version $Revision$, $Date$
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Hint
{
    /** The hint text to use. */
    String value();
}
