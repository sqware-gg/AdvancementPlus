package dev.advancementplus.reward;

import dev.advancementplus.advancement.AdvancementContext;
import dev.advancementplus.config.AdvancementPlusConfig;
import dev.advancementplus.config.AdvancementPlusConfig.MilestoneGroup;
import dev.advancementplus.config.AdvancementPlusConfig.MilestoneSettings;
import dev.advancementplus.config.AdvancementPlusConfig.RewardSelectionSettings;
import dev.advancementplus.config.AdvancementPlusConfig.RewardSettings;
import io.papermc.paper.advancement.AdvancementDisplay;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RewardService {
    private final JavaPlugin plugin;
    private final AdvancementPlusConfig config;
    private final RewardLedger ledger;

    public RewardService(JavaPlugin plugin, AdvancementPlusConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.ledger = new RewardLedger(plugin);
    }

    public void executeCompletionRewards(AdvancementContext context) {
        RewardInspection completionInspection = inspect(context);
        if (completionInspection.eligible()) {
            for (ResolvedReward reward : completionInspection.payableRewards()) {
                dispatchAndRecord(context, reward);
            }
        }

        MilestoneInspection milestoneInspection = inspectMilestones(context);
        if (milestoneInspection.eligible()) {
            for (ResolvedReward reward : milestoneInspection.payableRewards()) {
                dispatchAndRecord(context, reward);
            }
        }
    }

    public RewardInspection inspect(AdvancementContext context) {
        List<ResolvedReward> configuredRewards = configuredRewards(context.advancement());
        String blockReason = blockReason(context, configuredRewards);
        if (blockReason != null) {
            return new RewardInspection(false, blockReason, configuredRewards, List.of(), 0);
        }
        return payableInspection(configuredRewards, context.player(), "all configured rewards already claimed");
    }

    public MilestoneInspection inspectMilestones(AdvancementContext context) {
        MilestoneSnapshot snapshot = milestoneSnapshot(context.player());
        List<ResolvedReward> configuredRewards = configuredMilestoneRewards(snapshot, context.player());
        String blockReason = milestoneBlockReason(context, configuredRewards);
        if (blockReason != null) {
            return new MilestoneInspection(false, blockReason, snapshot, configuredRewards, List.of(), 0);
        }

        RewardInspection payable = payableInspection(configuredRewards, context.player(), "all configured milestones already claimed");
        return new MilestoneInspection(
                payable.eligible(),
                payable.reason(),
                snapshot,
                configuredRewards,
                payable.payableRewards(),
                payable.alreadyClaimed()
        );
    }

    public List<ResolvedReward> configuredRewards(Advancement advancement) {
        RewardSettings settings = config.rewards();
        String key = advancement.getKey().toString().toLowerCase(Locale.ROOT);
        String frameKey = frameKey(advancement);
        RewardEntry frameDefault = settings.frameDefault(frameKey);
        boolean hasOverride = settings.hasAdvancementOverride(key);

        List<ResolvedReward> rewards = new ArrayList<>();
        if (hasOverride) {
            RewardEntry exact = settings.advancement(key);
            if (exact != null && exact.active()) {
                rewards.add(new ResolvedReward("advancement", ledgerId("advancement:" + key, exact), exact));
                if (settings.stackFrameDefaults() && frameDefault.active()) {
                    rewards.add(new ResolvedReward("frame:" + frameKey, ledgerId("frame:" + frameKey + ":" + key, frameDefault), frameDefault));
                }
            }
            return List.copyOf(rewards);
        }

        if (frameDefault.active()) {
            rewards.add(new ResolvedReward("frame:" + frameKey, ledgerId("frame:" + frameKey + ":" + key, frameDefault), frameDefault));
        }
        return List.copyOf(rewards);
    }

    public MilestoneSnapshot milestoneSnapshot(Player player) {
        MilestoneSettings settings = config.rewards().milestones();
        List<Advancement> selected = advancements().stream()
                .filter(advancement -> matchesSelection(advancement, settings.selection()))
                .toList();

        int completed = 0;
        Map<String, MutableProgress> frameProgress = new HashMap<>();
        Map<String, MutableProgress> tabProgress = new HashMap<>();
        for (Advancement advancement : selected) {
            boolean done = player.getAdvancementProgress(advancement).isDone();
            if (done) {
                completed++;
            }

            frameProgress.computeIfAbsent(frameKey(advancement), MutableProgress::new).add(done);
            tabProgress.computeIfAbsent(advancement.getRoot().getKey().toString().toLowerCase(Locale.ROOT), MutableProgress::new).add(done);
        }

        return new MilestoneSnapshot(
                completed,
                selected.size(),
                progressMap(frameProgress),
                progressMap(tabProgress)
        );
    }

    public int claimCount() {
        return ledger.claimCount();
    }

    public int clearClaims(java.util.UUID playerId, String target) {
        return ledger.clearClaims(playerId, target);
    }

    public int milestoneCommandCount() {
        MilestoneSettings milestones = config.rewards().milestones();
        int total = rewardCommandCount(milestones.allSelected());
        total += milestones.completionCounts().values().stream().mapToInt(this::rewardCommandCount).sum();
        total += milestones.frameCounts().values().stream()
                .flatMap(thresholds -> thresholds.values().stream())
                .mapToInt(this::rewardCommandCount)
                .sum();
        total += milestones.tabCompletions().values().stream().mapToInt(this::rewardCommandCount).sum();
        total += milestones.allFrames().values().stream().mapToInt(this::rewardCommandCount).sum();
        total += milestones.groups().values().stream().map(MilestoneGroup::reward).mapToInt(this::rewardCommandCount).sum();
        return total;
    }

    private List<ResolvedReward> configuredMilestoneRewards(MilestoneSnapshot snapshot, Player player) {
        MilestoneSettings settings = config.rewards().milestones();
        List<ResolvedReward> rewards = new ArrayList<>();
        rewards.addAll(completedCountRewards(settings, snapshot));
        rewards.addAll(frameCountRewards(settings, snapshot));
        rewards.addAll(tabCompletionRewards(settings, snapshot));
        rewards.addAll(allFrameRewards(settings, snapshot));
        rewards.addAll(allSelectedRewards(settings, snapshot));
        rewards.addAll(groupRewards(settings, player));
        return List.copyOf(rewards);
    }

    private List<ResolvedReward> completedCountRewards(MilestoneSettings settings, MilestoneSnapshot snapshot) {
        List<ResolvedReward> rewards = new ArrayList<>();
        for (Map.Entry<Integer, RewardEntry> entry : settings.completionCounts().entrySet()) {
            int threshold = entry.getKey();
            RewardEntry reward = entry.getValue();
            if (threshold > 0 && snapshot.completed() >= threshold && reward.active()) {
                rewards.add(new ResolvedReward(
                        "milestone:completed-count:" + threshold,
                        ledgerId("milestone:completed-count:" + threshold, reward),
                        reward,
                        milestonePlaceholders("completed-count", Integer.toString(threshold), snapshot.completed(), snapshot.total())
                ));
            }
        }
        return rewards;
    }

    private List<ResolvedReward> frameCountRewards(MilestoneSettings settings, MilestoneSnapshot snapshot) {
        List<ResolvedReward> rewards = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, RewardEntry>> frameEntry : settings.frameCounts().entrySet()) {
            String frame = frameEntry.getKey().toLowerCase(Locale.ROOT);
            Progress progress = snapshot.frames().getOrDefault(frame, new Progress(frame, 0, 0));
            for (Map.Entry<Integer, RewardEntry> thresholdEntry : frameEntry.getValue().entrySet()) {
                int threshold = thresholdEntry.getKey();
                RewardEntry reward = thresholdEntry.getValue();
                if (threshold > 0 && progress.completed() >= threshold && reward.active()) {
                    rewards.add(new ResolvedReward(
                            "milestone:frame-count:" + frame + ":" + threshold,
                            ledgerId("milestone:frame-count:" + frame + ":" + threshold, reward),
                            reward,
                            milestonePlaceholders("frame-count", frame + ":" + threshold, progress.completed(), progress.total())
                    ));
                }
            }
        }
        return rewards;
    }

    private List<ResolvedReward> tabCompletionRewards(MilestoneSettings settings, MilestoneSnapshot snapshot) {
        List<ResolvedReward> rewards = new ArrayList<>();
        for (Map.Entry<String, RewardEntry> entry : settings.tabCompletions().entrySet()) {
            String rootKey = entry.getKey().toLowerCase(Locale.ROOT);
            Progress progress = snapshot.tabs().get(rootKey);
            RewardEntry reward = entry.getValue();
            if (progress != null && progress.total() > 0 && progress.completed() >= progress.total() && reward.active()) {
                rewards.add(new ResolvedReward(
                        "milestone:tab-completion:" + rootKey,
                        ledgerId("milestone:tab-completion:" + rootKey, reward),
                        reward,
                        milestonePlaceholders("tab-completion", rootKey, progress.completed(), progress.total())
                ));
            }
        }
        return rewards;
    }

    private List<ResolvedReward> allFrameRewards(MilestoneSettings settings, MilestoneSnapshot snapshot) {
        List<ResolvedReward> rewards = new ArrayList<>();
        for (Map.Entry<String, RewardEntry> entry : settings.allFrames().entrySet()) {
            String frame = entry.getKey().toLowerCase(Locale.ROOT);
            Progress progress = snapshot.frames().get(frame);
            RewardEntry reward = entry.getValue();
            if (progress != null && progress.total() > 0 && progress.completed() >= progress.total() && reward.active()) {
                rewards.add(new ResolvedReward(
                        "milestone:all-frame:" + frame,
                        ledgerId("milestone:all-frame:" + frame, reward),
                        reward,
                        milestonePlaceholders("all-frame", frame, progress.completed(), progress.total())
                ));
            }
        }
        return rewards;
    }

    private List<ResolvedReward> allSelectedRewards(MilestoneSettings settings, MilestoneSnapshot snapshot) {
        RewardEntry reward = settings.allSelected();
        if (!reward.active() || snapshot.total() <= 0 || snapshot.completed() < snapshot.total()) {
            return List.of();
        }
        return List.of(new ResolvedReward(
                "milestone:all-selected",
                ledgerId("milestone:all-selected", reward),
                reward,
                milestonePlaceholders("all-selected", "all", snapshot.completed(), snapshot.total())
        ));
    }

    private List<ResolvedReward> groupRewards(MilestoneSettings settings, Player player) {
        List<ResolvedReward> rewards = new ArrayList<>();
        for (Map.Entry<String, MilestoneGroup> entry : settings.groups().entrySet()) {
            String groupName = entry.getKey().toLowerCase(Locale.ROOT);
            MilestoneGroup group = entry.getValue();
            Progress progress = groupProgress(player, group, groupName);
            int target = group.requiredCount() > 0 ? group.requiredCount() : progress.total();
            if (target > 0 && progress.completed() >= target && group.reward().active()) {
                rewards.add(new ResolvedReward(
                        "milestone:group:" + groupName,
                        ledgerId("milestone:group:" + groupName, group.reward()),
                        group.reward(),
                        milestonePlaceholders("group", groupName, progress.completed(), progress.total())
                ));
            }
        }
        return rewards;
    }

    private Progress groupProgress(Player player, MilestoneGroup group, String groupName) {
        int completed = 0;
        int total = 0;
        for (Advancement advancement : advancements()) {
            if (!matchesSelection(advancement, group.selection())) {
                continue;
            }
            total++;
            if (player.getAdvancementProgress(advancement).isDone()) {
                completed++;
            }
        }
        return new Progress(groupName, completed, total);
    }

    private RewardInspection payableInspection(List<ResolvedReward> configuredRewards, Player player, String emptyReason) {
        List<ResolvedReward> payableRewards = new ArrayList<>();
        Set<String> seenLedgerIds = new HashSet<>();
        int alreadyClaimed = 0;
        for (ResolvedReward reward : configuredRewards) {
            if (!seenLedgerIds.add(reward.ledgerId())) {
                continue;
            }
            if (config.rewards().firstTimeOnly() && ledger.hasClaimed(player.getUniqueId(), reward.ledgerId())) {
                alreadyClaimed++;
                continue;
            }
            payableRewards.add(reward);
        }

        if (payableRewards.isEmpty()) {
            return new RewardInspection(false, emptyReason, configuredRewards, List.of(), alreadyClaimed);
        }

        return new RewardInspection(true, "eligible", configuredRewards, List.copyOf(payableRewards), alreadyClaimed);
    }

    private String blockReason(AdvancementContext context, List<ResolvedReward> configuredRewards) {
        String baseReason = baseBlockReason(context);
        if (baseReason != null) {
            return baseReason;
        }
        if (configuredRewards.isEmpty()) {
            return "no reward commands configured";
        }
        RewardSettings settings = config.rewards();
        if (!matchesAny(settings.includeNamespaces(), context.namespace())) {
            return "namespace not included";
        }
        if (matchesAny(settings.excludeNamespaces(), context.namespace())) {
            return "namespace excluded";
        }
        if (!matchesAny(settings.includeAdvancements(), context.key())) {
            return "advancement not included";
        }
        if (matchesAny(settings.excludeAdvancements(), context.key())) {
            return "advancement excluded";
        }
        if (!matchesSelectionVisibility(context.advancement(), settings.visibility())) {
            return "advancement visibility excluded";
        }
        return null;
    }

    private String milestoneBlockReason(AdvancementContext context, List<ResolvedReward> configuredRewards) {
        String baseReason = baseBlockReason(context);
        if (baseReason != null) {
            return baseReason;
        }
        if (!config.rewards().milestones().enabled()) {
            return "milestones disabled";
        }
        if (configuredRewards.isEmpty()) {
            return "no milestone reached or configured";
        }
        return null;
    }

    private String baseBlockReason(AdvancementContext context) {
        RewardSettings settings = config.rewards();
        if (!settings.enabled()) {
            return "rewards disabled";
        }
        if (!matchesAllowedWorld(settings.allowedWorlds(), context.player().getWorld().getName())) {
            return "world not allowed";
        }
        if (matchesAny(settings.blockedWorlds(), context.player().getWorld().getName())) {
            return "world blocked";
        }
        if (!gameModeAllowed(settings.allowedGameModes(), context.player().getGameMode())) {
            return "game mode not allowed";
        }
        if (!hasRequiredPermission(context.player(), settings.requiredPermissions())) {
            return "missing required permission";
        }
        if (hasExcludedPermission(context.player(), settings.excludedPermissions())) {
            return "player has excluded permission";
        }
        return null;
    }

    private void dispatchAndRecord(AdvancementContext context, ResolvedReward reward) {
        if (config.rewards().firstTimeOnly()) {
            ledger.markClaimed(context.player().getUniqueId(), reward.ledgerId(), context.key());
        }

        for (String command : reward.entry().commands()) {
            dispatchRewardCommand(context, reward, command);
        }
    }

    private void dispatchRewardCommand(AdvancementContext context, ResolvedReward reward, String commandTemplate) {
        String command = applyPlaceholders(commandTemplate, context, reward.placeholders()).trim();
        while (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (command.isEmpty()) {
            return;
        }

        boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (config.rewards().logExecutions()) {
            plugin.getLogger().info("Reward " + reward.ledgerId() + " for " + context.player().getName()
                    + " on " + context.key() + " dispatched command: " + command);
        }
        if (!dispatched) {
            plugin.getLogger().warning("Reward command was not handled: " + command);
        }
    }

    private String applyPlaceholders(String command, AdvancementContext context, Map<String, String> extraPlaceholders) {
        Player player = context.player();
        Location location = player.getLocation();
        String result = command
                .replace("<player>", player.getName())
                .replace("<uuid>", player.getUniqueId().toString())
                .replace("<world>", player.getWorld().getName())
                .replace("<key>", context.key())
                .replace("<namespace>", context.namespace())
                .replace("<path>", context.path())
                .replace("<frame>", context.frameKey())
                .replace("<criterion>", context.criterion())
                .replace("<completed>", Integer.toString(context.completedCriteria()))
                .replace("<total>", Integer.toString(context.totalCriteria()))
                .replace("<remaining>", Integer.toString(context.remainingCriteria()))
                .replace("<percent>", Integer.toString(context.percent()))
                .replace("<x>", Integer.toString(location.getBlockX()))
                .replace("<y>", Integer.toString(location.getBlockY()))
                .replace("<z>", Integer.toString(location.getBlockZ()));
        for (Map.Entry<String, String> entry : extraPlaceholders.entrySet()) {
            result = result.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return result;
    }

    private Map<String, String> milestonePlaceholders(String milestone, String value, int completed, int total) {
        int remaining = Math.max(0, total - completed);
        int percent = total <= 0 ? 0 : Math.min(100, Math.max(0, Math.round((completed * 100.0F) / total)));
        return Map.of(
                "milestone", milestone,
                "milestone_value", value,
                "milestone_completed", Integer.toString(completed),
                "milestone_total", Integer.toString(total),
                "milestone_remaining", Integer.toString(remaining),
                "milestone_percent", Integer.toString(percent)
        );
    }

    private boolean matchesSelection(Advancement advancement, RewardSelectionSettings selection) {
        String key = advancement.getKey().toString();
        String namespace = advancement.getKey().getNamespace();
        if (!matchesAny(selection.includeNamespaces(), namespace)) {
            return false;
        }
        if (matchesAny(selection.excludeNamespaces(), namespace)) {
            return false;
        }
        if (!matchesAny(selection.includeAdvancements(), key)) {
            return false;
        }
        if (matchesAny(selection.excludeAdvancements(), key)) {
            return false;
        }
        return matchesSelectionVisibility(advancement, selection.visibility());
    }

    private boolean matchesSelectionVisibility(Advancement advancement, AdvancementPlusConfig.RewardVisibilitySettings visibility) {
        AdvancementDisplay display = advancement.getDisplay();
        if (display == null && !visibility.includeNoDisplay()) {
            return false;
        }
        if (display != null && display.isHidden() && !visibility.includeHidden()) {
            return false;
        }
        if (advancement.getParent() == null && !visibility.includeRootAdvancements()) {
            return false;
        }
        return !visibility.requireDisplayAnnouncesToChat() || (display != null && display.doesAnnounceToChat());
    }

    private String frameKey(Advancement advancement) {
        AdvancementDisplay display = advancement.getDisplay();
        if (display == null) {
            return "no-display";
        }
        return display.frame().name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private String ledgerId(String fallback, RewardEntry entry) {
        if (entry.rewardId() == null || entry.rewardId().isBlank()) {
            return fallback;
        }
        return entry.rewardId().trim();
    }

    private int rewardCommandCount(RewardEntry reward) {
        return reward.active() ? reward.commands().size() : 0;
    }

    private Map<String, Progress> progressMap(Map<String, MutableProgress> progress) {
        Map<String, Progress> immutable = new HashMap<>();
        for (Map.Entry<String, MutableProgress> entry : progress.entrySet()) {
            immutable.put(entry.getKey(), entry.getValue().toProgress());
        }
        return Map.copyOf(immutable);
    }

    private List<Advancement> advancements() {
        List<Advancement> advancements = new ArrayList<>();
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            advancements.add(iterator.next());
        }
        advancements.sort(Comparator.comparing(advancement -> advancement.getKey().toString()));
        return advancements;
    }

    private boolean matchesAllowedWorld(List<String> allowedWorlds, String worldName) {
        return allowedWorlds == null || allowedWorlds.isEmpty() || matchesAny(allowedWorlds, worldName);
    }

    private boolean gameModeAllowed(List<String> allowedGameModes, GameMode gameMode) {
        if (allowedGameModes == null || allowedGameModes.isEmpty()) {
            return true;
        }
        String current = gameMode.name().toLowerCase(Locale.ROOT).replace('_', '-');
        return allowedGameModes.stream()
                .map(value -> value.toLowerCase(Locale.ROOT).replace('_', '-'))
                .anyMatch(value -> value.equals(current));
    }

    private boolean hasRequiredPermission(Player player, List<String> permissions) {
        return permissions == null || permissions.isEmpty() || permissions.stream().anyMatch(player::hasPermission);
    }

    private boolean hasExcludedPermission(Player player, List<String> permissions) {
        return permissions != null && permissions.stream().anyMatch(player::hasPermission);
    }

    private boolean matchesAny(List<String> patterns, String value) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        String normalizedValue = value.toLowerCase(Locale.ROOT);
        for (String pattern : patterns) {
            if (wildcardMatches(pattern.toLowerCase(Locale.ROOT), normalizedValue)) {
                return true;
            }
        }
        return false;
    }

    private boolean wildcardMatches(String pattern, String value) {
        if ("*".equals(pattern)) {
            return true;
        }
        if (!pattern.contains("*")) {
            return pattern.equals(value);
        }

        int valueIndex = 0;
        boolean firstPart = true;
        String[] parts = pattern.split("\\*", -1);
        for (String part : parts) {
            if (part.isEmpty()) {
                firstPart = false;
                continue;
            }
            int foundAt = value.indexOf(part, valueIndex);
            if (foundAt < 0) {
                return false;
            }
            if (firstPart && !pattern.startsWith("*") && foundAt != 0) {
                return false;
            }
            valueIndex = foundAt + part.length();
            firstPart = false;
        }
        String lastPart = parts.length == 0 ? "" : parts[parts.length - 1];
        return pattern.endsWith("*") || lastPart.isEmpty() || value.endsWith(lastPart);
    }

    private final class MutableProgress {
        private final String key;
        private int completed;
        private int total;

        private MutableProgress(String key) {
            this.key = key;
        }

        private void add(boolean done) {
            total++;
            if (done) {
                completed++;
            }
        }

        private Progress toProgress() {
            return new Progress(key, completed, total);
        }
    }

    public record ResolvedReward(String source, String ledgerId, RewardEntry entry, Map<String, String> placeholders) {
        public ResolvedReward(String source, String ledgerId, RewardEntry entry) {
            this(source, ledgerId, entry, Map.of());
        }

        public ResolvedReward {
            placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders);
        }
    }

    public record Progress(String key, int completed, int total) {
        public int remaining() {
            return Math.max(0, total - completed);
        }

        public int percent() {
            return total <= 0 ? 0 : Math.min(100, Math.max(0, Math.round((completed * 100.0F) / total)));
        }
    }

    public record MilestoneSnapshot(
            int completed,
            int total,
            Map<String, Progress> frames,
            Map<String, Progress> tabs
    ) {
        public int remaining() {
            return Math.max(0, total - completed);
        }

        public int percent() {
            return total <= 0 ? 0 : Math.min(100, Math.max(0, Math.round((completed * 100.0F) / total)));
        }
    }

    public record RewardInspection(
            boolean eligible,
            String reason,
            List<ResolvedReward> configuredRewards,
            List<ResolvedReward> payableRewards,
            int alreadyClaimed
    ) {
        public int configuredCommandCount() {
            return configuredRewards.stream().mapToInt(reward -> reward.entry().commands().size()).sum();
        }

        public int payableCommandCount() {
            return payableRewards.stream().mapToInt(reward -> reward.entry().commands().size()).sum();
        }
    }

    public record MilestoneInspection(
            boolean eligible,
            String reason,
            MilestoneSnapshot snapshot,
            List<ResolvedReward> configuredRewards,
            List<ResolvedReward> payableRewards,
            int alreadyClaimed
    ) {
        public int configuredCommandCount() {
            return configuredRewards.stream().mapToInt(reward -> reward.entry().commands().size()).sum();
        }

        public int payableCommandCount() {
            return payableRewards.stream().mapToInt(reward -> reward.entry().commands().size()).sum();
        }
    }
}
