package mx.kenzie.argo;

import mx.kenzie.grammar.Container;
import mx.kenzie.grammar.Series;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class WriterTest {

    @Test
    public void basic() throws IOException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(stream);
        var json = Json.simple().writer(writer);
        json.writeObject(Container.of("hello", "there"));
        assert stream.toString().equals("{\"hello\": \"there\"}") : stream;
    }

    @Test
    public void nest() {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(stream);
        var json = Json.simple().writer(writer);
        json.writeObject(Container.of("hello", Container.of("hello", "there")));
        assert stream.toString().equals("{\"hello\": {\"hello\": \"there\"}}") : stream;
    }

    @Test
    public void array() {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(stream);
        var json = Json.simple().writer(writer);
        json.writeArray(Series.of(
                Container.of("hello", "there"),
                Container.of("hello", "there")
        ));
        assert stream.toString().equals("[{\"hello\": \"there\"}, {\"hello\": \"there\"}]") : stream;
    }

    @Test
    public void arrayInside() {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(stream);
        var json = Json.simple().writer(writer);
        json.writeObject(Container.of(
                "hello", Series.of(
                        Container.of("hello", "there"),
                        Container.of("hello", "there")
                ),
                "there", Series.of(
                        Container.of("hello", "there"),
                        Container.of("hello", "there")
                )
        ));
        assert stream.toString().length() > 0;
        assert stream.toString().equals("{\"hello\": [{\"hello\": \"there\"}, {\"hello\": \"there\"}], \"there\": [{\"hello\": \"there\"}, {\"hello\": \"there\"}]}") : stream;
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
        var json = Json.allTypes().writer(writer);
        json.writeObject(new Thing());
        assert stream.toString().equals("{\"insides\": [{\"hello\": \"there\"}]}") : stream;
    }

}
