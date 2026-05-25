package dev.advancementplus.advancement;

import dev.advancementplus.config.AdvancementPlusConfig;
import java.util.List;
import java.util.Locale;

public final class AdvancementFilter {
    private final AdvancementPlusConfig config;

    public AdvancementFilter(AdvancementPlusConfig config) {
        this.config = config;
    }

    public boolean shouldBroadcast(AdvancementContext context) {
        if (context.kind() == AnnouncementKind.PROGRESS && !config.progress().enabled()) {
            return false;
        }
        if (context.kind() == AnnouncementKind.COMPLETION && !config.completion().enabled()) {
            return false;
        }
        if (!matchesAny(config.filter().includeNamespaces(), context.namespace())) {
            return false;
        }
        if (matchesAny(config.filter().excludeNamespaces(), context.namespace())) {
            return false;
        }
        if (!matchesAny(config.filter().includeAdvancements(), context.key())) {
            return false;
        }
        if (matchesAny(config.filter().excludeAdvancements(), context.key())) {
            return false;
        }
        if (!context.hasDisplay() && !config.visibility().includeNoDisplay()) {
            return false;
        }
        if (context.isHidden() && !config.visibility().includeHidden()) {
            return false;
        }
        if (context.isRoot() && !config.visibility().includeRootAdvancements()) {
            return false;
        }
        if (context.kind() == AnnouncementKind.COMPLETION
                && config.visibility().requireDisplayAnnouncesToChatForCompletion()
                && !context.doesAnnounceToChat()) {
            return false;
        }
        if (context.kind() == AnnouncementKind.PROGRESS) {
            if (context.totalCriteria() <= 1 && !config.progress().announceSingleCriterion()) {
                return false;
            }
            if (context.progress().isDone() && !config.progress().announceFinalCriterion()) {
                return false;
            }
        }
        return true;
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
}

