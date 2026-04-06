FACTIONS = {
    "USA": {
        "name": "United States Armed Forces",
        "abbreviation": "USA",
        "ansi_color": "\033[94m",
        "banner": "",
        "doctrine": "Power projection, network-centric warfare, precision strike",
        "last_names": ["Harrison", "Martinez", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Garcia", "Rodriguez"],
        "unit_templates": {
            "infantry_squad": {"type_code": "R", "rank": "Staff Sergeant", "emoji": "🪖", "personnel": 9, "base_morale": 85, "ammo": 300, "weapon": "M4A1", "armor": 0, "movement": 2, "accuracy_base": 75, "suppress_threshold": 40},
            "m1a2_tank": {"type_code": "T", "rank": "Sergeant", "emoji": "🚗", "personnel": 4, "base_morale": 90, "ammo": 40, "weapon": "120mm", "armor": 95, "movement": 3, "accuracy_base": 88, "suppress_threshold": 25},
            "bradley_ifv": {"type_code": "I", "rank": "Sergeant", "emoji": "🚙", "personnel": 9, "base_morale": 87, "ammo": 300, "weapon": "25mm", "armor": 60, "movement": 3, "accuracy_base": 82, "suppress_threshold": 30},
            "apache_helicopter": {"type_code": "H", "rank": "Chief Warrant Officer", "emoji": "🚁", "personnel": 2, "base_morale": 92, "ammo": 76, "weapon": "Hellfire", "armor": 20, "movement": 8, "accuracy_base": 90, "suppress_threshold": 15},
            "m109_artillery": {"type_code": "A", "rank": "Sergeant", "emoji": "💥", "personnel": 6, "base_morale": 80, "ammo": 60, "weapon": "155mm", "armor": 15, "movement": 2, "accuracy_base": 70, "suppress_threshold": 50}
        },
        "morale_bonus": 10,
        "accuracy_bonus": 8,
        "special_mechanic": {"name": "Layered Air Defense", "description": "Three-tier intercept", "tactical_effect": "CAS takes 1 turn"},
        "strategic_assets": {
            "bgm_109_tomahawk": {"count": 200, "range_km": 2500, "cep_m": 10, "warhead": "conventional"},
            "minuteman_iii": {"count": 400, "range_km": 13000, "cep_m": 120, "warhead": "MIRV nuclear"},
            "gps_constellation": {"operational": True, "jamming_resist": 85},
            "patriot_pac3": {"batteries": 40, "intercept_rate": 85}
        }
    },
    "RUSSIA": {
        "name": "Russian Armed Forces",
        "abbreviation": "RUS",
        "ansi_color": "\033[91m",
        "banner": "",
        "doctrine": "Deep battle, mass artillery, layered EW",
        "last_names": ["Volkov", "Smirnov", "Ivanov", "Kuznetsov", "Popov", "Sokolov", "Lebedev", "Kozlov", "Novikov", "Morozov"],
        "unit_templates": {
            "infantry_squad": {"type_code": "R", "rank": "Serzhant", "emoji": "🪖", "personnel": 10, "base_morale": 72, "ammo": 450, "weapon": "AK-74M", "armor": 0, "movement": 2, "accuracy_base": 65, "suppress_threshold": 35},
            "t90m_tank": {"type_code": "T", "rank": "Leytenant", "emoji": "🚗", "personnel": 3, "base_morale": 80, "ammo": 43, "weapon": "125mm", "armor": 90, "movement": 3, "accuracy_base": 78, "suppress_threshold": 20},
            "bmp3_ifv": {"type_code": "I", "rank": "Leytenant", "emoji": "🚙", "personnel": 8, "base_morale": 75, "ammo": 500, "weapon": "100mm+30mm", "armor": 50, "movement": 3, "accuracy_base": 72, "suppress_threshold": 30},
            "ka52_helicopter": {"type_code": "H", "rank": "Kapitan", "emoji": "🚁", "personnel": 2, "base_morale": 85, "ammo": 80, "weapon": "Vikhr", "armor": 25, "movement": 7, "accuracy_base": 80, "suppress_threshold": 20},
            "2s19_msta_artillery": {"type_code": "A", "rank": "Serzhant", "emoji": "💥", "personnel": 5, "base_morale": 78, "ammo": 80, "weapon": "152mm", "armor": 20, "movement": 2, "accuracy_base": 72, "suppress_threshold": 40}
        },
        "morale_bonus": -5,
        "accuracy_bonus": -3,
        "special_mechanic": {"name": "Maskirovka + EW", "description": "GPS jamming, hidden flanks", "tactical_effect": "Enemy accuracy -15 near EW"},
        "strategic_assets": {
            "kalibr_cruise_missile": {"count": 500, "range_km": 2500, "cep_m": 3, "warhead": "conv/nuclear"},
            "iskander_m": {"count": 150, "range_km": 500, "cep_m": 5, "warhead": "conv/nuclear"},
            "glonass_constellation": {"operational": True, "jamming_resist": 70},
            "s400_triumf": {"batteries": 60, "intercept_rate": 90}
        }
    },
    "CHINA": {
        "name": "People's Liberation Army",
        "abbreviation": "PRC",
        "ansi_color": "\033[93m",
        "banner": "",
        "doctrine": "Anti-access/area denial, cyber warfare",
        "last_names": ["Wang", "Li", "Zhang", "Liu", "Chen", "Yang", "Huang", "Zhao", "Zhou", "Wu"],
        "unit_templates": {
            "infantry_squad": {"type_code": "R", "rank": "Banzhang", "emoji": "🪖", "personnel": 10, "base_morale": 76, "ammo": 400, "weapon": "QBZ-95", "armor": 0, "movement": 2, "accuracy_base": 68, "suppress_threshold": 38},
            "type99a_tank": {"type_code": "T", "rank": "Paizhang", "emoji": "🚗", "personnel": 3, "base_morale": 82, "ammo": 42, "weapon": "125mm", "armor": 92, "movement": 3, "accuracy_base": 80, "suppress_threshold": 22},
            "zbd04_ifv": {"type_code": "I", "rank": "Paizhang", "emoji": "🚙", "personnel": 7, "base_morale": 78, "ammo": 400, "weapon": "100mm+30mm", "armor": 55, "movement": 3, "accuracy_base": 74, "suppress_threshold": 32},
            "z19_helicopter": {"type_code": "H", "rank": "Lianzhang", "emoji": "🚁", "personnel": 2, "base_morale": 80, "ammo": 60, "weapon": "HJ-10", "armor": 15, "movement": 7, "accuracy_base": 76, "suppress_threshold": 22},
            "plz05_artillery": {"type_code": "A", "rank": "Banzhang", "emoji": "💥", "personnel": 5, "base_morale": 80, "ammo": 70, "weapon": "155mm", "armor": 20, "movement": 2, "accuracy_base": 75, "suppress_threshold": 42}
        },
        "morale_bonus": 5,
        "accuracy_bonus": 5,
        "special_mechanic": {"name": "Cyber & Drone Swarm", "description": "Cyber strikes, drone ISR", "tactical_effect": "+10 accuracy near drone"},
        "strategic_assets": {
            "df21d_asbm": {"count": 100, "range_km": 1500, "cep_m": 20, "warhead": "conventional"},
            "df41_icbm": {"count": 300, "range_km": 15000, "cep_m": 100, "warhead": "MIRV nuclear"},
            "beihdou_constellation": {"operational": True, "jamming_resist": 90},
            "hq9_sam": {"batteries": 50, "intercept_rate": 80}
        }
    },
    "IRAN": {
        "name": "Islamic Revolutionary Guard Corps",
        "abbreviation": "IRN",
        "ansi_color": "\033[92m",
        "banner": "",
        "doctrine": "Asymmetric warfare, proxy networks, saturation strikes",
        "last_names": ["Rezaei", "Mohammadi", "Hosseini", "Karimi", "Ahmadi", "Gholami", "Moradi", "Jafari", "Abbasi", "Hashemi"],
        "unit_templates": {
            "irgc_squad": {"type_code": "R", "rank": "Sarjukhe", "emoji": "🪖", "personnel": 12, "base_morale": 88, "ammo": 600, "weapon": "AKM+RPG", "armor": 0, "movement": 2, "accuracy_base": 60, "suppress_threshold": 20},
            "t72s_tank": {"type_code": "T", "rank": "Sotvan Dovvom", "emoji": "🚗", "personnel": 3, "base_morale": 75, "ammo": 45, "weapon": "125mm", "armor": 75, "movement": 2, "accuracy_base": 65, "suppress_threshold": 30},
            "bmp2_ifv": {"type_code": "I", "rank": "Sotvan Dovvom", "emoji": "🚙", "personnel": 10, "base_morale": 70, "ammo": 500, "weapon": "30mm+ATGM", "armor": 40, "movement": 2, "accuracy_base": 62, "suppress_threshold": 35},
            "shahed136_swarm": {"type_code": "D", "rank": "Sarjukhe", "emoji": "✈️", "personnel": 1, "base_morale": 100, "ammo": 1, "weapon": "Shahed-136", "armor": 0, "movement": 5, "accuracy_base": 72, "suppress_threshold": 0},
            "proxy_militia": {"type_code": "P", "rank": "Basiji", "emoji": "👥", "personnel": 20, "base_morale": 80, "ammo": 800, "weapon": "AK+IED", "armor": 0, "movement": 3, "accuracy_base": 50, "suppress_threshold": 25}
        },
        "morale_bonus": 15,
        "accuracy_bonus": -10,
        "special_mechanic": {"name": "Guerrilla Repositioning", "description": "Ghost move, proxy spawn", "tactical_effect": "Disappear for 2 turns"},
        "strategic_assets": {
            "shahab3_ballistic": {"count": 200, "range_km": 2000, "cep_m": 190, "warhead": "conventional"},
            "shahed136_loitering": {"count": 5000, "range_km": 2500, "cep_m": 5, "type": "kamikaze_drone"},
            "bavar373_sam": {"batteries": 20, "intercept_rate": 65}
        }
    }
}
