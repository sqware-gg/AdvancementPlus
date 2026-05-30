package dev.advancementplus;

import dev.advancementplus.advancement.AdvancementFilter;
import dev.advancementplus.advancement.AdvancementListener;
import dev.advancementplus.command.AdvancementPlusCommand;
import dev.advancementplus.config.AdvancementPlusConfig;
import dev.advancementplus.config.ConfigReferenceWriter;
import dev.advancementplus.message.AdvancementBroadcaster;
import dev.advancementplus.message.MessageRenderer;
import dev.advancementplus.reward.RewardService;
import java.util.Iterator;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdvancementPlusPlugin extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 31600;

    private AdvancementPlusConfig advancementConfig;
    private AdvancementFilter advancementFilter;
    private MessageRenderer messageRenderer;
    private AdvancementBroadcaster broadcaster;
    private RewardService rewardService;

    @Override
    public void onEnable() {
        new Metrics(this, BSTATS_PLUGIN_ID);
        ConfigReferenceWriter.saveDefaultAndReferenceIfNeeded(this);

        advancementConfig = new AdvancementPlusConfig(this);
        rebuildServices();

        AdvancementListener listener = new AdvancementListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        AdvancementPlusCommand command = new AdvancementPlusCommand(this);
        PluginCommand pluginCommand = getCommand("advancementplus");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        applyAnnouncementGameruleToLoadedWorlds();
        getLogger().info("Loaded " + loadedAdvancementCount() + " advancements. Custom advancement messages are active.");
    }

    public void reloadPlugin() {
        ConfigReferenceWriter.saveDefaultAndReferenceIfNeeded(this);
        advancementConfig.reload();
        rebuildServices();
        applyAnnouncementGameruleToLoadedWorlds();
    }

    private void rebuildServices() {
        advancementFilter = new AdvancementFilter(advancementConfig);
        messageRenderer = new MessageRenderer(this, advancementConfig);
        broadcaster = new AdvancementBroadcaster(this, advancementConfig);
        rewardService = new RewardService(this, advancementConfig);
    }

    public void applyAnnouncementGameruleToLoadedWorlds() {
        if (!advancementConfig.gamerule().autoDisableAnnounceAdvancements()) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            disableAnnouncementGamerule(world);
        }
    }

    public void disableAnnouncementGamerule(World world) {
        if (!advancementConfig.gamerule().autoDisableAnnounceAdvancements()) {
            return;
        }
        Boolean currentValue = world.getGameRuleValue(GameRules.SHOW_ADVANCEMENT_MESSAGES);
        if (Boolean.TRUE.equals(currentValue)) {
            world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
            getLogger().info("Set showAdvancementMessages=false in world '" + world.getName() + "'.");
        }
    }

    public int loadedAdvancementCount() {
        int count = 0;
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    public AdvancementPlusConfig advancementConfig() {
        return advancementConfig;
    }

    public AdvancementFilter advancementFilter() {
        return advancementFilter;
    }

    public MessageRenderer messageRenderer() {
        return messageRenderer;
    }

    public AdvancementBroadcaster broadcaster() {
        return broadcaster;
    }

    public RewardService rewardService() {
        return rewardService;
    }
}
