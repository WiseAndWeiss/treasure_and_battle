package com.example.treasure_and_battle.manager;

import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.affix.BaseMonsterAffix;
import com.example.treasure_and_battle.affix.impl.equip.attribute.EquipAttributeAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerBattleStartAoeDamageAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerBuffAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerCritReduceApAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerOnKillRecoverAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerPurifyAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerRecoverAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerRoundStartRecoverAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerSkillCastRecoverMpAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerSkillCastReduceCdAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerThornsAffix;
import com.example.treasure_and_battle.affix.impl.monster.attribute.MonsterAttributeAffix;
import com.example.treasure_and_battle.affix.impl.monster.defensive.MonsterDamageCapAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerAoeBuffAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerBuffAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerManaBurnAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerOnDeathExplodeAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerRecoverAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerRoundStartRecoverAffix;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class AffixTypeAdapterFactory implements TypeAdapterFactory {
    private static final Map<String, Class<? extends BaseAffix>> CLASS_MAP = new HashMap<>();

    static {
        CLASS_MAP.put("EquipAttributeAffix", EquipAttributeAffix.class);
        CLASS_MAP.put("EquipTriggerBattleStartAoeDamageAffix", EquipTriggerBattleStartAoeDamageAffix.class);
        CLASS_MAP.put("EquipTriggerBuffAffix", EquipTriggerBuffAffix.class);
        CLASS_MAP.put("EquipTriggerCritReduceApAffix", EquipTriggerCritReduceApAffix.class);
        CLASS_MAP.put("EquipTriggerOnKillRecoverAffix", EquipTriggerOnKillRecoverAffix.class);
        CLASS_MAP.put("EquipTriggerPurifyAffix", EquipTriggerPurifyAffix.class);
        CLASS_MAP.put("EquipTriggerRecoverAffix", EquipTriggerRecoverAffix.class);
        CLASS_MAP.put("EquipTriggerRoundStartRecoverAffix", EquipTriggerRoundStartRecoverAffix.class);
        CLASS_MAP.put("EquipTriggerSkillCastRecoverMpAffix", EquipTriggerSkillCastRecoverMpAffix.class);
        CLASS_MAP.put("EquipTriggerSkillCastReduceCdAffix", EquipTriggerSkillCastReduceCdAffix.class);
        CLASS_MAP.put("EquipTriggerThornsAffix", EquipTriggerThornsAffix.class);
        CLASS_MAP.put("MonsterAttributeAffix", MonsterAttributeAffix.class);
        CLASS_MAP.put("MonsterDamageCapAffix", MonsterDamageCapAffix.class);
        CLASS_MAP.put("MonsterTriggerAoeBuffAffix", MonsterTriggerAoeBuffAffix.class);
        CLASS_MAP.put("MonsterTriggerBuffAffix", MonsterTriggerBuffAffix.class);
        CLASS_MAP.put("MonsterTriggerManaBurnAffix", MonsterTriggerManaBurnAffix.class);
        CLASS_MAP.put("MonsterTriggerOnDeathExplodeAffix", MonsterTriggerOnDeathExplodeAffix.class);
        CLASS_MAP.put("MonsterTriggerRecoverAffix", MonsterTriggerRecoverAffix.class);
        CLASS_MAP.put("MonsterTriggerRoundStartRecoverAffix", MonsterTriggerRoundStartRecoverAffix.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!BaseAffix.class.isAssignableFrom(type.getRawType())) return null;
        if (type.getRawType() == BaseAffix.class
                || type.getRawType() == BaseEquipAffix.class
                || type.getRawType() == BaseMonsterAffix.class) {
            return (TypeAdapter<T>) new AffixTypeAdapter(gson);
        }
        return null;
    }

    private static class AffixTypeAdapter extends TypeAdapter<BaseAffix> {
        private final Gson gson;

        AffixTypeAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, BaseAffix value) throws IOException {
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
        public BaseAffix read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            JsonObject obj = gson.fromJson(in, JsonObject.class);
            if (obj == null) return null;
            String type = obj.get("_type").getAsString();
            Class<? extends BaseAffix> clazz = CLASS_MAP.get(type);
            if (clazz == null) throw new JsonParseException("Unknown affix type: " + type);
            return gson.fromJson(obj, clazz);
        }
    }
}
