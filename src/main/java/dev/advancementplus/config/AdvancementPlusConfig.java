package dev.advancementplus.config;

import dev.advancementplus.reward.RewardEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
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
    private RewardSettings rewards;
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

        rewards = readRewards();

        Map<String, String> completionTemplates = new HashMap<>();
        completionTemplates.put("task", string("format.completion-templates.task",
                "<#2b98fd>AdvancementPlus</#2b98fd> <dark_gray>›</dark_gray> <white><player></white> <gray>made advancement</gray> <#8ecbff><title></#8ecbff>"));
        completionTemplates.put("goal", string("format.completion-templates.goal",
                "<#2b98fd>AdvancementPlus</#2b98fd> <dark_gray>›</dark_gray> <white><player></white> <gray>reached goal</gray> <#8ecbff><title></#8ecbff>"));
        completionTemplates.put("challenge", string("format.completion-templates.challenge",
                "<#2b98fd>AdvancementPlus</#2b98fd> <dark_gray>›</dark_gray> <white><player></white> <gray>completed challenge</gray> <#8ecbff><title></#8ecbff>"));
        completionTemplates.put("no-display", string("format.completion-templates.no-display",
                "<#2b98fd>AdvancementPlus</#2b98fd> <dark_gray>›</dark_gray> <white><player></white> <gray>completed</gray> <#8ecbff><key></#8ecbff>"));

        format = new FormatSettings(
                string("format.engine", "minimessage"),
                string("format.progress-template",
                        "<#2b98fd>AdvancementPlus</#2b98fd> <dark_gray>›</dark_gray> <white><player></white> <gray>progressed</gray> <#8ecbff><title></#8ecbff> <dark_gray>(<completed>/<total> <#2b98fd><bar></#2b98fd>)</dark_gray>"),
                string("format.no-display-progress-template",
                        "<#2b98fd>AdvancementPlus</#2b98fd> <dark_gray>›</dark_gray> <white><player></white> <gray>progressed</gray> <#8ecbff><key></#8ecbff> <dark_gray>(<completed>/<total>)</dark_gray>"),
                Map.copyOf(completionTemplates),
                bool("format.hover.enabled", true),
                string("format.hover.template",
                        "<#8ecbff><title></#8ecbff><newline><gray><description></gray><newline><dark_gray><key></dark_gray><newline><gray>Progress: <white><completed>/<total></white> <dark_gray>(<#2b98fd><percent>%</#2b98fd>)</dark_gray></gray><newline><gray>Criterion: <white><criterion></white></gray>"),
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

    private RewardSettings readRewards() {
        Map<String, RewardEntry> frameDefaults = new HashMap<>();
        frameDefaults.put("task", readRewardEntry("rewards.frame-defaults.task", false));
        frameDefaults.put("goal", readRewardEntry("rewards.frame-defaults.goal", false));
        frameDefaults.put("challenge", readRewardEntry("rewards.frame-defaults.challenge", false));
        frameDefaults.put("no-display", readRewardEntry("rewards.frame-defaults.no-display", false));

        Map<String, RewardEntry> advancementRewards = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("rewards.advancements");
        if (section != null) {
            for (Map.Entry<String, Object> configured : section.getValues(false).entrySet()) {
                String key = configured.getKey();
                RewardEntry entry = readRewardEntry(configured.getValue(), true);
                advancementRewards.put(key.toLowerCase(Locale.ROOT), entry);
            }
        }

        return new RewardSettings(
                bool("rewards.enabled", false),
                bool("rewards.first-time-only", true),
                bool("rewards.stack-frame-defaults", false),
                bool("rewards.log-executions", true),
                stringList("rewards.allowed-game-modes", List.of("SURVIVAL")),
                stringList("rewards.allowed-worlds", List.of()),
                stringList("rewards.blocked-worlds", List.of()),
                stringList("rewards.required-permissions", List.of()),
                stringList("rewards.excluded-permissions", List.of("advancementplus.reward.exempt")),
                stringList("rewards.include-namespaces", List.of("*")),
                stringList("rewards.exclude-namespaces", List.of()),
                stringList("rewards.include-advancements", List.of("*")),
                stringList("rewards.exclude-advancements", List.of(
                        "minecraft:recipes/*",
                        "minecraft:story/root",
                        "minecraft:nether/root",
                        "minecraft:end/root",
                        "minecraft:adventure/root",
                        "minecraft:husbandry/root"
                )),
                new RewardVisibilitySettings(
                        bool("rewards.visibility.include-hidden", true),
                        bool("rewards.visibility.include-no-display", false),
                        bool("rewards.visibility.include-root-advancements", false),
                        bool("rewards.visibility.require-display-announces-to-chat", false)
                ),
                Map.copyOf(frameDefaults),
                Map.copyOf(advancementRewards),
                readMilestones()
        );
    }

    private MilestoneSettings readMilestones() {
        RewardSelectionSettings defaultSelection = new RewardSelectionSettings(
                List.of("minecraft"),
                List.of(),
                List.of("*"),
                List.of(
                        "minecraft:recipes/*",
                        "minecraft:story/root",
                        "minecraft:nether/root",
                        "minecraft:end/root",
                        "minecraft:adventure/root",
                        "minecraft:husbandry/root"
                ),
                new RewardVisibilitySettings(true, false, false, false)
        );
        RewardSelectionSettings selection = readRewardSelection("rewards.milestones.selection", defaultSelection);

        return new MilestoneSettings(
                bool("rewards.milestones.enabled", true),
                selection,
                readThresholdRewards("rewards.milestones.completed-counts"),
                readFrameThresholdRewards("rewards.milestones.frame-counts"),
                readRewardEntryMap("rewards.milestones.tab-completions"),
                readRewardEntry("rewards.milestones.all-selected", false),
                readRewardEntryMap("rewards.milestones.all-frames"),
                readMilestoneGroups("rewards.milestones.groups", selection)
        );
    }

    private RewardSelectionSettings readRewardSelection(String path, RewardSelectionSettings fallback) {
        return new RewardSelectionSettings(
                stringList(path + ".include-namespaces", fallback.includeNamespaces()),
                stringList(path + ".exclude-namespaces", fallback.excludeNamespaces()),
                stringList(path + ".include-advancements", fallback.includeAdvancements()),
                stringList(path + ".exclude-advancements", fallback.excludeAdvancements()),
                new RewardVisibilitySettings(
                        bool(path + ".visibility.include-hidden", fallback.visibility().includeHidden()),
                        bool(path + ".visibility.include-no-display", fallback.visibility().includeNoDisplay()),
                        bool(path + ".visibility.include-root-advancements", fallback.visibility().includeRootAdvancements()),
                        bool(path + ".visibility.require-display-announces-to-chat", fallback.visibility().requireDisplayAnnouncesToChat())
                )
        );
    }

    private Map<Integer, RewardEntry> readThresholdRewards(String path) {
        Map<Integer, RewardEntry> rewards = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) {
            return Map.of();
        }

        for (Map.Entry<String, Object> configured : section.getValues(false).entrySet()) {
            try {
                int threshold = Integer.parseInt(configured.getKey());
                RewardEntry reward = readRewardEntry(configured.getValue(), true);
                rewards.put(threshold, reward);
            } catch (NumberFormatException ignored) {
                plugin.getLogger().warning(path + " has invalid threshold '" + configured.getKey() + "'. Skipping.");
            }
        }
        return Map.copyOf(rewards);
    }

    private Map<String, Map<Integer, RewardEntry>> readFrameThresholdRewards(String path) {
        Map<String, Map<Integer, RewardEntry>> rewards = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) {
            return Map.of();
        }

        for (String frame : section.getKeys(false)) {
            rewards.put(frame.toLowerCase(Locale.ROOT), readThresholdRewards(path + "." + frame));
        }
        return Map.copyOf(rewards);
    }

    private Map<String, RewardEntry> readRewardEntryMap(String path) {
        Map<String, RewardEntry> rewards = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) {
            return Map.of();
        }

        for (Map.Entry<String, Object> configured : section.getValues(false).entrySet()) {
            rewards.put(configured.getKey().toLowerCase(Locale.ROOT), readRewardEntry(configured.getValue(), true));
        }
        return Map.copyOf(rewards);
    }

    private Map<String, MilestoneGroup> readMilestoneGroups(String path, RewardSelectionSettings fallbackSelection) {
        Map<String, MilestoneGroup> groups = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) {
            return Map.of();
        }

        for (String groupName : section.getKeys(false)) {
            String groupPath = path + "." + groupName;
            RewardEntry reward = readRewardEntry(groupPath, true);
            RewardSelectionSettings selection = readRewardSelection(groupPath, fallbackSelection);
            groups.put(groupName.toLowerCase(Locale.ROOT), new MilestoneGroup(
                    selection,
                    Math.max(0, plugin.getConfig().getInt(groupPath + ".required-count", 0)),
                    reward
            ));
        }
        return Map.copyOf(groups);
    }

    private RewardEntry readRewardEntry(String path, boolean defaultEnabled) {
        FileConfiguration config = plugin.getConfig();
        if (config.isList(path)) {
            List<String> commands = stringList(path, List.of());
            return new RewardEntry(!commands.isEmpty(), "", commands);
        }

        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return new RewardEntry(false, "", List.of());
        }

        List<String> commands = stringList(path + ".commands", List.of());
        boolean enabled = bool(path + ".enabled", defaultEnabled && !commands.isEmpty());
        return new RewardEntry(enabled, string(path + ".reward-id", ""), commands);
    }

    private RewardEntry readRewardEntry(Object value, boolean defaultEnabled) {
        if (value instanceof List<?> values) {
            List<String> commands = values.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(command -> !command.isEmpty())
                    .toList();
            return new RewardEntry(!commands.isEmpty(), "", commands);
        }
        if (value instanceof ConfigurationSection section) {
            List<String> commands = section.getStringList("commands").stream()
                    .map(String::trim)
                    .filter(command -> !command.isEmpty())
                    .toList();
            boolean enabled = section.getBoolean("enabled", defaultEnabled && !commands.isEmpty());
            String rewardId = section.getString("reward-id", "");
            return new RewardEntry(enabled, rewardId == null ? "" : rewardId.trim(), commands);
        }
        return new RewardEntry(false, "", List.of());
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

    public RewardSettings rewards() {
        return rewards;
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

    public record RewardVisibilitySettings(
            boolean includeHidden,
            boolean includeNoDisplay,
            boolean includeRootAdvancements,
            boolean requireDisplayAnnouncesToChat
    ) {
    }

    public record RewardSettings(
            boolean enabled,
            boolean firstTimeOnly,
            boolean stackFrameDefaults,
            boolean logExecutions,
            List<String> allowedGameModes,
            List<String> allowedWorlds,
            List<String> blockedWorlds,
            List<String> requiredPermissions,
            List<String> excludedPermissions,
            List<String> includeNamespaces,
            List<String> excludeNamespaces,
            List<String> includeAdvancements,
            List<String> excludeAdvancements,
            RewardVisibilitySettings visibility,
            Map<String, RewardEntry> frameDefaults,
            Map<String, RewardEntry> advancements,
            MilestoneSettings milestones
    ) {
        public RewardEntry frameDefault(String frameKey) {
            return frameDefaults.getOrDefault(frameKey, new RewardEntry(false, "", List.of()));
        }

        public RewardEntry advancement(String key) {
            return advancements.get(key.toLowerCase(Locale.ROOT));
        }

        public boolean hasAdvancementOverride(String key) {
            return advancements.containsKey(key.toLowerCase(Locale.ROOT));
        }
    }

    public record RewardSelectionSettings(
            List<String> includeNamespaces,
            List<String> excludeNamespaces,
            List<String> includeAdvancements,
            List<String> excludeAdvancements,
            RewardVisibilitySettings visibility
    ) {
    }

    public record MilestoneSettings(
            boolean enabled,
            RewardSelectionSettings selection,
            Map<Integer, RewardEntry> completionCounts,
            Map<String, Map<Integer, RewardEntry>> frameCounts,
            Map<String, RewardEntry> tabCompletions,
            RewardEntry allSelected,
            Map<String, RewardEntry> allFrames,
            Map<String, MilestoneGroup> groups
    ) {
    }

    public record MilestoneGroup(
            RewardSelectionSettings selection,
            int requiredCount,
            RewardEntry reward
    ) {
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
