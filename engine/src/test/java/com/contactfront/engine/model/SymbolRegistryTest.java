package com.contactfront.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SymbolRegistryTest {

    @Test
    void infantryUnitGetsInfantrySidc() {
        SidcCode code = SymbolRegistry.getSidcForUnit("inf_squad", Faction.USA);
        assertNotNull(code);
        assertEquals("SFGPEU---------", code.code());
    }

    @Test
    void armorUnitGetsArmorSidc() {
        SidcCode code = SymbolRegistry.getSidcForUnit("m1a2_abrams", Faction.USA);
        assertNotNull(code);
        assertEquals("SFGPEUDL-------", code.code());
    }

    @Test
    void reconUnitGetsReconSidc() {
        SidcCode code = SymbolRegistry.getSidcForUnit("recon_squad", Faction.USA);
        assertNotNull(code);
    }

    @Test
    void hostileFactionGetsHostileSidc() {
        SidcCode code = SymbolRegistry.getSidcForUnit("inf_squad", Faction.RUSSIA);
        assertNotNull(code);
        assertTrue(code.isHostile());
    }

    @Test
    void unknownUnitGetsFallbackSidc() {
        SidcCode code = SymbolRegistry.getSidcForUnit("unknown_unit", Faction.USA);
        assertNotNull(code);
        assertTrue(code.isFriend());
    }

    @Test
    void chinaFactionGetsFriendlySidc() {
        SidcCode code = SymbolRegistry.getSidcForUnit("inf_squad", Faction.CHINA);
        assertNotNull(code);
        assertTrue(code.isFriend());
    }

    @Test
    void iranFactionGetsFriendlySidc() {
        SidcCode code = SymbolRegistry.getSidcForUnit("inf_squad", Faction.IRAN);
        assertNotNull(code);
        assertTrue(code.isFriend());
    }
}