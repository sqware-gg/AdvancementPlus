package dev.advancementplus.api.event;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class AdvancementPlusBroadcastEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID playerUuid;
    private final String playerName;
    private final String kind;
    private final String advancementKey;
    private final String namespace;
    private final String path;
    private final String title;
    private final String description;
    private final String frameType;
    private final String frameLabel;
    private final boolean hidden;
    private final boolean announcesToChat;
    private final String criterion;
    private final int completedCriteria;
    private final int totalCriteria;
    private final int remainingCriteria;
    private final int percent;
    private final String message;

    public AdvancementPlusBroadcastEvent(Player player, String kind, String advancementKey, String namespace,
                                         String path, String title, String description, String frameType,
                                         String frameLabel, boolean hidden, boolean announcesToChat,
                                         String criterion, int completedCriteria, int totalCriteria,
                                         int remainingCriteria, int percent, String message) {
        this.player = player;
        this.playerUuid = player.getUniqueId();
        this.playerName = player.getName();
        this.kind = kind;
        this.advancementKey = advancementKey;
        this.namespace = namespace;
        this.path = path;
        this.title = title;
        this.description = description;
        this.frameType = frameType;
        this.frameLabel = frameLabel;
        this.hidden = hidden;
        this.announcesToChat = announcesToChat;
        this.criterion = criterion == null ? "" : criterion;
        this.completedCriteria = completedCriteria;
        this.totalCriteria = totalCriteria;
        this.remainingCriteria = remainingCriteria;
        this.percent = percent;
        this.message = message == null ? "" : message;
    }

    public Player player() {
        return player;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String playerName() {
        return playerName;
    }

    public String kind() {
        return kind;
    }

    public String advancementKey() {
        return advancementKey;
    }

    public String namespace() {
        return namespace;
    }

    public String path() {
        return path;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String frameType() {
        return frameType;
    }

    public String frameLabel() {
        return frameLabel;
    }

    public boolean hidden() {
        return hidden;
    }

    public boolean announcesToChat() {
        return announcesToChat;
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
        return percent;
    }

    public String message() {
        return message;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
