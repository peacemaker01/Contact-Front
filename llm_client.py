# llm_client.py - Complete
import os
import json
import re
import requests

PROVIDERS = {
    "openrouter": {"url": "https://openrouter.ai/api/v1/chat/completions", "model": "openai/gpt-4o-mini"},
    "deepseek": {"url": "https://api.deepseek.com/v1/chat/completions", "model": "deepseek-chat"},
    "claude": {"url": "https://api.anthropic.com/v1/messages", "model": "claude-3-haiku-20240307"}
}

def terrain_to_zone(word: str) -> str:
    w = word.lower()
    if w in ["ditch", "trench", "foxhole", "defilade", "low ground", "dip", "cover"]:
        return "close"
    if w in ["building", "house", "farmhouse", "barn", "compound", "strongpoint", "bunker", "position"]:
        return "medium"
    if w in ["wall", "stone wall", "ridge", "tree line", "embankment", "crest"]:
        return "long"
    if w in ["hill", "far ridge", "distant", "horizon", "mount"]:
        return "extreme"
    return w

def local_parse(command: str) -> dict:
    cmd = command.lower().strip()

    # Specific info questions
    if re.search(r'\b(what does the drone see|what does drone see|drone view|drone feed)\b', cmd):
        return {"type": "info", "parser": "local"}

    # Surrender
    if re.search(r'\b(surrender|give up|lay down arms|cease fire)\b', cmd):
        return {"type": "surrender", "parser": "local"}

    # Recon by fire
    if re.search(r'\brecon by fire\b', cmd):
        return {"type": "attack", "zone": "medium", "parser": "local"}

    # General info
    if re.search(r'\b(what|show|tell|see|status|where|how many)\b', cmd) and not re.search(r'\b(mortar|advance|fire|attack|recon|surrender|fpv|missile|nuke|warship|intel|ew|cyber|space|psyops|resupply|produce|sanction|clear ieds|sigint|jamming|targets|roe|advise|overwatch|heal|interrogate|supply)\b', cmd):
        return {"type": "info", "parser": "local"}

    # Advisor
    if re.search(r'\b(advise|suggest|what should i do|recommend)\b', cmd):
        return {"type": "advise", "parser": "local"}

    # Intel
    if re.search(r'\b(intel|intelligence|enemy status|what are the enemy doing)\b', cmd):
        return {"type": "intel", "parser": "local"}

    # Recon drone
    if re.search(r'\b(recon|scan|spot|launch drone|recon drone|eye in the sky)\b', cmd):
        return {"type": "recon", "parser": "local"}

    # FPV drone
    if re.search(r'\b(fp?v?|drone|kamikaze|suicide drone|strike)\b', cmd) and not re.search(r'\b(recon|mortar)\b', cmd):
        target_match = re.search(r'(?:drone|fpv|strike|kill|after)\s+(.+?)(?:\.|$| and | then )', cmd)
        target = target_match.group(1).strip() if target_match else None
        if target and target in ["close", "medium", "long", "extreme"]:
            target = None
        return {"type": "fpv", "target": target, "parser": "local"}

    # Mortar
    if re.search(r'\b(mortar|motar|mortor|shell|bombard|artillery|rounds?)\b', cmd):
        zone = "medium"
        for z in ["close", "medium", "long", "extreme"]:
            if z in cmd:
                zone = z
                break
        for word in ["ditch", "trench", "farmhouse", "stone wall"]:
            if word in cmd:
                zone = terrain_to_zone(word)
                break
        qty_match = re.search(r'(\d+)\s*(?:round|shell|mortar)', cmd)
        qty = int(qty_match.group(1)) if qty_match else 1
        return {"type": "mortar", "zone": zone, "quantity": qty, "parser": "local"}

    # Advance
    if re.search(r'\b(advance|move|push|go to|approach|close in)\b', cmd):
        zone = "medium"
        for z in ["close", "medium", "long", "extreme"]:
            if z in cmd:
                zone = z
                break
        for word in ["ditch", "trench", "farmhouse", "stone wall"]:
            if word in cmd:
                zone = terrain_to_zone(word)
                break
        return {"type": "advance", "zone": zone, "parser": "local"}

    # Attack / suppress
    if re.search(r'\b(fire|shoot|attack|engage|suppress|light up|open fire)\b', cmd):
        zone = "medium"
        for z in ["close", "medium", "long", "extreme"]:
            if z in cmd:
                zone = z
                break
        for word in ["building", "strongpoint"]:
            if word in cmd:
                zone = terrain_to_zone(word)
                break
        return {"type": "attack", "zone": zone, "parser": "local"}

    # Overwatch
    if re.search(r'\b(overwatch|cover|watch)\b', cmd):
        zone = "medium"
        for z in ["close","medium","long","extreme"]:
            if z in cmd:
                zone = z
                break
        return {"type": "overwatch", "zone": zone, "parser": "local"}

    # Heal
    if re.search(r'\b(heal|medic)\b', cmd):
        return {"type": "heal", "parser": "local"}

    # Interrogate
    if re.search(r'\b(interrogate|question|prisoner)\b', cmd):
        return {"type": "interrogate", "parser": "local"}

    # Supply line
    if re.search(r'\b(supply|supply line|logistics)\b', cmd):
        return {"type": "supply", "parser": "local"}

    # ========== STRATEGIC COMMANDS ==========
    if re.search(r'\b(targets|what can i strike|strike options|list targets)\b', cmd):
        return {"type": "targets", "parser": "local"}
    if re.search(r'\b(ballistic missile|missile strike|launch missile|icbm)\b', cmd):
        intent = {"type": "missile", "missile_type": "ballistic", "parser": "local"}
        target_match = re.search(r'\b(at|against|target)\s+(\w+(?:\s+\w+)*?)(?=\.|$| and | then )', cmd)
        if target_match:
            target_str = target_match.group(2).lower()
            if "warship" in target_str or "naval" in target_str:
                intent["target_asset"] = "warships"
            elif "air defense" in target_str or "ad" in target_str:
                intent["target_asset"] = "air_defense"
            elif "silo" in target_str or "missile" in target_str:
                intent["target_asset"] = "missile_silos"
            elif "infrastructure" in target_str:
                intent["target_asset"] = "infrastructure"
            elif "command" in target_str:
                intent["target_asset"] = "command_centers"
            elif "nuclear" in target_str:
                intent["target_asset"] = "nuclear_sites"
            else:
                intent["target_asset"] = "military"
        else:
            intent["target_asset"] = "military"
        return intent
    if re.search(r'\b(cruise missile|tomahawk|calibr)\b', cmd):
        intent = {"type": "missile", "missile_type": "cruise", "parser": "local"}
        target_match = re.search(r'\b(at|against|target)\s+(\w+(?:\s+\w+)*?)(?=\.|$| and | then )', cmd)
        if target_match:
            target_str = target_match.group(2).lower()
            if "air defense" in target_str:
                intent["target_asset"] = "air_defense"
            elif "warship" in target_str:
                intent["target_asset"] = "warships"
            else:
                intent["target_asset"] = "military"
        else:
            intent["target_asset"] = "military"
        return intent
    if re.search(r'\b(hypersonic missile|hypersonic|hyper missile)\b', cmd):
        return {"type": "missile", "missile_type": "hypersonic", "target_asset": "air_defense", "parser": "local"}
    if re.search(r'\b(anti-ship missile|ship missile|naval strike)\b', cmd):
        return {"type": "missile", "missile_type": "anti_ship", "target_asset": "warships", "parser": "local"}
    if re.search(r'\b(loitering munition|kamikaze drone|suicide drone|swarm drone)\b', cmd):
        return {"type": "loitering", "parser": "local"}
    if re.search(r'\b(warship|naval|deploy fleet|send ships|carrier)\b', cmd):
        return {"type": "naval", "parser": "local"}
    if re.search(r'\b(intelligence|gather intel|spy|recon satellite|sigint)\b', cmd):
        return {"type": "intel", "parser": "local"}
    if re.search(r'\b(electronic warfare|jam|ew|cyber attack)\b', cmd):
        return {"type": "ew", "parser": "local"}
    if re.search(r'\b(cyber warfare|cyber defense|hack|malware)\b', cmd):
        return {"type": "cyber", "parser": "local"}
    if re.search(r'\b(space asset|satellite|asat|space weapon)\b', cmd):
        return {"type": "space", "parser": "local"}
    if re.search(r'\b(psyops|psychological operation|influence|propaganda)\b', cmd):
        return {"type": "psyops", "parser": "local"}
    if re.search(r'\b(resupply|supply|reload)\b', cmd):
        return {"type": "resupply", "parser": "local"}
    if re.search(r'\b(produce|manufacture|build)\s+(missiles?|weapons?)\b', cmd):
        return {"type": "produce", "asset": "missiles", "parser": "local"}
    if re.search(r'\b(impose sanctions|sanction)\b', cmd):
        return {"type": "sanction", "parser": "local"}
    if re.search(r'\b(clear ieds|sweep|demining)\b', cmd):
        return {"type": "clear_ieds", "parser": "local"}
    if re.search(r'\b(nuclear strike|nuke|atomic bomb|thermonuclear)\b', cmd):
        return {"type": "nuke", "parser": "local"}
    if re.search(r'\b(roe|rules of engagement|set roe)\b', cmd):
        return {"type": "roe", "parser": "local"}

    return {"type": "unknown", "parser": "local"}

def llm_parse(command: str, provider: str, api_key: str) -> dict:
    if not api_key:
        return local_parse(command)
    config = PROVIDERS.get(provider, PROVIDERS["openrouter"])
    system_prompt = """You are a military tactics interpreter. Convert the player's command into JSON.
Possible types: "mortar", "fpv", "recon", "advance", "attack", "surrender", "info", "advise", "intel", "targets",
"missile", "naval", "intel", "ew", "nuke", "loitering", "cyber", "space", "psyops", "resupply", "produce", "sanction", "clear_ieds", "roe", "compound", "unknown", "overwatch", "heal", "interrogate", "supply".
For mortar: include "zone" and "quantity".
For fpv: include "target".
For advance/attack: include "zone".
For missile: include "missile_type" and "target_asset".
For overwatch: include "zone".
Output ONLY valid JSON."""
    try:
        if provider == "claude":
            headers = {"x-api-key": api_key, "anthropic-version": "2023-06-01", "content-type": "application/json"}
            payload = {"model": config["model"], "max_tokens": 150, "temperature": 0.2, "system": system_prompt, "messages": [{"role": "user", "content": command}]}
        else:
            headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
            payload = {"model": config["model"], "messages": [{"role": "system", "content": system_prompt}, {"role": "user", "content": command}], "temperature": 0.2, "max_tokens": 150}
        response = requests.post(config["url"], headers=headers, json=payload, timeout=5)
        if response.status_code == 200:
            data = response.json()
            if provider == "claude":
                content = data["content"][0]["text"]
            else:
                content = data["choices"][0]["message"]["content"]
            match = re.search(r'\{.*\}', content, re.DOTALL)
            if match:
                intent = json.loads(match.group(0))
                intent["parser"] = "llm"
                return intent
    except Exception:
        pass
    return local_parse(command)

def validate_intent(intent: dict) -> tuple[bool, str]:
    if not isinstance(intent, dict):
        return False, "Intent is not a dictionary."
    if "type" not in intent:
        return False, "Intent missing 'type' field."
    itype = intent["type"]
    if itype in ["compound", "info", "advise", "intel", "targets", "roe", "unknown", "overwatch", "heal", "interrogate", "supply"]:
        return True, ""
    if itype in ["mortar", "advance", "attack"]:
        if "zone" not in intent:
            return False, f"{itype} intent missing 'zone'."
        if intent["zone"] not in ["close", "medium", "long", "extreme"]:
            return False, f"Invalid zone '{intent['zone']}'. Must be close, medium, long, extreme."
    if itype == "mortar":
        if "quantity" not in intent or not isinstance(intent["quantity"], int) or intent["quantity"] < 1:
            return False, "Mortar intent missing valid 'quantity' (positive integer)."
    if itype == "fpv":
        if "target" not in intent:
            return False, "FPV intent missing 'target'."
    if itype == "missile":
        if "missile_type" not in intent or intent["missile_type"] not in ["ballistic", "cruise", "hypersonic", "anti_ship"]:
            return False, "Missile intent missing valid 'missile_type'."
        if "target_asset" not in intent:
            return False, "Missile intent missing 'target_asset'."
    return True, ""

def get_gm_narration(event_summary: str, state, provider: str, api_key: str) -> str:
    if not api_key or not event_summary:
        if "mortar" in event_summary.lower():
            return "The sky rips open as shells rain down."
        elif "fpv" in event_summary.lower():
            return "The buzzing predator dives. A sharp explosion echoes."
        elif "missile" in event_summary.lower():
            return "Missiles streak across the sky, impacting enemy positions."
        elif "nuke" in event_summary.lower():
            return "A blinding flash. The world holds its breath."
        else:
            return "The battlefield holds its breath."
    config = PROVIDERS.get(provider, PROVIDERS["openrouter"])
    prompt = f"""You are a gritty military narrator. Describe this event in ONE short, intense sentence (max 20 words).
Event: {event_summary[:200]}
Weather: {state.weather if hasattr(state, 'weather') else 'clear'}
Return ONLY the sentence, no quotes."""
    try:
        if provider == "claude":
            headers = {"x-api-key": api_key, "anthropic-version": "2023-06-01", "content-type": "application/json"}
            payload = {"model": config["model"], "max_tokens": 50, "temperature": 0.7, "system": "You are a military narrator.", "messages": [{"role": "user", "content": prompt}]}
        else:
            headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
            payload = {"model": config["model"], "messages": [{"role": "user", "content": prompt}], "temperature": 0.7, "max_tokens": 50}
        response = requests.post(config["url"], headers=headers, json=payload, timeout=4)
        if response.status_code == 200:
            data = response.json()
            if provider == "claude":
                return data["content"][0]["text"].strip().strip('"')
            else:
                return data["choices"][0]["message"]["content"].strip().strip('"')
    except:
        pass
    return "The battle continues."

def generate_llm_intel(state, api_key, provider="openrouter"):
    if not api_key:
        return None
    enemies = [u for u in state.units if u.side != state.player_side and u.is_alive()]
    if not enemies:
        return "No enemy forces detected."
    enemy_list = "\n".join([f"- {u.name} (morale {u.morale}, {u.zone} zone, status {u.status})" for u in enemies])
    player_zone = next((u.zone for u in state.units if u.side == state.player_side and u.is_alive()), "unknown")
    prompt = f"""You are a tactical intelligence analyst. Based on the following data, write a short, vivid paragraph (2-3 sentences) describing the enemy's current situation, posture, likely intentions, and any notable behavior.

Player position: {player_zone}
Weather: {state.weather}
Time of day: {state.time_of_day}
Objective zone: {state.objective_zone}
Recon active: {state.recon_active > 0}
Enemy units:
{enemy_list}

Write in present tense, military jargon allowed, no markdown."""
    try:
        url = "https://openrouter.ai/api/v1/chat/completions"
        headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
        payload = {
            "model": "openai/gpt-4o-mini",
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.7,
            "max_tokens": 150
        }
        resp = requests.post(url, json=payload, headers=headers, timeout=5)
        if resp.status_code == 200:
            return resp.json()["choices"][0]["message"]["content"].strip()
    except:
        pass
    return None

def parse_player_intent(command: str, provider: str, api_key: str) -> dict:
    return llm_parse(command, provider, api_key)

def generate_narrative(event_summary: str, state, provider: str, api_key: str) -> str:
    return get_gm_narration(event_summary, state, provider, api_key)
