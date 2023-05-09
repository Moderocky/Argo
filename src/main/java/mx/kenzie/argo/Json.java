package mx.kenzie.argo;

import mx.kenzie.argo.meta.Optional;
import mx.kenzie.argo.meta.*;
import org.jetbrains.annotations.Contract;
import sun.reflect.ReflectionFactory;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@SuppressWarnings({"unchecked", "SameParameterValue"})
public class Json implements Closeable, AutoCloseable {

    protected static final Pattern CODE_POINT = Pattern.compile("\\\\u\\w{4}");
    static final byte
        START = 0,
        EXPECTING_KEY = 1,
        IN_KEY = 2,
        AFTER_KEY = 3,
        EXPECTING_VALUE = 4,
        EXPECTING_END = 5,
        END = -1;
    static final DecimalFormat FORMAT = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    static {
        FORMAT.setMaximumFractionDigits(340);
    }

    protected transient java.io.Reader reader;
    protected transient Writer writer;
    protected int state = START;
    protected WriteController controller = new WriteController(null);
    private transient StringBuilder currentKey, currentValue;

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

    protected static String charToCode(Object object) {
        final StringBuilder builder = new StringBuilder();
        for (final char c : object.toString().toCharArray()) {
            if (c >= 128) builder.append("\\u").append(String.format("%04X", (int) c));
            else builder.append(c);
        }
        return builder.toString();
    }

    protected static String codeToChar(Object object) {
        final String string = object.toString();
        final int point = Integer.parseInt(string.substring(2), 16);
        final char[] characters = Character.toChars(point);
        return new String(characters);
    }

    public static String toJson(Object object, String indent, String... keys) {
        final StringWriter writer = new StringWriter();
        final Json json = new Json(writer);
        final Map<String, Object> map = new LinkedHashMap<>();
        json.write(object, object.getClass(), map);
        final List<String> list = List.of(keys);
        map.keySet().removeIf(key -> !list.contains(key));
        new Json(writer).write(map, indent, 0);
        return writer.toString();
    }

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

    @Contract(pure = true)
    public static Object parseJson(String string) {
        if (string == null || string.isBlank()) return null;
        try (final Json json = new Json(string.trim())) {
            json.mark(4);
            json.state = EXPECTING_VALUE;
            return json.readElement(json.readChar());
        }
    }

    @Contract(pure = true)
    public static Map<String, Object> toMap(Object object) {
        final Map<String, Object> map = new LinkedHashMap<>();
        new Json(new StringWriter()).write(object, object.getClass(), map);
        return map;
    }

    @Contract(pure = true)
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

    @Contract(pure = true)
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

    static Object read(char initial, Json json) {
        final StringBuilder builder = new StringBuilder();
        if (initial == '"') {
            final String value = new StringReader(json.reader, builder).read();
            if (value.contains("\\u")) {
                return CODE_POINT.matcher(value)
                    .replaceAll(result -> Json.codeToChar(result.group()));
            } else return value;
        } else if (initial == '{') {
            json.reset();
            try (JsonObject object = new JsonObject(json)) {
                return object.readMap();
            }
        } else if (initial == '[') {
            json.reset();
            try (JsonArray array = new JsonArray(json)) {
                return array.readList();
            }
        } else if (initial >= '0' && initial <= '9' || initial == '-') {
            json.reset();
            return new NumberReader(json.reader, builder).read();
        } else if (initial == 'f' || initial == 't') {
            json.reset();
            return new BooleanReader(json.reader, builder).read();
        } else if (initial == 'n') {
            json.reset();
            return new NullReader(json.reader, builder).read();
        } else throw new JsonException("Expected value start, found illegal '" + initial + "'.");
    }

    static String sanitise(String string) {
        final String part = string
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\f", "\\f");
        return Json.charToCode(part);
    }

    static void write(Object value, Json json) {
        if (value instanceof Double d) json.writeString(FORMAT.format(d));
        else if (value instanceof Boolean || value instanceof Number) json.writeString(value.toString());
        else if (value instanceof String string) json.writeString('"' + sanitise(string) + '"');
        else if (value == null) json.writeString("null");
        else if (value instanceof Map<?, ?> child) try (JsonObject object = new JsonObject(json)) {
            object.write(child);
        }
        else if (value instanceof List<?> child) try (JsonArray array = new JsonArray(json)) {
            array.write(child);
        }
        json.flush();
    }

    public static Json of(String string) {
        return new Json(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
    }

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
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
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
            this.deconstructArray(value, component.getComponentType(), list, false);
            return list;
        }
        final Map<String, Object> map = new HashMap<>();
        this.write(value, component, map);
        return map;
    }

    private void deconstructArray(Object array, Class<?> component, List<Object> list, boolean any) {
        if (component.isPrimitive()) {
            if (array instanceof int[] numbers) for (int number : numbers) list.add(number);
            else if (array instanceof long[] numbers) for (long number : numbers) list.add(number);
            else if (array instanceof double[] numbers) for (double number : numbers) list.add(number);
            else if (array instanceof float[] numbers) for (float number : numbers) list.add(number);
            else if (array instanceof boolean[] numbers) for (boolean number : numbers) list.add(number);
        } else {
            final Object[] objects = (Object[]) array;
            if (any) for (final Object object : objects) list.add(this.deconstructSimple(object, object.getClass()));
            else for (final Object object : objects) list.add(this.deconstructSimple(object, component));
        }
    }

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
                if (key.equals("__data")) continue;
                if (field.isAnnotationPresent(Present.class) && value instanceof Boolean boo && boo) map.put(key, null);
                else if (value == null) map.put(key, null);
                else if (value instanceof String) map.put(key, value);
                else if (value instanceof Number) map.put(key, value);
                else if (value instanceof Boolean) map.put(key, value);
                else if (value instanceof List<?>) map.put(key, value);
                else if (value instanceof Map<?, ?>) map.put(key, value);
                else if (expected.isArray()) {
                    final List<Object> child = new ArrayList<>();
                    map.put(key, child);
                    this.deconstructArray(value, expected.getComponentType(), child, field.isAnnotationPresent(Any.class));
                } else {
                    final Map<String, Object> child = new HashMap<>();
                    map.put(key, child);
                    final Class<?> target;
                    if (field.isAnnotationPresent(Any.class)) target = value.getClass();
                    else target = expected;
                    this.write(value, target, child);
                }
            } catch (Throwable ex) {
                throw new JsonException("Unable to write to object:", ex);
            }
        }
    }

    @SuppressWarnings("all")
    public void write(Object object, Class<?> type, String indent) {
        assert object != null : "Object was null.";
        assert object instanceof Class<?> ^ true : "Classes cannot be read from.";
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

    private Object convertSimple(Object data, Class<?> expected) {
        if (data instanceof List<?> list) return this.convertList(expected, list);
        else if (data instanceof Map<?, ?> map) return this.toObject(this.createObject(expected), expected, map);
        else return data;
    }

    Object convertList(Class<?> type, List<?> list) {
        final Class<?> component = type.getComponentType();
        final Object object = Array.newInstance(component, list.size());
        final Object[] objects = list.toArray();
        if (component.isPrimitive()) {
            if (component == boolean.class) for (int i = 0; i < objects.length; i++) {
                final Object value = objects[i];
                Array.setBoolean(object, i, (boolean) value);
            }
            else if (component == int.class) for (int i = 0; i < objects.length; i++) {
                final Object value = objects[i];
                Array.setInt(object, i, ((Number) value).intValue());
            }
            else if (component == long.class) for (int i = 0; i < objects.length; i++) {
                final Object value = objects[i];
                Array.setLong(object, i, ((Number) value).longValue());
            }
            else if (component == double.class) for (int i = 0; i < objects.length; i++) {
                final Object value = objects[i];
                Array.setDouble(object, i, ((Number) value).doubleValue());
            }
            else if (component == float.class) for (int i = 0; i < objects.length; i++) {
                final Object value = objects[i];
                Array.setFloat(object, i, ((Number) value).floatValue());
            }
        } else {
            final Object[] array = (Object[]) object;
            for (int i = 0; i < objects.length; i++) {
                final Object value = objects[i];
                array[i] = component.cast(this.convertSimple(value, component));
            }
        }
        return object;
    }

    @SuppressWarnings("all")
    protected <Type> Type toObject(Type object, Class<?> type, Map<?, ?> map) {
        assert object != null : "Object was null.";
        assert object instanceof Class<?> ^ true : "Classes cannot be written to.";
        final Set<Field> fields = new HashSet<>();
        fields.addAll(List.of(type.getDeclaredFields()));
        fields.addAll(List.of(type.getFields()));
        for (final Field field : fields) {
            final int modifiers = field.getModifiers();
            if ((modifiers & 0x00000002) != 0) continue;
            if ((modifiers & 0x00000008) != 0) continue;
            if ((modifiers & 0x00001000) != 0) continue;
            final String key;
            if (field.isAnnotationPresent(Name.class)) key = field.getAnnotation(Name.class).value();
            else key = field.getName();
            if (key.equals("__data")) try {
                if (!field.canAccess(object)) field.trySetAccessible();
                assert Map.class.isAssignableFrom(field.getType()) : "Dataset field must accept map.";
                final Map<String, Object> initial = (Map<String, Object>) field.get(object);
                if (initial != null) initial.putAll((Map<? extends String, ?>) map);
                else field.set(object, map);
                continue;
            } catch (IllegalAccessException ex) {
                throw new JsonException("Unable to store dataset object.", ex);
            }
            if ((modifiers & 0x00000080) != 0) continue;
            if (!map.containsKey(key)) continue;
            if (!field.canAccess(object)) field.trySetAccessible();
            final Object value = map.get(key);
            final Class<?> expected = field.getType();
            final Object lock;
            if ((modifiers & 0x00000040) != 0) lock = object;
            else lock = new Object();
            try {
                if (field.isAnnotationPresent(Present.class)) {
                    field.setBoolean(object, true);
                    continue;
                }
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
                        this.toObject(sub, expected, child);
                    } else if (expected.isArray() && value instanceof List<?> list) {
                        final Object array = this.convertList(expected, list);
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
        assert object != null : "Object was null.";
        assert object instanceof Class<?> ^ true : "Classes cannot be written to.";
        final Map<String, Object> map = this.toMap(new HashMap<>());
        return this.toObject(object, type, map);
    }

    public <Type> Type toObject(Type object) {
        assert object != null : "Object was null.";
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
        try (JsonArray source = new JsonArray(this)) {
            return source.toArray(array);
        }
    }

    @Deprecated(since = "1.2.0")
    public boolean willBeMap() {
        char c;
        while (true) {
            this.mark(4);
            c = this.readChar();
            if (c <= 32 || c == 160) continue;
            break;
        }
        this.reset();
        return c == '{';
    }

    @Deprecated(since = "1.2.0")
    public Object toSomething() {
        return this.readObject();
    }

    public Object readObject() {
        char c;
        while (true) {
            this.mark(4);
            c = this.readChar();
            if (c <= 32 || c == 160) continue;
            break;
        }
        this.reset();
        return Json.read(c, this);
    }

    public List<Object> toList() {
        return this.toList(new ArrayList<>());
    }

    public <Container extends List<Object>> Container toNewList(Supplier<Container> supplier) {
        final Container list = supplier.get();
        return this.toList(list);
    }

    public <Container extends List<Object>> Container toList(Container list) {
        try (JsonArray array = new JsonArray(this)) {
            return array.toList(list);
        }
    }

    public Map<String, Object> toMap() {
        return this.toMap(new HashMap<>());
    }

    public <Container extends Map<String, Object>> Container toNewMap(Supplier<Container> supplier) {
        final Container map = supplier.get();
        return this.toMap(map);
    }

    public <Container extends Map<String, Object>> Container toMap(final Container map) {
        try (JsonObject object = new JsonObject(this)) {
            return object.toMap(map);
        }
    }

    public Object readElement(char initial) {
        return Json.read(initial, this);
    }

    public void write(List<?> list) {
        this.write(list, null, 0);
    }

    public void write(List<?> list, String indent, int level) {
        if (writer == null) throw new JsonException("This Json controller has no writer.");
        this.setController(new WriteController(indent, level));
        try (JsonArray array = new JsonArray(this)) {
            array.write(list);
        }
    }

    public void write(Map<?, ?> map) {
        this.write(map, null, 0);
    }

    public void write(Map<?, ?> map, String indent, int level) {
        this.setController(new WriteController(indent, level));
        try (JsonObject object = new JsonObject(this)) {
            object.write(map);
        }
    }

    protected void flush() {
        if (writer != null) {
            try {
                this.writer.flush();
            } catch (IOException e) {
                throw new JsonException(e);
            }
        }
    }

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
        this.flush();
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

    public boolean isWritable() {
        return writer != null;
    }

    WriteController writeController() {
        return controller;
    }

    void setController(WriteController controller) {
        this.controller = controller;
    }

    private interface Reader {
        Object read();
    }

    protected record BooleanReader(java.io.Reader stream, StringBuilder builder)
        implements Json.Reader {
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
        implements Json.Reader {
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
        implements Json.Reader {

        @Override
        public String read() {
            try {
                boolean escape = false;
                while (true) {
                    final char c = (char) stream.read();
                    if (c == '\\' && !escape) escape = true;
                    else if (c == '"' && !escape) return builder.toString();
                    else {
                        if (escape) {
                            switch (c) {
                                case 'n' -> this.builder.append('\n');
                                case 'r' -> this.builder.append('\r');
                                case 't' -> this.builder.append('\t');
                                case 'f' -> this.builder.append('\f');
                                case 'b' -> this.builder.append('\b');
                                case 'u' -> this.builder.append("\\u");
                                default -> this.builder.append(c);
                            }
                            escape = false;
                        } else this.builder.append(c);
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
        implements Json.Reader {

        @Override
        public Number read() {
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

    static class WriteController {

        final String indent;
        int level;

        public WriteController(String indent) {
            this.indent = indent;
        }

        public WriteController(String indent, int level) {
            this(indent);
            this.level = level;
        }

        public boolean isPretty() {
            return indent != null;
        }

        public void enter() {
            this.level++;
        }

        public void exit() {
            this.level--;
        }

        public String getIndent() {
            if (!this.isPretty()) return "";
            return "\n" + String.valueOf(indent).repeat(Math.max(0, level));
        }

    }
}

abstract class JsonElement implements Closeable {

    protected transient final Json json;
    protected transient final Json.WriteController controller;

    public JsonElement(Json json) {
        this.json = json;
        this.controller = json.writeController();
    }

    public abstract void open();

    @Override
    public abstract void close();

    public boolean isWritable() {
        return json.isWritable();
    }

    public boolean isPretty() {
        return controller.isPretty();
    }

    protected void flush() {
        this.json.flush();
    }

    protected void mark(int chars) {
        if (!this.isWritable()) json.mark(chars);
    }

    protected void writeString(String value) {
        if (this.isWritable()) json.writeString(value);
    }

    protected void writeChar(char c) {
        if (this.isWritable()) json.writeChar(c);
    }

    protected char readChar() {
        if (!this.isWritable()) return json.readChar();
        else return 0;
    }

    protected void reset() {
        this.json.reset();
    }


}

