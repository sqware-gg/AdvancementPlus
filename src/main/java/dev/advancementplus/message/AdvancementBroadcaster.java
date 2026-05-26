package dev.advancementplus.message;

import dev.advancementplus.api.event.AdvancementPlusBroadcastEvent;
import dev.advancementplus.advancement.AdvancementContext;
import dev.advancementplus.advancement.AnnouncementKind;
import dev.advancementplus.config.AdvancementPlusConfig;
import dev.advancementplus.config.AdvancementPlusConfig.SoundSettings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdvancementBroadcaster {
    private final JavaPlugin plugin;
    private final AdvancementPlusConfig config;

    public AdvancementBroadcaster(JavaPlugin plugin, AdvancementPlusConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void broadcast(AdvancementContext context, Component message) {
        Collection<Player> recipients = recipients(context);
        for (Player recipient : recipients) {
            recipient.sendMessage(message);
            playSound(recipient, context);
        }

        if (config.broadcast().console()) {
            Bukkit.getConsoleSender().sendMessage(message);
        }

        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        Bukkit.getPluginManager().callEvent(new AdvancementPlusBroadcastEvent(
                context.player(),
                context.kind().name().toLowerCase(Locale.ROOT),
                context.key(),
                context.namespace(),
                context.path(),
                plain.serialize(context.title()),
                plain.serialize(context.description()),
                context.frameKey(),
                context.frameTitle(),
                context.isHidden(),
                context.doesAnnounceToChat(),
                context.criterion(),
                context.completedCriteria(),
                context.totalCriteria(),
                context.remainingCriteria(),
                context.percent(),
                plain.serialize(message)
        ));
    }

    private Collection<Player> recipients(AdvancementContext context) {
        String audience = config.broadcast().audience().toLowerCase(Locale.ROOT);
        if ("self".equals(audience)) {
            return List.of(context.player());
        }
        if ("world".equals(audience)) {
            return context.player().getWorld().getPlayers();
        }
        if ("permission".equals(audience)) {
            List<Player> permitted = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(config.broadcast().permission())) {
                    permitted.add(player);
                }
            }
            return permitted;
        }
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }

    private void playSound(Player recipient, AdvancementContext context) {
        SoundSettings sound = context.kind() == AnnouncementKind.PROGRESS
                ? config.progressSound()
                : config.completionSound(context.frameKey());
        if (!sound.enabled() || sound.key().isBlank()) {
            return;
        }

        try {
            recipient.playSound(recipient.getLocation(), sound.key(), sound.category(), sound.volume(), sound.pitch());
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Could not play sound '" + sound.key() + "': " + e.getMessage());
        }
    }
}
