package mx.kenzie.argo;

import mx.kenzie.grammar.Container;
import mx.kenzie.grammar.GrammarException;
import mx.kenzie.grammar.Null;
import mx.kenzie.grammar.Series;
import mx.kenzie.grammar.unwrap.Unwrap;
import org.junit.Test;
import org.valross.constantine.Array;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;


@SuppressWarnings("FieldMayBeFinal")
public class InterfaceTest {

    @Test
    public void list() {
        class MyClass {
            List<String> list = List.of("hello", "there");
        }

        StringBuilder builder = new StringBuilder();

        MyClass object = new MyClass();
        Json.allTypes().writer(builder).writeObject(object);

        String value = builder.toString();

        assertEquals("{\"list\": [\"hello\", \"there\"]}", value);

        MyClass myClass = Json.allTypes().reader(value).readObject(MyClass.class);
        assertEquals(myClass.list, List.of("hello", "there"));
    }

    @Test(expected = GrammarException.class)
    public void uncheckedCollectionNoHint() {
        record Foo(String name) {

        }
        class MyClass {

            Collection<Foo> list = List.of(new Foo("hello"), new Foo("there"));
        }
        MyClass _ = Json.allTypes().reader("{\"list\": [{\"name\": \"hello\"}, {\"name\": \"there\"}]}").readObject(MyClass.class);
    }

    @Test
    public void uncheckedCollection() {
        record Foo(String name) {

        }
        class MyClass {

            @Unwrap(marshalAs = ArrayList.class, componentType = Foo.class)
            Collection<Foo> list = List.of(new Foo("hello"), new Foo("there"));
        }

        StringBuilder builder = new StringBuilder();

        MyClass object = new MyClass();
        Json.allTypes().writer(builder).writeObject(object);

        String value = builder.toString();

        assertEquals("{\"list\": [{\"name\": \"hello\"}, {\"name\": \"there\"}]}", value);

        MyClass myClass = Json.allTypes().reader(value).readObject(MyClass.class);
        assertEquals(List.of(new Foo("hello"), new Foo("there")), myClass.list);
    }

}
