Argo
=====

### Opus #18

A lightweight J(a)son library.

### Description

This library converts JSON (_JavaScript_ Object Notation) to _Java_'s structures and vice versa. \
It is designed to be less bloated than the standard serialisers and allow more tailored use.

It can also be used to convert _Java_ objects to and from JSON data directly, although this is _caveat emptor_.
This is mainly designed for use with local classes for simpler data dispersion:
users can build their in-program data structures with JSON serialisation in mind.

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
    <version>2.0.0</version>
</dependency>
```

## How to Use

### Glossary

| Term           | Explanation                                                                                                                   |
|----------------|-------------------------------------------------------------------------------------------------------------------------------|
| JSON           | _JavaScript_ Object Notation                                                                                                  |
| Primitive Data | Data types that can't be broken down into smaller, simpler types (integers, decimals, ...)                                    |
| Constant Data  | Data types that can't be edited and won't change over time                                                                    |
| Marshalling    | Turning live objects into simpler, constant data types that can be taken out of the program and inserted into another program |
| Serialising    | Turning program data into a _purely_ textual/numeric representation that can be written to a file                             |
|                |                                                                                                                               |
|                |                                                                                                                               |

### The 'Universe'

We start by creating (or obtaining) a 'universe': a `Json` API object that knows how to convert _Java_ types into text.
Two are provided in the library: `Json.simple()` which can handle only primitive data,
and `Json.allTypes()` which will attempt to unpack **any** _Java_ object.

Users may also create their own `new Json()` and register specific conversion strategies for their types.
This is recommended as it allows the user to control how their data is arranged.

Note: these `Json` universes are [Grammar](https://github.com/Moderocky/Grammar) objects.
Other grammar objects are compatible with them.

### Writing Simple Strings

If the only goal is to turn simple data into JSON strings, then the library contains static methods.

Any simple data (strings, primitive numbers) can be converted.

```java
var data = Json.toString("hello"); // -> "hello"
var data = Json.toString(55); // -> 55
var data = Json.toString(false); // -> false
```

JSON 'objects' `{...}` can be serialised from string-to-data maps called "containers".
The `Container` type is safe as it can _only_ contain safe (string) keys and (constant) values.

```java
var map = Container.of("hello", "there");
var data = Json.toString(map); // -> {"hello": "there"}

var map = Container.of("hello", 55);
var data = Json.toString(map); // -> {"hello": 55}
```

Use of _Java_'s `Map` is also permitted, but this conversion is discouraged and unchecked.
Reasoning: there is no way to establish the type safety.

```java
var map = Map.of("hello", "there"); // :(
var data = Json.toString(map); // -> {"hello": "there"}
```

JSON 'arrays' `[...]` can be serialised from value sequences called "series".
There are two safe built-in array types: `Series` and `Array`.
As with maps, the built-in types are preferred over unknown `Collection<?>`s because they guarantee
their contents are safe.

```java
var array = Series.of("hello", "there");
var data = Json.toString(array); // -> ["hello", "there"]

var array = new Array("hello", "there");
var data = Json.toString(array); // -> ["hello", "there"]
```

Arrays can contain mixed data types.

```java
var array = Series.of("hello", -3, Container.empty());
var data = Json.toString(array); // -> ["hello", -3, {}]
```

A `Series` can be used like a regular list.
`Series.empty()` creates a new (ArrayList-backed) collection that allows values to be added and removed.
`Series.of(...)` creates a _frozen_, unmodifiable array with faster access and a lower memory footprint.

### Reading Simple Strings

The short-cut methods also allow for reading JSON data.

```java
String json = "5";
var object = Json.fromString(json); // -> (Integer) 5

String json = "{\"hello\": \"there\"}";
var object = Json.fromString(json); // -> Container of (String) hello = (String) there
```

As the JSON schema is unambiguous the parser should always be able to discern the correct primitive data type.
However, numbers in JSON can be truncated (e.g. 1.0 is schematically equivalent to 1)
and so unchecked JSON input data may yield an unexpected output type.

Where this might be a problem it is advised to manually check the type.
For example, we can ask for the result as any `Number` (the common supertype) and then convert it to the kind we want.

```java
String json = "5";
float value = Json.fromString(json); // ERROR! 5 yields an Integer
float value = Json.<Number>fromString(json).floatValue(); // OK! (5).floatValue() = 5.0
```

JSON key-value objects will be de-serialised as `Container`s.
JSON arrays will be de-serialised as a `Series`.

## Automatic Serialisation

### Records

Record types have a safe, built-in serialisation system.
When using an unsafe `Json` or the all-types instance, the record type will be automatically registered at first encounter if absent.

```jshelllanguage
Json json = new Json();
json.registerRecord(MyRecord.class); // Creates the serialiser

json.writer(output).writeObject(myRecord); // Serialises 'myRecord' to 'output'
```

The serialiser works by analysing the structure of the record to create a constructor-based marshalling strategy.

```java
record MyRecord(String foo, int bar) {
    
}
// {"foo": "...", "bar": ...}
record MyRecord(String foo, MyRecord inner) {

}
// {"foo": "...", "inner": {"foo": "...", "inner": ...}}
```

Calling the marshalling function will take the record parameters and store them in a name-bound `Container`.
Calling the unmarshalling function will invoke the record constructor with values from a container.
Complex types in the parameters are also marshalled as required.

### Enums
Enumerated types can be automatically serialised either by their name or their ordinal index.
Both strategies have a risk: if the user changes the name of an enum then old name-based JSON cannot be de-serialised.
However, if the user changes the order of existing enums in the class then ordinal-based JSON cannot be de-serialised.

```jshelllanguage
Json json = new Json();
json.registerEnum(RetentionPolicy.class);
// Stored as "SOURCE", "CLASS", or "RUNTIME"
json.registerEnumByOrdinal(RetentionPolicy.class);
// Stored as 0, 1, or 2
```

### Unchecked Objects

Unchecked objects are serialised like records but without the stringent safety checks.

```jshelllanguage
class MyObject {
    String a = "...";
    boolean b;
    MyObject inner;
}

Json json = new Json();
json.registerUncheckedObject(MyObject.class);
```

Instead of using the record parameters, the available fields are accessed directly and converted to a `Container`.

```json
{
  "a": "...",
  "b": false,
  "inner": null
}
```

The marshalling system will attempt to identify safe fields, but this could accidentally include fields the
user did not intend to be serialised, e.g. fields from superclasses or metadata inserted by the compiler.

Users should _never_ register unchecked types for serialisation without verifying they are safe.

For better control in user-created classes, the following strategies are available:
1. Fields marked `transient` will be ignored.
2. The `@Unwrap` annotation can be used to control _how_ a field is serialised.

```java
class MyObject {
    @Unwrap(name="my string") 
    String a = "...";
    
    @Unwrap(marshalAs = ArrayList.class, componentType = String.class)
    Collection<String> list = List.of("hello");
}
// {"my string": "...", list: ["hello"]}
```

Abstract field types (especially lists, maps, and data structures!) should be given hints to tell the marshaller
how the data is supposed to be stored.
This is not a problem during serialisation (we can infer the types from the actual values)
but during de-serialisation it is:
a list of JSON objects does not tell the serialiser what they should become.

```json
[
  {"foo": "what class is this?"},
  {"foo": "and this?", "bar": null}
]
```

### Constructors

De-serialisation will call a type's constructor's safe constructor where possible.
For example, record de-serialisation will always call the canonical constructor with the canonical parameters.
```json
{
  "foo": 1,
  "bar": 2
}
```

```java
record MyRecord(int foo, int bar) {}

// De-serialised as... new MyRecord(1, 2);
```
If any data entry is missing in the `Container`, the unmarshalling process will either attempt to substitute a default
value (e.g. `0`, `null`) or may fail entirely.

For non-record types (i.e. unchecked objects) the zero-parameter constructor is called, after which the fields are
assigned mechanically.

If the class has no zero-parameters constructor then one will be **synthesised**.

```java
class MyObject {
    int x, y;
    MyObject(int[] ints) { 
        // ...
    }
}

var myObject = Json.fromString(json, MyObject.class);
// creates a synthetic 'MyObject()' constructor
// calls the constructor
// sets x and y
```

Note: this process may cause unexpected behaviour.
Anything inside the regular constructor may not be called as expected, and internal synthetic fields may be unavailable.
Users are advised to treat these instances cautiously or to use manual serialisation.

## Manual Serialisation

Marshalling strategies can be registered with the [Grammar](https://github.com/Moderocky/Grammar) object to give
users more control of how certain types are serialised and de-serialised.
This can also help with registering types from third-party code.

Complex types can be converted to any primitive data value (numbers, strings, booleans) when they are encountered.

```jshelllanguage
Json json = new Json();
json.registerMarshallingStrategy(Date.class, Date::getTime);
json.registerUnmarshallingStrategy(Date.class, value -> new Date((long) value));
// Converts to a long timestamp in JSON
```

Complex types can also be converted to a `Container` or `Series` to be stored as JSON objects or arrays.

```jshelllanguage
json.registerMarshallingStrategy(Date.class, date -> Container.of(
        "day", date.getDay(),
        "month", date.getMonth(),
        "year", date.getYear()
));

json.registerUnmarshallingStrategy(Date.class, Container.class,
        value -> new Date(value.get("year"), value.get("month"), value.get("day"))
);
// {"day": 1, "month": 2, "year": 3}
```


```jshelllanguage
json.registerMarshallingStrategy(Date.class, date -> Series.of(
        date.getDay(), date.getMonth(), date.getYear()
));

json.registerUnmarshallingStrategy(Date.class, Series.class,
        value -> new Date(value.at(2), value.at(1), value.at(0))
);
// [1, 2, 3]
```


