package com.contactfront.engine.data;

import com.contactfront.engine.model.ArmorClass;
import com.contactfront.engine.model.DamageClass;
import com.contactfront.engine.model.TargetType;
import com.contactfront.engine.model.UnitCategory;
import com.contactfront.engine.model.UnitProfile;
import com.contactfront.engine.model.Weapon;
import com.contactfront.engine.model.WeaponProfile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Profiles {
    private final Map<String, WeaponProfile> weapons = new HashMap<>();
    private final Map<String, UnitProfile> units = new HashMap<>();

    public void add(WeaponProfile wp) { weapons.put(wp.id(), wp); }
    public void add(UnitProfile up) { units.put(up.id(), up); }

    public boolean hasWeapon(String id) { return weapons.containsKey(id); }
    public boolean hasUnit(String id) { return units.containsKey(id); }

    public WeaponProfile weapon(String id) {
        WeaponProfile wp = weapons.get(id);
        if (wp == null) throw new IllegalArgumentException("unknown weapon: " + id);
        return wp;
    }

    public UnitProfile unit(String id) {
        UnitProfile up = units.get(id);
        if (up == null) throw new IllegalArgumentException("unknown unit: " + id);
        return up;
    }

    public List<UnitProfile> allUnits() { return new ArrayList<>(units.values()); }
    public List<WeaponProfile> allWeapons() { return new ArrayList<>(weapons.values()); }

    public List<Weapon> instantiateWeapons(UnitProfile up) {
        List<Weapon> out = new ArrayList<>();
        for (String wid : up.weapons()) out.add(new Weapon(weapon(wid)));
        return out;
    }

    public static Profiles load() {
        Profiles p = new Profiles();
        p.loadWeapons(resource("/com/contactfront/engine/data/weapon_profiles.json"));
        p.loadUnits(resource("/com/contactfront/engine/data/unit_profiles.json"));
        return p;
    }

    private static InputStream resource(String path) {
        InputStream in = Profiles.class.getResourceAsStream(path);
        if (in == null) throw new IllegalStateException("missing resource: " + path);
        return in;
    }

    private void loadWeapons(InputStream in) {
        JSONObject root = new JSONObject(new JSONTokener(in));
        for (Object o : root.getJSONArray("weapons")) {
            JSONObject j = (JSONObject) o;
            Set<TargetType> types = EnumSet.of(TargetType.GROUND);
            if (j.has("target_types")) {
                types = EnumSet.noneOf(TargetType.class);
                for (Object t : j.getJSONArray("target_types")) {
                    types.add(TargetType.valueOf(t.toString().toUpperCase()));
                }
            }
            add(new WeaponProfile(
                    j.getString("id"),
                    j.optString("name", j.getString("id")),
                    j.getInt("range"),
                    DamageClass.valueOf(j.getString("damage_class").toUpperCase()),
                    j.getInt("rof"),
                    j.getInt("suppression_value"),
                    j.optInt("max_ammo", 6),
                    types));
        }
    }

    private void loadUnits(InputStream in) {
        JSONObject root = new JSONObject(new JSONTokener(in));
        for (Object o : root.getJSONArray("units")) {
            JSONObject j = (JSONObject) o;
            List<String> special = new ArrayList<>();
            if (j.has("special")) {
                for (Object s : j.getJSONArray("special")) special.add(s.toString());
            }
            List<String> wps = new ArrayList<>();
            for (Object w : j.getJSONArray("weapons")) wps.add(w.toString());
            add(new UnitProfile(
                    j.getString("id"),
                    j.optString("name", j.getString("id")),
                    UnitCategory.valueOf(j.getString("category").toUpperCase()),
                    wps,
                    j.getInt("move"),
                    ArmorClass.valueOf(j.getString("armor_class").toUpperCase()),
                    j.optInt("recon_radius", 4),
                    j.optInt("base_accuracy", 75),
                    j.optInt("radio_range", 10),
                    special,
                    j.optDouble("rcs_m2", 1.0),
                    j.optDouble("thermal_signature", 0.5),
                    j.optInt("armor_thickness", 0),
                    j.optBoolean("era_present", false)));
        }
    }
}
