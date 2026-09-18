package com.cuac_xd.zeneconomy.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageManager {

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> messages = new ConcurrentHashMap<>();
    private String prefix = "";

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        messages.clear();
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("messages.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (Exception ignored) {}
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        prefix = config.getString("prefix", "");

        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messages.put(key, config.getString(key, ""));
            }
        }
    }

    public String getRaw(String key) {
        return messages.getOrDefault(key, "<red>Missing message: " + key + "</red>");
    }

    public Component parse(String text, TagResolver... tagResolvers) {
        return miniMessage.deserialize(text, tagResolvers);
    }

    public Component get(String key, TagResolver... tagResolvers) {
        String raw = getRaw(key);
        List<TagResolver> resolvers = new ArrayList<>(List.of(tagResolvers));
        resolvers.add(Placeholder.parsed("prefix", prefix));
        return miniMessage.deserialize(prefix + raw, resolvers.toArray(new TagResolver[0]));
    }

    public Component getWithoutPrefix(String key, TagResolver... tagResolvers) {
        String raw = getRaw(key);
        return miniMessage.deserialize(raw, tagResolvers);
    }

    public void sendMessage(Audience audience, String key, TagResolver... tagResolvers) {
        audience.sendMessage(get(key, tagResolvers));
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
}
