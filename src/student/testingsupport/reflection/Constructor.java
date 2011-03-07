package student.testingsupport.reflection;

public class Constructor<ClassType>
    extends NameFilter<Method<ClassType>,
        java.lang.reflect.Constructor<ClassType>>
{

    protected Constructor(Filter<?, ?> previous, String descriptionOfConstraint)
    {
        super(previous, descriptionOfConstraint);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected String nameOf(java.lang.reflect.Constructor<ClassType> object)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Method<ClassType> createFreshFilter(
        Filter<?, ?> previous,
        String descriptionOfThisStage)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected int modifiersFor(java.lang.reflect.Constructor<ClassType> object)
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
    protected Method<ClassType> createFreshFilter(
        java.lang.reflect.Constructor<ClassType> object)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
