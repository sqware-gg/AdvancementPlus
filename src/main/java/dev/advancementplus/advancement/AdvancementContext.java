package dev.advancementplus.advancement;

import io.papermc.paper.advancement.AdvancementDisplay;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.advancement.AdvancementRequirement;
import org.bukkit.entity.Player;

public final class AdvancementContext {
    private final AnnouncementKind kind;
    private final Player player;
    private final Advancement advancement;
    private final AdvancementProgress progress;
    private final AdvancementDisplay display;
    private final String criterion;
    private final int completedCriteria;
    private final int totalCriteria;
    private final int remainingCriteria;

    private AdvancementContext(
            AnnouncementKind kind,
            Player player,
            Advancement advancement,
            AdvancementProgress progress,
            AdvancementDisplay display,
            String criterion,
            int completedCriteria,
            int totalCriteria,
            int remainingCriteria
    ) {
        this.kind = kind;
        this.player = player;
        this.advancement = advancement;
        this.progress = progress;
        this.display = display;
        this.criterion = criterion;
        this.completedCriteria = completedCriteria;
        this.totalCriteria = totalCriteria;
        this.remainingCriteria = remainingCriteria;
    }

    public static AdvancementContext create(
            AnnouncementKind kind,
            Player player,
            Advancement advancement,
            AdvancementProgress progress,
            String criterion
    ) {
        Set<String> awarded = Set.copyOf(progress.getAwardedCriteria());
        List<AdvancementRequirement> requirements = advancement.getRequirements().getRequirements();
        int total = Math.max(1, requirements.size());
        int completed = requirements.isEmpty()
                ? awarded.size()
                : (int) requirements.stream()
                .filter(requirement -> requirement.getRequiredCriteria().stream().anyMatch(awarded::contains))
                .count();
        if (requirements.isEmpty()) {
            total = Math.max(1, advancement.getCriteria().size());
        }
        return new AdvancementContext(
                kind,
                player,
                advancement,
                progress,
                advancement.getDisplay(),
                criterion == null ? "" : criterion,
                Math.min(completed, total),
                total,
                Math.max(0, total - completed)
        );
    }

    public AnnouncementKind kind() {
        return kind;
    }

    public Player player() {
        return player;
    }

    public Advancement advancement() {
        return advancement;
    }

    public AdvancementProgress progress() {
        return progress;
    }

    public AdvancementDisplay display() {
        return display;
    }

    public boolean hasDisplay() {
        return display != null;
    }

    public boolean isHidden() {
        return display != null && display.isHidden();
    }

    public boolean doesAnnounceToChat() {
        return display != null && display.doesAnnounceToChat();
    }

    public boolean isRoot() {
        return advancement.getParent() == null;
    }

    public String criterion() {
        return criterion;
    }

    public int completedCriteria() {
        return completedCriteria;
    }

    public int totalCriteria() {
        return totalCriteria;
    }

    public int remainingCriteria() {
        return remainingCriteria;
    }

    public int percent() {
        return Math.min(100, Math.max(0, Math.round((completedCriteria * 100.0F) / totalCriteria)));
    }

    public String key() {
        return advancement.getKey().toString();
    }

    public String namespace() {
        return advancement.getKey().getNamespace();
    }

    public String path() {
        return advancement.getKey().getKey();
    }

    public String frameKey() {
        if (display == null) {
            return "no-display";
        }
        return display.frame().name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public String frameTitle() {
        return switch (frameKey()) {
            case "goal" -> "Goal";
            case "challenge" -> "Challenge";
            case "task" -> "Advancement";
            default -> "Advancement";
        };
    }

    public Component title() {
        if (display != null) {
            return display.title();
        }
        return Component.text(humanTitle(advancement.getKey()), NamedTextColor.WHITE);
    }

    public Component description() {
        if (display != null) {
            return display.description();
        }
        return Component.text(key(), NamedTextColor.DARK_GRAY);
    }

    public static String humanTitle(NamespacedKey key) {
        String path = key.getKey();
        int slash = path.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < path.length()) {
            path = path.substring(slash + 1);
        }
        String[] words = path.replace('-', '_').split("_+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.isEmpty() ? key.toString() : builder.toString();
    }
}
