package mx.kenzie.argo;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class JsonObjectTest {

    private static final String SIMPLE_MAP = """
        {
            "hello": "there"
        }
        """;

    static boolean check(Object value, Object test) {
        assert Objects.equals(value, test) : value;
        return Objects.equals(test, value);
    }

    @Test
    public void readSimple() {
        final InputStream stream = new ByteArrayInputStream(SIMPLE_MAP.getBytes(StandardCharsets.UTF_8));
        try (Json json = new Json(stream); JsonObject object = new JsonObject(json)) {
            assert check(object.readKey(), "hello");
            assert check(object.readValue(), "there");
        }
    }

    @Test
    public void writeSimple() {
        final Map<String, Object> map = Map.of("hello", "there");
        final StringWriter writer = new StringWriter();
        try (Json json = new Json(writer); JsonObject object = new JsonObject(json)) {
            object.write(map);
        }
        assert check(writer.toString(), "{\"hello\": \"there\"}");
    }

    @Test
    public void both() {
        final StringWriter writer = new StringWriter();
        try (Json json = new Json(writer); JsonObject object = new JsonObject(json)) {
            object.write(Map.of("hello", "there"));
            object.writeKey("general");
            object.writeValue("kenobi");
            object.write("test", 10);
        }
        assert check(writer.toString(), "{\"hello\": \"there\", \"general\": \"kenobi\", \"test\": 10}");
        try (Json json = Json.of(writer.toString()); JsonObject object = new JsonObject(json)) {
            assert check(object.readKey(), "hello");
            assert check(object.readValue(), "there");
            assert check(object.readKey(), "general");
            assert check(object.readValue(), "kenobi");
            assert check(object.readKey(), "test");
            assert check(object.readValue(), 10);
        }
    }

}
