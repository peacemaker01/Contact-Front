# campaign.py
import json
import os
from config import COLORS

def print_c(text, color="RESET"):
    print(f"{COLORS.get(color, COLORS['RESET'])}{text}{COLORS['RESET']}")

CAMPAIGN_FILE = "campaign.json"

def save_campaign(state):
    """Save attacker units' experience and kills."""
    data = {
        "player_faction": state.player_faction,
        "units": []
    }
    for u in state.units:
        if u.side == "attacker":
            data["units"].append({
                "name": u.name,
                "type": u.type,
                "exp": u.exp,
                "kills": u.kills
            })
    with open(CAMPAIGN_FILE, "w") as f:
        json.dump(data, f, indent=2)

def load_campaign():
    """Load campaign data if exists."""
    if not os.path.exists(CAMPAIGN_FILE):
        return None
    with open(CAMPAIGN_FILE, "r") as f:
        return json.load(f)

def apply_campaign_bonus(state):
    """Boost new units based on campaign progress."""
    camp = load_campaign()
    if not camp:
        return
    for unit in state.units:
        if unit.side == "attacker":
            for old in camp["units"]:
                if old["name"] == unit.name:
                    unit.exp = old.get("exp", 0)
                    unit.kills = old.get("kills", 0)
                    unit.morale += unit.exp // 10
                    if unit.exp >= 100:
                        unit.leadership = 20
                        unit.is_leader = True
                    break
    print_c("Campaign bonuses applied! Veteran units have higher morale.", "GREEN")
