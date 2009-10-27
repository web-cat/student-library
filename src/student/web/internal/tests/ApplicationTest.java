package student.web.internal.tests;

import java.util.Map;
import student.web.Application;
import student.web.internal.ObjectFieldExtractor;
import student.web.internal.PersistentStorageManager;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.JVM;

public class ApplicationTest
{
    public static class MyApp
        extends Application
    {
        public MyApp()
        {
            super("MyApp");
        }

        public boolean login(String userName, String password)
        {
            return false;
        }
    }


    public static void main(String[] args)
    {
//        Person p = new Person(null, null);
//        p.setFirstName("Barny");
//        p.setLastName("Rubble");

        Application app = new MyApp();
//        app.setSharedObject("p1", p);
//        app.setApplicationObject("p2", p);

        Person q = app.getSharedObject("p1", Person.class);
        System.out.println("shared p1 = " + q);

        q.setFirstName("Fred");
        app.setSharedObject("p1", q);

//        q = app.getSharedObject("p2", Person.class);
//        System.out.println("shared p2 = " + q);
//
//        q = app.getApplicationObject("p1", Person.class);
//        System.out.println("app p1 = " + q);
//
//        q = app.getApplicationObject("p2", Person.class);
//        System.out.println("app p2 = " + q);

//        XStream xstream = new XStream();
//        System.out.println(xstream.toXML(p));
//
//        Person q = (Person)xstream.fromXML(
//            "<student.web.internal.tests.Person>"
//            + "<firstName>Barny</firstName>"
//            + "<lastName>Rubble</lastName>"
//            + "</student.web.internal.tests.Person>");
//
//        System.out.println("result = " + q);
//
//        ObjectFieldExtractor extractor = new ObjectFieldExtractor();
//
//        Map<String, Object> fields = extractor.objectToFieldMap(p);
//
//        System.out.println("map = " + fields);
//        System.out.println(xstream.toXML(fields));
//
//        fields.put("unused", "bogus");
//        System.out.println("map = " + fields);
//        Person p2 = extractor.fieldMapToObject(Person.class, fields);
//        System.out.println("p2 = " + p2);
//
//        PersistentStorageManager.getInstance().storeChangedFields("p2", fields);
//        fields.remove("unused");
//        PersistentStorageManager.getInstance().storeChangedFields("p", fields);
//
//        System.out.println("Reconstituting p = "
//            + extractor.fieldMapToObject(
//                Person.class,
//                PersistentStorageManager.getInstance().getFieldSet("p").value)
//            );
//        System.out.println("Reconstituting p2 = "
//            + extractor.fieldMapToObject(
//                Person.class,
//                PersistentStorageManager.getInstance().getFieldSet("p2").value)
//            );
//        System.out.println("Reconstituting p2(raw) = "
//            + PersistentStorageManager.getInstance().getFieldSet("p2").value
//            );
    }
}
