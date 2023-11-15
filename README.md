Argo
=====

### Opus #18

A lightweight J(a)son library.

### Description

This library converts `json` to Java's map/list object structure and vice-versa. \
It is designed to be very small (one file plus a `JsonException` for nicer errors.) \
It is designed to be as light as possible (interacts directly with streams with a ~16 byte buffer) and disposes of any closeable resources.

Json arrays are converted to `ArrayList`s for easier modification.

It is also possible to convert Java objects to and from json data directly.
Note that the data is applied to the appropriate fields.
Fields with illegal Java names (e.g. containing whitespace) will be ignored.
Errors will be thrown if the field cannot be written to or the data is incompatible.

This is mainly designed for use with local classes for simpler data dispersion.
Converting a json array will not convert its `Object` contents.

## Maven Information
```xml
<repository>
    <id>kenzie</id>
    <name>Kenzie's Repository</name>
    <url>https://repo.kenzie.mx/releases</url>
</repository>
``` 

```xml
<dependency>
    <groupId>mx.kenzie</groupId>
    <artifactId>argo</artifactId>
    <version>1.2.2</version>
</dependency>
```

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

Converting json to an object:
```java 
class Result { int a, b; String hello; }
final String string = // { "a": 1, "b": 6, "hello": "there" }
final Result result = Json.fromJson(string, new Result());
```

Converting an object to json:
```java 
class Child { int bean = 3; }
class Result { String hello = "there"; Child child = new Child(); }
final Result result = new Result();
final String string = Json.toJson(result);
// string = { "hello": "there", "child": { "bean": 3 } }
```

Converting a type with an array:
```java 
// data = { "numbers": [0.5, 2.2, ...], "children": [ ... ] }
class Result { double[] numbers; Child[] children; }
final Result result = Json.fromJson(data, new Result());
assert result.numbers[1] == 2.2;
```
