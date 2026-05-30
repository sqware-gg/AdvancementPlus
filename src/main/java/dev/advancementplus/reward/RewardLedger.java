package dev.advancementplus.reward;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class RewardLedger {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration data;

    public RewardLedger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "reward-history.yml");
        reload();
    }

    public void reload() {
        data = YamlConfiguration.loadConfiguration(file);
    }

    public boolean hasClaimed(UUID playerId, String rewardId) {
        return data.contains(claimPath(playerId, rewardId));
    }

    public void markClaimed(UUID playerId, String rewardId, String advancementKey) {
        String path = claimPath(playerId, rewardId);
        data.set(path + ".reward-id", rewardId);
        data.set(path + ".advancement", advancementKey);
        data.set(path + ".claimed-at", Instant.now().toString());
        save();
    }

    public int clearClaims(UUID playerId, String target) {
        String playerPath = playerPath(playerId);
        ConfigurationSection playerSection = data.getConfigurationSection(playerPath);
        if (playerSection == null) {
            return 0;
        }

        if (target == null || target.isBlank() || "*".equals(target)) {
            int count = playerSection.getKeys(false).size();
            data.set(playerPath, null);
            save();
            return count;
        }

        int cleared = 0;
        for (String key : playerSection.getKeys(false)) {
            String path = playerPath + "." + key;
            String rewardId = data.getString(path + ".reward-id", "");
            String advancement = data.getString(path + ".advancement", "");
            if (target.equalsIgnoreCase(rewardId) || target.equalsIgnoreCase(advancement)) {
                data.set(path, null);
                cleared++;
            }
        }
        if (cleared > 0) {
            save();
        }
        return cleared;
    }

    public int claimCount() {
        ConfigurationSection claimed = data.getConfigurationSection("claimed");
        if (claimed == null) {
            return 0;
        }

        int count = 0;
        for (String playerId : claimed.getKeys(false)) {
            ConfigurationSection playerSection = claimed.getConfigurationSection(playerId);
            if (playerSection != null) {
                count += playerSection.getKeys(false).size();
            }
        }
        return count;
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save reward-history.yml: " + e.getMessage());
        }
    }

    private String claimPath(UUID playerId, String rewardId) {
        return playerPath(playerId) + "." + storageKey(rewardId);
    }

    private String playerPath(UUID playerId) {
        return "claimed." + playerId;
    }

    private String storageKey(String rewardId) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rewardId.getBytes(StandardCharsets.UTF_8));
    }
}
