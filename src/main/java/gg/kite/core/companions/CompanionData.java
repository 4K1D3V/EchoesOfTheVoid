package gg.kite.core.companions;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.inventory.Inventory;

public record CompanionData(String type, int tier, int xp, boolean corrupted, int bondLevel, String customName, DyeColor color, String skillPath, Inventory inventory) {
    public static CompanionData create(String type, int inventorySize) {
        return new CompanionData(type, 1, 0, false, 0, null, DyeColor.WHITE, null, Bukkit.createInventory(null, inventorySize, type + " Inventory"));
    }

    public CompanionData withTier(int tier) {
        return new CompanionData(type, tier, xp, corrupted, bondLevel, customName, color, skillPath, inventory);
    }

    public CompanionData withXp(int xp) {
        return new CompanionData(type, tier, xp, corrupted, bondLevel, customName, color, skillPath, inventory);
    }

    public CompanionData withCorrupted(boolean corrupted) {
        return new CompanionData(type, tier, xp, corrupted, bondLevel, customName, color, skillPath, inventory);
    }

    public CompanionData withBond(int bondLevel) {
        return new CompanionData(type, tier, xp, corrupted, bondLevel, customName, color, skillPath, inventory);
    }

    public CompanionData withName(String customName) {
        return new CompanionData(type, tier, xp, corrupted, bondLevel, customName, color, skillPath, inventory);
    }

    public CompanionData withColor(DyeColor color) {
        return new CompanionData(type, tier, xp, corrupted, bondLevel, customName, color, skillPath, inventory);
    }

    public CompanionData withSkillPath(String skillPath) {
        return new CompanionData(type, tier, xp, corrupted, bondLevel, customName, color, skillPath, inventory);
    }
}