Argo
=====

### Opus #18

A lightweight J(a)son library.

### Description

This library converts `json` to Java's map/list object structure and vice-versa. \
It is designed to be very small (one file plus a `JsonException` for nicer errors.) \
It is designed to be as light as possible (interacts directly with streams with a ~16 byte buffer) and disposes of any closeable resources.

Json arrays are converted to `ArrayList`s for easier modification.

### How to Use

Reading data:

```java 
// data = { "hello": true, "there": null }
// String / File / InputStream
try (final Json json = new Json(data)) {
    final Map<String, Object> map = json.toMap();
    assert map.size() == 2;
    assert map.get("hello").equals(true): map;
    assert map.get("there") == null: map;
}
// data = [ 1, 2, 3 ]
try (final Json json = new Json(data)) {
    final List<Object> list = json.toList();
    assert map.size() == 3;
    assert list.contains(2);
}
```

Writing data:

```java 
// output is File / Writer
new Json(output).write(map);
Json.toJson(map); // -> { ... }
Json.toJson(list); // -> [ ... ]
```
