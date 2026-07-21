package com.contactfront.engine.model;

import java.util.HashMap;
import java.util.Map;

public final class SymbolRegistry {
    private SymbolRegistry() {}

    private static final Map<String, SidcCode> FRIENDLY_CODES = new HashMap<>();
    private static final Map<String, SidcCode> HOSTILE_CODES = new HashMap<>();

    static {
        FRIENDLY_CODES.put("m1a2_abrams", SidcCode.FRIEND_ARMOR);
        FRIENDLY_CODES.put("t90m", SidcCode.FRIEND_ARMOR);
        FRIENDLY_CODES.put("inf_squad", SidcCode.FRIEND_INFANTRY);
        FRIENDLY_CODES.put("artillery", SidcCode.FRIEND_ARTILLERY);
        FRIENDLY_CODES.put("logistics", SidcCode.FRIEND_INFANTRY);
        FRIENDLY_CODES.put("drone", SidcCode.FRIEND_INFANTRY);

        HOSTILE_CODES.put("m1a2_abrams", SidcCode.HOSTILE_ARMOR);
        HOSTILE_CODES.put("t90m", SidcCode.HOSTILE_ARMOR);
        HOSTILE_CODES.put("inf_squad", SidcCode.HOSTILE_INFANTRY);
        HOSTILE_CODES.put("motostrelki", SidcCode.HOSTILE_INFANTRY);
        HOSTILE_CODES.put("artillery", SidcCode.HOSTILE_ARTILLERY);
    }

    public static SidcCode getSidcForUnit(String profileId, Faction faction) {
        SidcCode code = faction == Faction.USA || faction == Faction.CHINA || faction == Faction.IRAN
            ? FRIENDLY_CODES.get(profileId)
            : HOSTILE_CODES.get(profileId);
        return code != null ? code : SidcCode.FRIEND_INFANTRY;
    }
}