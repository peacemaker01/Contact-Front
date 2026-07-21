package com.contactfront.engine.model;

public enum NetworkTopology {
    Type_A("Centralized", "HQ-dependent sharing, high bandwidth but vulnerable to EW", 8, true),
    Type_B("Hierarchical", "Command vehicle relay, medium bandwidth with node dependency", 5, true),
    Type_C("Decentralized", "Peer-to-peer only, low bandwidth but EW immune", 2, false);

    private final String name;
    private final String description;
    private final int sharedContactBandwidth;
    private final boolean vulnerableToEw;

    NetworkTopology(String name, String description, int sharedContactBandwidth, boolean vulnerableToEw) {
        this.name = name;
        this.description = description;
        this.sharedContactBandwidth = sharedContactBandwidth;
        this.vulnerableToEw = vulnerableToEw;
    }

    public String id() { return name; }
    public String description() { return description; }
    public int sharedContactBandwidth() { return sharedContactBandwidth; }
    public boolean vulnerableToEw() { return vulnerableToEw; }

    public static NetworkTopology fromId(String id) {
        for (NetworkTopology nt : values()) {
            if (nt.name.equalsIgnoreCase(id)) return nt;
        }
        return Type_A;
    }
}