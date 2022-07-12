package mx.kenzie.argo.meta;

import java.util.HashMap;
import java.util.Map;

public class JsonException extends RuntimeException {
    
    protected Map<String, Object> data;
    
    public JsonException() {
        super();
    }
    
    public JsonException(String message) {
        super(message);
    }
    
    public JsonException(String message, Map<String, Object> map) {
        this(message);
        this.data = map;
    }
    
    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public JsonException(Throwable cause) {
        super(cause);
    }
    
    protected JsonException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
    public Map<String, Object> getData() {
        return data != null ? data : new HashMap<>();
    }
}
