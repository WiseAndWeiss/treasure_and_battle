package com.example.treasure_and_battle.manager;

import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.model.item.material.MaterialItem;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class ItemTypeAdapterFactory implements TypeAdapterFactory {
    private static final Map<String, Class<? extends Item>> CLASS_MAP = new HashMap<>();

    static {
        CLASS_MAP.put("EquipItem", EquipItem.class);
        CLASS_MAP.put("ConsumableItem", ConsumableItem.class);
        CLASS_MAP.put("MaterialItem", MaterialItem.class);
        CLASS_MAP.put("GemItem", GemItem.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<?> raw = type.getRawType();
        if (!Item.class.isAssignableFrom(raw)) return null;

        if (raw == Item.class || CLASS_MAP.containsValue(raw)) {
            return (TypeAdapter<T>) new ItemTypeAdapter(gson);
        }

        if (raw == EquipItem.class || raw == ConsumableItem.class
                || raw == MaterialItem.class || raw == GemItem.class) {
            return (TypeAdapter<T>) new ConcreteItemAdapter<>(gson, (Class<? extends Item>) raw);
        }

        return null;
    }

    private static class ConcreteItemAdapter<T extends Item> extends TypeAdapter<T> {
        private final Gson gson;
        private final Class<T> clazz;

        ConcreteItemAdapter(Gson gson, Class<T> clazz) {
            this.gson = gson;
            this.clazz = clazz;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (value == null) { out.nullValue(); return; }
            JsonElement tree = gson.toJsonTree(value, value.getClass());
            JsonObject obj = tree.getAsJsonObject();
            obj.addProperty("_type", value.getClass().getSimpleName());
            gson.toJson(obj, out);
        }

        @Override
        public T read(JsonReader in) throws IOException {
            JsonObject obj = gson.fromJson(in, JsonObject.class);
            if (obj == null) return null;
            return gson.fromJson(obj, clazz);
        }
    }

    private static class ItemTypeAdapter extends TypeAdapter<Item> {
        private final Gson gson;

        ItemTypeAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, Item value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            JsonElement tree = gson.toJsonTree(value, value.getClass());
            JsonObject obj = tree.getAsJsonObject();
            obj.addProperty("_type", value.getClass().getSimpleName());
            gson.toJson(obj, out);
        }

        @Override
        public Item read(JsonReader in) throws IOException {
            JsonObject obj = gson.fromJson(in, JsonObject.class);
            if (obj == null) return null;
            String type = obj.get("_type").getAsString();
            Class<? extends Item> clazz = CLASS_MAP.get(type);
            if (clazz == null) throw new JsonParseException("Unknown item type: " + type);
            return gson.fromJson(obj, clazz);
        }
    }
}
