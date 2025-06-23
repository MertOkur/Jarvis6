package com.example.jarvis6;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

class FunctionCallBuilder {
    private final JsonArray functions = new JsonArray();

    public FunctionCallBuilder addFunction(String name, String description) {
        var function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", description);
        function.add("parameters", createEmptyParams());
        functions.add(function);
        return this;
    }

    public FunctionCallBuilder addFunction(String name, String description, Map<String, String> params, List<String> required) {
        var function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", description);
        function.add("parameters", createParamObject(params, required));
        functions.add(function);
        return this;
    }

    public JsonArray build() {
        return functions;
    }

    // helping methods
    private JsonObject createEmptyParams() {
        var empty = new JsonObject();
        empty.addProperty("type", "object");
        empty.add("properties", new JsonObject());
        empty.add("required", new JsonArray());
        return empty;
    }

    private JsonObject createParamObject(Map<String, String> params, List<String> required) {
        var param = new JsonObject();
        param.addProperty("type", "object");

        var props = new JsonObject();
        for (var entry : params.entrySet()) {
            var prop = new JsonObject();
            prop.addProperty("type", "string");
            prop.addProperty("description", entry.getValue());
            props.add(entry.getKey(), prop);
        }

        param.add("properties", props);

        var req = new JsonArray();
        for (var r : required) {
            req.add(r);
        }

        param.add("required", req);

        return param;
    }
}
