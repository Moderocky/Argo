package mx.kenzie.argo;

import mx.kenzie.grammar.Container;
import mx.kenzie.grammar.Series;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("FieldMayBeFinal")
public class BasicWriteTest {

    @Test
    public void simple() {
        final Container start = Container.of();
        final String string = Json.toString(start);
        assert string.equals("{}") : string;
        Container end = Json.fromString(string);
        assertEquals(start, end);
    }

    @Test
    public void keyValue() {
        final Container start = Container.empty();
        start.put("hello", "there");
        final String string = Json.toString(start);
        assert string.equals("{\"hello\": \"there\"}") : string;
        final Container end = Json.fromString(string);
        assert start.equals(end);
    }

    @Test
    public void multiKey() {
        final Container start = Container.empty();
        start.put("hello", "there");
        start.put("general", "kenobi");
        final String string = Json.toString(start);
        assert string.equals("{\"hello\": \"there\", \"general\": \"kenobi\"}") : string;
        final Container end = Json.fromString(string);
        assert start.equals(end);
    }

    @Test
    public void types() {
        final Container start = Container.empty();
        start.put("hello", "there");
        start.put("a", 1);
        start.put("b", -12.5);
        start.put("c", null);
        start.put("d", true);
        final String string = Json.toString(start);
        assert string.equals("{\"hello\": \"there\", \"a\": 1, \"b\": -12.5, \"c\": null, \"d\": true}") : string;
        final Container end = Json.fromString(string);
        assertEquals(start, end);
    }

    @Test
    public void complex() {
        final Container start = Container.empty();
        final Container child = Container.empty();
        final Series list = Series.empty();
        child.put("hello", "there");
        start.put("hello", "there");
        start.put("child", child);
        list.add(12);
        list.add("bean");
        start.put("list", list);
        final String string = Json.toString(start);
        assert string.equals("{\"hello\": \"there\", \"child\": {\"hello\": \"there\"}, \"list\": [12, \"bean\"]}") :
                string;
        final Container end = Json.fromString(string);
        assert start.equals(end);
    }

    @Test
    public void simpleList() {
        final Series start = Series.empty();
        final String string = Json.toString(start);
        assert string.equals("[]") : string;
        final Series end = Json.fromString(string);
        assert start.equals(end);
    }

    @Test
    public void complexList() {
        final Series start = Series.empty();
        start.add("beans");
        start.add(null);
        start.add(23);
        final String string = Json.toString(start);
        assert string.equals("[\"beans\", null, 23]") : string;
        final Series end = Json.fromString(string);
        assert start.equals(end);
    }

    @Test
    public void pretty() {
        final Container start = Container.empty();
        final Container child = Container.empty();
        final Series list = Series.empty();
        child.put("hello", "there");
        start.put("hello", "there");
        start.put("child", child);
        list.add(12);
        list.add("bean");
        start.put("list", list);
        final String string = Json.toString(start, true);
        assert string.equals("""
                {
                \t"hello": "there",
                \t"child": {
                \t\t"hello": "there"
                \t},
                \t"list": [
                \t\t12,
                \t\t"bean"
                \t]
                }""") : string;
    }

    @Test
    public void escapes() {
        class Result {

            final String hello = "there\ngeneral\tkenobi";

        }
        final String string = Json.toString(new Result());
        assert string.equals("{\"hello\": \"there\\ngeneral\\tkenobi\"}") : string;
    }

    @Test
    public void array() {
        class Thing {

            String hello = "there";

        }
        final String string = Json.toString(new Thing(), new Thing());
        assert string.equals("[{\"hello\": \"there\"}, {\"hello\": \"there\"}]") : string;
    }

}
