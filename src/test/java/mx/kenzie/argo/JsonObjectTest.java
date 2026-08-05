package mx.kenzie.argo;

import mx.kenzie.grammar.Container;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.constant.Constable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("FieldMayBeFinal")
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

        Container container = Json.simple().reader(stream).readContainer();
        assertEquals(1, container.size());
        assertEquals("there", container.get("hello"));
    }

    @Test
    public void writeSimple() {
        final Map<String, Constable> map = Container.empty();
        final StringWriter writer = new StringWriter();
        final var write = Json.simple().writer(writer);
        write.writeObject(map);
        assert check(writer.toString(), "{}");
    }

    @Test
    public void both() {
        final StringWriter writer = new StringWriter();
        final var write = Json.simple().writer(writer);
        write.getWriter().writeContainerOpen(write.getHook());
        write.getWriter().writeContainerPair(write.getHook(), "hello", "there");
        write.getWriter().writeContainerAnd(write.getHook(), true);
        write.getWriter().writeContainerKey(write.getHook(), "general");
        write.getWriter().writeContainerSeparator(write.getHook());
        write.getWriter().writeContainerValue(write.getHook(), "kenobi");
        write.getWriter().writeContainerAnd(write.getHook(), true);
        write.getWriter().writeContainerPair(write.getHook(), "test", 10);
        write.getWriter().writeContainerAnd(write.getHook(), false);
        write.getWriter().writeContainerClose(write.getHook());
        assert check(writer.toString(), "{\"hello\": \"there\", \"general\": \"kenobi\", \"test\": 10}");
    }

    @Test
    public void writeObject() {
        final Object first = new Object() {
            String hello = "there";
        };
        final Object second = new Object() {
            String general = "kenobi";
        };
        final StringWriter writer = new StringWriter();
        try (var object = Json.allTypes().writer(writer).object()) {
            object.writeObject(first);
            object.writeObject(second);
        }
        assert check(writer.toString(), "{\"hello\": \"there\", \"general\": \"kenobi\"}");
    }

}
