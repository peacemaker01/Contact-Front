package com.contactfront.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SidcCodeTest {

    @Test
    void validSidcCode() {
        SidcCode code = new SidcCode("SFGPU------****");
        assertEquals("SFGPU------****", code.code());
        assertTrue(code.isFriend());
    }

    @Test
    void hostileSidcCode() {
        SidcCode code = new SidcCode("SHGPU------****");
        assertTrue(code.isHostile());
    }

    @Test
    void friendlySidcCode() {
        SidcCode code = new SidcCode("SFGPU------****");
        assertTrue(code.isFriend());
    }

    @Test
    void unknownSidcCode() {
        SidcCode code = new SidcCode("SUGPU------****");
        assertTrue(code.isUnknown());
    }

    @Test
    void neutralSidcCode() {
        SidcCode code = new SidcCode("SNGPU------****");
        assertTrue(code.isNeutral());
    }

    @Test
    void shortCodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new SidcCode("SFGPU"));
    }

    @Test
    void nullCodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new SidcCode(null));
    }

    @Test
    void emptyCodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new SidcCode(""));
    }

    @Test
    void firstCharDeterminesAffiliation() {
        assertEquals('F', new SidcCode("SFGPU------****").affiliation());
        assertEquals('H', new SidcCode("SHGPU------****").affiliation());
        assertEquals('U', new SidcCode("SUGPU------****").affiliation());
        assertEquals('N', new SidcCode("SNGPU------****").affiliation());
    }
}