package mx.kenzie.argo;

import mx.kenzie.argo.meta.JsonException;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonObject extends JsonElement {

    protected boolean first = true;

    public JsonObject(Json json) {
        super(json);
        this.open();
    }

    public void write(Map<?, ?> map) {
        if (!this.isWritable()) throw new JsonException("This Json controller has no writer.");
        for (Map.Entry<?, ?> entry : map.entrySet()) this.write(String.valueOf(entry.getKey()), entry.getValue());
    }

    public void writeKey(String key) {
        if (!first) this.writeString(", ");
        this.writeString(controller.getIndent());
        assert key != null;
        this.writeChar('"');
        this.writeString(key);
        this.writeString("\": ");
        this.first = false;
    }

    public void writeValue(Object value) {
        this.first = false;
        Json.write(value, json);
    }

    public void write(String key, Object value) {
        this.writeKey(key);
        this.writeValue(value);
    }

    public <Type> void writeObject(Type object) {
        this.writeObject(object, object.getClass());
    }

    public <Type> void writeObject(Object object, Class<Type> type) {
        final Map<String, Object> map = new LinkedHashMap<>();
        this.json.marshal(object, type, map);
        this.write(map);
    }

    public String readKey() {
        char c;
        do {
            this.mark(4);
            c = this.readChar();
        } while (c == ',' || Character.isWhitespace(c));
        if (c == '"') {
            final StringBuilder builder = new StringBuilder();
            return new Json.StringReader(json.reader, builder).read();
        } else if (c == '}') {
            this.reset();
            return null;
        } else throw new JsonException("Expected key start '\"', found '" + c + "'.");
    }

    protected void skipSeparator() {
        while (this.readChar() != ':') ;
    }

    public Object readValue() {
        this.skipSeparator();
        char c;
        do {
            this.mark(2);
            c = this.readChar();
        } while (Character.isWhitespace(c));
        return Json.read(c, json);
    }

    public Map<String, Object> readMap() {
        return this.toMap(new LinkedHashMap<>());
    }

    public <Container extends Map<String, Object>> Container toMap(Container map) {
        String key;
        while ((key = this.readKey()) != null) map.put(key, this.readValue());
        return map;
    }

    public <Type> Type toObject(Type object) {
        return json.unmarshal(object, object.getClass(), this.readMap());
    }

    public <Type> Type toObject(Class<Type> type) {
        return json.unmarshal(type, this.readMap());
    }

    @Override
    public void open() {
        if (this.isWritable()) {
            this.json.writeChar('{');
            this.controller.enter();
        } else while (true) switch (this.readChar()) {
            case '{':
                return;
            case 65535:
                throw new JsonException("Reached end of stream while opening object.");
        }
    }

    @Override
    public void close() {
        if (this.isWritable()) {
            this.controller.exit();
            this.writeString(controller.getIndent());
            this.json.writeChar('}');
            this.flush();
        } else while (true) switch (this.readChar()) {
            case '}':
                return;
            case 65535:
                throw new JsonException("Reached end of stream while closing object.");
        }
    }

}
