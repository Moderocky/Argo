package mx.kenzie.argo;

import mx.kenzie.grammar.Container;
import mx.kenzie.grammar.Null;
import mx.kenzie.grammar.Series;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.lang.constant.Constable;

import static mx.kenzie.argo.Json.*;

public class JsonReader implements mx.kenzie.grammar.io.Reader<Reader, IOException> {
    private static final char TERMINAL = 65535;

    static String codeToChar(Object object) {
        final String string = object.toString();
        final int point = Integer.parseInt(string.substring(2), 16);
        final char[] characters = Character.toChars(point);
        return new String(characters);
    }

    private boolean read(Reader reader, String string) throws IOException {
        reader.mark(string.length());
        for (char c : string.toCharArray()) {
            if (reader.read() != c) {
                reader.reset();
                return false;
            }
        }
        return true;
    }

    @Override
    public Constable readValue(Reader reader) throws IOException {
        return switch (this.lookahead(reader)) {
            case BEGIN_OBJECT -> this.readContainer(reader);
            case BEGIN_ARRAY -> this.readSeries(reader);
            case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> this.readNumber(reader);
            case '"' -> this.readString(reader);
            case 't' -> this.readConstant(reader, "true", true);
            case 'f' -> this.readConstant(reader, "false", false);
            case 'n' -> this.readConstant(reader, "null", Null.INSTANCE);
            case TERMINAL -> throw new JsonException("Reached end of JSON while expecting value");
            default -> throw new IllegalStateException("Unexpected value: " + (char) reader.read());
        };
    }

    private Constable readConstant(Reader reader, String expected, Constable value) throws IOException {
        if (this.read(reader, expected)) return value;
        throw new JsonException("Unknown value in the bagging area");
    }

    private boolean readUnicodeEscape(Reader reader, StringBuilder builder) throws IOException {
        StringBuilder inner = new StringBuilder("\\u");
        reader.mark(4);
        for (int i = 0; i < 4; i++) {
            int c = reader.read();
            if (Character.isAlphabetic(c) || Character.isDigit(c))
                inner.appendCodePoint(c);
            else {
                reader.reset();
                return false;
            }
        }
        try {
            builder.append(codeToChar(inner.toString()));
        } catch (IllegalArgumentException | NullPointerException _) {
            reader.reset();
            return false;
        }
        return true;
    }

    protected String readString(Reader reader) throws IOException {
        while (switch (reader.read()) {
            case '"' -> false;
            case -1 -> throw new EOFException("Reached end of JSON while looking for string");
            default -> true;
        }) ;

        StringBuilder builder = new StringBuilder();
        building:
        try {
            boolean escape = false;
            do {
                final int r = reader.read();
                if (r == -1)
                    throw new EOFException();
                final char c = (char) r;
                if (escape) {
                    if (c == 'u' && this.readUnicodeEscape(reader, builder)) /* do nothing*/ ;
                    else builder.append(switch (c) {
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 'f' -> '\f';
                        case 't' -> '\t';
                        case 'b' -> '\b';
                        case 'u' -> "\\u";
                        default -> c;
                    });
                    escape = false;
                } else {
                    switch (c) {
                        case '\\':
                            escape = true;
                            continue;
                        case '"':
                            break building;
                        default:
                            builder.append(c);
                    }
                }
            } while (true);
        } catch (EOFException ex) {
            throw new JsonException("Reached end of Json without closing quote '\"'");
        } catch (IOException ex) {
            throw new JsonException(ex);
        }

        return builder.toString();
    }

    protected Constable readNumber(Reader reader) throws IOException {
        try {
            StringBuilder builder = new StringBuilder();
            boolean first = true, decimal = false, exp = false;
            while (true) {
                reader.mark(1);
                final char c = (char) reader.read();
                if (first) {
                    first = false;
                    if (c == TERMINAL) throw new EOFException();
                    else if ((c >= '0' && c <= '9') || c == '-') ;
                    else throw new JsonException("Found non-number character '" + c + "' at start of number");
                } else if (c == '.' && !decimal) {
                    decimal = true;
                } else if ((c == 'e' || c == 'E') && !exp) {
                    exp = true;
                } else if (c < '0' || c > '9') {
                    reader.reset();
                    if (decimal || exp) return Double.valueOf(builder.toString());
                    final long value = Long.parseLong(builder.toString());
                    if (value == (int) value) return (int) value;
                    else return value;
                }
                builder.append(c);
            }
        } catch (EOFException ex) {
            throw new JsonException("Reached end of JSON while expecting value");
        } catch (IOException ex) {
            throw new JsonException(ex);
        }
    }

    @Override
    public void look(Reader reader) throws IOException {
        this.lookahead(reader);
    }

    public char lookahead(Reader reader) throws IOException {
        int read;
        do {
            reader.mark(1);
            read = reader.read();
            if (read == -1) return TERMINAL;
        } while (read <= 32 || read == 160); // whitespace
        reader.reset();
        return (char) read;
    }

    @Override
    public Series readSeries(Reader reader) throws IOException {
        Series series = Series.empty();
        this.readSeriesOpen(reader);
        do {
            char lookahead = this.lookahead(reader);
            if (lookahead == END_ARRAY) break;
            Constable value = this.readSeriesValue(reader);
            series.add(value);
        } while (this.readAnd(reader));
        this.readSeriesClose(reader);
        return series;
    }

    @Override
    public void readSeriesOpen(Reader reader) throws IOException {
        char lookahead = this.lookahead(reader);
        int read = reader.read();
        assert lookahead == read;
        switch (read) {
            case BEGIN_ARRAY:
                return;
            case -1:
                throw new JsonException("Reached end of stream while looking for array");
            default:
                throw new JsonException("Expected an array opener '{' but found '" + read + "'");
        }
    }

    @Override
    public Constable readSeriesValue(Reader reader) throws IOException {
        return this.readValue(reader);
    }

    private boolean readAnd(Reader reader) throws IOException {
        char lookahead = this.lookahead(reader);
        if (lookahead == VALUE_SEPARATOR) {
            int _ = reader.read();
            return true;
        }
        return false;
    }

    @Override
    public void readSeriesAnd(Reader reader) throws IOException {
        char lookahead = this.lookahead(reader);
        int read = reader.read();
        assert lookahead == read;
        throw new JsonException("Expected a value separator ',' but found '" + read + "'");
    }

    @Override
    public void readSeriesClose(Reader reader) throws IOException {
        char lookahead = this.lookahead(reader);
        int read = reader.read();
        assert lookahead == read;
        switch (read) {
            case END_ARRAY:
                return;
            case -1:
                throw new JsonException("Reached end of stream while closing array");
            default:
                throw new JsonException("Expected an array close ']' but found '" + read + "'");
        }
    }

    @Override
    public Container readContainer(Reader reader) throws IOException {
        Container container = Container.empty();
        this.readContainerOpen(reader, container);
        do {
            String key = this.readContainerKey(reader);
            if (key == null) break;
            this.readContainerSeparator(reader);
            Constable value = this.readContainerValue(reader);
            container.put(key, value);
        } while (this.readAnd(reader));
        this.readContainerClose(reader, container);
        return container;
    }

    @Override
    public void readContainerOpen(Reader reader, Container container) throws IOException {
        char lookahead = this.lookahead(reader);
        int read = reader.read();
        assert lookahead == read;
        switch (read) {
            case BEGIN_OBJECT:
                return;
            case -1:
                throw new JsonException("Reached end of stream while looking for object");
            default:
                throw new JsonException("Expected an object opener '{' but found '" + read + "'");
        }
    }

    @Override
    public String readContainerKey(Reader reader) throws IOException {
        if (this.lookahead(reader) == '"') return this.readString(reader);
        return null;
    }

    @Override
    public void readContainerSeparator(Reader reader) throws IOException {
        char lookahead = this.lookahead(reader);
        int read = reader.read();
        assert lookahead == read;
        if (lookahead == NAME_SEPARATOR) return;
        throw new JsonException("Expected a name separator ':' but found '" + read + "'");
    }

    @Override
    public void readContainerAnd(Reader reader, Container container) throws IOException {
        this.readSeriesAnd(reader); // it's the same!
    }

    @Override
    public void readContainerClose(Reader reader, Container container) throws IOException {
        char lookahead = this.lookahead(reader);
        int read = reader.read();
        assert lookahead == read;
        switch (read) {
            case END_OBJECT:
                return;
            case -1:
                throw new JsonException("Reached end of stream while closing object");
            default:
                throw new JsonException("Expected an object close '}' but found '" + read + "'");
        }
    }
}
