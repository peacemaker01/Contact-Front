package com.contactfront.engine;

import com.contactfront.engine.data.DamageMatrix;
import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.ArmorClass;
import com.contactfront.engine.model.DamageClass;
import com.contactfront.engine.model.UnitCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProfilesTest {

    @Test
    void loadsRosterFromJson() {
        Profiles p = Profiles.load();
        assertTrue(p.hasWeapon("rifle_556"));
        assertTrue(p.hasUnit("mbt"));
        assertEquals(UnitCategory.ARMOR, p.unit("mbt").category());
        assertEquals(ArmorClass.HEAVY, p.unit("mbt").armorClass());
        assertEquals(DamageClass.AT, p.weapon("at4_84mm").damageClass());
    }

    @Test
    void damageMatrixEncodesHardCounters() {
        assertTrue(DamageMatrix.multiplier(DamageClass.AT, ArmorClass.HEAVY) >
                DamageMatrix.multiplier(DamageClass.LIGHT, ArmorClass.HEAVY));
        assertTrue(DamageMatrix.multiplier(DamageClass.LIGHT, ArmorClass.NONE) >
                DamageMatrix.multiplier(DamageClass.LIGHT, ArmorClass.HEAVY));
        assertTrue(DamageMatrix.multiplier(DamageClass.AT, ArmorClass.HEAVY) >= 1.0);
    }

    @Test
    void manpadsTargetsAirOnly() {
        Profiles p = Profiles.load();
        assertTrue(p.weapon("manpads").canTarget(com.contactfront.engine.model.TargetType.AIR));
        assertFalse(p.weapon("manpads").canTarget(com.contactfront.engine.model.TargetType.GROUND));
    }
}
