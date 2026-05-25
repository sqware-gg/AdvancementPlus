package dev.advancementplus.config;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdvancementPlusConfig {
    private final JavaPlugin plugin;

    private GameruleSettings gamerule;
    private BroadcastSettings broadcast;
    private FilterSettings filter;
    private VisibilitySettings visibility;
    private ProgressSettings progress;
    private CompletionSettings completion;
    private FormatSettings format;
    private SoundSettings progressSound;
    private Map<String, SoundSettings> completionSounds;

    public AdvancementPlusConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();

        gamerule = new GameruleSettings(
                bool("gamerule.auto-disable-announce-advancements", true),
                bool("gamerule.suppress-vanilla-event-message", true)
        );

        broadcast = new BroadcastSettings(
                string("broadcast.audience", "all"),
                string("broadcast.permission", "advancementplus.see"),
                bool("broadcast.console", true)
        );

        filter = new FilterSettings(
                stringList("filter.include-namespaces", List.of("*")),
                stringList("filter.exclude-namespaces", List.of()),
                stringList("filter.include-advancements", List.of("*")),
                stringList("filter.exclude-advancements", List.of("minecraft:recipes/*"))
        );

        visibility = new VisibilitySettings(
                bool("visibility.include-hidden", true),
                bool("visibility.include-no-display", false),
                bool("visibility.include-root-advancements", false),
                bool("visibility.require-display-announces-to-chat-for-completion", false)
        );

        progress = new ProgressSettings(
                bool("progress.enabled", true),
                bool("progress.announce-single-criterion", false),
                bool("progress.announce-final-criterion", false),
                Math.max(0L, plugin.getConfig().getLong("progress.cooldown-millis", 500L))
        );

        completion = new CompletionSettings(
                bool("completion.enabled", true),
                Math.max(0L, plugin.getConfig().getLong("completion.cooldown-millis", 0L))
        );

        Map<String, String> completionTemplates = new HashMap<>();
        completionTemplates.put("task", string("format.completion-templates.task",
                "<dark_gray>[<green>Advancement</green>]</dark_gray> <white><player></white> <gray>made advancement</gray> <title>"));
        completionTemplates.put("goal", string("format.completion-templates.goal",
                "<dark_gray>[<aqua>Goal</aqua>]</dark_gray> <white><player></white> <gray>reached goal</gray> <title>"));
        completionTemplates.put("challenge", string("format.completion-templates.challenge",
                "<dark_gray>[<light_purple>Challenge</light_purple>]</dark_gray> <white><player></white> <gray>completed challenge</gray> <title>"));
        completionTemplates.put("no-display", string("format.completion-templates.no-display",
                "<dark_gray>[<gray>Advancement</gray>]</dark_gray> <white><player></white> <gray>completed</gray> <white><key></white>"));

        format = new FormatSettings(
                string("format.engine", "minimessage"),
                string("format.progress-template",
                        "<dark_gray>[<yellow>Advancement</yellow>]</dark_gray> <white><player></white> <gray>progressed</gray> <title> <dark_gray>(<completed>/<total> <bar>)</dark_gray>"),
                string("format.no-display-progress-template",
                        "<dark_gray>[<yellow>Advancement</yellow>]</dark_gray> <white><player></white> <gray>progressed</gray> <white><key></white> <dark_gray>(<completed>/<total>)</dark_gray>"),
                Map.copyOf(completionTemplates),
                bool("format.hover.enabled", true),
                string("format.hover.template",
                        "<title><newline><gray><description></gray><newline><dark_gray><key></dark_gray><newline><gray>Progress: <completed>/<total> (<percent>%)</gray><newline><gray>Criterion: <criterion></gray>"),
                Math.max(1, plugin.getConfig().getInt("format.progress-bar.width", 12)),
                string("format.progress-bar.filled", "#"),
                string("format.progress-bar.empty", "-")
        );

        progressSound = readSound("sound.progress", false, "entity.experience_orb.pickup", 0.35F, 1.4F);

        Map<String, SoundSettings> sounds = new HashMap<>();
        sounds.put("task", readSound("sound.completion.task", false, "ui.toast.in", 0.7F, 1.0F));
        sounds.put("goal", readSound("sound.completion.goal", false, "ui.toast.in", 0.8F, 1.1F));
        sounds.put("challenge", readSound("sound.completion.challenge", false, "ui.toast.challenge_complete", 0.9F, 1.0F));
        sounds.put("no-display", readSound("sound.completion.no-display", false, "ui.toast.in", 0.6F, 1.0F));
        completionSounds = Map.copyOf(sounds);
    }

    private SoundSettings readSound(String path, boolean enabled, String key, float volume, float pitch) {
        return new SoundSettings(
                bool(path + ".enabled", enabled),
                string(path + ".key", key),
                readCategory(path + ".category"),
                (float) plugin.getConfig().getDouble(path + ".volume", volume),
                (float) plugin.getConfig().getDouble(path + ".pitch", pitch)
        );
    }

    private SoundCategory readCategory(String path) {
        String value = string(path, "master").toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return SoundCategory.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning(path + " has invalid sound category '" + value + "'. Falling back to MASTER.");
            return SoundCategory.MASTER;
        }
    }

    private boolean bool(String path, boolean fallback) {
        return plugin.getConfig().getBoolean(path, fallback);
    }

    private String string(String path, String fallback) {
        FileConfiguration config = plugin.getConfig();
        String value = config.getString(path, fallback);
        if (value == null) {
            return fallback;
        }
        return value.trim();
    }

    private List<String> stringList(String path, List<String> fallback) {
        FileConfiguration config = plugin.getConfig();
        if (!config.isList(path)) {
            return fallback;
        }
        return config.getStringList(path).stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    public GameruleSettings gamerule() {
        return gamerule;
    }

    public BroadcastSettings broadcast() {
        return broadcast;
    }

    public FilterSettings filter() {
        return filter;
    }

    public VisibilitySettings visibility() {
        return visibility;
    }

    public ProgressSettings progress() {
        return progress;
    }

    public CompletionSettings completion() {
        return completion;
    }

    public FormatSettings format() {
        return format;
    }

    public SoundSettings progressSound() {
        return progressSound;
    }

    public SoundSettings completionSound(String frameKey) {
        return completionSounds.getOrDefault(frameKey, completionSounds.get("no-display"));
    }

    public record GameruleSettings(boolean autoDisableAnnounceAdvancements, boolean suppressVanillaMessage) {
    }

    public record BroadcastSettings(String audience, String permission, boolean console) {
    }

    public record FilterSettings(
            List<String> includeNamespaces,
            List<String> excludeNamespaces,
            List<String> includeAdvancements,
            List<String> excludeAdvancements
    ) {
    }

    public record VisibilitySettings(
            boolean includeHidden,
            boolean includeNoDisplay,
            boolean includeRootAdvancements,
            boolean requireDisplayAnnouncesToChatForCompletion
    ) {
    }

    public record ProgressSettings(
            boolean enabled,
            boolean announceSingleCriterion,
            boolean announceFinalCriterion,
            long cooldownMillis
    ) {
    }

    public record CompletionSettings(boolean enabled, long cooldownMillis) {
    }

    public record FormatSettings(
            String engine,
            String progressTemplate,
            String noDisplayProgressTemplate,
            Map<String, String> completionTemplates,
            boolean hoverEnabled,
            String hoverTemplate,
            int barWidth,
            String barFilled,
            String barEmpty
    ) {
        public String completionTemplate(String frameKey) {
            return completionTemplates.getOrDefault(frameKey, completionTemplates.get("no-display"));
        }
    }

    public record SoundSettings(boolean enabled, String key, SoundCategory category, float volume, float pitch) {
    }
}

