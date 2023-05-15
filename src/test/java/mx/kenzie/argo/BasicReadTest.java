package mx.kenzie.argo;

import org.junit.Test;

import java.util.List;
import java.util.Map;

public class BasicReadTest {

    @Test
    public void simple() {
        try (final Json json = new Json("{}")) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 0;
        }
    }

    @Test
    public void spaced() {
        try (final Json json = new Json("""
            {
            }
            """)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 0;
        }
    }

    @Test
    public void keyValue() {
        final String string = """
            { "hello": "there" }
            """;
        try (final Json json = new Json(string)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 1;
            assert map.get("hello").equals("there");
        }
    }

    @Test
    public void multiKey() {
        final String string = """
            { "hello": "there", "general": "kenobi" }
            """;
        try (final Json json = new Json(string)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 2;
            assert map.get("hello").equals("there");
            assert map.get("general").equals("kenobi");
        }
    }

    @Test
    public void replaceKey() {
        final String string = """
            { "hello": "world", "hello": "there" }
            """;
        try (final Json json = new Json(string)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 1;
            assert !map.get("hello").equals("world");
            assert map.get("hello").equals("there");
        }
    }

    @Test
    public void booleans() {
        final String string = """
            { "a": true, "b": false }
            """;
        try (final Json json = new Json(string)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 2;
            assert map.get("a").equals(true);
            assert map.get("b").equals(false);
        }
    }

    @Test
    public void numbers() {
        final String string = """
            { "a": 1, "b": 6, "c": 23, "d": -4.5 }
            """;
        try (final Json json = new Json(string)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 4;
            assert map.get("a").equals(1) : map;
            assert map.get("b").equals(6) : map;
            assert map.get("c").equals(23) : map;
            assert map.get("d").equals(-4.5) : map;
        }
    }

    @Test
    public void noValue() {
        final String string = """
            { "hello": true, "there": null }
            """;
        try (final Json json = new Json(string)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 2;
            assert map.get("hello").equals(true) : map;
            assert map.get("there") == null : map;
        }
    }

    @Test
    public void mixed() {
        final String string = """
            { "a": "yes", "b": true, "c": 1, "d": null }
            """;
        try (final Json json = new Json(string)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 4;
            assert map.get("a").equals("yes") : map;
            assert map.get("b").equals(true) : map;
            assert map.get("c").equals(1) : map;
            assert map.get("d") == null : map;
        }
    }

    @Test
    public void compound() {
        final String string = """
            { "hello": { "hello": "there" } }
            """;
        try (final Json json = new Json(string)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 1;
            assert map.get("hello") instanceof Map<?, ?>;
            assert ((Map<?, ?>) map.get("hello")).get("hello").equals("there");
        }
    }

    @Test
    public void emptyList() {
        final String string = """
            { "hello": [ ] }
            """;
        try (final Json json = new Json(string)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 1;
            assert map.get("hello") instanceof List<?>;
            assert map.get("hello") != null;
            assert ((List<?>) map.get("hello")).isEmpty();
        }
    }

    @Test
    public void singleList() {
        final String string = """
            { "hello": [ "there" ] }
            """;
        try (final Json json = new Json(string)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 1;
            assert map.get("hello") instanceof List<?>;
            assert map.get("hello") != null;
            assert ((List<?>) map.get("hello")).size() == 1;
            assert ((List<?>) map.get("hello")).contains("there");
        }
    }

    @Test
    public void multiList() {
        final String string = """
            { "hello": [ 1, 2, 6.5 ] }
            """;
        try (final Json json = new Json(string)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 1;
            assert map.get("hello") instanceof List<?>;
            assert map.get("hello") != null;
            assert ((List<?>) map.get("hello")).size() == 3;
            assert ((List<?>) map.get("hello")).contains(6.5);
            assert ((List<?>) map.get("hello")).contains(1);
        }
    }

    @Test
    public void complexList() {
        final String string = """
            { "hello": [ "there", [ {}, 2 ] ] }
            """;
        try (final Json json = new Json(string)) {
            final Map<String, Object> map = json.toMap();
            assert map != null;
            assert map.size() == 1;
            assert map.get("hello") instanceof List<?>;
            assert map.get("hello") != null;
            final List<?> list = ((List<?>) map.get("hello"));
            assert list.size() == 2;
            assert list.contains("there");
            assert list.get(1) instanceof List<?> inner && inner.size() == 2;
            assert list.get(1) instanceof List<?> inner && inner.contains(2);
        }
    }

    @Test
    public void onlyList() {
        final String string = """
            ["hello", "there"]
            """;
        try (final Json json = new Json(string)) {
            final List<?> list = json.toList();
            assert list != null;
            assert list.size() == 2;
            assert list.get(0).equals("hello");
            assert list.get(1).equals("there");
        }
    }

    @Test
    public void escapes() {
        class Result {
            String hello;
        }
        final String string = "{\"hello\": \"there\\ngeneral\\tkenobi\"}";
        final Result result = Json.fromJson(string, new Result());
        assert result != null;
        assert result.hello != null;
        assert result.hello.equals("there\ngeneral\tkenobi") : result.hello;
    }

}
