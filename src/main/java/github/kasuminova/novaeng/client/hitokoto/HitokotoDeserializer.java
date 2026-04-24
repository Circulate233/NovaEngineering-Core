package github.kasuminova.novaeng.client.hitokoto;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;

public class HitokotoDeserializer implements JsonDeserializer<HitokotoResult> {

    private static int getJsonNodeInt(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            JsonElement element = json.get(memberName);
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    return primitive.getAsInt();
                }
            }
        }
        return -1;
    }

    private static String getJsonNodeString(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            JsonElement element = json.get(memberName);
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isString()) {
                    return primitive.getAsString();
                }
            }
        }
        return "";
    }

    @Override
    public HitokotoResult deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();
        return new HitokotoResult(
            getJsonNodeInt(root, "id"),
            getJsonNodeString(root, "uuid"),
            getJsonNodeString(root, "hitokoto"),
            getJsonNodeString(root, "type"),
            getJsonNodeString(root, "from"),
            getJsonNodeString(root, "from_who"),
            getJsonNodeString(root, "creator"),
            getJsonNodeInt(root, "creator_uid"),
            getJsonNodeInt(root, "reviewer"),
            getJsonNodeString(root, "commit_from"),
            getJsonNodeString(root, "created_at"),
            getJsonNodeInt(root, "length")
        );
    }
}
