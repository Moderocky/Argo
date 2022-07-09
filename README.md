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

Reading data...

From a json object:
```java 
// data = { "hello": true, "there": null }
// data is String / File / InputStream
try (final Json json = new Json(data)) {
    final Map<String, Object> map = json.toMap();
}

// Simple string -> map
final Map<String, Object> map = Json.fromJson(data);
```

From a json array:
```java
// data = [ 1, 2, 3 ]
// data is String / File / InputStream
try (final Json json = new Json(data)) {
    final List<Object> list = json.toList();
}
```

Writing as json data:
```java 
// output is a File / Writer
// map is the data
new Json(output).write(map);

// fast object to json string
Json.toJson(map); // -> { ... }
Json.toJson(list); // -> [ ... ]
```
