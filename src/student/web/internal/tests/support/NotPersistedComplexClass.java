package student.web.internal.tests.support;

public class NotPersistedComplexClass  extends PlainClass implements Comparable{
    public String complexStuff = "!!!!";

    public int compareTo(Object o) {
        return 0;
    }
}
