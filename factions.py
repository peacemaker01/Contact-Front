# factions.py
FACTIONS = {
    "USA": {
        "banner": "[ USA ]",
        "flag": "★ USA ★",
        "color": "BLUE",
        "ranks": ["Sgt.", "Cpl.", "Pvt.", "Spc.", "Lt."],
        "personnel_names": ["Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"],
        "units": {
            "rifleman": {"name": "Rifleman", "weapon": "M4 Carbine", "ammo": 30, "morale": 85},
            "lmg": {"name": "Automatic Rifleman", "weapon": "M249 SAW", "ammo": 100, "morale": 90},
            "marksman": {"name": "Marksman", "weapon": "M110 SASS", "ammo": 20, "morale": 88}
        },
        "enemy_names": ["Insurgent", "Fighter", "Militant", "Gunner", "Sniper"],
        "morale_bonus": 5,
        "accuracy_bonus": 0.05,
        "assets": {
            "nukes": 100, "ballistic_missiles": 500, "cruise_missiles": 1000,
            "warships": 80, "intelligence_level": 90, "air_defense": 95,
            "electronic_warfare": 90, "hypersonic_missiles": 50,
            "loitering_munitions": 100, "anti_ship_missiles": 300,
            "cyber_warfare": 95, "space_assets": 85,
            "missile_silos": 50, "infrastructure": 200, "command_centers": 30, "nuclear_sites": 20
        },
        "guerrilla": False
    },
    "RUSSIA": {
        "banner": "[ RUSSIA ]",
        "flag": "☭ RUSSIA ☭",
        "color": "RED",
        "ranks": ["St. Serzhant", "Yefreytor", "Ryadovoy", "Mladshiy Serzhant", "Serzhant"],
        "personnel_names": ["Ivanov", "Smirnov", "Kuznetsov", "Popov", "Sokolov", "Lebedev", "Kozlov", "Volkov", "Zaytsev", "Sidorov"],
        "units": {
            "rifleman": {"name": "Strelok", "weapon": "AK-74M", "ammo": 30, "morale": 80},
            "lmg": {"name": "Pulemyotchik", "weapon": "PKP Pecheneg", "ammo": 100, "morale": 85},
            "marksman": {"name": "Snayper", "weapon": "SVD Dragunov", "ammo": 20, "morale": 82}
        },
        "enemy_names": ["Separatist", "Militia", "Fighter", "Gunner", "Sniper"],
        "morale_bonus": 10,
        "accuracy_bonus": 0.0,
        "assets": {
            "nukes": 200, "ballistic_missiles": 600, "cruise_missiles": 800,
            "warships": 60, "intelligence_level": 70, "air_defense": 85,
            "electronic_warfare": 80, "hypersonic_missiles": 120,
            "loitering_munitions": 200, "anti_ship_missiles": 400,
            "cyber_warfare": 85, "space_assets": 70,
            "missile_silos": 80, "infrastructure": 150, "command_centers": 40, "nuclear_sites": 40
        },
        "guerrilla": False
    },
    "CHINA": {
        "banner": "[ CHINA ]",
        "flag": "✩ CHINA ✩",
        "color": "YELLOW",
        "ranks": ["Shangwei", "Zhongshi", "Liebing", "Xiaowei", "Shangshi"],
        "personnel_names": ["Wang", "Li", "Zhang", "Liu", "Chen", "Yang", "Huang", "Zhao", "Zhou", "Wu"],
        "units": {
            "rifleman": {"name": "Zhanshi", "weapon": "QBZ-95", "ammo": 30, "morale": 82},
            "lmg": {"name": "Machine Gunner", "weapon": "QJY-88", "ammo": 100, "morale": 87},
            "marksman": {"name": "Sniper", "weapon": "CS/LR4", "ammo": 20, "morale": 84}
        },
        "enemy_names": ["Militant", "Insurgent", "Fighter", "Gunner", "Sniper"],
        "morale_bonus": 8,
        "accuracy_bonus": 0.02,
        "assets": {
            "nukes": 50, "ballistic_missiles": 400, "cruise_missiles": 600,
            "warships": 90, "intelligence_level": 75, "air_defense": 80,
            "electronic_warfare": 75, "hypersonic_missiles": 100,
            "loitering_munitions": 250, "anti_ship_missiles": 500,
            "cyber_warfare": 80, "space_assets": 75,
            "missile_silos": 60, "infrastructure": 180, "command_centers": 35, "nuclear_sites": 15
        },
        "guerrilla": False
    },
    "IRAN": {
        "banner": "[ IRAN ]",
        "flag": "🇮🇷 IRAN 🇮🇷",
        "color": "GREEN",
        "ranks": ["Sargom", "Goruhban", "Sarbaz", "Sarhang", "Tahmasebi"],
        "personnel_names": ["Reza", "Ali", "Mohammad", "Hossein", "Saeed", "Mehdi", "Hamid", "Abbas", "Karim", "Nasir"],
        "units": {
            "rifleman": {"name": "Basiji", "weapon": "G3A6", "ammo": 30, "morale": 75},
            "lmg": {"name": "Machine Gunner", "weapon": "MG3", "ammo": 100, "morale": 80},
            "marksman": {"name": "Marksman", "weapon": "SVD", "ammo": 20, "morale": 78}
        },
        "enemy_names": ["Militant", "Insurgent", "Fighter", "Gunner", "Sniper"],
        "morale_bonus": 0,
        "accuracy_bonus": -0.05,
        "assets": {
            "nukes": 0, "ballistic_missiles": 200, "cruise_missiles": 100,
            "warships": 20, "intelligence_level": 50, "air_defense": 60,
            "electronic_warfare": 40, "hypersonic_missiles": 0,
            "loitering_munitions": 500, "anti_ship_missiles": 300,
            "cyber_warfare": 60, "space_assets": 20,
            "missile_silos": 30, "infrastructure": 100, "command_centers": 15, "nuclear_sites": 5
        },
        "guerrilla": True
    }
}

def get_faction_banner(faction_name):
    return FACTIONS.get(faction_name, FACTIONS["USA"])["banner"]

def get_faction_flag(faction_name):
    return FACTIONS.get(faction_name, FACTIONS["USA"])["flag"]

def get_faction_color(faction_name):
    return FACTIONS.get(faction_name, FACTIONS["USA"])["color"]
