package mx.kenzie.argo.test;

import mx.kenzie.argo.Json;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicWriteTest {

    @Test
    public void simple() {
        final Map<String, Object> start = new HashMap<>();
        final String string = Json.toJson(start);
        assert string.equals("{}") : string;
        try (final Json json = new Json(string)) {
            final Map<String, Object> end = json.toMap();
            assert start.equals(end);
        }
    }

    @Test
    public void keyValue() {
        final Map<String, Object> start = new HashMap<>();
        start.put("hello", "there");
        final String string = Json.toJson(start);
        assert string.equals("{\"hello\": \"there\"}") : string;
        try (final Json json = new Json(string)) {
            final Map<String, Object> end = json.toMap();
            assert start.equals(end);
        }
    }

    @Test
    public void multiKey() {
        final Map<String, Object> start = new HashMap<>();
        start.put("hello", "there");
        start.put("general", "kenobi");
        final String string = Json.toJson(start);
        assert string.equals("{\"general\": \"kenobi\", \"hello\": \"there\"}") : string;
        try (final Json json = new Json(string)) {
            final Map<String, Object> end = json.toMap();
            assert start.equals(end);
        }
    }

    @Test
    public void types() {
        final Map<String, Object> start = new HashMap<>();
        start.put("hello", "there");
        start.put("a", 1);
        start.put("b", -12.5);
        start.put("c", null);
        start.put("d", true);
        final String string = Json.toJson(start);
        assert string.equals("{\"a\": 1, \"b\": -12.5, \"c\": null, \"d\": true, \"hello\": \"there\"}") : string;
        try (final Json json = new Json(string)) {
            final Map<String, Object> end = json.toMap();
            assert start.equals(end);
        }
    }

    @Test
    public void complex() {
        final Map<String, Object> start = new HashMap<>();
        final Map<String, Object> child = new HashMap<>();
        final List<Object> list = new ArrayList<>();
        child.put("hello", "there");
        start.put("hello", "there");
        start.put("child", child);
        list.add(12);
        list.add("bean");
        start.put("list", list);
        final String string = Json.toJson(start);
        assert string.equals("{\"hello\": \"there\", \"list\": [12, \"bean\"], \"child\": {\"hello\": \"there\"}}") : string;
        try (final Json json = new Json(string)) {
            final Map<String, Object> end = json.toMap();
            assert start.equals(end);
        }
    }

    @Test
    public void simpleList() {
        final List<Object> start = new ArrayList<>();
        final String string = Json.toJson(start);
        assert string.equals("[]") : string;
        try (final Json json = new Json(string)) {
            final List<Object> end = json.toList();
            assert start.equals(end);
        }
    }

    @Test
    public void complexList() {
        final List<Object> start = new ArrayList<>();
        start.add("beans");
        start.add(null);
        start.add(23);
        final String string = Json.toJson(start);
        assert string.equals("[\"beans\", null, 23]") : string;
        try (final Json json = new Json(string)) {
            final List<Object> end = json.toList();
            assert start.equals(end);
        }
    }

    @Test
    public void pretty() {
        final Map<String, Object> start = new HashMap<>();
        final Map<String, Object> child = new HashMap<>();
        final List<Object> list = new ArrayList<>();
        child.put("hello", "there");
        start.put("hello", "there");
        start.put("child", child);
        list.add(12);
        list.add("bean");
        start.put("list", list);
        final String string = Json.toJson(start, "  ");
        assert string.equals("""
            {
              "hello": "there",\s
              "list": [
                12,\s
                "bean"
              ],\s
              "child": {
                "hello": "there"
              }
            }""") : string;
    }

    @Test
    public void escapes() {
        class Result {
            final String hello = "there\ngeneral\tkenobi";
        }
        final String string = Json.toJson(new Result());
        assert string.equals("{\"hello\": \"there\\ngeneral\\tkenobi\"}") : string;
    }

}
