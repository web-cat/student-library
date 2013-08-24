/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2007-2011 Virginia Tech
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//-------------------------------------------------------------------------
/**
 *  This class provides static helper methods to use reflection to check
 *  for and invoke methods on objects.  It is intended for use in test
 *  cases, and makes it easier to write test cases that compile successfully
 *  but fail at run-time if the class under test fails to provide
 *  required methods (or provides them with the wrong signature).  For
 *  Web-CAT users, this makes it possible to get partial scores, even when
 *  some methods are not even provided in the class under test.
 *  <p>
 *  Consider a situation where you are writing a test case that invokes the
 *  <code>doIt()</code> method on a given object.  You might do it like
 *  this:
 *  </p>
 *  <pre>
 *  MyType result = receiver.doIt(param1, param2);  // returns a value
 *  // ...
 *  receiver.doSomethingElse();  // a void method with no parameters
 *  </pre>
 *  <p>The first line passes two parameters to <code>doIt()</code> and stores
 *  the return value in a local variable called <code>result</code>.  All
 *  this would be fine if the class under test actually provides a method
 *  called <code>doIt()</code> with the appropriate signature and return
 *  type.  Otherwise, you would get a compile-time error.  But on Web-CAT
 *  a compilation error in a test suite would give a resulting score of
 *  zero--no successful compilation, no partial credit.  The story is similar
 *  with the second line, which simply calls a void method on the class
 *  under test.
 *  </p>
 *  <p>What if, instead, you wanted to write test cases that <em>checked
 *  for</em> specific methods, and either succeeded or failed depending on
 *  whether the method was present?  You can use the methods in this
 *  class to write such test cases.  So instead of writing the call
 *  above, you could do this:</p>
 *  <pre>
 *  // At the top of your test class
 *  import static net.sf.webcat.ReflectionSupport.*;
 *
 *  // ...
 *
 *  // For a method that returns a result:
 *  MyType result = invoke(receiver, MyType.class, "doIt", param1, param2);
 *
 *  // Or for a void method:
 *  invoke(receiver, "doSomethingElse");
 *  </pre>
 *  <p>
 *  The syntax is simple and straight forward.  The overloaded invoke
 *  method can be used on functions (methods that return a value) or
 *  procedures (void methods that return nothing).  For methods that
 *  return a value, you specify the type of the return value as the
 *  second parameter.  If you omit the return type, then it is assumed to
 *  be "void".  You specify the name of the method as a string, and then
 *  a variable length set of arguments.
 *  </p>
 *  <p>
 *  If you are calling a method that is returning a primitive type, be
 *  sure to use the corresponding wrapper class as the expected return
 *  value:
 *  </p>
 *  <pre>
 *  // Instead of boolean answer = receiver.equals(anotherObject);
 *  Boolean answer = invoke(receiver, Boolean.class, "equals", anotherObject);

 *  // Or in an assert, using auto-unboxing:
 *  assertTrue(invoke(receiver, Boolean.class, "equals", anotherObject));
 *  </pre>
 *  <p>
 *  Any errors that occur during reflection, such as failing to find the
 *  required method, failing to find a method with the required signature,
 *  finding a method that is not public, etc., are converted into
 *  appropriate test case errors with meaningful diagnostic hints for the
 *  student.  Any exceptions are wrapped in a RuntimeException and need not
 *  be explicitly caught by the caller (they will turn into test case
 *  failures as well).
 *  </p>
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public class ReflectionSupport
{
    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * This class only provides static methods, so the constructor is
     * private.
     */
    private ReflectionSupport()
    {
        // Nothing to do
    }


    //~ Public classes used inside this class .................................

    // ----------------------------------------------------------
    /**
     * A custom error class that represents any error conditions that
     * arise in the reflection-based methods provided by this class.
     */
    public static class ReflectionError
        extends AssertionError
    {
        private static final long serialVersionUID = 4456797064805957471L;

        ReflectionError(String message)
        {
            super(message);
        }
    }


    // ----------------------------------------------------------
    /**
     * An enumeration that represents a set of constants for specifying
     * constraints on the visibility of a declaration.
     */
    public static enum VisibilityConstraint
    {
        /** Declared with private visibility (only). */
        DECLARED_PRIVATE("declared private", Modifier.PRIVATE),
        /** Declared with package-level (default) visibility (only). */
        DECLARED_PACKAGE("declared package-level",
            Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC, true),
        /** Declared with protected visibility (only). */
        DECLARED_PROTECTED("declared protected", Modifier.PROTECTED),
        /** Declared with public visibility (only). */
        DECLARED_PUBLIC("declared public", Modifier.PUBLIC),
        /** Declared with protected or public visibility. */
        AT_LEAST_PROTECTED("declared public or protected",
            Modifier.PROTECTED | Modifier.PUBLIC),
        /** Declared with package-level (default), protected, or public
         * visibility.
         */
        AT_LEAST_PACKAGE("declared public, protected, or package-level",
            Modifier.PRIVATE, true),
        /** Declared with any visibility. */
        ANY_VISIBILITY("declared with any visibility", 0)
        {
            public boolean accepts(int modifiers) { return true; }
        };


        // ----------------------------------------------------------
        private VisibilityConstraint(String message, int mask)
        {
            this(message, mask, false);
        }


        // ----------------------------------------------------------
        private VisibilityConstraint(
            String message, int mask, boolean reverseMask)
        {
            this.message = message;
            this.mask = mask;
            this.reverseMask = reverseMask;
        }


        // ----------------------------------------------------------
        /**
         * Determine if a given set of modifiers, expressed as an integer
         * mask, meets this visibility constraint.
         * @param modifiers The modifiers to check.
         * @return True if the modifiers are consistent with this constraint.
         */
        public boolean accepts(int modifiers)
        {
            boolean result = reverseMask;
            if ((mask & modifiers) != 0)
            {
                result = !result;
            }
            return result;
        }


        // ----------------------------------------------------------
        /**
         * Determine if a given member meets this visibility constraint.
         * @param member The member to check.
         * @return True if the member's visibility is consistent with this
         * constraint.
         */
        public boolean accepts(java.lang.reflect.Member member)
        {
            return accepts(member.getModifiers());
        }


        // ----------------------------------------------------------
        /**
         * Get the string message describing this constraint, for use in
         * diagnostic output.
         * @return A readable description of this constraint.
         */
        public String getMessage()
        {
            return message;
        }


        // ----------------------------------------------------------
        /**
         * {@inheritDoc}
         */
        public String toString()
        {
            return getMessage();
        }


        //~ Fields ............................................................

        private int mask = 0;
        private boolean reverseMask = false;
        private final String message;
    };


    // ----------------------------------------------------------
    /**
     * An enumeration that represents the different categories of
     * parameter matching possible between a method and possible
     * parameters.  Section
     */
    public static enum ParameterAcceptanceCategory
    {
        /** The method will not accept the parameters. */
        DOES_NOT_ACCEPT,
        /**
         * The method will accept the parameters with identity or widening
         * conversions, but does not require auto-boxing or unboxing.
         */
        ACCEPTS_WITHOUT_BOXING,
        /**
         * The method will accept the parameters with identity or widening
         * conversions, but also requires auto-boxing or unboxing for one
         * or more of the parameters.
         */
        ACCEPTS_WITH_BOXING
    }


    // ----------------------------------------------------------
    /**
     * An enumeration that represents the different kinds of conversions
     * supported for method invocation conversions (see JLS Section 5.3).
     */
    public static enum ParameterConversionCategory
    {
        /** The formal and actual types are the same. */
        IDENTITY_CONVERSION,
        /** Widening conversion from one primitive type to another. */
        PRIMITIVE_WIDENING,
        /** Widening conversion from one reference type to another. */
        REFERENCE_WIDENING,
        /** Boxing conversion from a primitive type to its wrapper. */
        BOXING,
        /** Unboxing conversion from a wrapper type to its primitive. */
        UNBOXING,
        /** Boxing followed by a reference widening conversion. */
        BOXING_WITH_REFERENCE_WIDENING,
        /** Unboxing followed by a primitive widening conversion. */
        UNBOXING_WITH_PRIMITIVE_WIDENING,
        /** No method invocation conversion possible. */
        CANNOT_CONVERT
    }


    // ----------------------------------------------------------
    /**
     * This class only represents the parameter list signature for a
     * method as a list of Class values.
     */
    public static class ParameterSignature
        extends ArrayList<Class<?>>
    {
        private static final long serialVersionUID = -2346356585877970638L;

        //~ Constructors ......................................................

        // ----------------------------------------------------------
        /**
         * Create an empty parameter list signature.
         */
        public ParameterSignature()
        {
            super();
        }


        // ----------------------------------------------------------
        /**
         * Create a parameter list signature corresponding to the declared
         * parameter list of a Method.
         * @param method The method whose signature will be represented.
         */
        public ParameterSignature(Method method)
        {
            this(method.getParameterTypes());
        }


        // ----------------------------------------------------------
        /**
         * Create a parameter list signature corresponding to the declared
         * parameter list of a Constructor.
         * @param constructor The constructor whose signature will be
         *                    represented.
         */
        public ParameterSignature(Constructor<?> constructor)
        {
            this(constructor.getParameterTypes());
        }


        // ----------------------------------------------------------
        /**
         * Create a parameter list signature from an array (or variable
         * argument list) of Class values.
         * @param parameterTypes The sequence of Class objects representing
         * the types of the parameters in this signature.
         */
        public ParameterSignature(Class<?>... parameterTypes)
        {
            super(parameterTypes == null ? 0 : parameterTypes.length);
            if (parameterTypes != null)
            {
                for (Class<?> c : parameterTypes)
                {
                    add(c);
                }
            }
        }


        // ----------------------------------------------------------
        /**
         * Create a parameter list signature from an array (or variable
         * argument list) of actuals.
         * @param parameters A sequence of actual parameters from which
         * the types are deduced.  Any nulls in the list are treated to
         * be of type Object.
         */
        public ParameterSignature(Object... parameters)
        {
            super(parameters == null ? 0 : parameters.length);
            if (parameters != null)
            {
                for (Object parameter : parameters)
                {
                    if (parameter == null)
                    {
                        add(null);
                    }
                    else
                    {
                        // Convert from wrapper classes back to corresponding
                        // primitive types, since all primitives are wrapped
                        // in a varargs method (one of the other constructors
                        // should be used in wrapper parameter types are
                        // required).
                        Class<?> raw = parameter.getClass();
                        Class<?> c = WRAPPER_TO_PRIMITIVE.get(raw);
                        if (c == null)
                        {
                            c = raw;
                        }
                        add(c);
                    }
                }
            }
        }


        //~ Methods ...........................................................

        // ----------------------------------------------------------
        /**
         * Determine if the given set of actual argument types would be
         * accepted by a method with this parameter signature.
         * @param typesOfActuals The types of the actual arguments to check
         *                       against.
         * @return True If the specified actual types would be accepted by
         * a method with this parameter signature.
         */
        public boolean accepts(ParameterSignature typesOfActuals)
        {
            return acceptanceCategory(typesOfActuals) !=
                ParameterAcceptanceCategory.DOES_NOT_ACCEPT;
        }


        // ----------------------------------------------------------
        /**
         * Determine if the given set of actual argument types would be
         * accepted by a method with this parameter signature.
         * @param typesOfActuals The types of the actual arguments to check
         *                       against.
         * @return True If the specified actual types would be accepted by
         * a method with this parameter signature.
         */
        public ParameterAcceptanceCategory acceptanceCategory(
            ParameterSignature typesOfActuals)
        {
            ParameterAcceptanceCategory result =
                ParameterAcceptanceCategory.DOES_NOT_ACCEPT;
            if (size() == typesOfActuals.size())
            {
                result = ParameterAcceptanceCategory.ACCEPTS_WITHOUT_BOXING;

                loop:
                for (int i = 0; i < size(); i++)
                {
                    switch (canCoerceFromActualToFormal(
                        typesOfActuals.get(i), get(i)))
                    {
                        case CANNOT_CONVERT:
                            result = ParameterAcceptanceCategory
                                .DOES_NOT_ACCEPT;
                            break loop;
                        case BOXING:
                        case UNBOXING:
                        case BOXING_WITH_REFERENCE_WIDENING:
                        case UNBOXING_WITH_PRIMITIVE_WIDENING:
                            result = ParameterAcceptanceCategory
                                .ACCEPTS_WITH_BOXING;
                            break;
                    }
                }
            }
            return result;
        }


        // ----------------------------------------------------------
        /**
         * Determine if the given set of actual parameter values would be
         * accepted by a method with this parameter signature.
         * @param actuals The types of the actual arguments to check
         *                against.
         * @return True If the specified actuals would be accepted by
         * a method with this parameter signature.
         */
        public boolean accepts(Object... actuals)
        {
            return accepts(new ParameterSignature(actuals));
        }


        // ----------------------------------------------------------
        /**
         * Determine if this signature is a subsignature of the other one,
         * as defined by Section 8.4.2 of the JLS.  Since generic parameters
         * are represented as their erasures in this representation,
         * the subsignature check boils down to an equality check.
         * @param other The parameter signature to check against.
         * @return True If this signature is a subsignature of the other.
         */
        public boolean isSubSignatureOf(ParameterSignature other)
        {
            return equals(other);
        }


        // ----------------------------------------------------------
        /**
         * Determine if this parameter signature is more specific than the
         * given parameter signature.
         * @param other The other parameter signature to check against.
         * @return True If this parameter signature is more specific than
         * the other.
         */
        public boolean isMoreSpecificThan(ParameterSignature other)
        {
            return other.accepts(this) && !isSubSignatureOf(other);
        }


        // ----------------------------------------------------------
        /**
         * Retrieve a plain array containing the parameter types in
         * this signature.
         * @return An array of Class values representing the parameter
         * types in this parameter signature object.
         */
        public Class<?>[] asArray()
        {
            return toArray(new Class<?>[size()]);
        }


        // ----------------------------------------------------------
        /**
         * {@inheritDoc}
         */
        public String toString()
        {
            String result = "(";
            boolean needsComma = false;
            for (Class<?> c : this)
            {
                if (needsComma)
                {
                    result += ", ";
                }
                if (c == null)
                {
                    result += "null";
                }
                else
                {
                    result += simpleClassName(c);
                }
                needsComma = true;
            }
            result += ")";
            return result;

        }
    }


    //~ Object Creation Methods ...............................................

    // ----------------------------------------------------------
    /**
     * Dynamically look up a class by name, with appropriate hints if the
     * class cannot be found.
     * @param className The type of object to create
     * @return The corresponding Class object
     */
    public static Class<?> getClassForName(String className)
    {
        for (ClassLoader loader : getCandidateLoaders())
        {
            try
            {
                if (loader != null)
                {
                    return loader.loadClass(className);
                }
            }
            catch (ClassNotFoundException e)
            {
                // Ignore it and try the next loader
            }
        }

        // Class wasn't found in any candidate loader
        fail("cannot find class " + className);

        // Unreachable, so just to make the compiler happy:
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Look up a constructor by parameter profile, finding the
     * constructor that will accept the given list of parameters (not
     * requiring an exact match on parameter types).  It turns any
     * errors into test case failures with appropriate hint messages.
     * Assumes the intended constructor should be public, and fails with an
     * appropriate hint if it is not.
     * Note that this method <b>does not handle variable argument lists</b>
     * in the target constructor for which it is searching.
     * @param c The type of object to create
     * @param params The constructor's parameter profile
     * @return The corresponding Constructor object
     */
    public static Constructor<?> getMatchingConstructor(
        Class<?> c, Class<?> ... params)
    {
        return getMatchingConstructor(c, new ParameterSignature(params));
    }


    // ----------------------------------------------------------
    /**
     * Look up a constructor by parameter profile, finding the
     * constructor that will accept the given list of parameters (not
     * requiring an exact match on parameter types).  It turns any
     * errors into test case failures with appropriate hint messages.
     * Assumes the intended constructor should be public, and fails with an
     * appropriate hint if it is not.
     * Note that this method <b>does not handle variable argument lists</b>
     * in the target constructor for which it is searching.
     * @param c The type of object to create
     * @param params The constructor's parameter profile
     * @return The corresponding Constructor object
     */
    public static Constructor<?> getMatchingConstructor(
        Class<?> c, ParameterSignature params)
    {
        return getMatchingConstructor(
            c, VisibilityConstraint.DECLARED_PUBLIC, params);
    }


    // ----------------------------------------------------------
    /**
     * Look up a constructor by parameter profile, finding the
     * constructor that will accept the given list of parameters (not
     * requiring an exact match on parameter types).  It turns any
     * errors into test case failures with appropriate hint messages.
     * Assumes the intended constructor should be public, and fails with an
     * appropriate hint if it is not.
     * Note that this method <b>does not handle variable argument lists</b>
     * in the target constructor for which it is searching.
     * @param c The type of object to create
     * @param visibility The required visibility of the method
     * @param params The constructor's parameter profile
     * @return The corresponding Constructor object
     */
    public static Constructor<?> getMatchingConstructor(
        Class<?> c, VisibilityConstraint visibility, Class<?> ... params)
    {
        return getMatchingConstructor(
            c, visibility, new ParameterSignature(params));
    }


    // ----------------------------------------------------------
    /**
     * Look up a constructor by parameter profile, finding the
     * constructor that will accept the given list of parameters (not
     * requiring an exact match on parameter types).  It turns any
     * errors into test case failures with appropriate hint messages.
     * Assumes the intended constructor should be public, and fails with an
     * appropriate hint if it is not.
     * Note that this method <b>does not handle variable argument lists</b>
     * in the target constructor for which it is searching.
     * @param c The type of object to create
     * @param visibility The required visibility of the method
     * @param params The constructor's parameter profile
     * @return The corresponding Constructor object
     */
    public static Constructor<?> getMatchingConstructor(
        Class<?> c, VisibilityConstraint visibility, ParameterSignature params)
    {
        // Non-boxing (preferred) answer
        Constructor<?> result = null;
        ParameterSignature resultSig = null;

        // Auto-boxing/unboxing answer
        Constructor<?> ctorWithBoxing = null;
        ParameterSignature ctorWithBoxingSig = null;

        // Wrong visibility, no boxing, nearest match?
        Constructor<?> notVisible = null;
        ParameterSignature notVisibleSig = null;

        // Wrong visibility, with auto-boxing/unboxing, nearest match?
        Constructor<?> notVisibleWithBoxing = null;
        ParameterSignature notVisibleWithBoxingSig = null;

        // Same parameter count, if nothing else, also no match
        Constructor<?> ctorWithSameParamCount = null;

        for (Constructor<?> ctor : c.getDeclaredConstructors())
        {
            ParameterSignature sig = new ParameterSignature(ctor);
            if (params.size() == sig.size() && ctorWithSameParamCount == null)
            {
                ctorWithSameParamCount = ctor;
            }

            ParameterAcceptanceCategory category =
                sig.acceptanceCategory(params);
            switch (category)
            {
                case ACCEPTS_WITHOUT_BOXING:
                    if (visibility.accepts(ctor.getModifiers()))
                    {
                        if (result == null
                            || sig.isMoreSpecificThan(resultSig))
                        {
                            result = ctor;
                            resultSig = sig;
                        }
                    }
                    else
                    {
                        if (notVisible == null
                            || sig.isMoreSpecificThan(notVisibleSig))
                        {
                            notVisible = ctor;
                            notVisibleSig = sig;
                        }
                    }
                    break;
                case ACCEPTS_WITH_BOXING:
                    if (visibility.accepts(ctor.getModifiers()))
                    {
                        if (ctorWithBoxing == null
                            || sig.isMoreSpecificThan(ctorWithBoxingSig))
                        {
                            ctorWithBoxing = ctor;
                            ctorWithBoxingSig = sig;
                        }
                    }
                    else
                    {
                        if (notVisibleWithBoxing == null
                            || sig.isMoreSpecificThan(notVisibleWithBoxingSig))
                        {
                            notVisibleWithBoxing = ctor;
                            notVisibleWithBoxingSig = sig;
                        }
                    }
                    break;
            }
        }
        if (result == null)
        {
            // Use non-boxing signature, if one exists, according to
            // JLS 15.12.2.
            result = ctorWithBoxing;
            resultSig = ctorWithBoxingSig;
        }
        if (result == null)
        {
            String message = null;
            if (notVisible != null)
            {
                message = "cannot call constructor "
                    + simpleClassName(c) + notVisibleSig
                    + " because it is not " + visibility;
            }
            else if (notVisibleWithBoxing != null)
            {
                message = "cannot call constructor "
                    + simpleClassName(c) + notVisibleWithBoxingSig
                    + " because it is not " + visibility;
            }
            else if (ctorWithSameParamCount != null)
            {
                message = "constructor cannot be called with argument"
                    + ((params.size() == 1) ? "" : "s")
                    + " of type "
                    + params
                    + ": incorrect parameter type(s)";
            }
            else
            {
                message = "" + c + " is missing constructor "
                    + simpleClassName(c) + params
                    + " that is " + visibility;
            }
            fail(message);
        }
        if (!result.isAccessible())
        {
            final Constructor<?> constructor = result;
            AccessController.doPrivileged( new PrivilegedAction<Void>()
                {
                    public Void run()
                    {
                        constructor.setAccessible(true);
                        return null;
                    }
                });
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Just like {@link Constructor#newInstance(Object...)}, but converts
     * any thrown exceptions into RuntimeExceptions.
     * @param constructor The constructor to invoke
     * @param params The parameters to pass to the constructor
     * @param <T> The generic parameter T is deduced from the provided
     *            constructor
     * @return The newly created object
     */
    public static <T> T create(Constructor<T> constructor, Object ... params)
    {
        T result = null;
        try
        {
            result = constructor.newInstance(params);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e;
            while (cause.getCause() != null)
            {
                cause = cause.getCause();
            }
            throw new RuntimeException(cause);
        }
        catch (InstantiationException e)
        {
            Throwable cause = e;
            while (cause.getCause() != null)
            {
                cause = cause.getCause();
            }
            throw new RuntimeException(cause);
        }
        catch (IllegalAccessException e)
        {
            // This should never happen, since getMethod() has already
            // done the appropriate checks.
            throw new RuntimeException(e);
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Dynamically look up and invoke a class constructor for the target
     * class, with appropriate hints if any failures happen along the way.
     * @param returnType The type of object to create.
     * @param params The parameters to pass to the constructor
     * @param <T> The generic parameter T is deduced from the returnType
     * @return The newly created object
     */
    public static <T> T create(
        Class<T> returnType,
        Object ... params)
    {
        Object result = null;
        ParameterSignature paramProfile = new ParameterSignature(params);
        Constructor<?> c = getMatchingConstructor(returnType, paramProfile);

        result = create(c, params);

        if (result != null)
        {
            assertTrue("constructor "
                + simpleMethodName(simpleClassName(returnType), paramProfile)
                + " did not produce result of type "
                + simpleClassName(returnType),
                returnType.isAssignableFrom(result.getClass()));
        }
        // The cast below is technically unsafe, according to the compiler,
        // but will never be violated, due to the assertion above.
        @SuppressWarnings("unchecked")
        T answer = (T)result;
        return answer;
    }


    // ----------------------------------------------------------
    /**
     * Dynamically look up and invoke a class constructor for the target
     * class, with appropriate hints if any failures happen along the way.
     * @param className The type of object to create
     * @param params The parameters to pass to the constructor
     * @return The newly created object
     */
    public static Object create(String className, Object ... params)
    {
        return create(getClassForName(className), params);
    }


    // ----------------------------------------------------------
    /**
     * Just like {@link #create(Constructor, Object...)}, but unwraps
     * any InvocationTargetExceptions and throws the true cause.  This
     * version is provided when you want to write test cases where you
     * are intending to check for Exceptions as expected results.
     * @param constructor The constructor to invoke
     * @param params The parameters to pass to the constructor
     * @return The newly created object
     * @throws Exception if the underlying method throws one
     */
    public static Object createEx(Constructor<?> constructor, Object ... params)
        throws Exception
    {
        Object result = null;
        try
        {
            result = constructor.newInstance(params);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e;
            Exception ex = null;
            if (cause instanceof Exception)
            {
                ex = (Exception)cause;
            }
            while (cause.getCause() != null)
            {
                cause = cause.getCause();
                if (cause instanceof Exception)
                {
                    ex = (Exception)cause;
                }
            }
            if (ex != null)
            {
                throw ex;
            }
            else
            {
                // the cause is a raw Throwable of some kind, rather than
                // an Exception, so it needs to be wrapped anyway
                throw new RuntimeException(cause);
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Just like {@link #create(Class, Object...)}, but unwraps
     * any InvocationTargetExceptions and throws the true cause.  This
     * version is provided when you want to write test cases where you
     * are intending to check for Exceptions as expected results.
     * @param returnType The type of object to create.
     * @param params The parameters to pass to the constructor
     * @param <T> The generic parameter T is deduced from the returnType
     * @return The newly created object
     * @throws Exception if the underlying method throws one
     */
    public static <T> T createEx(
        Class<T> returnType,
        Object ... params)
        throws Exception
    {
        Object result = null;
        ParameterSignature paramProfile = new ParameterSignature(params);
        Constructor<?> c = getMatchingConstructor(returnType, paramProfile);

        result = createEx(c, params);

        if (result != null)
        {
            assertTrue("constructor "
                + simpleMethodName(simpleClassName(returnType), paramProfile)
                + " did not produce result of type "
                + simpleClassName(returnType),
                returnType.isAssignableFrom(result.getClass()));
        }
        // The cast below is technically unsafe, according to the compiler,
        // but will never be violated, due to the assertion above.
        @SuppressWarnings("unchecked")
        T answer = (T)result;
        return answer;
    }


    // ----------------------------------------------------------
    /**
     * Just like {@link #create(String, Object...)}, but unwraps
     * any InvocationTargetExceptions and throws the true cause.  This
     * version is provided when you want to write test cases where you
     * are intending to check for Exceptions as expected results.
     * @param className The type of object to create
     * @param params The parameters to pass to the constructor
     * @return The newly created object
     * @throws Exception if the underlying method throws one
     */
    public static Object createEx(String className, Object ... params)
        throws Exception
    {
        return createEx(getClassForName(className), params);
    }


    //~ Method Invocation Methods .............................................

    // ----------------------------------------------------------
    /**
     * Look up a method by name and parameter profile, turning any
     * errors into test case failures with appropriate hint messages.
     * Only looks up methods that are declared in the specified class,
     * not inherited methods.  Assumes the intended method should be
     * public, and fails with an appropriate hint if it is not.
     * @param c The type of the receiver
     * @param name The method name
     * @param params The method's parameter profile
     * @return The corresponding Method object
     */
    public static Method getMethod(
        Class<?> c, String name, Class<?> ... params)
    {
        return getMethod(
            c, VisibilityConstraint.DECLARED_PUBLIC, name, params);
    }


    // ----------------------------------------------------------
    /**
     * Look up a method by name and parameter profile, turning any
     * errors into test case failures with appropriate hint messages.
     * Only looks up methods that are declared in the specified class,
     * not inherited methods.  Assumes the intended method should be
     * public, and fails with an appropriate hint if it is not.
     * @param c The type of the receiver
     * @param visibility The required visibility of the method
     * @param name The method name
     * @param params The method's parameter profile
     * @return The corresponding Method object
     */
    public static Method getMethod(
        Class<?> c,
        VisibilityConstraint visibility,
        String name,
        Class<?> ... params)
    {
        Method m = null;
        try
        {
            m = c.getDeclaredMethod(name, params);
        }
        catch (NoSuchMethodException e)
        {
            String message = c + " is missing method "
                + simpleMethodName(name, params);
            fail(message);
        }
        catch (SecurityException e)
        {
            String message = "method " + simpleMethodName(name, params)
                + " in " + c + " cannot be accessed (should be public)";
            fail(message);
        }
        if (m != null && !visibility.accepts(m.getModifiers()))
        {
            fail(simpleMethodName(m) + " should be " + visibility);
        }
        if (!m.isAccessible())
        {
            final Method method = m;
            AccessController.doPrivileged( new PrivilegedAction<Void>()
                {
                    public Void run()
                    {
                        method.setAccessible(true);
                        return null;
                    }
                });
        }
        return m;
    }


    // ----------------------------------------------------------
    /**
     * Look up a method by name and parameter profile, finding the
     * method that will accept the given list of parameters (not requiring
     * an exact match on parameter types).  It turns any
     * errors into test case failures with appropriate hint messages.
     * Only looks up methods that are declared in the specified class,
     * not inherited methods.  Assumes the intended method should be
     * public, and fails with an appropriate hint if it is not.
     * Note that this method <b>does not handle variable argument lists</b>
     * in the target method for which it is searching.
     * @param c The type of the receiver
     * @param name The method name
     * @param params The method's parameter profile
     * @return The corresponding Method object
     */
    public static Method getMatchingMethod(
        Class<?> c, String name, Class<?> ... params)
    {
        return getMatchingMethod(c, name, new ParameterSignature(params));
    }


    // ----------------------------------------------------------
    /**
     * Look up a method by name and parameter profile, finding the
     * method that will accept the given list of parameters (not requiring
     * an exact match on parameter types).  It turns any
     * errors into test case failures with appropriate hint messages.
     * Only looks up methods that are declared in the specified class,
     * not inherited methods.  Assumes the intended method should be
     * public, and fails with an appropriate hint if it is not.
     * Note that this method <b>does not handle variable argument lists</b>
     * in the target method for which it is searching.
     * @param c The type of the receiver
     * @param name The method name
     * @param params The method's parameter profile
     * @return The corresponding Method object
     */
    public static Method getMatchingMethod(
        Class<?> c, String name, ParameterSignature params)
    {
        return getMatchingMethod(
            c, VisibilityConstraint.DECLARED_PUBLIC, name, params);
    }


    // ----------------------------------------------------------
    /**
     * Look up a method by name and parameter profile, finding the
     * method that will accept the given list of parameters (not requiring
     * an exact match on parameter types).  It turns any
     * errors into test case failures with appropriate hint messages.
     * Only looks up methods that are declared in the specified class,
     * not inherited methods.  Assumes the intended method should be
     * public, and fails with an appropriate hint if it is not.
     * Note that this method <b>does not handle variable argument lists</b>
     * in the target method for which it is searching.
     * @param c The type of the receiver
     * @param visibility The required visibility of the method
     * @param name The method name
     * @param params The method's parameter profile
     * @return The corresponding Method object
     */
    public static Method getMatchingMethod(
        Class<?> c,
        VisibilityConstraint visibility,
        String name,
        Class<?> ... params)
    {
        return getMatchingMethod(
            c, visibility, name, new ParameterSignature(params));
    }


    // ----------------------------------------------------------
    /**
     * Look up a method by name and parameter profile, finding the
     * method that will accept the given list of parameters (not requiring
     * an exact match on parameter types).  It turns any
     * errors into test case failures with appropriate hint messages.
     * Only looks up methods that are declared in the specified class,
     * not inherited methods.  Assumes the intended method should be
     * public, and fails with an appropriate hint if it is not.
     * Note that this method <b>does not handle variable argument lists</b>
     * in the target method for which it is searching.
     * @param c The type of the receiver
     * @param visibility The required visibility of the method
     * @param name The method name
     * @param params The method's parameter profile
     * @return The corresponding Method object
     */
    public static Method getMatchingMethod(
        Class<?> c,
        VisibilityConstraint visibility,
        String name,
        ParameterSignature params)
    {
        // Non-boxing (preferred) answer
        Method result = null;
        ParameterSignature resultSig = null;

        // Auto-boxing/unboxing answer
        Method withBoxing = null;
        ParameterSignature withBoxingSig = null;

        // Wrong visibility, no boxing, nearest match?
        Method notVisible = null;
        ParameterSignature notVisibleSig = null;

        // Wrong visibility, with auto-boxing/unboxing, nearest match?
        Method notVisibleWithBoxing = null;
        ParameterSignature notVisibleWithBoxingSig = null;

        // Same parameter count, also no match
        Method methodWithSameParamCount = null;
        // Same name, also no match
        Method methodWithSameName = null;

        Class<?> currentClass = c;
        while (currentClass != null)
        {
            for (Method m : currentClass.getDeclaredMethods())
            {
                if (m.getName().equals(name))
                {
                    if (methodWithSameName == null)
                    {
                        methodWithSameName = m;
                    }
                    ParameterSignature sig = new ParameterSignature(m);
                    if (params.size() == sig.size()
                        && methodWithSameParamCount == null)
                    {
                        methodWithSameParamCount = m;
                    }

                    ParameterAcceptanceCategory category =
                        sig.acceptanceCategory(params);
                    switch (category)
                    {
                        case ACCEPTS_WITHOUT_BOXING:
                            if (visibility.accepts(m.getModifiers()))
                            {
                                if (result == null
                                    || sig.isMoreSpecificThan(resultSig))
                                {
                                    result = m;
                                    resultSig = sig;
                                }
                            }
                            else
                            {
                                if (notVisible == null
                                    || sig.isMoreSpecificThan(notVisibleSig))
                                {
                                    notVisible = m;
                                    notVisibleSig = sig;
                                }
                            }
                            break;
                        case ACCEPTS_WITH_BOXING:
                            if (visibility.accepts(m.getModifiers()))
                            {
                                if (withBoxing == null
                                    || sig.isMoreSpecificThan(withBoxingSig))
                                {
                                    withBoxing = m;
                                    withBoxingSig = sig;
                                }
                            }
                            else
                            {
                                if (notVisibleWithBoxing == null
                                    || sig.isMoreSpecificThan(
                                        notVisibleWithBoxingSig))
                                {
                                    notVisibleWithBoxing = m;
                                    notVisibleWithBoxingSig = sig;
                                }
                            }
                            break;
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        if (result == null)
        {
            result = withBoxing;
            resultSig = withBoxingSig;
        }
        if (result == null)
        {
            String message = null;
            if (notVisible != null)
            {
                message = simpleMethodName(notVisible)
                    + " cannot be called because it is not " + visibility;
            }
            else if (notVisibleWithBoxing != null)
            {
                message = simpleMethodName(notVisibleWithBoxing)
                    + " cannot be called because it is not " + visibility;
            }
            else if (methodWithSameParamCount != null)
            {
                message = simpleMethodName(methodWithSameParamCount)
                    + " cannot be called with argument"
                    + ((params.size() == 1) ? "" : "s")
                    + " of type "
                    + params
                    + ": incorrect parameter type(s)";
            }
            else if (methodWithSameName != null)
            {
                message = simpleMethodName(methodWithSameName);
                if (params.size() == 0)
                {
                    message += " cannot be called with no arguments";
                }
                else
                {
                    message += " cannot be called with argument"
                        + ((params.size() == 1) ? "" : "s")
                        + " of type "
                        + params;
                }
                message += ": incorrect number of parameters";
            }
            else
            {
                message = "class " + simpleClassName(c)
                    + " is missing method "
                    + simpleMethodName(name, params)
                    + " that is " + visibility;
            }
            fail(message);
        }
        if (!result.isAccessible())
        {
            final Method method = result;
            AccessController.doPrivileged( new PrivilegedAction<Void>()
                {
                    public Void run()
                    {
                        method.setAccessible(true);
                        return null;
                    }
                });
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Dynamically look up and invoke a method on a target object, with
     * appropriate hints if any failures happen along the way.
     * @param receiver The object to invoke the method on
     * @param returnType The expected type of the method's return value.
     *     Use null (or <code>void.class</code>) if the method that is
     *     looked up is a void method.
     * @param methodName The name of the method to invoke
     * @param params The parameters to pass to the method
     * @param <T> The generic parameter T is deduced from the returnType
     * @return The results from invoking the given method
     */
    public static <T> T invoke(
        Object receiver,
        Class<T> returnType,
        String methodName,
        Object ... params)
    {
        return invoke(receiver, VisibilityConstraint.DECLARED_PUBLIC,
            returnType, methodName, params);
    }


    // ----------------------------------------------------------
    /**
     * Dynamically look up and invoke a method on a target object, with
     * appropriate hints if any failures happen along the way.
     * @param receiver The object to invoke the method on
     * @param visibility The required visibility of the method
     * @param returnType The expected type of the method's return value.
     *     Use null (or <code>void.class</code>) if the method that is
     *     looked up is a void method.
     * @param methodName The name of the method to invoke
     * @param params The parameters to pass to the method
     * @param <T> The generic parameter T is deduced from the returnType
     * @return The results from invoking the given method
     */
    public static <T> T invoke(
        Object receiver,
        VisibilityConstraint visibility,
        Class<T> returnType,
        String methodName,
        Object ... params)
    {
        Object result = null;
        Class<?> targetClass = receiver.getClass();
        ParameterSignature paramProfile = new ParameterSignature(params);
        Method m = getMatchingMethod(
            targetClass, visibility, methodName, paramProfile);

        if (returnType == null || returnType == void.class)
        {
            Class<?> declaredReturnType = m.getReturnType();
            assertTrue("method " + simpleMethodName(m)
                + " should be a void method",
                declaredReturnType == void.class ||
                declaredReturnType == null);
        }
        else
        {
            Class<?> declaredReturnType = m.getReturnType();
            assertTrue("method " + simpleMethodName(m)
                + " should be declared with a return type of "
                + simpleClassNameUsingPrimitives(returnType),
                declaredReturnType != void.class &&
                declaredReturnType != null &&
                actualMatchesFormal(declaredReturnType, returnType));
        }

        result = invoke(receiver, m, params);

        if (result != null)
        {
            if (returnType != null)
            {
                assertTrue("method " + simpleMethodName(m)
                    + " should be a void method",
                    returnType != void.class);
                assertTrue("method " + simpleMethodName(m)
                    + " did not produce result of type "
                    + simpleClassName(returnType),
                    actualMatchesFormal(result.getClass(), returnType));
            }
            else
            {
                fail("method " + simpleMethodName(m)
                    + " should be a void method");
            }
        }
        // The cast below is technically unsafe, according to the compiler,
        // but will never be violated, due to the assertion above.
        @SuppressWarnings("unchecked")
        T answer = (T)result;
        return answer;
    }


    // ----------------------------------------------------------
    /**
     * Dynamically look up and invoke a method on a target object, with
     * appropriate hints if any failures happen along the way.  This
     * version is intended for calling "void" methods that have no
     * return value.
     * @param receiver The object to invoke the method on
     * @param methodName The name of the method to invoke
     * @param params The parameters to pass to the method
     */
    public static void invoke(
        Object receiver,
        String methodName,
        Object ... params)
    {
        invoke(receiver, VisibilityConstraint.DECLARED_PUBLIC, methodName,
            params);
    }


    // ----------------------------------------------------------
    /**
     * Dynamically look up and invoke a method on a target object, with
     * appropriate hints if any failures happen along the way.  This
     * version is intended for calling "void" methods that have no
     * return value.
     * @param receiver The object to invoke the method on
     * @param visibility The required visibility of the method
     * @param methodName The name of the method to invoke
     * @param params The parameters to pass to the method
     */
    public static void invoke(
        Object receiver,
        VisibilityConstraint visibility,
        String methodName,
        Object ... params)
    {
        invoke(receiver, visibility, void.class, methodName, params);
    }


    // ----------------------------------------------------------
    /**
     * Just like {@link Method#invoke(Object, Object...)}, but converts
     * any thrown exceptions into RuntimeExceptions.
     * @param receiver The object to invoke the method on
     * @param method The method to invoke
     * @param params The parameters to pass to the method
     * @return The result from the method
     */
    public static Object invoke(
        Object receiver, Method method, Object ... params)
    {
        Object result = null;
        try
        {
            result = method.invoke(receiver, params);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e;
            while (cause.getCause() != null)
            {
                cause = cause.getCause();
            }

            if (cause instanceof Error)
            {
                throw (Error)cause;
            }
            else if (cause instanceof RuntimeException)
            {
                throw (RuntimeException)cause;
            }
            else
            {
                throw new RuntimeException(cause);
            }
        }
        catch (IllegalAccessException e)
        {
            // This should never happen, since getMethod() has already
            // done the appropriate checks.
            throw new RuntimeException(e);
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Just like {@link #invoke(Object, Class, String, Object...)}, but unwraps
     * any InvocationTargetExceptions and throws the true cause.  This
     * version is provided when you want to write test cases where you
     * are intending to check for Exceptions as expected results.
     * @param receiver The object to invoke the method on
     * @param returnType The expected type of the method's return value.
     *     Use null (or <code>void.class</code>) if the method that is
     *     looked up is a void method.
     * @param methodName The name of the method to invoke
     * @param params The parameters to pass to the method
     * @param <T> The generic parameter T is deduced from the returnType
     * @return The results from invoking the given method
     * @throws Exception if the underlying method throws one
     */
    public static <T> T invokeEx(
        Object receiver,
        Class<T> returnType,
        String methodName,
        Object ... params)
        throws Exception
    {
        return invokeEx(receiver, VisibilityConstraint.DECLARED_PUBLIC,
            returnType, methodName, params);
    }


    // ----------------------------------------------------------
    /**
     * Just like {@link #invoke(Object, Class, String, Object...)}, but unwraps
     * any InvocationTargetExceptions and throws the true cause.  This
     * version is provided when you want to write test cases where you
     * are intending to check for Exceptions as expected results.
     * @param receiver The object to invoke the method on
     * @param visibility The required visibility of the method
     * @param returnType The expected type of the method's return value.
     *     Use null (or <code>void.class</code>) if the method that is
     *     looked up is a void method.
     * @param methodName The name of the method to invoke
     * @param params The parameters to pass to the method
     * @param <T> The generic parameter T is deduced from the returnType
     * @return The results from invoking the given method
     * @throws Exception if the underlying method throws one
     */
    public static <T> T invokeEx(
        Object receiver,
        VisibilityConstraint visibility,
        Class<T> returnType,
        String methodName,
        Object ... params)
        throws Exception
    {
        Object result = null;
        Class<?> targetClass = receiver.getClass();
        ParameterSignature paramProfile = new ParameterSignature(params);
        Method m = getMatchingMethod(targetClass, methodName, paramProfile);

        if (returnType == null || returnType == void.class)
        {
            Class<?> declaredReturnType = m.getReturnType();
            assertTrue("method " + simpleMethodName(m)
                + " should be a void method",
                declaredReturnType == void.class ||
                declaredReturnType == null);
        }
        else
        {
            Class<?> declaredReturnType = m.getReturnType();
            assertTrue("method " + simpleMethodName(m)
                + " should be declared with a return type of "
                + simpleClassNameUsingPrimitives(returnType),
                declaredReturnType != void.class &&
                declaredReturnType != null &&
                actualMatchesFormal(declaredReturnType, returnType));
        }

        result = invokeEx(receiver, m, params);

        if (result != null)
        {
            if (returnType != null)
            {
                assertTrue("method " + simpleMethodName(m)
                    + " should be a void method",
                    returnType != void.class);
                assertTrue("method " + simpleMethodName(m)
                    + " did not produce result of type "
                    + simpleClassName(returnType),
                    returnType.isAssignableFrom(result.getClass()));
            }
            else
            {
                fail("method " + simpleMethodName(m)
                    + " should be a void method");
            }
        }
        // The cast below is technically unsafe, according to the compiler,
        // but will never be violated, due to the assertion above.
        @SuppressWarnings("unchecked")
        T answer = (T)result;
        return answer;
    }


    // ----------------------------------------------------------
    /**
     * Just like {@link #invoke(Object, String, Object...)}, but unwraps
     * any InvocationTargetExceptions and throws the true cause.  This
     * version is provided when you want to write test cases where you
     * are intending to check for Exceptions as expected results.
     * @param receiver The object to invoke the method on
     * @param methodName The name of the method to invoke
     * @param params The parameters to pass to the method
     * @throws Exception if the underlying method throws one
     */
    public static void invokeEx(
        Object receiver,
        String methodName,
        Object ... params)
        throws Exception
    {
        invokeEx(receiver, VisibilityConstraint.DECLARED_PUBLIC, methodName,
            params);
    }


    // ----------------------------------------------------------
    /**
     * Just like {@link #invoke(Object, String, Object...)}, but unwraps
     * any InvocationTargetExceptions and throws the true cause.  This
     * version is provided when you want to write test cases where you
     * are intending to check for Exceptions as expected results.
     * @param receiver The object to invoke the method on
     * @param visibility The required visibility of the method
     * @param methodName The name of the method to invoke
     * @param params The parameters to pass to the method
     * @throws Exception if the underlying method throws one
     */
    public static void invokeEx(
        Object receiver,
        VisibilityConstraint visibility,
        String methodName,
        Object ... params)
        throws Exception
    {
        invokeEx(receiver, visibility, void.class, methodName, params);
    }


    // ----------------------------------------------------------
    /**
     * Just like {@link Method#invoke(Object, Object...)}, but unwraps
     * any InvocationTargetExceptions and throws the true cause.  This
     * version is provided when you want to write test cases where you
     * are intending to check for Exceptions as expected results.
     * @param receiver The object to invoke the method on
     * @param method The method to invoke
     * @param params The parameters to pass to the method
     * @return The result from the method
     * @throws Exception if the underlying method throws one
     */
    public static Object invokeEx(
        Object receiver, Method method, Object ... params)
        throws Exception
    {
        Object result = null;
        try
        {
            result = method.invoke(receiver, params);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e;
            Exception ex = null;
            Error     error = null;
            if (cause instanceof Exception)
            {
                ex = (Exception)cause;
            }
            else if (cause instanceof Error)
            {
                error = (Error)cause;
            }
            while (cause.getCause() != null)
            {
                cause = cause.getCause();
                if (cause instanceof Exception)
                {
                    ex = (Exception)cause;
                }
                else if (cause instanceof Error)
                {
                    error = (Error)cause;
                }
            }
            if (error != null)
            {
                throw error;
            }
            else if (ex != null)
            {
                throw ex;
            }
            else
            {
                // the cause is a raw Throwable of some kind, rather than
                // an Exception, so it needs to be wrapped anyway
                throw new RuntimeException(cause);
            }
        }
        return result;
    }


    //~ Field Manipulation Methods ............................................

    // ----------------------------------------------------------
    /**
     * Look up a field by name, receiver class, and type, finding the
     * field that will accept the given type (not requiring
     * an exact match on type).  Any errors are thrown as instances of
     * {@link ReflectionError}.
     * It looks up fields that are declared in the specified class,
     * as well as inherited classes.  It sets up accessibility if the field
     * is not public.
     * @param receiverClass The class of the receiver
     * @param type The type of this field
     * @param fieldName The name of the field
     * @return The corresponding Field
     */
    public static Field getField(
        Class<?> receiverClass, Class<?> type, String fieldName)
    {
        Field field = null;
        Class<?> declaringClass = receiverClass;
        // TODO: This approach will not find public fields declared in
        //       implemented interfaces.
        while (field == null && declaringClass != null)
        {
            try
            {
                field = declaringClass.getDeclaredField(fieldName);
            }
            catch (NoSuchFieldException e)
            {
               // check parent class
               declaringClass = declaringClass.getSuperclass();
            }
        }

        if (field == null)
        {
            fail("Cannot find field " + fieldName
                + " in " + simpleClassName(receiverClass));
        }
        if (!actualMatchesFormal(field.getType(), type)
            && !actualMatchesFormal(type, field.getType()))
        {
            String msg = "Field " + fieldName + " in class ";
            if (declaringClass == receiverClass)
            {
                msg += simpleClassName(declaringClass);
            }
            else
            {
                msg += simpleClassName(receiverClass) + " (inherited from "
                    + simpleClassName(declaringClass) + ")";
            }
            fail(msg + " is declared of type "
                + simpleClassName(field.getType())
                + ", not "
                + simpleClassName(type)
                + ".");
        }

        //check and set access permission
        if (!field.isAccessible())
        {
            final Field theField = field;
            AccessController.doPrivileged( new PrivilegedAction<Void>()
                {
                    public Void run()
                    {
                        theField.setAccessible(true);
                        return null;
                    }
                });
        }
        return field;
    }


    //------------------------------------------------------------------
    /**
     * Get the value of a field. The field is looked up a field by name,
     * receiver object and type, finding the field that will accept the
     * given type (not requiring an exact match on type).  It turns any
     * errors into ReflectionErrors with appropriate hint messages.
     * @param receiver The object containing the field
     * @param type The type of this field
     * @param fieldName The name of the field
     * @param <T> The generic parameter T is deduced from the returnType
     * @return The value corresponding Field
     */
    public static <T> T get(Object receiver, Class<T> type, String fieldName)
    {
        assert fieldName != null    : "fieldName cannot be null";
        assert !fieldName.isEmpty() : "fieldName cannot be empty";
        assert receiver != null     : "Receiver object cannot be empty";

        Field field = getField(receiver.getClass(), type, fieldName);
        Object fieldValue = null;
        if (field != null)
        {
            try
            {
                fieldValue = field.get(receiver);
            }
            catch (IllegalArgumentException e)
            {
                String msg = e.getMessage();
                if (msg == null)
                {
                    msg = e.getClass().getSimpleName();
                }
                fail(field.getName() + " in "
                    + simpleClassName(receiver.getClass())
                    + " cannot be retrieved: " + msg);
            }
            catch (IllegalAccessException e)
            {
                // Shouldn't happen, since setAccessible() was called,
                // but the compiler requires a handler.
                fail(e.getMessage());
            }

        }
        if (fieldValue != null && !type.isInstance(fieldValue))
        {
            throw new ReflectionError(
                "Field " + field.getName()
                + " of type " + field.getType().getSimpleName()
                + " in an object of class "
                + receiver.getClass().getSimpleName()
                + " contains a(n) " + fieldValue.getClass().getSimpleName()
                + " value, but a(n) " + type.getSimpleName()
                + " was expected.");
        }
        @SuppressWarnings("unchecked")
        T value = (T)fieldValue;
        return value;
    }


    //------------------------------------------------------------------
    /**
     * Get the value of a field. The field is looked up a field by name,
     * receiver object and type, finding the field that will accept the
     * given type (not requiring an exact match on type).  It turns any
     * errors into ReflectionErrors with appropriate hint messages.
     * Note that the field must be a static field.
     * @param receiverClass The class of the receiver
     * @param type The type of this field
     * @param fieldName The name of the field
     * @param <T> The generic parameter T is deduced from the returnType
     * @return The value corresponding Field
     */
    public static <T> T get(
        Class<?> receiverClass, Class<T> type, String fieldName)
    {
        assert fieldName != null     : "fieldName cannot be null";
        assert !fieldName.isEmpty()  : "fieldName cannot be empty";
        assert receiverClass != null : "Receiver object cannot be empty";

        Field field = getField(receiverClass, type, fieldName);
        Object fieldValue = null;
        if (field != null)
        {
            try
            {
                fieldValue = field.get(null);
            }
            catch (IllegalArgumentException e)
            {
                String msg = e.getMessage();
                if (msg == null)
                {
                    msg = e.getClass().getSimpleName();
                }
                fail(field.getName() + " in "
                    + simpleClassName(receiverClass)
                    + " cannot be retrieved: " + msg);
            }
            catch (IllegalAccessException e)
            {
                // Shouldn't happen, since setAccessible() was called,
                // but the compiler requires a handler.
                fail(e.getMessage());
            }
        }
        if (fieldValue != null && !type.isInstance(fieldValue))
        {
            throw new ReflectionError(
                "Static field " + field.getName()
                + " of type " + field.getType().getSimpleName()
                + " in class "
                + receiverClass.getSimpleName()
                + " contains a(n) " + fieldValue.getClass().getSimpleName()
                + " value, but a(n) " + type.getSimpleName()
                + " was expected.");
        }
        @SuppressWarnings("unchecked")
        T value = (T)fieldValue;
        return value;
    }


    //------------------------------------------------------------------
    /**
     * Sets value of a field.
     * @param receiver The object of the receiver
     * @param fieldName The name of the field
     * @param value The value to set in the field
     */
    public static void set(Object receiver, String fieldName, Object value)
    {
        assert fieldName != null    : "fieldName cannot be null";
        assert !fieldName.isEmpty() : "fieldName cannot be empty";
        assert receiver != null     : "Receiver object cannot be empty";

        Field field =
            getField(receiver.getClass(), value.getClass(), fieldName);
        if (field != null)
        {
            try
            {
                field.set(receiver, value);
            }
            catch (IllegalArgumentException e)
            {
                fail(field.getName() +" of type "
                    + simpleClassName(field.getType())
                    +" cannot be assigned a value of "
                    + value
                    + ((value == null)
                        ? ""
                        : "(of type "
                            + simpleClassName(value.getClass())
                            + ")")
                    + ".");
            }
            catch (IllegalAccessException e)
            {
                // Shouldn't happen, since setAccessible() was called,
                // but the compiler requires a handler.
                fail(e.getMessage());
            }
        }
    }


    //------------------------------------------------------------------
    /**
     * Sets value of a field.
     * @param receiverClass The class of the receiver
     * @param fieldName The name of the field
     * @param value The value will be set in the field
     */
    public static void set(
        Class<?> receiverClass, String fieldName,  Object value)
    {
        assert fieldName != null     : "fieldName cannot be null";
        assert !fieldName.isEmpty()  : "fieldName cannot be empty";
        assert receiverClass != null : "Receiver object cannot be empty";

        Field field = getField(receiverClass, value.getClass(), fieldName);
        if (field != null)
        {
            try
            {
                field.set(null, value);
            }
            catch (IllegalArgumentException e)
            {
                fail(field.getName() +" of type "
                    + simpleClassName(field.getType())
                    +" cannot be assigned a value of "
                    + value
                    + ((value == null)
                        ? ""
                        : "(of type "
                            + simpleClassName(value.getClass())
                            + ")")
                    + ".");
            }
            catch (IllegalAccessException e)
            {
                // Shouldn't happen, since setAccessible() was called,
                // but the compiler requires a handler.
                fail(e.getMessage());
            }
        }
    }


    //~ Public Utility Methods ................................................

    //  simple printing methods ---------------------------------

    // ----------------------------------------------------------
    /**
     * Returns the name of the given class without any package prefix.
     * If the argument is an array type, square brackets are added to
     * the name as appropriate.  This method isuseful in generating
     * diagnostic messages or feedback.
     * @param aClass The class to generate a name for
     * @return The class' name, without the package part, e.g., "String"
     *     instead of "java.lang.String"
     */
    public static String simpleClassName(Class<?> aClass)
    {
        return (aClass == null) ? "null" : aClass.getSimpleName();
    }


    // ----------------------------------------------------------
    /**
     * Returns the name of the given class without any package prefix.
     * If the argument is an array type, square brackets are added to
     * the name as appropriate.  This method is useful in generating
     * diagnostic messages or feedback.
     * @param aClass The class to generate a name for
     * @return The class' name, without the package part, e.g., "String"
     *     instead of "java.lang.String"
     */
    public static String simpleClassNameUsingPrimitives(Class<?> aClass)
    {
        Class<?> result = WRAPPER_TO_PRIMITIVE.get(aClass);
        if (result == null)
        {
            result = aClass;
        }
        return simpleClassName(result);
    }


    // ----------------------------------------------------------
    /**
     * Constructs a printable version of a method's name, given
     * the method name and its parameter type(s), if any.
     * Useful in generating diagnostic messages or feedback.
     * @param name   The method name
     * @param params The method's parameter type(s), in order
     * @return A printable version of the method name, like
     *     "myMethod()" or "yourMethod(String, int)"
     */
    public static String simpleMethodName(String name, Class<?> ... params)
    {
        return name + simpleArgumentList(params);
    }


    // ----------------------------------------------------------
    /**
     * Constructs a printable version of a method's name, given
     * the method name and its parameter type(s), if any.
     * Useful in generating diagnostic messages or feedback.
     * @param name   The method name
     * @param params The method's parameter type(s), in order
     * @return A printable version of the method name, like
     *     "myMethod()" or "yourMethod(String, int)"
     */
    public static String simpleMethodName(
        String name, ParameterSignature params)
    {
        return name + params;
    }


    // ----------------------------------------------------------
    /**
     * Constructs a printable version of a method's argument list, including
     * the parentheses, given the method's parameter type(s), if any.
     * @param params The method's parameter type(s), in order
     * @return A printable version of the argument list built using
     *     {@link #simpleClassName(Class)}, like "(String, int)"
     */
    public static String simpleArgumentList(Class<?> ... params)
    {
        String result = "(";
        boolean needsComma = false;
        for (Class<?> c : params)
        {
            if (needsComma)
            {
                result += ", ";
            }
            if (c == null)
            {
                result += "null";
            }
            else
            {
                result += simpleClassName(c);
            }
            needsComma = true;
        }
        result += ")";
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Constructs a printable version of a method's name.  Unlike
     * {@link Method#toString()}, this one uses {@link #simpleClassName(Class)}
     * so package info is eliminated from the types in the resulting
     * string.  It also omits exception information, unlike Method.toString().
     * @param method The method to print
     * @return A printable version of the method name, like
     *     "public void MyClass.myMethod()" or
     *     "public String YourClass.yourMethod(String, int)"
     */
    public static String simpleMethodName(Method method)
    {
        StringBuffer sb = new StringBuffer();
        int mod = method.getModifiers();
        if (mod != 0)
        {
            sb.append(Modifier.toString(mod) + " ");
        }
        sb.append(simpleClassName(method.getReturnType()) + " ");
        sb.append(method.getName());
        sb.append(simpleArgumentList(method.getParameterTypes()));
        sb.append(" in class ");
        sb.append(simpleClassName(method.getDeclaringClass()));
        return sb.toString();
    }


    // argument matching methods --------------------------------

    // ----------------------------------------------------------
    /**
     * Determine whether an actual argument type matches a formal argument
     * type, based on the rules of JLS Section 5.3.
     * @param actual The type of the actual parameter
     * @param formal The type of the formal parameter
     * @return True if the actual value can be passed into a parameter
     *    declared using the formal type.
     */
    public static boolean actualMatchesFormal(Class<?> actual, Class<?> formal)
    {
        return canCoerceFromActualToFormal(actual, formal)
            != ParameterConversionCategory.CANNOT_CONVERT;
    }


    // ----------------------------------------------------------
    /**
     * Determine the type of method invocation conversion, if any, Java would
     * apply to convert from an actual argument of type <code>actual</code>
     * into a formal parameter type <code>formal</code>, possibly including
     * "auto-boxing" or "auto-unboxing", widening primitive conversions, or
     * widening reference conversions, according to the rules of JLS Section
     * 5.3.
     * @param actual The type of the actual value.
     * @param formal The type of the formal parameter.
     * @return The type of conversion that would be performed, or
     * CANNOT_CONVERT if the actual is not compatible with the formal.
     */
    public static ParameterConversionCategory canCoerceFromActualToFormal(
        Class<?> actual, Class<?> formal)
    {
        if (actual == null)
        {
            // Represents a null value, which is compatible with any
            // reference type
            return formal.isPrimitive()
                ? ParameterConversionCategory.CANNOT_CONVERT
                : ParameterConversionCategory.IDENTITY_CONVERSION;
        }
        if (formal.equals(actual))
        {
            return ParameterConversionCategory.IDENTITY_CONVERSION;
        }
        if (!actual.isPrimitive() && formal.isAssignableFrom(actual))
        {
            return ParameterConversionCategory.REFERENCE_WIDENING;
        }

        // Check for boxing
        if (actual.isPrimitive())
        {
            if (formal.isPrimitive())
            {
                Integer actualRank = WIDENING_RANK.get(actual);
                Integer formalRank = WIDENING_RANK.get(formal);
                if (actual.equals(char.class))
                {
                    actualRank = 1;
                }
                if (actualRank != null
                    && formalRank != null
                    // Uses <, since the only possibility where they would be
                    // equal is char -> short, which isn't allowable
                    && actualRank < formalRank)
                {
                    return ParameterConversionCategory.PRIMITIVE_WIDENING;
                }

                // otherwise, both are primitive and differ, but can't widen
            }
            else
            {
                // Otherwise, actual is primitive but formal is not
                Class<?> wrapper = PRIMITIVE_TO_WRAPPER.get(actual);
                if (formal.equals(wrapper))
                {
                    return ParameterConversionCategory.BOXING;
                }
                if (formal.isAssignableFrom(wrapper))
                {
                    return ParameterConversionCategory
                        .BOXING_WITH_REFERENCE_WIDENING;
                }
            }
            return ParameterConversionCategory.CANNOT_CONVERT;
        }

        // Check for unboxing
        Class<?> primitive = WRAPPER_TO_PRIMITIVE.get(actual);
        if (primitive != null && formal.isPrimitive())
        {
            if (formal.equals(primitive))
            {
                return ParameterConversionCategory.UNBOXING;
            }
            Integer actualRank = WIDENING_RANK.get(primitive);
            Integer formalRank = WIDENING_RANK.get(formal);
            if (primitive.equals(char.class))
            {
                actualRank = 1;
            }
            if (actualRank != null
                && formalRank != null
                // Uses <, since the only possibility where they would be
                // equal is char -> short, which isn't allowable
                && actualRank < formalRank)
            {
                return ParameterConversionCategory
                    .UNBOXING_WITH_PRIMITIVE_WIDENING;
            }
        }

        return ParameterConversionCategory.CANNOT_CONVERT;
    }


    //~ Private Utility Methods ...............................................

    //-----------------------------------------------------------------
    /**
     * Throws a ReflectionError with the given message if the given
     * condition is not true.
     * @param message The message will be used to create ReflectionError
     * @param condition The condition to check
     */
    private static void assertTrue(String message, boolean condition)
    {
        if (!condition)
        {
            fail(message, 2);
        }
    }


    //-----------------------------------------------------------------
    /**
     * Throws a ReflectionError with the given message.
     * @param message The message will be used to create ReflectionError
     */
    private static void fail(String message)
    {
        fail(message, 2);
    }


    //-----------------------------------------------------------------
    /**
     * Throws a ReflectionError with the given message.
     * @param message The message will be used to create ReflectionError
     * @param stackFramesToStrip The number of levels to strip from the
     * top of the stack frame (e.g., 1 will strip the call to fail() itself).
     */
    private static void fail(String message, int stackFramesToStrip)
    {
        ReflectionError error = new ReflectionError(message);
        StackTraceElement[] trace = error.getStackTrace();
        // remove the call to fail() from the stack trace
        error.setStackTrace(java.util.Arrays.copyOfRange(
            trace, stackFramesToStrip, trace.length));
        throw error;
    }


    //-----------------------------------------------------------------
    /**
     * Return an array of potential classloaders to use to look up classes.
     * This array contains three loaders, including the loader
     * used to load ReflectionSupport itself, the current thread's
     * context classloader, and the classloader used to load the nearest
     * non-library class in the method calling sequence.  These loaders
     * are arranged in order from most-specific to least-specific,
     * in terms of delegation (i.e., if any ancestor/descendant delegation
     * relationships exist among the loaders, the descendant appears before
     * the ancestor in the returned array).
     */
    private static ClassLoader[] getCandidateLoaders()
    {
        ClassLoader[] result = new ClassLoader[] {
            RESOLVER.getNonlibraryCallerClass().getClassLoader(),
            Thread.currentThread().getContextClassLoader(),
            ReflectionSupport.class.getClassLoader()
        };

        // Sort them in most-specific-to-least-specific order
        // using a simple bubble sort (augh!!)
        boolean moved = false;
        if (hasAsChild(result[1], result[2]))
        {
            ClassLoader temp = result[2];
            result[2] = result[1];
            result[1] = temp;
            moved = true;
        }
        if (hasAsChild(result[0], result[1]))
        {
            ClassLoader temp = result[1];
            result[1] = result[0];
            result[0] = temp;
            moved = true;
        }
        if (moved)
        {
            if (hasAsChild(result[1], result[2]))
            {
                ClassLoader temp = result[2];
                result[2] = result[1];
                result[1] = temp;
            }
        }
        else
        {
            if (hasAsChild(result[0], result[2]))
            {
                ClassLoader temp = result[2];
                result[2] = result[0];
                result[0] = temp;
            }
        }
        return result;
    }


    //-----------------------------------------------------------------
    /**
     * Returns true if 'loader2' is a delegation child of 'loader1' or if
     * 'loader1' == 'loader2'. Of course, this works only for classloaders that
     * set their parent pointers correctly. 'null' is interpreted as the
     * primordial loader (i.e., everybody's parent).
     */
    private static boolean hasAsChild(ClassLoader loader1, ClassLoader loader2)
    {
        if (loader1 == loader2)
        {
            return true;
        }
        if (loader2 == null)
        {
            return false;
        }
        if (loader1 == null)
        {
            return true;
        }

        for ( ; loader2 != null; loader2 = loader2.getParent())
        {
            if (loader2 == loader1)
            {
                return true;
            }
        }

        return false;
    }


    //-----------------------------------------------------------------
    /**
     * This interface represents a strategy for finding the nearest
     * non-library class in the calling sequence.
     */
    private static interface CallerResolver
    {
        Class<?> getNonlibraryCallerClass();
    }


    //-----------------------------------------------------------------
    /**
     * A stock implementation of CallerResolver that uses features from
     * the SecurityManager class to get access to the declaring classes of
     * methods on the call stack.  Note that this class need NOT be installed
     * as the current security manager at all (and shouldn't be).  It
     * simply uses inherited features to implement the CallerResolver
     * interface.
     */
    private static class SecurityManagerCallerResolver
        extends SecurityManager
        implements CallerResolver
    {
        public Class<?> getNonlibraryCallerClass()
        {
            Class<?>[] stack = getClassContext();
            for (Class<?> c : getClassContext())
            {
                boolean isClientClass = true;
                String name = c.getName();
                for (String prefix : STACK_FILTERS)
                {
                    if (name.startsWith(prefix))
                    {
                        isClientClass = false;
                    }
                    break;
                }
                if (isClientClass
                    && c.getClassLoader()
                        != ReflectionSupport.class.getClassLoader()
                    && c.getClassLoader()
                        != Thread.currentThread().getContextClassLoader())
                {
                    return c;
                }
            }
            if (stack.length > 0)
            {
                // If no non-library classes were found, return the bottom
                // of stack
                return stack[stack.length - 1];
            }
            else
            {
                // If there's no stack (!), just return this class
                return ReflectionSupport.class;
            }
        }
    }


    //~ Private Fields ........................................................

    private static /*final*/ CallerResolver RESOLVER;

    static
    {
        try
        {
            // this can fail if the current SecurityManager does not allow
            // RuntimePermission ("createSecurityManager"):

            AccessController.doPrivileged( new PrivilegedAction<Void>()
                {
                    public Void run()
                    {
                        RESOLVER = new SecurityManagerCallerResolver();
                        return null;
                    }
                });
        }
        catch (SecurityException e)
        {
            System.out.println("Warning: " + ReflectionSupport.class
                + " could not create CallerResolver:");
            e.printStackTrace();
            RESOLVER = new CallerResolver()
            {
                public java.lang.Class<?> getNonlibraryCallerClass()
                {
                    return ReflectionSupport.class;
                };
            };
        }
    }


    private static final String[] STACK_FILTERS = {
        "student.",
        "cs1705.",
        // JUnit 4 support:
        "org.junit.",
        // JUnit 3 support:
        "junit.",
        "java.",
        "sun.",
        "org.apache.tools.ant.",
        // Web-CAT infrastructure
        "net.sf.webcat."
    };

    private static final Map<Class<?>, Integer> WIDENING_RANK =
        new HashMap<Class<?>, Integer>();
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER =
        new HashMap<Class<?>, Class<?>>();
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE =
        new HashMap<Class<?>, Class<?>>();
    static {
        // See Section 5.1.2 of Java language specification
        WIDENING_RANK.put(byte.class, 0);
        WIDENING_RANK.put(short.class, 1);
        WIDENING_RANK.put(int.class, 2);
        WIDENING_RANK.put(long.class, 3);
        WIDENING_RANK.put(float.class, 4);
        WIDENING_RANK.put(double.class, 5);

        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);

        WRAPPER_TO_PRIMITIVE.put(Byte.class, byte.class);
        WRAPPER_TO_PRIMITIVE.put(Short.class, short.class);
        WRAPPER_TO_PRIMITIVE.put(Integer.class, int.class);
        WRAPPER_TO_PRIMITIVE.put(Long.class, long.class);
        WRAPPER_TO_PRIMITIVE.put(Float.class, float.class);
        WRAPPER_TO_PRIMITIVE.put(Double.class, double.class);
        WRAPPER_TO_PRIMITIVE.put(Character.class, char.class);
        WRAPPER_TO_PRIMITIVE.put(Boolean.class, boolean.class);
    }
}
