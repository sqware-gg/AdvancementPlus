package dev.advancementplus.reward;

import java.util.List;

public record RewardEntry(boolean enabled, String rewardId, List<String> commands) {
    public RewardEntry {
        rewardId = rewardId == null ? "" : rewardId.trim();
        commands = commands == null ? List.of() : List.copyOf(commands);
    }

    public boolean active() {
        return enabled && !commands.isEmpty();
    }
}
