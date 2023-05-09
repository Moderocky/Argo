package mx.kenzie.argo;

import mx.kenzie.argo.meta.JsonException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"SameParameterValue", "null"})
public class JsonArray extends JsonElement {

    private static final Object END = new Object();
    protected boolean first = true;

    public JsonArray(Json json) {
        super(json);
        this.open();
    }

    public void write(List<?> list) {
        if (!this.isWritable()) throw new JsonException("This Json controller has no writer.");
        for (Object value : list) this.writeValue(value);
    }

    public void writeValue(Object value) {
        if (!first) this.writeString(", ");
        this.writeString(controller.getIndent());
        Json.write(value, json);
        this.first = false;
    }

    public Object readValue() {
        char c;
        do {
            this.mark(4);
            c = this.readChar();
        } while (c == ',' || Character.isWhitespace(c));
        if (c == ']') {
            this.reset();
            return END;
        }
        return Json.read(c, json);
    }

    @Override
    public void open() {
        if (this.isWritable()) {
            this.json.writeChar('[');
            this.controller.enter();
        } else while (true) switch (this.readChar()) {
            case '[':
                return;
            case 65535:
                throw new JsonException("Reached end of stream while opening array.");
        }
    }

    @Override
    public void close() {
        if (this.isWritable()) {
            this.controller.exit();
            this.writeString(controller.getIndent());
            this.json.writeChar(']');
            this.flush();
        } else while (true) switch (this.readChar()) {
            case ']':
                return;
            case 65535:
                throw new JsonException("Reached end of stream while closing array.");
        }
    }

    @Deprecated(since = "1.2.0")
    public void write(List<?> list, String indent, int level) {
        this.write(list);
    }

    public List<Object> readList() {
        return this.toList(new ArrayList<>());
    }

    public <Container extends List<Object>> Container toList(Container list) {
        if (json.reader == null) throw new JsonException("This Json controller has no reader.");
        Object value;
        while ((value = this.readValue()) != END) list.add(value);
        return list;
    }

    @SuppressWarnings("unchecked")
    public <Component> Component[] toArray(Class<Component> type) {
        return (Component[]) this.toArray(Array.newInstance(type, 0));
    }

    @SuppressWarnings({"all"})
    public <Container> Container toArray(Container array) {
        if (array == null) throw new JsonException("Provided array was null.");
        final Class<?> type = array.getClass();
        if (!type.isArray()) throw new JsonException("Provided object was not an array.");
        final Class<?> component = type.getComponentType();
        final List<?> list = this.readList();
        final Container container;
        if (Array.getLength(array) < 1) container = (Container) Array.newInstance(component, list.size());
        else container = array;
        final Object source = json.convertList(container.getClass(), list);
        final int a = Array.getLength(container), b = Array.getLength(source);
        System.arraycopy(source, 0, container, 0, Math.min(a, b));
        return container;
    }

}
