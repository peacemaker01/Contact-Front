package com.contactfront.engine.model;

public final class SidcCode {
    private final String code;

    public SidcCode(String code) {
        if (code == null || code.length() != 15) {
            throw new IllegalArgumentException("SIDC code must be 15 characters: " + code);
        }
        this.code = code;
    }

    public SidcCode(char scheme, char affiliation, char battleDimension, char status, 
                    char size, char type, char subtype) {
        this.code = "" + scheme + affiliation + "G" + battleDimension + status + 
                    size + "---" + type + subtype + "----------";
    }

    public String code() { return code; }

    public char scheme() { return code.charAt(0); }
    public char affiliation() { return code.charAt(1); }
    public char context() { return code.charAt(2); }
    public char battleDimension() { return code.charAt(3); }
    public char status() { return code.charAt(4); }
    public char size() { return code.charAt(5); }
    public char type() { return code.charAt(6); }
    public char subtype() { return code.charAt(7); }

    public boolean isFriend() { return affiliation() == 'F'; }
    public boolean isHostile() { return affiliation() == 'H'; }
    public boolean isNeutral() { return affiliation() == 'N'; }
    public boolean isUnknown() { return affiliation() == 'U'; }

    public boolean isGround() { return battleDimension() == 'G'; }
    public boolean isAir() { return battleDimension() == 'A'; }
    public boolean isSurface() { return battleDimension() == 'S'; }

    @Override
    public String toString() { return code; }
    @Override
    public boolean equals(Object o) { return o instanceof SidcCode s && code.equals(s.code); }
    @Override
    public int hashCode() { return code.hashCode(); }

    public static final SidcCode FRIEND_ARMOR = new SidcCode("SFGPEUDL-------");
    public static final SidcCode HOSTILE_ARMOR = new SidcCode("SHGPEUDL-------");
    public static final SidcCode FRIEND_INFANTRY = new SidcCode("SFGPEU---------");
    public static final SidcCode HOSTILE_INFANTRY = new SidcCode("SHGPEU---------");
    public static final SidcCode FRIEND_ARTILLERY = new SidcCode("SFGPGUDL-------");
    public static final SidcCode HOSTILE_ARTILLERY = new SidcCode("SHGPGUDL-------");
}