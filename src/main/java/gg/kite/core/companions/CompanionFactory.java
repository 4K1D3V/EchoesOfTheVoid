package gg.kite.core.companions;

import gg.kite.core.MessageManager;
import gg.kite.core.config.ConfigManager;
import org.bukkit.entity.Player;

public final class CompanionFactory {
    private CompanionFactory() {}

    public static VoidCompanion create(String type, Player owner, ConfigManager configManager, MessageManager messageManager) {
        return switch (type.toLowerCase()) {
            case "shadow_wisp" -> new ShadowWisp(owner, configManager, messageManager);
            case "crimson_golem" -> new CrimsonGolem(owner, configManager, messageManager);
            case "aether_sprite" -> new AetherSprite(owner, configManager, messageManager);
            case "void_titan" -> new FusionCompanion(owner, configManager, messageManager, "void_titan");
            default -> throw new IllegalArgumentException("Unknown companion type: " + type);
        };
    }
}