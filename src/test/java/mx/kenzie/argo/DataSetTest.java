package mx.kenzie.argo;

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
        final String json = Json.toJson(result);
        assert json != null;
        assert json.startsWith("{") && json.endsWith("}");
        assert json.contains("\"name\": \"test\"") : json;
        assert json.contains("\"bean\": 5") : json;
    }

}
