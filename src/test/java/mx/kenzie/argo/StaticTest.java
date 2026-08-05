package mx.kenzie.argo;

import mx.kenzie.grammar.Container;
import mx.kenzie.grammar.Null;
import mx.kenzie.grammar.Series;
import org.junit.Test;
import org.valross.constantine.Array;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class StaticTest {

    @Test
    public void simple() {
        assertEquals("\"hello\"", Json.toString("hello"));
        assertEquals("55", Json.toString(55));
        assertEquals("false", Json.toString(false));
        assertEquals("null", Json.toString(Null.INSTANCE));
        assertEquals("null", Json.toString((Void) null));
    }

    @Test
    public void container() {
        assertEquals("{\"hello\": \"there\"}", Json.toString(Container.of("hello", "there")));
        assertEquals("{\"number\": 55}", Json.toString(Container.of("number", 55)));
        assertEquals("{\"number\": 55}", Json.toString(Map.of("number", 55)));
    }

    @Test
    public void array() {
        assertEquals("[\"hello\", \"there\"]", Json.toString(Series.of("hello", "there")));
        assertEquals("[\"hello\", \"there\", 55]", Json.toString(Series.of("hello", "there", 55)));
        assertEquals("[\"hello\", \"there\"]", Json.toString(new Array("hello", "there")));
        assertEquals("[]", Json.toString(Series.empty()));
        assertEquals("[\"hello\", -3, {}]", Json.toString(new Array("hello", -3, Container.empty())));
    }

}
