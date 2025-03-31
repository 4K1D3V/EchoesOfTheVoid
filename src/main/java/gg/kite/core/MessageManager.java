package gg.kite.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class MessageManager {
    private final EchoesPlugin plugin;
    private final Map<String, FileConfiguration> messages = new HashMap<>();

    @Inject
    public MessageManager(EchoesPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public String getMessage(String language, String key, String... placeholders) {
        FileConfiguration config = messages.getOrDefault(language, messages.get(plugin.getConfig().getString("general.default_language", "en")));
        String message = config.getString(key, "&cMissing message: " + key);
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return message.replace('&', '§');
    }

    private void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        messages.put("en", YamlConfiguration.loadConfiguration(file));
    }
}