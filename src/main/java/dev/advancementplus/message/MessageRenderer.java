package dev.advancementplus.message;

import dev.advancementplus.advancement.AdvancementContext;
import dev.advancementplus.advancement.AnnouncementKind;
import dev.advancementplus.config.AdvancementPlusConfig;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageRenderer {
    private final JavaPlugin plugin;
    private final AdvancementPlusConfig config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private final Set<String> warnedTemplates = new HashSet<>();

    public MessageRenderer(JavaPlugin plugin, AdvancementPlusConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public Component render(AdvancementContext context) {
        String template = templateFor(context);
        Component message = renderTemplate(template, context);
        if (!config.format().hoverEnabled()) {
            return message;
        }

        Component hover = renderTemplate(config.format().hoverTemplate(), context);
        return message.hoverEvent(HoverEvent.showText(hover));
    }

    private String templateFor(AdvancementContext context) {
        if (context.kind() == AnnouncementKind.PROGRESS) {
            return context.hasDisplay()
                    ? config.format().progressTemplate()
                    : config.format().noDisplayProgressTemplate();
        }
        return config.format().completionTemplate(context.frameKey());
    }

    private Component renderTemplate(String template, AdvancementContext context) {
        if ("legacy".equalsIgnoreCase(config.format().engine())) {
            return legacy.deserialize(applyLegacyPlaceholders(template, context));
        }

        try {
            return miniMessage.deserialize(template, resolver(context));
        } catch (RuntimeException e) {
            warnInvalidTemplate(template, e);
            return Component.text(applyLegacyPlaceholders(template, context));
        }
    }

    private TagResolver resolver(AdvancementContext context) {
        return TagResolver.resolver(
                Placeholder.component("player", context.player().displayName()),
                Placeholder.component("title", context.title()),
                Placeholder.component("description", context.description()),
                Placeholder.unparsed("display_name", plain.serialize(context.player().displayName())),
                Placeholder.unparsed("world", context.player().getWorld().getName()),
                Placeholder.unparsed("key", context.key()),
                Placeholder.unparsed("namespace", context.namespace()),
                Placeholder.unparsed("path", context.path()),
                Placeholder.unparsed("frame", context.frameKey()),
                Placeholder.unparsed("frame_title", context.frameTitle()),
                Placeholder.unparsed("criterion", context.criterion()),
                Placeholder.unparsed("completed", Integer.toString(context.completedCriteria())),
                Placeholder.unparsed("total", Integer.toString(context.totalCriteria())),
                Placeholder.unparsed("remaining", Integer.toString(context.remainingCriteria())),
                Placeholder.unparsed("percent", Integer.toString(context.percent())),
                Placeholder.unparsed("bar", progressBar(context))
        );
    }

    private String applyLegacyPlaceholders(String template, AdvancementContext context) {
        return template
                .replace("{player}", plain.serialize(context.player().displayName()))
                .replace("{display_name}", plain.serialize(context.player().displayName()))
                .replace("{world}", context.player().getWorld().getName())
                .replace("{title}", plain.serialize(context.title()))
                .replace("{description}", plain.serialize(context.description()))
                .replace("{key}", context.key())
                .replace("{namespace}", context.namespace())
                .replace("{path}", context.path())
                .replace("{frame}", context.frameKey())
                .replace("{frame_title}", context.frameTitle())
                .replace("{criterion}", context.criterion())
                .replace("{completed}", Integer.toString(context.completedCriteria()))
                .replace("{total}", Integer.toString(context.totalCriteria()))
                .replace("{remaining}", Integer.toString(context.remainingCriteria()))
                .replace("{percent}", Integer.toString(context.percent()))
                .replace("{bar}", progressBar(context));
    }

    private String progressBar(AdvancementContext context) {
        int width = config.format().barWidth();
        int filled = Math.round((context.percent() / 100.0F) * width);
        filled = Math.max(0, Math.min(width, filled));
        String filledPart = config.format().barFilled().repeat(filled);
        String emptyPart = config.format().barEmpty().repeat(width - filled);
        return filledPart + emptyPart;
    }

    private void warnInvalidTemplate(String template, RuntimeException e) {
        String key = template.toLowerCase(Locale.ROOT);
        if (warnedTemplates.add(key)) {
            plugin.getLogger().warning("Invalid MiniMessage template in config: " + template);
            plugin.getLogger().warning("MiniMessage error: " + e.getMessage());
        }
    }
}

