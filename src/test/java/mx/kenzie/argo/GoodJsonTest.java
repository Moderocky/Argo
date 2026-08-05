package mx.kenzie.argo;

import org.junit.Test;

public class GoodJsonTest {

    private static final String[] inputs = {
            "{\"A\": \"B\"}",
            "{\"A\": 1}",
            "\"A\"",
            "1",
            "true",
            "false",
            "-4",
            "3.22"
    };

    @Test
    public void testAllOkay() {
        for (String input : inputs) {
            final Object value = Json.fromString(input);
            assert value != null;
        }
        assert Json.fromString("null") == Json.NULL;
    }

    @Test
    public void booleans() {
        assert Json.fromString(inputs[4]) instanceof Boolean boo && boo;
        assert Json.fromString(inputs[5]) instanceof Boolean boo && !boo;
    }

    @Test
    public void strings() {
        assert Json.fromString(inputs[2]) instanceof String string && string.equals("A");
    }

    @Test
    public void numbers() {
        assert Json.fromString(inputs[3]) instanceof Number number && number.equals(1);
        assert Json.fromString(inputs[3]) instanceof Integer number && number.equals(1);
        assert Json.fromString(inputs[6]) instanceof Integer number && number.equals(-4);
        assert Json.fromString(inputs[7]) instanceof Double number && number.equals(3.22);
    }


}
