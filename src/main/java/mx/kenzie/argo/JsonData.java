package mx.kenzie.argo;

import mx.kenzie.argo.meta.JsonException;

public interface JsonData {

    default void read(Json json) throws JsonException {
        try (JsonObject object = new JsonObject(json)) {
            object.toObject(this);
        }
    }

    default void write(Json json) throws JsonException {
        try (JsonObject object = new JsonObject(json)) {
            object.writeObject(this);
        }
    }

}
