package mx.kenzie.argo;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("FieldMayBeFinal")
public class ObjectAccessTest {

    @Test
    public void simple() {
        final String string = """
                { "hello": "there" }
                """;
        final Simple result = Json.fromString(string, Simple.class);
        assert result != null;
        assert result.hello.equals("there");
    }

    @Test
    @SuppressWarnings("FieldMayBeFinal")
    public void inner() {
        class Result {

            String hello = null;

        }
        final String string = """
                { "hello": "there" }
                """;
        final Result result = Json.fromString(string, Result.class);
        assert result != null;
        assert result.hello.equals("there");
    }

    @Test
    public void primitive() {
        class Result {

            int a, b;

        }
        final String string = """
                { "a": 1, "b": 6 }
                """;
        final Result result = Json.fromString(string, Result.class);
        assert result != null;
        assert result.a == 1;
        assert result.b == 6;
    }

    @Test
    public void mixed() {
        class Result {

            int a, b;
            String hello;

        }
        final String string = """
                { "a": 1, "b": 6, "hello": "there" }
                """;
        final Result result = Json.fromString(string, Result.class);
        assert result != null;
        assert result.a == 1 : result.a;
        assert result.b == 6 : result.b;
        assert result.hello.equals("there") : result.hello;
    }

    @Test
    public void reverse() {
        class Result {

            int a = 3, b;
            String hello;

        }
        final Result result = new Result();
        result.a--;
        result.hello = "there";
        final String string = Json.toString(result);
        assert string != null;
        assert string.startsWith("{") && string.endsWith("}");
        assert string.contains("\"hello\": \"there\"") : string;
        assert string.contains("\"b\": 0") : string;
        assert string.contains("\"a\": 2") : string;
    }

    @Test
    public void children() {
        class Child {

            int bean;

        }
        class Result {

            String hello;
            Child child;

        }
        final String string = """
                { "hello": "there", "child": { "bean": 5 } }
                """;
        final Result result = Json.fromString(string, Result.class);
        assert result != null;
        assert result.hello.equals("there") : result.hello;
        assert result.child != null;
        assert result.child.bean == 5 : result.child.bean;
    }

    @Test
    @SuppressWarnings("FieldMayBeFinal")
    public void reverseChildren() {
        class Child {

            int bean = 3;

        }
        class Result {

            String hello = "there";
            Child child = new Child();

        }
        final Result result = new Result();
        final String string = Json.toString(result);
        assert string != null;
        assert string.startsWith("{") && string.endsWith("}");
        assert string.contains("\"hello\": \"there\"") : string;
        assert string.contains("\"child\": {\"bean\": 3}") : string;
    }

    @Test
    @SuppressWarnings("FieldMayBeFinal")
    public void simpleArrayTest() {
        class Result {

            String hello = "there";
            int[] numbers = {5, 6, 7};

        }
        final Result result = new Result();
        final String string = Json.toString(result);
        assertNotNull(string);
        assert string.equals("{\"hello\": \"there\", \"numbers\": [5, 6, 7]}") : string;
        final Result test = Json.fromString(string, Result.class);
        assert test != null;
        assert test.hello.equals(result.hello) : test.hello;
        assert Arrays.equals(test.numbers, result.numbers) : test.numbers;
    }

    @Test
    @SuppressWarnings("FieldMayBeFinal")
    public void complexArrayTest() {
        class Child {

            int a = 1;

        }
        class Result {

            Child[] children = {new Child(), new Child()};

        }
        final Result result = new Result();
        result.children[0].a = 2;
        final String string = Json.toString(result);
        assert string != null;
        assert string.equals("{\"children\": [{\"a\": 2}, {\"a\": 1}]}") : string;
        final Result test = Json.fromString(string, Result.class);
        assert test != null;
        assert test.children != null;
        assert test.children.length == 2;
        assert test.children[0].a == 2;
        assert test.children[1].a == 1;
    }

    @Test
    @SuppressWarnings("FieldMayBeFinal")
    public void arrayFromData() {
        class Child {

            int a;

        }
        class Result {

            Child[] children;

        }
        final String string = """
                { "children": [ { "a": 5 }, { "a": 3 } ] }
                """;
        final Result result = Json.fromString(string, Result.class);
        assert result != null;
        assert result.children != null;
        assert result.children.length == 2;
        assert result.children[0].a == 5;
        assert result.children[1].a == 3;
    }

    @Test
    @SuppressWarnings("FieldMayBeFinal")
    public void directArray() {
        final String string = """
                [ 1, 3, 5 ]
                """;
        final int[] result = Json.fromString(string, int[].class);
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1, result[0]);
        assertEquals(3, result[1]);
        assertEquals(5, result[2]);
    }

    @Test
    public void readAnySubType() {
        class Bean {
        }
        class Child extends Bean {

            int number = 5;

        }
        class Result {

            Bean child = new Child();

        }
        final String string = Json.toString(new Result());
        assertNotNull(string);
        assert string.equals("{\"child\": {\"number\": 5}}") : string;
    }

    @Test
    public void writeAnySubType() {
        class Bean {
        }
        class Child extends Bean {

            int number = 5;

        }
        @SuppressWarnings("FieldMayBeFinal")
        class Result {

            Bean child = new Child();

        }
        final String string = Json.toString(new Result());
        assertNotNull(string);
        assert string.equals("{\"child\": {\"number\": 5}}") : string;
    }

    @Test
    public void existingMesh() {
        class Child {

            final int bar = 6;
            int foo;

        }
        class Result {

            Child child = new Child();

        }
        final String string = """
                { "hello": "there", "child": { "foo": 1 } }
                """;
        final Result result = Json.fromString(string, Result.class);
        assert result != null;
        assert result.child.foo == 1 : result.child.foo;
        assert result.child.bar == 6 : result.child.bar;
    }

    @Test
    public void record() {
        record Person(String name, int age) {
        }
        final String string = """
                { "name": "Jeremy", "age": 66 }""";
        final Person result = Json.fromString(string, Person.class);
        assert result != null;
        assert result.name.equals("Jeremy");
        assert result.age == 66;
        final Person person = new Person("Bearimy", 61);
        final String json = Json.toString(person);
        assert json.startsWith("{") && json.endsWith("}");
        assert json.contains("\"age\": 61") : json;
        assert json.contains("\"name\": \"Bearimy\"") : json;
    }

    public static final class Simple {

        public String hello = null;

    }

}
