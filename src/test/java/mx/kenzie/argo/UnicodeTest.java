package mx.kenzie.argo;

import mx.kenzie.grammar.Container;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;


@SuppressWarnings("FieldMayBeFinal")
public class UnicodeTest {

    @Test
    public void test() {
        class Blob {
            String value = "Hello \uD83C\uDF08 There";
        }
        final String string = Json.toString(new Blob());
        assert string.equals("{\"value\": \"Hello \\uD83C\\uDF08 There\"}") : string;
        final Blob blob = Json.fromString(string, Blob.class);
        assert Objects.equals(blob.value, new Blob().value);
    }

    @Test
    public void testFakeEscape() {
        String value = "Hello \\uD83C\\uDF08 There";
        final String json = Json.toString(value);
        assertEquals("\"Hello \\\\uD83C\\\\uDF08 There\"", json);
        String result = Json.fromString(json);
        assertEquals(value, result);
    }

    @Test
    public void second() {
        final Container map = Container.empty();
        map.put("hello", "Test \u001b Test");
        map.put("there", "Test \\u001b Test");
        final String json = Json.toString(map);
        assert json.equals("{\"hello\": \"Test \u001B Test\", \"there\": \"Test \\\\u001b Test\"}") : json;
        final Container result = Json.fromString(json);
        assertEquals(map.get("hello"), result.get("hello"));
        assertEquals(map.get("there"), result.get("there"));
        assertEquals(map, result);
    }

}
