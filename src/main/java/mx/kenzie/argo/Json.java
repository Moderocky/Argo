package mx.kenzie.argo;

import mx.kenzie.argo.error.JsonException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static mx.kenzie.argo.Json.*;

public class Json implements Closeable, AutoCloseable {
    
    static final byte
        START = 0,
        EXPECTING_KEY = 1,
        IN_KEY = 2,
        AFTER_KEY = 3,
        EXPECTING_VALUE = 4,
        EXPECTING_END = 5,
        END = -1;
    private static final byte
        NULL = 0,
        STRING = 1,
        NUMBER = 2,
        BOOLEAN = 3,
        OBJECT = 4,
        ARRAY = 5;
    
    protected transient java.io.Reader reader;
    protected transient Writer writer;
    protected int state = START;
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
    
    public List<Object> toList() {
        return this.toNewList(ArrayList::new);
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
        return this.toNewMap(HashMap::new);
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
                        final Map<String, Object> value = json.toMap();
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
        final boolean pretty = indent != null && !indent.isEmpty();
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
            this.reader.close();
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
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
            return json.toMap();
        }
    }
    
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
    
}
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
        final boolean pretty = indent != null && !indent.isEmpty();
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
                        final Map<String, Object> value = json.toMap();
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
    
    protected void mark(int chars) {
        try {
            this.reader.mark(chars);
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
    
}
