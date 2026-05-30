package dev.advancementplus.advancement;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import dev.advancementplus.AdvancementPlusPlugin;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.world.WorldLoadEvent;

public final class AdvancementListener implements Listener {
    private final AdvancementPlusPlugin plugin;
    private final Map<String, Long> progressCooldowns = new HashMap<>();
    private final Map<String, Long> completionCooldowns = new HashMap<>();

    public AdvancementListener(AdvancementPlusPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void suppressVanillaCompletionMessage(PlayerAdvancementDoneEvent event) {
        if (plugin.advancementConfig().gamerule().suppressVanillaMessage()) {
            event.message(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCriterionGranted(PlayerAdvancementCriterionGrantEvent event) {
        AdvancementProgress progress = event.getAdvancementProgress();
        AdvancementContext context = AdvancementContext.create(
                AnnouncementKind.PROGRESS,
                event.getPlayer(),
                event.getAdvancement(),
                progress,
                event.getCriterion()
        );

        if (!plugin.advancementFilter().shouldBroadcast(context)) {
            return;
        }
        if (isCoolingDown(progressCooldowns, context, plugin.advancementConfig().progress().cooldownMillis())) {
            return;
        }

        plugin.broadcaster().broadcast(context, plugin.messageRenderer().render(context));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        AdvancementProgress progress = event.getPlayer().getAdvancementProgress(event.getAdvancement());
        AdvancementContext context = AdvancementContext.create(
                AnnouncementKind.COMPLETION,
                event.getPlayer(),
                event.getAdvancement(),
                progress,
                ""
        );

        if (plugin.advancementFilter().shouldBroadcast(context)
                && !isCoolingDown(completionCooldowns, context, plugin.advancementConfig().completion().cooldownMillis())) {
            plugin.broadcaster().broadcast(context, plugin.messageRenderer().render(context));
        }

        plugin.rewardService().executeCompletionRewards(context);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.disableAnnouncementGamerule(event.getWorld());
    }

    private boolean isCoolingDown(Map<String, Long> cooldowns, AdvancementContext context, long cooldownMillis) {
        if (cooldownMillis <= 0L) {
            return false;
        }

        long now = System.currentTimeMillis();
        String key = context.player().getUniqueId() + "|" + context.kind() + "|" + context.key();
        Long lastSeen = cooldowns.get(key);
        cooldowns.put(key, now);
        return lastSeen != null && now - lastSeen < cooldownMillis;
    }
}
