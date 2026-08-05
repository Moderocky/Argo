package mx.kenzie.argo;


import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.constant.Constable;
import java.nio.charset.StandardCharsets;

public class JsonOutputStreamWriterTest {


    @Test
    public void ascii() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.Write<OutputStream> writer = Json.simple().writer(baos, StandardCharsets.US_ASCII);
        String value = "AHSAEGUHFJDVLKjsvdfdikfhjvfj oijewuh9289437845981909.,,/[]!";
        writer.writeValue(value);
        writer.close();
        String string = baos.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals('"' + value + '"', string);

    }


    @Test
    public void asciiAll() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.Write<OutputStream> writer = Json.simple().writer(baos, StandardCharsets.US_ASCII);
        StringBuilder builder = new StringBuilder();
        for (int i = 32; i < 256; i++) {
            char c = (char) i;
            builder.append(c);
        }
        String value = builder.toString();
        writer.writeValue(value);
        writer.close();
        String string = baos.toString(StandardCharsets.US_ASCII);
        Constable constable = Json.fromString(string);
        Assert.assertEquals(value, constable.toString());

    }


}