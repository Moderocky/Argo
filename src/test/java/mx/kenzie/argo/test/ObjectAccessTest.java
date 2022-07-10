package mx.kenzie.argo.test;

import mx.kenzie.argo.Json;
import org.junit.Test;

public class ObjectAccessTest {
    
    public static final class Simple {
        public String hello = null;
    }
    
    @Test
    public void simple() {
        final String string = """
            { "hello": "there" }
            """;
        final Simple result = Json.fromJson(string, new Simple());
        assert result != null;
        assert result.hello.equals("there");
    }
    
    @Test
    public void inner() {
        class Result { String hello = null; }
        final String string = """
            { "hello": "there" }
            """;
        final Result result = Json.fromJson(string, new Result());
        assert result != null;
        assert result.hello.equals("there");
    }
    
    @Test
    public void primitive() {
        class Result { int a, b; }
        final String string = """
            { "a": 1, "b": 6 }
            """;
        final Result result = Json.fromJson(string, new Result());
        assert result != null;
        assert result.a == 1;
        assert result.b == 6;
    }
    
    @Test
    public void mixed() {
        class Result { int a, b; String hello; }
        final String string = """
            { "a": 1, "b": 6, "hello": "there" }
            """;
        final Result result = Json.fromJson(string, new Result());
        assert result != null;
        assert result.a == 1: result.a;
        assert result.b == 6: result.b;
        assert result.hello.equals("there"): result.hello;
    }
    
    @Test
    public void reverse() {
        class Result { int a = 3, b; String hello; }
        final Result result = new Result();
        result.a--;
        result.hello = "there";
        final String string = Json.toJson(result);
        assert string != null;
        assert string.equals("{\"a\": 2,\"b\": 0,\"hello\": \"there\"}"): string;
    }
    
    @Test
    public void children() {
        class Child { int bean; }
        class Result { String hello; Child child; }
        final String string = """
            { "hello": "there", "child": { "bean": 5 } }
            """;
        final Result result = Json.fromJson(string, new Result());
        assert result != null;
        assert result.hello.equals("there"): result.hello;
        assert result.child != null;
        assert result.child.bean == 5: result.child.bean;
    }
    
    @Test
    public void reverseChildren() {
        class Child { int bean = 3; }
        class Result { String hello = "there"; Child child = new Child(); }
        final Result result = new Result();
        final String string = Json.toJson(result);
        assert string != null;
        assert string.equals("{\"hello\": \"there\",\"child\": {\"bean\": 3}}"): string;
    }
    
}
