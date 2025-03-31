package gg.kite.core.seasonal;

import gg.kite.core.EchoesPlugin;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class SeasonalManager {
    private final EchoesPlugin plugin;

    @Inject
    public SeasonalManager(EchoesPlugin plugin) {
        this.plugin = plugin;
    }

    public void tickSeasonalEvents() {
        ConfigurationSection events = plugin.getConfig().getConfigurationSection("seasonal_events");
        if (!events.getBoolean("enabled")) return;

        LocalDate now = LocalDate.now();
        events.getKeys(false).forEach(event -> {
            String start = events.getString(event + ".start");
            String end = events.getString(event + ".end");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            LocalDate startDate = LocalDate.parse(start, formatter);
            LocalDate endDate = LocalDate.parse(end, formatter);

            if (now.isAfter(startDate.minusDays(1)) && now.isBefore(endDate.plusDays(1))) {
                // Seasonal event logic here (e.g., spawn special companions, boost rift rates)
            }
        });
    }
}