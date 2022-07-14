package mx.kenzie.argo;

import mx.kenzie.argo.meta.Any;
import mx.kenzie.argo.meta.JsonException;
import mx.kenzie.argo.meta.Name;
import mx.kenzie.argo.meta.Optional;
import sun.reflect.ReflectionFactory;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

import static mx.kenzie.argo.Json.*;

@SuppressWarnings({"unchecked", "TypeParameterHidesVisibleType", "SameParameterValue"})
public class Json implements Closeable, AutoCloseable {
    
    static final byte
        START = 0,
        EXPECTING_KEY = 1,
        IN_KEY = 2,
        AFTER_KEY = 3,
        EXPECTING_VALUE = 4,
        EXPECTING_END = 5,
        END = -1;
    
    protected transient java.io.Reader reader;
    protected transient Writer writer;
    protected int state = START;
    private transient StringBuilder currentKey, currentValue;
    
    //<editor-fold desc="Constructors" defaultstate="collapsed">
    public Json(java.io.Reader reader) {
        this.reader = reader;
    }

    public Json(String string) {
        this(string, StandardCharsets.UTF_8);
    }

    public Json(String string, Charset charset) {
        this(new ByteArrayInputStream(string.getBytes(charset)));
    }
    public Json(InputStream reader) {
        this.reader = new BufferedReader(new InputStreamReader(reader));
    }

    public Json(File file) {
        try {
            this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            this.writer = new OutputStreamWriter(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new JsonException(e);
        }
    }

    public Json(OutputStream stream) {
        this.writer = new OutputStreamWriter(stream);
    }

    public Json(java.io.Writer writer) {
        this.writer = writer;
    }
    //</editor-fold>
    
    //<editor-fold desc="Construction" defaultstate="collapsed">
    @SuppressWarnings("unchecked")
    private <Type> Constructor<Type> createConstructor0(Class<Type> type) throws NoSuchMethodException {
        final Constructor<?> shift = Object.class.getConstructor();
        return (Constructor<Type>) ReflectionFactory.getReflectionFactory().newConstructorForSerialization(type, shift);
    }
    
    protected <Type> Type createObject(Class<Type> type) {
        try {
            if (type.isLocalClass() || type.getEnclosingClass() != null) {
                final Constructor<Type> constructor = this.createConstructor0(type);
                assert constructor != null;
                return constructor.newInstance();
            } else {
                final Constructor<Type> constructor = type.getDeclaredConstructor();
                final boolean result = constructor.trySetAccessible();
                assert result || constructor.canAccess(null);
                return constructor.newInstance();
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new JsonException("Unable to create '" + type.getSimpleName() + "' object.", e);
        }
    }
    
    private Object deconstructSimple(Object value, Class<?> component) {
        if (value == null) return null;
        if (value instanceof String) return value;
        if (value instanceof Number) return value;
        if (value instanceof Boolean) return value;
        if (value.getClass().isArray()) {
            final List<Object> list = new ArrayList<>();
            this.deconstructArray(value, component, list);
            return list;
        }
        final Map<String, Object> map = new HashMap<>();
        this.write(value, component, map);
        return map;
    }
    
    private void deconstructArray(Object array, Class<?> type, List<Object> list) {
        final Class<?> component = type.getComponentType();
        if (component.isPrimitive()) {
            if (array instanceof int[] numbers) for (int number : numbers) list.add(number);
            else if (array instanceof long[] numbers) for (long number : numbers) list.add(number);
            else if (array instanceof double[] numbers) for (double number : numbers) list.add(number);
            else if (array instanceof float[] numbers) for (float number : numbers) list.add(number);
            else if (array instanceof boolean[] numbers) for (boolean number : numbers) list.add(number);
        } else {
            final Object[] objects = (Object[]) array;
            for (final Object object : objects) list.add(this.deconstructSimple(object, component));
        }
    }
    //</editor-fold>
    
    //<editor-fold desc="Writers" defaultstate="collapsed">
    protected void write(Object object, Class<?> type, Map<String, Object> map) {
        final Set<Field> fields = new HashSet<>();
        fields.addAll(List.of(type.getDeclaredFields()));
        fields.addAll(List.of(type.getFields()));
        for (final Field field : fields) {
            final int modifiers = field.getModifiers();
            if ((modifiers & 0x00000002) != 0) continue;
            if ((modifiers & 0x00000008) != 0) continue;
            if ((modifiers & 0x00000080) != 0) continue;
            if ((modifiers & 0x00001000) != 0) continue;
            if (!field.canAccess(object)) field.trySetAccessible();
            try {
                final Object value = field.get(object);
                if (value == null && field.isAnnotationPresent(Optional.class)) continue;
                final Class<?> expected = field.getType();
                final String key;
                if (field.isAnnotationPresent(Name.class)) key = field.getAnnotation(Name.class).value();
                else key = field.getName();
                if (value == null) map.put(key, null);
                else if (value instanceof String) map.put(key, value);
                else if (value instanceof Number) map.put(key, value);
                else if (value instanceof Boolean) map.put(key, value);
                else if (value instanceof List<?>) map.put(key, value);
                else if (expected.isArray()) {
                    final List<Object> child = new ArrayList<>();
                    map.put(key, child);
                    this.deconstructArray(value, expected, child);
                } else {
                    final Map<String, Object> child = new HashMap<>();
                    map.put(key, child);
                    this.write(value, expected, child);
                }
            } catch (Throwable ex) {
                throw new JsonException("Unable to write to object:", ex);
            }
        }
    }
    
    @SuppressWarnings("all")
    public void write(Object object, Class<?> type, String indent) {
        assert object != null: "Object was null.";
        assert object instanceof Class<?> ^ true: "Classes cannot be read from.";
        final Map<String, Object> map = new HashMap<>();
        this.write(object, type, map);
        this.write(map, indent, 0);
    }
    
    public void write(Object object, String indent) {
        if (object instanceof Map<?, ?> map) this.write(map, indent, 0);
        else if (object instanceof List<?> list) this.write(list, indent, 0);
        else this.write(object, object.getClass(), indent);
    }
    
    public void write(Object object) {
        this.write(object, object.getClass(), (String) null);
    }
    //</editor-fold>
    
    //<editor-fold desc="Converters" defaultstate="collapsed">
    private Object convertSimple(Object data, Class<?> expected) {
        if (data instanceof List<?> list) return this.convertList(expected, list, false);
        else if (data instanceof Map<?, ?> map) return this.toObject(this.createObject(expected), expected, map);
        else return data;
    }
    
    private Object convertList(Class<?> type, List<?> list, boolean any) {
        final Class<?> component = type.getComponentType();
        final Object object = Array.newInstance(component, list.size());
        final Object[] objects = list.toArray();
        if (component.isPrimitive()) {
            if (component == boolean.class) for (int i = 0; i < objects.length; i++) {
                final Object value = objects[i];
                Array.setBoolean(object, i, (boolean) value);
            } else if (component == int.class) for (int i = 0; i < objects.length; i++) {
                final Object value = objects[i];
                Array.setInt(object, i, ((Number) value).intValue());
            } else if (component == long.class) for (int i = 0; i < objects.length; i++) {
                final Object value = objects[i];
                Array.setLong(object, i, ((Number) value).longValue());
            } else if (component == double.class) for (int i = 0; i < objects.length; i++) {
                final Object value = objects[i];
                Array.setDouble(object, i, ((Number) value).doubleValue());
            } else if (component == float.class) for (int i = 0; i < objects.length; i++) {
                final Object value = objects[i];
                Array.setFloat(object, i, ((Number) value).floatValue());
            }
        } else {
            final Object[] array = (Object[]) object;
            for (int i = 0; i < objects.length; i++) {
                final Object value = objects[i];
                final Class<?> target;
                if (any) target = value.getClass();
                else target = component;
                array[i] = this.convertSimple(value, target);
            }
        }
        return object;
    }
    //</editor-fold>
    
    //<editor-fold desc="Object Wrappers" defaultstate="collapsed">
    @SuppressWarnings("all")
    protected <Type> Type toObject(Type object, Class<?> type, Map<?, ?> map) {
        assert object != null: "Object was null.";
        assert object instanceof Class<?> ^ true: "Classes cannot be written to.";
        final Set<Field> fields = new HashSet<>();
        fields.addAll(List.of(type.getDeclaredFields()));
        fields.addAll(List.of(type.getFields()));
        for (final Field field : fields) {
            final int modifiers = field.getModifiers();
            if ((modifiers & 0x00000002) != 0) continue;
            if ((modifiers & 0x00000008) != 0) continue;
            if ((modifiers & 0x00000080) != 0) continue;
            if ((modifiers & 0x00001000) != 0) continue;
            if (!field.canAccess(object)) field.trySetAccessible();
            final String key;
            if (field.isAnnotationPresent(Name.class)) key = field.getAnnotation(Name.class).value();
            else key = field.getName();
            if (!map.containsKey(key)) continue;
            final Object value = map.get(key);
            final Class<?> expected = field.getType();
            final Object lock;
            if ((modifiers & 0x00000040) != 0) lock = object;
            else lock = new Object();
            try {
                synchronized (lock) {
                    if (expected.isPrimitive()) {
                        if (value instanceof Boolean boo) field.setBoolean(object, boo.booleanValue());
                        else if (value instanceof Number number) {
                            if (expected == int.class) field.setInt(object, number.intValue());
                            else if (expected == long.class) field.setLong(object, number.longValue());
                            else if (expected == double.class) field.setDouble(object, number.doubleValue());
                            else if (expected == float.class) field.setFloat(object, number.floatValue());
                        }
                    } else if (value == null) field.set(object, null);
                    else if (expected.isAssignableFrom(value.getClass())) field.set(object, value);
                    else if (value instanceof Map<?, ?> child) {
                        final Object sub, existing = field.get(object);
                        if (existing == null) field.set(object, sub = this.createObject(expected));
                        else sub = existing;
                        final Class<?> target;
                        if (field.isAnnotationPresent(Any.class)) target = object.getClass();
                        else target = expected;
                        this.toObject(sub, target, child);
                    } else if (expected.isArray() && value instanceof List<?> list) {
                        final Object array = this.convertList(expected, list, field.isAnnotationPresent(Any.class));
                        field.set(object, array);
                    } else throw new JsonException("Value of '" + field.getName() + "' (" + object.getClass()
                        .getSimpleName() + ") could not be mapped to type " + expected.getSimpleName());
                }
            } catch (Throwable ex) {
                throw new JsonException("Unable to write to object:", ex);
            }
        }
        return object;
    }
    
    @SuppressWarnings({"all"})
    public <Type> Type toObject(Type object, Class<?> type) {
        assert object != null: "Object was null.";
        assert object instanceof Class<?> ^ true: "Classes cannot be written to.";
        final Map<String, Object> map = this.toMap(new HashMap<>());
        return this.toObject(object, type, map);
    }
    
    public <Type> Type toObject(Type object) {
        assert object != null: "Object was null.";
        return this.toObject(object, object.getClass());
    }
    
    public <Type> Type toObject(Class<Type> type) {
        final Type object = this.createObject(type);
        return this.toObject(object, type);
    }
    
    public Object[] toArray() {
        return this.toArray(new Object[0]);
    }
    
    public <Component> Component[] toArray(Class<Component> type) {
        return (Component[]) this.toArray(Array.newInstance(type, 0));
    }
    
    @SuppressWarnings({"all"})
    public <Container> Container toArray(Container array) {
        if (array == null) throw new JsonException("Provided array was null.");
        final Class<?> type = array.getClass();
        if (!type.isArray()) throw new JsonException("Provided object was not an array.");
        final Class<?> component = type.getComponentType();
        final List<?> list = this.toList();
        final Container container;
        if (Array.getLength(array) < 1) container = (Container) Array.newInstance(component, list.size());
        else container = array;
        final Object source = this.convertList(container.getClass(), list, false);
        final int a = Array.getLength(container), b = Array.getLength(source);
        System.arraycopy(source, 0, container, 0, Math.min(a, b));
        return container;
    }
    //</editor-fold>
    
    //<editor-fold desc="Readers" defaultstate="collapsed">
    public List<Object> toList() {
        return this.toList(new ArrayList<>());
    }
    
    public <Container extends List<Object>> Container toNewList(Supplier<Container> supplier) {
        final Container list = supplier.get();
        return this.toList(list);
    }
    
    public <Container extends List<Object>> Container toList(Container list) {
        new JsonArray(reader).toList(list);
        return list;
    }
    
    public Map<String, Object> toMap() {
        return this.toMap(new HashMap<>());
    }
    
    public <Container extends Map<String, Object>> Container toNewMap(Supplier<Container> supplier) {
        final Container map = supplier.get();
        return this.toMap(map);
    }
    
    public <Container extends Map<String, Object>> Container toMap(final Container map) {
        if (reader == null) throw new JsonException("This Json controller has no reader.");
        loop: while (this.state != END) {
            this.mark(4);
            final char c = this.readChar();
            state: switch (state) {
                case START:
                    if (c == '{') {
                        this.state = EXPECTING_KEY;
                        continue;
                    } else if (c > 32 && c != 160) {
                        throw new JsonException("Expected opening '{', found '" + c + "'.", map);
                    }
                    break;
                case EXPECTING_KEY:
                    assert currentValue == null;
                    assert currentKey == null;
                    if (c == '"') {
                        this.state = IN_KEY;
                        this.currentKey = new StringBuilder();
                        new StringReader(reader, currentKey).read();
                        this.state = AFTER_KEY;
                        continue;
                    } else if (c == '}') {
                        break loop;
                    } else if (c > 32 && c != 160) {
                        throw new JsonException("Expected key start '\"', found '" + c + "'.", map);
                    }
                    break;
                case IN_KEY:
                    assert currentKey != null;
                case AFTER_KEY:
                    assert currentValue == null;
                    assert currentKey != null;
                    assert currentKey.length() > 0;
                    if (c == ':') {
                        this.state = EXPECTING_VALUE;
                        continue;
                    } else if (c > 32 && c != 160) {
                        throw new JsonException("Expected separator ':', found '" + c + "'.", map);
                    }
                    break;
                case EXPECTING_VALUE:
                    if (c <= 32 || c == 160) continue;
                    assert currentValue == null;
                    assert currentKey != null;
                    assert currentKey.length() > 0;
                    if (c == '"') {
                        this.currentValue = new StringBuilder();
                        final Object value = new StringReader(reader, currentValue).read();
                        map.put(currentKey.toString(), value);
                        this.state = EXPECTING_END;
                        continue;
                    } else if (c == '{') {
                        this.reset();
                        final Json json = new Json(this.reader);
                        final Map<String, Object> value = json.toMap(new HashMap<>());
                        map.put(currentKey.toString(), value);
                        this.state = EXPECTING_END;
                        continue;
                    } else if (c == '[') {
                        this.reset();
                        final JsonArray array = new JsonArray(this.reader);
                        final List<Object> value = array.toList(new ArrayList<>());
                        map.put(currentKey.toString(), value);
                        this.state = EXPECTING_END;
                        continue;
                    } else if (c >= '0' && c <= '9' || c == '-') {
                        this.reset();
                        this.currentValue = new StringBuilder();
                        final Object value = new NumberReader(reader, currentValue).read();
                        map.put(currentKey.toString(), value);
                        this.state = EXPECTING_END;
                        continue;
                    } else if (c == 'f' || c == 't') {
                        this.reset();
                        this.currentValue = new StringBuilder();
                        final Object value = new BooleanReader(reader, currentValue).read();
                        map.put(currentKey.toString(), value);
                        this.state = EXPECTING_END;
                        continue;
                    } else if (c == 'n') {
                        this.reset();
                        this.currentValue = new StringBuilder();
                        final Object value = new NullReader(reader, currentValue).read();
                        map.put(currentKey.toString(), value);
                        this.state = EXPECTING_END;
                        continue;
                    } else {
                        throw new JsonException("Expected value start, found illegal '" + c + "'.", map);
                    }
                case EXPECTING_END:
                    if (c <= 32 || c == 160) continue;
                    if (c == ',') {
                        this.state = EXPECTING_KEY;
                        assert currentKey != null;
                        this.currentKey = null;
                        this.currentValue = null;
                        continue;
                    } else if (c == '}') {
                        break loop;
                    }
                    throw new JsonException("Expected delimiter ',' or end '}', found '" + c + "'.", map);
                default:
                    throw new JsonException("Unexpected header state.", map);
            }
        }
        return map;
    }
    
    public void write(List<?> list) {
        this.write(list, null, 0);
    }
    
    public void write(List<?> list, String indent, int level) {
        if (writer == null) throw new JsonException("This Json controller has no writer.");
        new JsonArray(writer).write(list, indent, level);
    }
    
    public void write(Map<?, ?> map) {
        this.write(map, null, 0);
    }
    
    public void write(Map<?, ?> map, String indent, int level) {
        if (writer == null) throw new JsonException("This Json controller has no writer.");
        final boolean pretty = indent != null && !indent.isEmpty() && !map.isEmpty();
        this.writeChar('{');
        if (pretty) this.writeString(System.lineSeparator());
        level++;
        boolean first = true;
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (first) first = false;
            else {
                this.writeChar(',');
                if (pretty) this.writeString(System.lineSeparator());
            }
            if (pretty) for (int i = 0; i < level; i++) this.writeString(indent);
            this.writeChar('"');
            assert entry.getKey() != null;
            this.writeString(entry.getKey().toString());
            this.writeString("\": ");
            final Object value = entry.getValue();
            if (value instanceof Boolean || value instanceof Number) this.writeString(value.toString());
            else if (value instanceof String string) this.writeString( '"' + string + '"');
            else if (value == null) this.writeString("null");
            else if (value instanceof Map<?,?> child) new Json(writer).write(child, indent, level);
            else if (value instanceof List<?> child) new JsonArray(writer).write(child, indent, level);
        }
        level--;
        if (pretty) this.writeString(System.lineSeparator());
        if (pretty) for (int i = 0; i < level; i++) this.writeString(indent);
        this.writeChar('}');
    }
    //</editor-fold>
    
    //<editor-fold desc="Helpers" defaultstate="collapsed">
    protected void mark(int chars) {
        try {
            this.reader.mark(chars);
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
    }
    
    protected void writeString(String value) {
        try {
            this.writer.write(value);
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
    }
    
    protected void writeChar(char c) {
        try {
            this.writer.write(c);
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
    }
    
    protected char readChar() {
        try {
            return (char) reader.read();
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
    }
    
    protected void reset() {
        try {
            this.reader.reset();
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
    }
    
    @Override
    public void close() {
        try {
            this.state = 0;
            this.currentValue = null;
            this.currentKey = null;
            if (reader != null) reader.close();
            if (writer != null) this.writer.close();
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
    }
    //</editor-fold>
    
    //<editor-fold desc="Static Helper Methods" defaultstate="collapsed">
    public static String toJson(Object object, Class<?> type, String indent) {
        final StringWriter writer = new StringWriter();
        new Json(writer).write(object, type, indent);
        return writer.toString();
    }
    
    public static String toJson(Object object, String indent) {
        final StringWriter writer = new StringWriter();
        new Json(writer).write(object, indent);
        return writer.toString();
    }
    
    public static String toJson(Object object) {
        final StringWriter writer = new StringWriter();
        new Json(writer).write(object);
        return writer.toString();
    }
    
    public static String toJson(Map<?, ?> map, String indent) {
        final StringWriter writer = new StringWriter();
        new Json(writer).write(map, indent, 0);
        return writer.toString();
    }
    
    public static String toJson(Map<?, ?> map) {
        final StringWriter writer = new StringWriter();
        new Json(writer).write(map);
        return writer.toString();
    }
    
    public static String toJson(List<?> list, String indent) {
        final StringWriter writer = new StringWriter();
        new Json(writer).write(list, indent, 0);
        return writer.toString();
    }
    
    public static String toJson(List<?> list) {
        final StringWriter writer = new StringWriter();
        new Json(writer).write(list);
        return writer.toString();
    }
    
    public static Map<String, Object> fromJson(String string) {
        try (final Json json = new Json(string)) {
            return json.toMap(new HashMap<>());
        }
    }
    
    public static <Type> Type fromJson(String string, Type object) {
        try (final Json json = new Json(string)) {
            if (object.getClass().isArray()) return json.toArray(object);
            else return json.toObject(object);
        }
    }
    
    public static <Type> Type fromJson(String string, Class<Type> object) {
        try (final Json json = new Json(string)) {
            if (object.isArray()) return (Type) json.toArray(object.getComponentType());
            else return json.toObject(object);
        }
    }
    
    public static <Type> Type fromJson(String string, Type object, Class<?> type) {
        try (final Json json = new Json(string)) {
            return json.toObject(object, type);
        }
    }
    //</editor-fold>
    
    //<editor-fold desc="Reader Classes" defaultstate="collapsed">
    private interface Reader {
        Object read();
    }
    
    protected record BooleanReader(java.io.Reader stream, StringBuilder builder)
        implements Reader {
        public Object read() {
            try {
                this.stream.mark(4);
                if (this.stream.read() == 't'
                    && this.stream.read() == 'r'
                    && this.stream.read() == 'u'
                    && this.stream.read() == 'e'
                ) return true;
                this.stream.reset();
                if (this.stream.read() == 'f'
                    && this.stream.read() == 'a'
                    && this.stream.read() == 'l'
                    && this.stream.read() == 's'
                    && this.stream.read() == 'e'
                ) return false;
                this.stream.reset();
                throw new JsonException("Unable to decipher value starting '"
                    + (char) this.stream.read()
                    + (char) this.stream.read()
                    + (char) this.stream.read()
                    + (char) this.stream.read() + "...' when expecting boolean.");
            } catch (EOFException ex) {
                throw new JsonException("Reached end of Json without finishing expected boolean.");
            } catch (IOException ex) {
                throw new JsonException(ex);
            }
        }
    }
    
    protected record NullReader(java.io.Reader stream, StringBuilder builder)
        implements Reader {
        public Object read() {
            try {
                this.stream.mark(4);
                if (this.stream.read() == 'n'
                    && this.stream.read() == 'u'
                    && this.stream.read() == 'l'
                    && this.stream.read() == 'l'
                ) return null;
                this.stream.reset();
                throw new JsonException("Unable to decipher value starting '"
                    + (char) this.stream.read()
                    + (char) this.stream.read()
                    + (char) this.stream.read()
                    + (char) this.stream.read() + "...' when expecting null.");
            } catch (EOFException ex) {
                throw new JsonException("Reached end of Json without finishing expected null.");
            } catch (IOException ex) {
                throw new JsonException(ex);
            }
        }
    }
    
    protected record StringReader(java.io.Reader stream, StringBuilder builder)
        implements Reader {
        
        @Override
        public Object read() {
            try {
                boolean escape = false;
                while (true) {
                    final char c = (char) stream.read();
                    if (c == '\\' && !escape) escape = true;
                    else if (c == '"' && !escape) return builder.toString();
                    else {
                        this.builder.append(c);
                        if (escape) escape = false;
                    }
                }
            } catch (EOFException ex) {
                throw new JsonException("Reached end of Json without closing quote '\"'");
            } catch (IOException ex) {
                throw new JsonException(ex);
            }
        }
        
    }
    
    protected record NumberReader(java.io.Reader stream, StringBuilder builder)
        implements Reader {
        
        @Override
        public Object read() {
            try {
                boolean first = true, decimal = false;
                while (true) {
                    this.stream.mark(4);
                    final char c = (char) stream.read();
                    if (first) {
                        first = false;
                        assert (c >= '0' && c <= '9') || c == '-';
                    } else if (c == '.' && !decimal) {
                        decimal = true;
                    } else if (c < '0' || c > '9') {
                        this.stream.reset();
                        if (decimal) return Double.valueOf(builder.toString());
                        final long value = Long.parseLong(builder.toString());
                        if (value == (int) value) return (int) value;
                        else return value;
                    }
                    this.builder.append(c);
                }
            } catch (EOFException ex) {
                throw new JsonException("Reached end of Json without closing quote '\"'");
            } catch (IOException ex) {
                throw new JsonException(ex);
            }
        }
        
    }
    //</editor-fold>
    
    @SuppressWarnings({"SameParameterValue", "TypeParameterHidesVisibleType"})
    public static class JsonHelper extends Json {
        
        public JsonHelper() {
            super("{}");
        }
        
        public <Type> Type createObject(Class<Type> type) {
            return super.createObject(type);
        }
        
        public void mapToObject(Object object, Class<?> type, Map<?, ?> map) {
            super.toObject(object, type, map);
        }
        
        public void objectToMap(Object object, Class<?> type, Map<String, Object> map) {
            super.write(object, type, map);
        }
        
    }
    
}

@SuppressWarnings({"TypeParameterHidesVisibleType", "SameParameterValue", "null"})
class JsonArray {
    
    private final Reader reader;
    private final Writer writer;
    protected int state = START;
    private transient StringBuilder currentValue;
    
    JsonArray(Reader stream) {
        this.reader = stream;
        this.writer = null;
    }
    
    JsonArray(Writer writer) {
        this.reader = null;
        this.writer = writer;
    }
    
    public void write(List<?> list, String indent, int level) {
        final boolean pretty = indent != null && !indent.isEmpty() && !list.isEmpty();
        this.writeChar('[');
        if (pretty) this.writeString(System.lineSeparator());
        level++;
        boolean first = true;
        for (final Object value : list) {
            if (first) first = false;
            else {
                this.writeChar(',');
                if (pretty) this.writeString(System.lineSeparator());
            }
            if (pretty) for (int i = 0; i < level; i++) this.writeString(indent);
            if (value instanceof Boolean || value instanceof Number) this.writeString(value.toString());
            else if (value instanceof String string) this.writeString( '"' + string + '"');
            else if (value == null) this.writeString("null");
            else if (value instanceof Map<?,?> child) new Json(writer).write(child, indent, level);
            else if (value instanceof List<?> child) new JsonArray(writer).write(child, indent, level);
        }
        level--;
        if (pretty) this.writeString(System.lineSeparator());
        if (pretty) for (int i = 0; i < level; i++) this.writeString(indent);
        this.writeChar(']');
        
    }
    
    public <Container extends List<Object>> Container toList(Container list) {
        if (reader == null) throw new JsonException("This Json controller has no reader.");
        loop: while (this.state != END) {
            this.mark(4);
            final char c = this.readChar();
            state: switch (state) {
                case START:
                    if (c == '[') {
                        this.state = EXPECTING_VALUE;
                        continue;
                    } else if (c > 32 && c != 160) {
                        throw new JsonException("Expected opening '[', found '" + c + "'.");
                    }
                    break;
                case EXPECTING_VALUE:
                    if (c <= 32 || c == 160) continue;
                    assert currentValue == null;
                    if (c == '"') {
                        this.currentValue = new StringBuilder();
                        final Object value = new Json.StringReader(reader, currentValue).read();
                        list.add(value);
                        this.state = EXPECTING_END;
                        continue;
                    } else if (c == '{') {
                        this.reset();
                        final Json json = new Json(this.reader);
                        final Map<String, Object> value = json.toMap(new HashMap<>());
                        list.add(value);
                        this.state = EXPECTING_END;
                        continue;
                    } else if (c == '[') {
                        this.reset();
                        final JsonArray array = new JsonArray(this.reader);
                        final List<Object> value = array.toList(new ArrayList<>());
                        list.add(value);
                        this.state = EXPECTING_END;
                        continue;
                    } else if (c >= '0' && c <= '9' || c == '-') {
                        this.reset();
                        this.currentValue = new StringBuilder();
                        final Object value = new Json.NumberReader(reader, currentValue).read();
                        list.add(value);
                        this.state = EXPECTING_END;
                        continue;
                    } else if (c == 'f' || c == 't') {
                        this.reset();
                        this.currentValue = new StringBuilder();
                        final Object value = new Json.BooleanReader(reader, currentValue).read();
                        list.add(value);
                        this.state = EXPECTING_END;
                        continue;
                    } else if (c == 'n') {
                        this.reset();
                        this.currentValue = new StringBuilder();
                        final Object value = new Json.NullReader(reader, currentValue).read();
                        list.add(value);
                        this.state = EXPECTING_END;
                        continue;
                    } else if (c == ']') {
                        break loop;
                    } else {
                        throw new JsonException("Expected value start, found illegal '" + c + "'.");
                    }
                case EXPECTING_END:
                    if (c <= 32 || c == 160) continue;
                    if (c == ',') {
                        this.state = EXPECTING_VALUE;
                        this.currentValue = null;
                        continue;
                    } else if (c == ']') {
                        break loop;
                    }
                    throw new JsonException("Expected delimiter ',' or end ']', found '" + c + "'.");
                default:
                    throw new JsonException("Unexpected header state.");
            }
        }
        return list;
    }
    
    protected void writeString(String value) {
        try {
            assert writer != null;
            this.writer.write(value);
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
    }
    
    protected void writeChar(char c) {
        try {
            assert writer != null;
            this.writer.write(c);
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
    }
    
    protected void mark(int chars) {
        try {
            assert reader != null;
            this.reader.mark(chars);
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
    }
    
    protected char readChar() {
        try {
            assert reader != null;
            return (char) reader.read();
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
    }
    
    protected void reset() {
        try {
            assert reader != null;
            this.reader.reset();
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
    }
    
}
