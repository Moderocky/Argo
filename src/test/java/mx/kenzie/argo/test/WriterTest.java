package mx.kenzie.argo.test;

import mx.kenzie.argo.Json;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

public class WriterTest {

    @Test
    public void basic() {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(stream);
        final Json json = new Json(writer);
        json.write(Map.of("hello", "there"));
        assert stream.toString().equals("{\"hello\": \"there\"}") : stream;
    }

    @Test
    public void nest() {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(stream);
        final Json json = new Json(writer);
        json.write(Map.of("hello", Map.of("hello", "there")));
        assert stream.toString().equals("{\"hello\": {\"hello\": \"there\"}}") : stream;
    }

    @Test
    public void array() {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(stream);
        final Json json = new Json(writer);
        json.write(List.of(
            Map.of("hello", "there"),
            Map.of("hello", "there")
        ));
        assert stream.toString().equals("[{\"hello\": \"there\"},{\"hello\": \"there\"}]") : stream;
    }

    @Test
    public void arrayInside() {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(stream);
        final Json json = new Json(writer);
        json.write(Map.of(
            "hello", List.of(
                Map.of("hello", "there"),
                Map.of("hello", "there")
            ),
            "there", List.of(
                Map.of("hello", "there"),
                Map.of("hello", "there")
            )
        ));
        assert stream.toString().length() > 0;
    }

    @Test
    public void objects() {
        class Thing {
            public final Inside[] insides = {new Inside()};

            class Inside {
                public final String hello = "there";
            }
        }
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(stream);
        final Json json = new Json(writer);
        json.write(new Thing());
        assert stream.toString().equals("{\"insides\": [{\"hello\": \"there\"}]}") : stream;
    }

}
