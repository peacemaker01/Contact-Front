package com.contactfront.engine.ai;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitCategory;

import java.util.ArrayList;
import java.util.List;

public final class HtnPlanner {
    private HtnPlanner() {}

    public static List<String> decompose(Unit u, GameState s) {
        List<String> actions = new ArrayList<>();
        
        double strength = u.strength;
        boolean hasArmor = u.profile.category() == UnitCategory.ARMOR;
        boolean hasInfantry = u.profile.category() == UnitCategory.INFANTRY;
        
        if (!hasArmor && !hasInfantry) {
            return List.of("move_to_objective");
        }
        
        if (strength < 30) {
            actions.add("withdraw_from_threat");
            actions.add("seek_cover");
        }
        
        if (hasArmor) {
            actions.add("use_road_network");
            actions.add("maintain_formation");
            actions.add("flank_target");
        }
        
        if (hasInfantry) {
            actions.add("seek_cover");
            actions.add("coordinate_with_nearby");
            actions.add("harass_approach");
        }
        
        actions.add("engage_visible_target");
        
        return actions;
    }
}