package mx.kenzie.argo.test;

import mx.kenzie.argo.Json;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UnicodeTest {

    @Test
    public void test() {
        class Blob {
            final String value = "Hello \uD83C\uDF08 There";
        }
        final String string = Json.toJson(new Blob());
        assert string.equals("{\"value\": \"Hello \\uD83C\\uDF08 There\"}") : string;
        final Blob blob = Json.fromJson(string, new Blob());
        assert Objects.equals(blob.value, new Blob().value);
    }

    @Test
    public void second() {
        final Map<String, Object> map = new HashMap<>();
        map.put("hello", "Test \u001b Test");
        map.put("there", "Test \\u001b Test");
        final String json = Json.toJson(map);
        assert json.equals("{\"there\": \"Test \\u001b Test\", \"hello\": \"Test \u001B Test\"}") : json;
        final Map<String, Object> result = Json.fromJson(json);
        assert result.get("hello").equals(map.get("hello"));
    }

}
