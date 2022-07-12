package mx.kenzie.argo.test;

import mx.kenzie.argo.Json;
import org.junit.Test;

import java.util.Arrays;

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
    @SuppressWarnings("FieldMayBeFinal")
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
    @SuppressWarnings("FieldMayBeFinal")
    public void reverseChildren() {
        class Child { int bean = 3; }
        class Result { String hello = "there"; Child child = new Child(); }
        final Result result = new Result();
        final String string = Json.toJson(result);
        assert string != null;
        assert string.equals("{\"hello\": \"there\",\"child\": {\"bean\": 3}}"): string;
    }
    
    @Test
    @SuppressWarnings("FieldMayBeFinal")
    public void simpleArrayTest() {
        class Result { String hello = "there"; int[] numbers = {5, 6, 7};  }
        final Result result = new Result();
        final String string = Json.toJson(result);
        assert string != null;
        assert string.equals("{\"numbers\": [5,6,7],\"hello\": \"there\"}"): string;
        final Result test = Json.fromJson(string, new Result());
        assert test != null;
        assert test.hello.equals(result.hello): test.hello;
        assert Arrays.equals(test.numbers, result.numbers): test.numbers;
    }
    
    @Test
    @SuppressWarnings("FieldMayBeFinal")
    public void complexArrayTest() {
        class Child { int a = 1; }
        class Result { Child[] children = { new Child(), new Child() }; }
        final Result result = new Result();
        result.children[0].a = 2;
        final String string = Json.toJson(result);
        assert string != null;
        assert string.equals("{\"children\": [{\"a\": 2},{\"a\": 1}]}"): string;
        final Result test = Json.fromJson(string, new Result());
        assert test != null;
        assert test.children != null;
        assert test.children.length == 2;
        assert test.children[0].a == 2;
        assert test.children[1].a == 1;
    }
    
    @Test
    @SuppressWarnings("FieldMayBeFinal")
    public void arrayFromData() {
        class Child { int a; }
        class Result { Child[] children; }
        final String string = """
            { "children": [ { "a": 5 }, { "a": 3 } ] }
            """;
        final Result result = Json.fromJson(string, new Result());
        assert result != null;
        assert result.children != null;
        assert result.children.length == 2;
        assert result.children[0].a == 5;
        assert result.children[1].a == 3;
    }
    
    @Test
    @SuppressWarnings("FieldMayBeFinal")
    public void directArray() {
        final String string = """
            [ 1, 3, 5 ]
            """;
        final int[] result = Json.fromJson(string, new int[0]);
        assert result != null;
        assert result.length == 3;
        assert result[1] == 3;
    }
    
    @Test
    public void existingMesh() {
        class Child { int foo, bar = 6; }
        class Result { final Child child = new Child(); }
        final String string = """
            { "hello": "there", "child": { "foo": 1 } }
            """;
        final Result result = Json.fromJson(string, new Result());
        assert result != null;
        assert result.child.foo == 1: result.child.foo;
        assert result.child.bar == 6: result.child.bar;
        
    }
    
}
