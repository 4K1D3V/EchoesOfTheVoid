package gg.kite.core.companions;

import gg.kite.core.MessageManager;
import gg.kite.core.companions.behaviors.ScoutBehavior;
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

public final class ShadowWisp implements VoidCompanion {
    private final Entity entity;
    private final Player owner;
    private volatile CompanionData data;
    private final ScoutBehavior behavior;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private int soundTicks;

    @Inject
    public ShadowWisp(Player owner, ConfigManager configManager, MessageManager messageManager) {
        this.owner = Objects.requireNonNull(owner);
        this.data = CompanionData.create("shadow_wisp", configManager.getInventorySize("shadow_wisp"));
        this.behavior = new ScoutBehavior(this);
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.entity = owner.getWorld().spawnEntity(owner.getLocation(), EntityType.valueOf(configManager.getEntityType("shadow_wisp")));
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
    public ScoutBehavior getBehavior() {
        return behavior;
    }

    @Override
    public void spawn(Location location) {
        entity.teleport(location);
        applyAttributes();
        entity.getWorld().playSound(location, Sound.valueOf(configManager.getSpawnSound("shadow_wisp")), 1.0F, 1.0F);
    }

    @Override
    public void despawn() {
        entity.getWorld().spawnParticle(Particle.valueOf(configManager.getDespawnParticle("shadow_wisp")), entity.getLocation(), 20, 0.5, 0.5, 0.5, 0);
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
        entity.getWorld().spawnParticle(data.tier() == 1 ? Particle.valueOf(configManager.getTickParticle("shadow_wisp")) : Particle.valueOf(configManager.getTickParticleEvolved("shadow_wisp")),
            entity.getLocation(), 5, 0.2, 0.2, 0.2, 0);
        if (soundTicks++ % configManager.getAmbientSoundInterval("shadow_wisp") == 0) {
            entity.getWorld().playSound(entity.getLocation(), Sound.valueOf(configManager.getAmbientSound("shadow_wisp")), 0.5F, 1.0F);
        }
        updateProgress();
    }

    private void applyAttributes() {
        entity.setCustomName(data.customName() != null ? data.customName() : "Shadow Wisp" + (data.corrupted() ? " (Corrupted)" : ""));
        entity.setCustomNameVisible(true);
        entity.setAI(false);
        entity.setMaxHealth(configManager.getMaxHealth("shadow_wisp"));
        entity.setHealth(entity.getMaxHealth());
        entity.setGlowing(true);
        entity.setGlowColor(data.color());
    }

    private void updateProgress() {
        int xp = data.xp() + 1;
        String path = data.skillPath() != null ? data.skillPath() : "scout";
        configManager.getSkills("shadow_wisp").ifPresent(skills -> {
            Map<Integer, ConfigManager.SkillConfig> skillPath = skills.getOrDefault(path, Map.of());
            int nextTier = data.tier() + 1;
            if (skillPath.containsKey(nextTier) && xp >= skillPath.get(nextTier).xp()) {
                data = data.withTier(nextTier);
                owner.sendMessage(messageManager.getMessage("en", "evolution", "{type}", "Shadow Wisp", "{tier}", String.valueOf(nextTier)));
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                entity.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, entity.getLocation(), 30, 0.5, 0.5, 0.5, 0);
                applySkillEffect(skillPath.get(nextTier).ability());
                applyBondEffects();
            } else {
                data = data.withXp(xp);
            }
        });
        if (Math.random() < configManager.getCorruptionChance("shadow_wisp")) {
            data = data.withCorrupted(true);
            owner.sendMessage(messageManager.getMessage("en", "corruption", "{type}", "Shadow Wisp"));
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.8F, 1.0F);
        }
    }

    private void applySkillEffect(String ability) {
        switch (ability) {
            case "ore_vision" -> owner.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 1));
            case "teleport_strike" -> entity.teleport(owner.getLocation().add(0, 1, 0));
            case "shadow_cloak" -> owner.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 1));
            case "backstab" -> entity.getNearbyEntities(3, 3, 3).stream()
                .filter(e -> e instanceof LivingEntity && !e.equals(owner))
                .forEach(e -> ((LivingEntity) e).damage(5.0));
        }
    }

    private void applyBondEffects() {
        configManager.getBondEffects("shadow_wisp").ifPresent(effects -> {
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