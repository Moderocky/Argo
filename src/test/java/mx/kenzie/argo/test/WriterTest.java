package mx.kenzie.argo.test;

import mx.kenzie.argo.Json;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
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

}
