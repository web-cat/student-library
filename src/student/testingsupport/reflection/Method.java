package student.testingsupport.reflection;

public class Method<ReturnType>
    extends NameFilter<Method<ReturnType>, java.lang.reflect.Method>
{

    protected Method(Filter<?, ?> previous, String descriptionOfConstraint)
    {
        super(previous, descriptionOfConstraint);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected String nameOf(java.lang.reflect.Method object)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Method<ReturnType> createFreshFilter(
        Filter<?, ?> previous,
        String descriptionOfThisStage)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected int modifiersFor(java.lang.reflect.Method object)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected String filteredObjectDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Method<ReturnType> createFreshFilter(
        java.lang.reflect.Method object)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
