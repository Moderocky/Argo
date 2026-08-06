package mx.kenzie.argo;

import mx.kenzie.grail.function.Function;
import mx.kenzie.grail.function.Supplier;
import mx.kenzie.grammar.*;
import mx.kenzie.grammar.unwrap.Unwrapper;
import org.valross.constantine.Array;

import java.io.*;
import java.lang.constant.Constable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class Json extends Grammar {

    public static final Null NULL = Null.INSTANCE;
    protected static final char BEGIN_ARRAY = '[', BEGIN_OBJECT = '{', END_ARRAY = ']', END_OBJECT = '}', NAME_SEPARATOR = ':', VALUE_SEPARATOR = ',', SPACE = ' ', QUOTE = '"';

    protected static final String STANDARD_INDENT_UNIT = "\t";
    protected static final Charset STANDARD_CHARSET = StandardCharsets.UTF_8;

    @SuppressWarnings({"StaticInitializerReferencesSubClass", "RedundantSuppression"})
    private static final Supplier<Json, RuntimeException> NO_TYPES = Supplier.memoise(Frozen::new), ALL_OBJECTS = Supplier.memoise(() -> new Frozen(new Unsafe()));


    public Json(Grammar grammar) {
        super(grammar);
    }

    public Json() {
        super();
    }

    public static Json allTypes() {
        return ALL_OBJECTS.get();
    }

    public static Json simple() {
        return NO_TYPES.get();
    }

    public static Json serialising(Class<?>... classes) {
        Json json = new Json();
        for (Class<?> type : classes)
            json.registerUncheckedObject(type);
        return json;
    }

    public static String toString(Constable value, boolean prettyPrint) {
        StringBuilder builder = new StringBuilder();
        Write<?> writer = simple().writer(builder);
        if (prettyPrint) writer.indent(STANDARD_INDENT_UNIT);
        writer.writeValue(value);
        return builder.toString();
    }

    public static String toString(Constable value) {
        return toString(value, false);
    }

    public static String toString(Object unchecked, boolean prettyPrint) {
        StringBuilder builder = new StringBuilder();
        Write<?> writer = allTypes().writer(builder);
        if (prettyPrint) writer.indent(STANDARD_INDENT_UNIT);
        writer.writeObject(unchecked);
        return builder.toString();
    }

    public static String toString(Object unchecked) {
        return toString(unchecked, false);
    }

    public static String toString(Object[] uncheckedArray, boolean prettyPrint) {
        return toString((Object) uncheckedArray, prettyPrint);
    }

    public static String toString(Object... uncheckedArray) {
        return toString((Object) uncheckedArray, false);
    }

    public static void printOut(Constable value, PrintStream stream, boolean prettyPrint) {
        Write<?> writer = simple().writer(stream);
        if (prettyPrint) writer.indent(STANDARD_INDENT_UNIT);
        writer.writeValue(value);
    }

    public static void printOut(Constable value, PrintStream stream) {
        printOut(value, stream, false);
    }

    public static void printOut(Object unchecked, PrintStream stream, boolean prettyPrint) {
        Write<?> writer = allTypes().writer(stream);
        if (prettyPrint) writer.indent(STANDARD_INDENT_UNIT);
        writer.writeObject(unchecked);
    }

    public static void printOut(Object unchecked, PrintStream stream) {
        printOut(unchecked, stream, false);
    }

    public static <Data extends Constable> Data fromString(String source) throws ClassCastException {
        //noinspection unchecked
        return (Data) simple().reader(source).readValue();
    }

    public static <Value> Value fromString(String source, Class<Value> valueType) {
        return allTypes().reader(source).readObject(valueType);
    }

    public Read reader(Reader reader) {
        return new Read(reader);
    }

    public Read reader(InputStream stream, Charset charset) {
        return new Read(new BufferedReader(new InputStreamReader(stream, charset)));
    }

    public Read reader(InputStream stream) {
        return this.reader(stream, STANDARD_CHARSET);
    }

    public Read reader(File file, Charset charset) throws FileNotFoundException {
        return this.reader(new FileInputStream(file), charset);
    }

    public Read reader(File file) throws FileNotFoundException {
        return this.reader(file, STANDARD_CHARSET);
    }

    public Read reader(String source) {
        return new Read(new StringReader(source));
    }

    public Write<StringBuilder> writer(StringBuilder builder) {
        return new Write<>(new JsonStringBuilderWriter(), builder);
    }

    public Write<PrintStream> writer(PrintStream stream) {
        return new Write<>(new JsonPrintStreamWriter(), stream);
    }

    public <Output extends Writer> Write<Output> writer(Output writer) {
        if (writer instanceof PrintWriter) { // noinspection rawtypes, unchecked
            return new Write(new JsonPrintWriter(), writer);
        }
        return new Write<>(new JsonAnyWriter<>(), writer);
    }

    public Write<OutputStream> writer(OutputStream stream, Charset charset) {
        return new Write<>(new JsonOutputStreamWriter(charset), stream);
    }

    public Write<OutputStream> writer(OutputStream stream) {
        return this.writer(stream, STANDARD_CHARSET);
    }

    public Write<OutputStream> writer(File file, Charset charset) throws FileNotFoundException {
        return this.writer(new FileOutputStream(file), charset);
    }

    public Write<OutputStream> writer(File file) throws FileNotFoundException {
        return this.writer(file, STANDARD_CHARSET);
    }

    protected static class Unsafe extends Grammar.Unsafe {
    }

    protected static class Frozen extends Json {

        final boolean frozen;

        {
            frozen = true;
        }

        public Frozen(Grammar grammar) {
            super(grammar);
        }

        public Frozen() {
            super();
        }

        @Override
        public <Type> void register(Class<Type> type, Unwrapper<Type> unwrapper) {
            if (frozen) throw new UnsupportedOperationException();
            else super.register(type, unwrapper);
        }

        @Override
        public <Type extends Marshalled.Unmarshalled> void registerConstructor(Class<Type> type, Supplier<Type, GrammarException> noArgsConstructor) {
            if (frozen) throw new UnsupportedOperationException();
            else super.registerConstructor(type, noArgsConstructor);
        }

        @Override
        public <Type> void registerMarshallingStrategy(Predicate<Object> predicate, Function<Type, Constable, GrammarException> strategy) {
            if (frozen) throw new UnsupportedOperationException();
            else super.registerMarshallingStrategy(predicate, strategy);
        }

        @Override
        public <Type> void registerUnmarshallingStrategy(Class<Type> type, Function<Constable, Type, GrammarException> strategy) {
            if (frozen) throw new UnsupportedOperationException();
            else super.registerUnmarshallingStrategy(type, strategy);
        }

        @Override
        public <Type> void registerFallbackMarshallingStrategy(Predicate<Object> predicate, Function<Type, Constable, GrammarException> strategy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <Type> void registerMarshallingStrategy(Class<Type> type, Function<Type, Constable, GrammarException> strategy) {
            if (frozen) throw new UnsupportedOperationException();
            else super.registerMarshallingStrategy(type, strategy);
        }
    }

    public class Write<To> implements AutoCloseable {

        //<editor-fold desc="Writing methods" defaultstate="collapsed">
        protected transient final JsonWriter<To> writer;
        protected transient final To hook;

        public Write(JsonWriter<To> writer, To hook) {
            this.writer = writer;
            this.hook = hook;
        }

        @SuppressWarnings("UnusedReturnValue")
        public Write<To> pretty() {
            return this.indent(STANDARD_INDENT_UNIT);
        }

        @SuppressWarnings("UnusedReturnValue")
        public Write<To> indent(String indentUnit) {
            this.writer.indent = indentUnit;
            return this;
        }

        public void writeValue(Constable value) {
            this.writer.writeValue(hook, value);
        }

        public void writeObject(Container map) {
            this.writer.writeContainer(hook, map);
        }

        public void writeObject(Map<? super String, ? extends Constable> map) {
            //noinspection unchecked,rawtypes
            this.writer.writeContainer(hook, Container.of((Map) map));
        }

        public void writeObject(Object object) {
            if (object instanceof Map<?, ?>) //noinspection unchecked
                this.writeObject((Map<? super String, ? extends Constable>) object);
            else
                this.writeValue(Json.this.marshal(object));
        }

        public void writeObject(Object object, Class<?> type) {
            this.writeValue(Json.this.marshal(type, object));
        }

        public void writeArray(Array value) {
            this.writer.writeSeries(hook, value);
        }

        public void writeArray(Series value) {
            this.writer.writeSeries(hook, value);
        }

        public void writeArray(int[] values) {
            this.writeArray(Series.of(values));
        }

        public void writeArray(long[] values) {
            this.writeArray(Series.of(values));
        }

        public void writeArray(float[] values) {
            this.writeArray(Series.of(values));
        }

        public void writeArray(double[] values) {
            this.writeArray(Series.of(values));
        }

        public void writeArray(boolean[] values) {
            this.writeArray(Series.of(values));
        }

        public void writeArray(Constable... values) {
            this.writeArray(new Array(values));
        }

        public void writeObjectArray(Collection<?> values) {
            Constable[] array = values.stream().map(Json.this::marshal).toArray(Constable[]::new);
            this.writeArray(array);
        }

        public void writeObjectArray(Object[] values) {
            Constable[] array = Arrays.stream(values).map(Json.this::marshal).toArray(Constable[]::new);
            this.writeArray(array);
        }

        public JsonObject object() {
            return new JsonObject();
        }

        public JsonArray array() {
            return new JsonArray();
        }

        protected To getHook() {
            return hook;
        }

        protected JsonWriter<To> getWriter() {
            return writer;
        }

        @Override
        public void close() {
            if (hook instanceof AutoCloseable closer) {
                try {
                    closer.close();
                } catch (Exception e) {
                    throw new JsonException(e);
                }
            }
        }

        public class JsonArray implements AutoCloseable {
            private static final int START = 0, AFTER = 1;
            int state = START;

            public JsonArray() {
                writer.writeSeriesOpen(hook);
            }

            public void write(Series series) {
                series.forEach(this::writeValue);
            }

            public void writeArray(Constable... objects) {
                for (Constable object : objects) {
                    this.writeValue(object);
                }
            }

            public void writeArray(int[] primitives) {
                for (Constable object : primitives) {
                    this.writeValue(object);
                }
            }

            public void writeArray(float[] primitives) {
                for (Constable object : primitives) {
                    this.writeValue(object);
                }
            }

            public void writeArray(double[] primitives) {
                for (Constable object : primitives) {
                    this.writeValue(object);
                }
            }

            public void writeArray(long[] primitives) {
                for (Constable object : primitives) {
                    this.writeValue(object);
                }
            }

            public void writeObjects(List<?> list) {
                list.forEach(this::writeObject);
            }

            public void writeObject(Object unchecked) {
                Constable marshal = Json.this.marshal(unchecked);
                this.writeValue(marshal);
            }

            public void writeValue(Constable value) {
                if (state == AFTER) {
                    writer.writeSeriesAnd(hook, true);
                }
                writer.writeContainerValue(hook, value);
                state = AFTER;
            }

            public void writeValue(Object unchecked) {
                this.writeValue(Json.this.marshal(unchecked));
            }

            public JsonObject object() {
                if (state == AFTER) {
                    writer.writeSeriesAnd(hook, true);
                }
                this.state = AFTER;
                return new JsonObject();
            }

            public JsonArray array() {
                return new JsonArray();
            }

            @Override
            public void close() {
                writer.writeSeriesAnd(hook, false);
                writer.writeContainerClose(hook);
            }
        }

        public class JsonObject implements AutoCloseable {
            private static final int KEY = 0, VALUE = 1, AFTER = 2;
            int state = KEY;

            public JsonObject() {
                writer.writeContainerOpen(hook);
            }

            public void write(Container container) {
                container.forEach(this::write);
            }

            public void write(Map<String, Constable> map) {
                map.forEach(this::write);
            }

            public void write(String key, Constable value) {
                if (state == AFTER) {
                    writer.writeContainerAnd(hook, true);
                    state = KEY;
                }
                if (state != KEY) throw new IllegalStateException("Key/value pair cannot be written here");
                writer.writeContainerPair(hook, key, value);
                state = AFTER;
            }

            public void write(String key, Object unchecked) {
                this.write(key, Json.this.marshal(unchecked));
            }

            public void writeObject(Object unchecked) {
                Constable marshal = Json.this.marshal(unchecked);
                if (marshal instanceof Container container) this.write(container);
                else throw new IllegalArgumentException("Object '" + unchecked + "' is not a key/value container");
            }

            public void writeKey(String key) {
                if (state == AFTER) {
                    writer.writeContainerAnd(hook, true);
                    state = KEY;
                }
                if (state != KEY) throw new IllegalStateException("Key cannot be written here");
                writer.writeContainerKey(hook, key);
                writer.writeContainerSeparator(hook);
                state = VALUE;
            }

            public void writeValue(Constable value) {
                if (state != VALUE) throw new IllegalStateException("Key cannot be written here");
                writer.writeContainerValue(hook, value);
                state = AFTER;
            }

            public void writeValue(Object unchecked) {
                this.writeValue(Json.this.marshal(unchecked));
            }

            public JsonObject object() {
                if (state != VALUE) throw new IllegalStateException("Inner object must follow a key");
                this.state = AFTER;
                return new JsonObject();
            }

            public JsonArray array() {
                if (state != VALUE) throw new IllegalStateException("Inner array must follow a key");
                this.state = AFTER;
                return new JsonArray();
            }

            @Override
            public void close() {
                writer.writeContainerAnd(hook, false);
                writer.writeContainerClose(hook);
            }
        }
        //</editor-fold>

    }

    public class Read implements AutoCloseable {
        //<editor-fold desc="Reading methods" defaultstate="collapsed">
        protected transient final JsonReader reader;
        protected transient final Reader hook;

        public Read(Reader reader) {
            this.reader = new JsonReader();
            if (reader.markSupported()) this.hook = reader;
            else this.hook = new BufferedReader(reader);
        }

        public Constable readValue() {
            try {
                return reader.readValue(hook);
            } catch (IOException e) {
                throw new JsonException(e);
            }
        }

        public Series readSeries() {
            try {
                return reader.readSeries(hook);
            } catch (IOException e) {
                throw new JsonException(e);
            }
        }

        public Container readContainer() {
            try {
                return reader.readContainer(hook);
            } catch (IOException e) {
                throw new JsonException(e);
            }
        }

        public <Value> Value readObject(Class<Value> type) {
            return Json.this.unmarshal(type, this.readValue());
        }

        public <ArrayType> ArrayType readArray(Class<ArrayType> arrayType) {
            if (!arrayType.isArray()) throw new IllegalArgumentException(arrayType.getName() + " is not an array type");
            Class<?> componentType = arrayType.getComponentType();
            return Json.this.unmarshalArray(arrayType, componentType, this.readSeries());
        }

        public <Value> Value[] readArrayOf(Class<Value> componentType) {
            //noinspection unchecked
            return Json.this.unmarshalArray((Class<Value[]>) componentType.arrayType(), componentType, this.readSeries());
        }

        @Override
        public void close() {
            if (hook instanceof AutoCloseable closer) {
                try {
                    closer.close();
                } catch (Exception e) {
                    throw new JsonException(e);
                }
            }
        }

        protected Reader getHook() {
            return hook;
        }
        //</editor-fold>
    }


}
