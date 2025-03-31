package gg.kite.core.companions;

import gg.kite.core.MessageManager;
import gg.kite.core.companions.behaviors.TankBehavior;
import gg.kite.core.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import java.util.Objects;

public final class CrimsonGolem implements VoidCompanion {
    private final Entity entity;
    private final Player owner;
    private volatile CompanionData data;
    private final TankBehavior behavior;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private int soundTicks;

    @Inject
    public CrimsonGolem(Player owner, ConfigManager configManager, MessageManager messageManager) {
        this.owner = Objects.requireNonNull(owner);
        this.data = CompanionData.create("crimson_golem", configManager.getInventorySize("crimson_golem"));
        this.behavior = new TankBehavior(this);
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.entity = owner.getWorld().spawnEntity(owner.getLocation(), EntityType.valueOf(configManager.getEntityType("crimson_golem")));
        this.soundTicks = 0;
        applyAttributes();
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    @Override
    public Player getOwner() {
        return owner;
    }

    @Override
    public CompanionData getData() {
        return data;
    }

    @Override
    public TankBehavior getBehavior() {
        return behavior;
    }

    @Override
    public void spawn(Location location) {
        entity.teleport(location);
        applyAttributes();
        entity.getWorld().playSound(location, Sound.valueOf(configManager.getSpawnSound("crimson_golem")), 1.0F, 1.0F);
    }

    @Override
    public void despawn() {
        entity.getWorld().spawnParticle(Particle.valueOf(configManager.getDespawnParticle("crimson_golem")), entity.getLocation(), 20, 0.5, 0.5, 0.5, 0);
        entity.remove();
    }

    @Override
    public void tick() {
        if (!entity.isValid() || owner == null || !owner.isOnline()) {
            despawn();
            return;
        }
        if (data.corrupted()) {
            entity.teleport(entity.getLocation().add(Math.random() - 0.5, 0, Math.random() - 0.5));
            entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation(), 10, 0.3, 0.3, 0.3, 0);
            return;
        }
        behavior.execute();
        entity.getWorld().spawnParticle(data.tier() == 1 ? Particle.valueOf(configManager.getTickParticle("crimson_golem")) : Particle.valueOf(configManager.getTickParticleEvolved("crimson_golem")),
            entity.getLocation(), 5, 0.2, 0.2, 0.2, 0);
        if (soundTicks++ % configManager.getAmbientSoundInterval("crimson_golem") == 0) {
            entity.getWorld().playSound(entity.getLocation(), Sound.valueOf(configManager.getAmbientSound("crimson_golem")), 0.5F, 1.0F);
        }
        updateProgress();
    }

    private void applyAttributes() {
        entity.setCustomName(data.customName() != null ? data.customName() : "Crimson Golem" + (data.corrupted() ? " (Corrupted)" : ""));
        entity.setCustomNameVisible(true);
        entity.setAI(false);
        entity.setMaxHealth(configManager.getMaxHealth("crimson_golem"));
        entity.setHealth(entity.getMaxHealth());
        entity.setGlowing(true);
        entity.setGlowColor(data.color());
    }

    private void updateProgress() {
        int xp = data.xp() + 1;
        String path = data.skillPath() != null ? data.skillPath() : "tank";
        configManager.getSkills("crimson_golem").ifPresent(skills -> {
            Map<Integer, ConfigManager.SkillConfig> skillPath = skills.getOrDefault(path, Map.of());
            int nextTier = data.tier() + 1;
            if (skillPath.containsKey(nextTier) && xp >= skillPath.get(nextTier).xp()) {
                data = data.withTier(nextTier);
                owner.sendMessage(messageManager.getMessage("en", "evolution", "{type}", "Crimson Golem", "{tier}", String.valueOf(nextTier)));
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                entity.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, entity.getLocation(), 30, 0.5, 0.5, 0.5, 0);
                applySkillEffect(skillPath.get(nextTier).ability());
                applyBondEffects();
            } else {
                data = data.withXp(xp);
            }
        });
        if (Math.random() < configManager.getCorruptionChance("crimson_golem")) {
            data = data.withCorrupted(true);
            owner.sendMessage(messageManager.getMessage("en", "corruption", "{type}", "Crimson Golem"));
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.8F, 1.0F);
        }
    }

    private void applySkillEffect(String ability) {
        switch (ability) {
            case "taunt" -> entity.getNearbyEntities(5, 5, 5).stream()
                .filter(e -> e instanceof LivingEntity && !e.equals(owner))
                .forEach(e -> ((LivingEntity) e).setTarget((LivingEntity) entity));
            case "ground_slam" -> entity.getNearbyEntities(5, 5, 5).stream()
                .filter(e -> e instanceof LivingEntity && !e.equals(owner))
                .forEach(e -> ((LivingEntity) e).damage(6.0));
            case "shield" -> owner.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 1));
            case "fortify" -> owner.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
        }
    }

    private void applyBondEffects() {
        configManager.getBondEffects("crimson_golem").ifPresent(effects -> {
            effects.entrySet().stream()
                .filter(e -> data.bondLevel() >= e.getKey())
                .map(Map.Entry::getValue)
                .forEach(effect -> owner.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByName(effect.effect()),
                    effect.duration(),
                    effect.amplifier(),
                    false,
                    false
                )));
        });
    }
}