package mx.kenzie.argo;

import org.junit.Test;

public class DataSetTest {

    @Test
    public void test() {
        class Child {

            String name;
            int bean;

        }
        final String string = """
                { "hello": "there", "bean": 5, "name": "test" }
                """;
        final Child result = Json.fromString(string, Child.class);
        assert result != null;
        assert result.name.equals("test") : result.name;
        assert result.bean == 5 : result.bean;
        final String json = Json.toString(result);
        assert json != null;
        assert json.startsWith("{") && json.endsWith("}");
        assert json.contains("\"name\": \"test\"") : json;
        assert json.contains("\"bean\": 5") : json;
    }

}
