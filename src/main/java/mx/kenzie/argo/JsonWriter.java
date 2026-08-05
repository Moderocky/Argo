package mx.kenzie.argo;

import mx.kenzie.grammar.Container;
import mx.kenzie.grammar.Null;
import mx.kenzie.grammar.Series;
import mx.kenzie.grammar.io.Writer;
import org.valross.constantine.Array;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.constant.Constable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static mx.kenzie.argo.Json.*;

public abstract class JsonWriter<Output> implements Writer<Output, JsonException> {
    protected int level = 0;
    protected String indent;
    protected String lineSeparator = System.lineSeparator();

    protected static String stringify(Object value) {
        assert value != null;
        return sanitise(value.toString());
    }

    static String sanitise(String string) {
        final String part = string
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
        return charToCode(part);
    }

    static String charToCode(Object object) {
        final StringBuilder builder = new StringBuilder();
        for (final char c : object.toString().toCharArray()) {
            if (c >= 128) builder.append("\\u").append(String.format("%04X", (int) c));
            else builder.append(c);
        }
        return builder.toString();
    }

    protected abstract void writeNewLine(Output output) throws JsonException;

}

abstract class SimpleWriter<Output> extends JsonWriter<Output> {
    protected DecimalFormat format = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    {
        format.setMaximumFractionDigits(340);
    }

    @Override
    public void writeValue(Output output, Constable value) throws JsonException {
        switch (value) {
            case Container container:
                this.writeContainer(output, container);
                break;
            case Array array:
                this.writeSeries(output, array);
                break;
            case Series array:
                this.writeSeries(output, array);
                break;
            case String _, Character _, CharSequence _:
                this.writeString(output, value);
                break;
            case Double d:
                this.write(output, format.format(d));
                break;
            case Number _, Boolean _, Null _:
                this.write(output, value.toString());
                break;
            case null:
                this.write(output, Null.INSTANCE.toString());
                break;
            default:
                throw new JsonException("Unsupported constant value: " + value + " (" + value.getClass() + ")");
        }
    }

    protected void writeString(Output output, Object string) throws JsonException {
        this.write(output, QUOTE);
        this.write(output, stringify(string));
        this.write(output, QUOTE);
    }

    @Override
    public void writeSeriesOpen(Output output) throws JsonException {
        this.write(output, BEGIN_ARRAY);
        ++level;
    }

    @Override
    public void writeSeriesValue(Output output, Constable value) throws JsonException {
        this.writeNewLine(output);
        super.writeSeriesValue(output, value);
    }

    @Override
    public void writeSeriesAnd(Output output, boolean hasFollowing) throws JsonException {
        if (hasFollowing) {
            this.write(output, VALUE_SEPARATOR);
            if (indent == null) this.write(output, SPACE);
        } else {
            --level;
            this.writeNewLine(output);
        }
    }

    @Override
    public void writeSeriesClose(Output output) throws JsonException {
        this.write(output, END_ARRAY);
    }

    @Override
    public void writeContainerOpen(Output output) throws JsonException {
        this.write(output, BEGIN_OBJECT);
        ++level;
    }

    @Override
    public void writeContainerKey(Output output, String key) throws JsonException {
        this.writeNewLine(output);
        this.writeString(output, key);
    }

    @Override
    public void writeContainerSeparator(Output output) throws JsonException {
        this.write(output, NAME_SEPARATOR);
        this.write(output, SPACE);
    }

    @Override
    public void writeContainerAnd(Output output, boolean hasFollowing) throws JsonException {
        if (hasFollowing) {
            this.write(output, VALUE_SEPARATOR);
            if (indent == null) this.write(output, SPACE);
        } else {
            --level;
            this.writeNewLine(output);
        }
    }

    @Override
    public void writeContainerClose(Output output) throws JsonException {
        this.write(output, END_OBJECT);
    }

    @Override
    protected void writeNewLine(Output output) throws JsonException {
        if (indent == null) return;
        this.write(output, lineSeparator);
        for (int i = 0; i < level; i++)
            this.write(output, indent);
    }

    abstract void write(Output output, String string) throws JsonException;

    abstract void write(Output output, char character) throws JsonException;

}

class JsonStringBuilderWriter extends SimpleWriter<StringBuilder> {

    @Override
    void write(StringBuilder builder, String string) throws JsonException {
        builder.append(string);
    }

    @Override
    void write(StringBuilder builder, char character) throws JsonException {
        builder.append(character);
    }
}

class JsonPrintWriter extends JsonAnyWriter<PrintWriter> {
    @Override
    void write(PrintWriter writer, String string) {
        writer.write(string);
    }

    @Override
    void write(PrintWriter writer, char character) {
        writer.write(character);
    }
}

class JsonAnyWriter<Output extends java.io.Writer> extends SimpleWriter<Output> {
    @Override
    void write(Output writer, String string) {
        try {
            writer.write(string);
            writer.flush();
        } catch (IOException e) {
            throw new JsonException(e);
        }
    }

    @Override
    void write(Output writer, char character) {
        try {
            writer.write(character);
            writer.flush();
        } catch (IOException e) {
            throw new JsonException(e);
        }
    }
}

class JsonPrintStreamWriter extends SimpleWriter<PrintStream> {
    @Override
    void write(PrintStream writer, String string) {
        writer.print(string);
    }

    @Override
    void write(PrintStream writer, char character) {
        writer.print(character);
    }
}

class JsonOutputStreamWriter extends SimpleWriter<OutputStream> {

    protected final Charset charset;

    JsonOutputStreamWriter(Charset charset) {
        this.charset = charset;
    }

    @Override
    void write(OutputStream writer, String string) {
        try {
            writer.write(string.getBytes(charset));
        } catch (IOException error) {
            throw new JsonException(error);
        }
    }

    @Override
    void write(OutputStream writer, char character) {
        try {
            // This is *theoretically* okay because UTF-8 kept legacy ascii material in its first byte
            // charset unpacking is extremely costly for what it does and we can shortcut this
            if (charset == StandardCharsets.US_ASCII)
                writer.write((byte) character);
            else
                writer.write(Character.toString(character).getBytes(charset));
        } catch (IOException error) {
            throw new JsonException(error);
        }
    }
}
