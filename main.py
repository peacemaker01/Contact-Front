#!/usr/bin/env python3
import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from hud import clear_screen, ANSI
from factions import FACTIONS
from tactical.tactical_game import TacticalGame
from strategic.strategic_game import StrategicGame

SPLASH = f"""
{ANSI['TITLE']}
 ██████╗ ██████╗ ███╗   ██╗████████╗ █████╗  ██████╗████████╗
██╔════╝██╔═══██╗████╗  ██║╚══██╔══╝██╔══██╗██╔════╝╚══██╔══╝
██║     ██║   ██║██╔██╗ ██║   ██║   ███████║██║        ██║   
██║     ██║   ██║██║╚██╗██║   ██║   ██╔══██║██║        ██║   
╚██████╗╚██████╔╝██║ ╚████║   ██║   ██║  ██║╚██████╗   ██║   
 ╚═════╝ ╚═════╝ ╚═╝  ╚═══╝   ╚═╝   ╚═╝  ╚═╝ ╚═════╝   ╚═╝   
{ANSI['RESET']}
          ⚔️  LLM-DRIVEN ASYMMETRIC WARGAME  ⚔️
               CONTACT FRONT v1.6
"""

def select_faction():
    print("\nSelect your faction:")
    factions = list(FACTIONS.keys())
    for i, f in enumerate(factions, 1):
        print(f"{i}. {FACTIONS[f]['name']}")
    choice = input("> ")
    try:
        return factions[int(choice)-1]
    except:
        return "USA"

def select_mode():
    print("\nSelect mode:\n1. TACTICAL (company-level)\n2. STRATEGIC (theater-level)")
    choice = input("> ")
    return "TACTICAL" if choice == "1" else "STRATEGIC"

def select_submode():
    print("\nTactical role:\n1. ATTACKER\n2. DEFENDER")
    choice = input("> ")
    return "attacker" if choice == "1" else "defender"

def select_difficulty():
    print("\nDifficulty:\n1. EASY\n2. NORMAL\n3. HARD")
    choice = input("> ")
    return ["easy","normal","hard"][int(choice)-1] if choice in "123" else "normal"

def select_opponent(exclude):
    factions = [f for f in FACTIONS.keys() if f != exclude]
    print("Select opponent:")
    for i, f in enumerate(factions,1):
        print(f"{i}. {FACTIONS[f]['name']}")
    choice = input("> ")
    try:
        return factions[int(choice)-1]
    except:
        return factions[0]

def main():
    clear_screen()
    print(SPLASH)
    input("Press Enter to begin...")
    faction = select_faction()
    mode = select_mode()
    if mode == "TACTICAL":
        submode = select_submode()
        difficulty = select_difficulty()
        game = TacticalGame(faction, submode, difficulty)
        game.run()
    else:
        opponent = select_opponent(faction)
        game = StrategicGame(faction, opponent)
        game.run()

if __name__ == "__main__":
    main()
