package gg.kite.core.config;

import gg.kite.core.EchoesPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

public final class ConfigManager {
    private final EchoesPlugin plugin;

    @Inject
    public ConfigManager(EchoesPlugin plugin) {
        this.plugin = plugin;
    }

    public String getEntityType(String type) {
        return plugin.getConfig().getString("companions." + type + ".entity_type", "VEX");
    }

    public double getMaxHealth(String type) {
        return plugin.getConfig().getDouble("companions." + type + ".max_health", 10.0);
    }

    public double getCorruptionChance(String type) {
        return plugin.getConfig().getDouble("companions." + type + ".corruption_chance", 0.01);
    }

    public double getSpeed(String type) {
        return plugin.getConfig().getDouble("companions." + type + ".speed", 0.5);
    }

    public String getSpawnSound(String type) {
        return plugin.getConfig().getString("companions." + type + ".spawn_sound", "ENTITY_WITHER_SPAWN");
    }

    public String getDespawnParticle(String type) {
        return plugin.getConfig().getString("companions." + type + ".despawn_particle", "SMOKE_LARGE");
    }

    public String getTickParticle(String type) {
        return plugin.getConfig().getString("companions." + type + ".tick_particle", "SMOKE_NORMAL");
    }

    public String getTickParticleEvolved(String type) {
        return plugin.getConfig().getString("companions." + type + ".tick_particle_evolved", "SOUL_FIRE_FLAME");
    }

    public String getAmbientSound(String type) {
        return plugin.getConfig().getString("companions." + type + ".ambient_sound", "ENTITY_VEX_AMBIENT");
    }

    public int getAmbientSoundInterval(String type) {
        return plugin.getConfig().getInt("companions." + type + ".ambient_sound_interval", 100);
    }

    public int getInventorySize(String type) {
        return plugin.getConfig().getInt("companions." + type + ".inventory_size", 9);
    }

    public Optional<Map<String, Map<Integer, SkillConfig>>> getSkills(String type) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("companions." + type + ".skills");
        if (section == null) return Optional.empty();
        Map<String, Map<Integer, SkillConfig>> skills = section.getKeys(false).stream()
            .collect(Collectors.toMap(
                path -> path,
                path -> section.getConfigurationSection(path).getKeys(false).stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toMap(
                        tier -> tier,
                        tier -> new SkillConfig(
                            section.getInt(path + "." + tier + ".xp"),
                            section.getString(path + "." + tier + ".ability")
                        )
                    ))
            ));
        return Optional.of(skills);
    }

    public Optional<Map<Integer, BondEffect>> getBondEffects(String type) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("companions." + type + ".bond_effects");
        if (section == null) return Optional.empty();
        Map<Integer, BondEffect> effects = section.getKeys(false).stream()
            .map(Integer::parseInt)
            .collect(Collectors.toMap(
                level -> level,
                level -> new BondEffect(
                    section.getString(level + ".effect"),
                    section.getInt(level + ".duration"),
                    section.getInt(level + ".amplifier")
                )
            ));
        return Optional.of(effects);
    }

    public record SkillConfig(int xp, String ability) {}
    public record BondEffect(String effect, int duration, int amplifier) {}
}