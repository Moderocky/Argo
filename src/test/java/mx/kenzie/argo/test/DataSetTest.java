package mx.kenzie.argo.test;

import mx.kenzie.argo.Json;
import org.junit.Test;

import java.util.Map;

public class DataSetTest {
    
    @Test
    public void test() {
        class Child {
            String name;
            int bean;
            transient Map<String, Object> __data;
        }
        final String string = """
            { "hello": "there", "bean": 5, "name": "test" }
            """;
        final Child result = Json.fromJson(string, new Child());
        assert result != null;
        assert result.name.equals("test") : result.name;
        assert result.bean == 5 : result.bean;
        assert result.__data != null;
        assert result.__data.get("name").equals("test") : result.__data.get("name");
        assert result.__data.get("bean").equals(5) : result.__data.get("bean");
        assert result.__data.get("hello").equals("there") : result.__data.get("hello");
        assert Json.toJson(result).equals("{\"name\": \"test\",\"bean\": 5}") : Json.toJson(result);
    }
    
}
