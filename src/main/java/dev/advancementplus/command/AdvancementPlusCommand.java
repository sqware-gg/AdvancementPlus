package dev.advancementplus.command;

import dev.advancementplus.AdvancementPlusPlugin;
import dev.advancementplus.advancement.AdvancementContext;
import io.papermc.paper.advancement.AdvancementDisplay;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class AdvancementPlusCommand implements CommandExecutor, TabCompleter {
    private static final int PAGE_SIZE = 10;

    private final AdvancementPlusPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

    public AdvancementPlusCommand(AdvancementPlusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("advancementplus.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use AdvancementPlus admin commands.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            sendStatus(sender);
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            plugin.reloadPlugin();
            sender.sendMessage(miniMessage.deserialize("<green>AdvancementPlus reloaded.</green>"));
            return true;
        }

        if ("list".equalsIgnoreCase(args[0])) {
            handleList(sender, args);
            return true;
        }

        if ("inspect".equalsIgnoreCase(args[0])) {
            handleInspect(sender, args);
            return true;
        }

        sendUsage(sender, label);
        return true;
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(miniMessage.deserialize("<gold>AdvancementPlus</gold> <gray>" + plugin.getPluginMeta().getVersion() + "</gray>"));
        sender.sendMessage(Component.text("Loaded advancements: " + plugin.loadedAdvancementCount(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Progress messages: " + enabledText(plugin.advancementConfig().progress().enabled()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Completion messages: " + enabledText(plugin.advancementConfig().completion().enabled()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Broadcast audience: " + plugin.advancementConfig().broadcast().audience(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Format engine: " + plugin.advancementConfig().format().engine(), NamedTextColor.GRAY));

        for (World world : Bukkit.getWorlds()) {
            Boolean gamerule = world.getGameRuleValue(GameRules.SHOW_ADVANCEMENT_MESSAGES);
            sender.sendMessage(Component.text("World " + world.getName() + " showAdvancementMessages=" + gamerule, NamedTextColor.DARK_GRAY));
        }
    }

    private String enabledText(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    private void handleList(CommandSender sender, String[] args) {
        String namespace = args.length >= 2 && !isInteger(args[1]) ? args[1].toLowerCase(Locale.ROOT) : "";
        int pageArgIndex = namespace.isEmpty() ? 1 : 2;
        int page = args.length > pageArgIndex && isInteger(args[pageArgIndex])
                ? Math.max(1, Integer.parseInt(args[pageArgIndex]))
                : 1;

        List<Advancement> advancements = advancements().stream()
                .filter(advancement -> namespace.isEmpty() || advancement.getKey().getNamespace().equalsIgnoreCase(namespace))
                .sorted(Comparator.comparing(advancement -> advancement.getKey().toString()))
                .toList();

        if (advancements.isEmpty()) {
            sender.sendMessage(Component.text("No advancements matched.", NamedTextColor.YELLOW));
            return;
        }

        int maxPage = Math.max(1, (int) Math.ceil(advancements.size() / (double) PAGE_SIZE));
        page = Math.min(page, maxPage);
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, advancements.size());

        sender.sendMessage(miniMessage.deserialize("<gold>Advancements</gold> <gray>page " + page + "/" + maxPage + " (" + advancements.size() + ")</gray>"));
        for (Advancement advancement : advancements.subList(start, end)) {
            AdvancementDisplay display = advancement.getDisplay();
            String label = display == null ? AdvancementContext.humanTitle(advancement.getKey()) : plain.serialize(display.title());
            String frame = display == null ? "no-display" : display.frame().name().toLowerCase(Locale.ROOT);
            sender.sendMessage(Component.text(advancement.getKey() + " - " + label + " [" + frame + ", criteria=" + advancement.getCriteria().size() + "]",
                    NamedTextColor.GRAY));
        }
    }

    private void handleInspect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /advancementplus inspect <namespace:path>", NamedTextColor.YELLOW));
            return;
        }

        NamespacedKey key = NamespacedKey.fromString(args[1].toLowerCase(Locale.ROOT));
        if (key == null) {
            sender.sendMessage(Component.text("Invalid advancement key: " + args[1], NamedTextColor.RED));
            return;
        }

        Advancement advancement = Bukkit.getAdvancement(key);
        if (advancement == null) {
            sender.sendMessage(Component.text("Unknown advancement: " + key, NamedTextColor.RED));
            return;
        }

        AdvancementDisplay display = advancement.getDisplay();
        sender.sendMessage(miniMessage.deserialize("<gold>Advancement</gold> <gray>" + advancement.getKey() + "</gray>"));
        sender.sendMessage(Component.text("Title: " + (display == null ? AdvancementContext.humanTitle(key) : plain.serialize(display.title())), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Display: " + (display == null ? "none" : "yes"), NamedTextColor.GRAY));
        if (display != null) {
            sender.sendMessage(Component.text("Frame: " + display.frame().name().toLowerCase(Locale.ROOT), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Hidden: " + display.isHidden(), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Announces to chat: " + display.doesAnnounceToChat(), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Description: " + plain.serialize(display.description()), NamedTextColor.GRAY));
        }
        sender.sendMessage(Component.text("Root: " + advancement.getRoot().getKey(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Parent: " + (advancement.getParent() == null ? "none" : advancement.getParent().getKey()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Criteria: " + advancement.getCriteria().size() + " " + advancement.getCriteria(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Requirement groups: " + advancement.getRequirements().getRequirements().size(), NamedTextColor.GRAY));

        if (sender instanceof Player player) {
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            sender.sendMessage(Component.text("Your progress: " + progress.getAwardedCriteria().size() + "/"
                    + Math.max(1, advancement.getCriteria().size()) + " done=" + progress.isDone(), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Awarded: " + progress.getAwardedCriteria(), NamedTextColor.DARK_GRAY));
            sender.sendMessage(Component.text("Remaining: " + progress.getRemainingCriteria(), NamedTextColor.DARK_GRAY));
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text("Usage: /" + label + " <status|reload|list|inspect>", NamedTextColor.YELLOW));
    }

    private List<Advancement> advancements() {
        List<Advancement> advancements = new ArrayList<>();
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            advancements.add(iterator.next());
        }
        return advancements;
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("advancementplus.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(List.of("status", "reload", "list", "inspect"), args[0]);
        }

        if (args.length == 2 && "list".equalsIgnoreCase(args[0])) {
            return filter(namespaces(), args[1]);
        }

        if (args.length == 2 && "inspect".equalsIgnoreCase(args[0])) {
            return filter(advancementKeys(), args[1]).stream().limit(50).toList();
        }

        return List.of();
    }

    private List<String> namespaces() {
        return advancements().stream()
                .map(advancement -> advancement.getKey().getNamespace())
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> advancementKeys() {
        return advancements().stream()
                .map(advancement -> advancement.getKey().toString())
                .sorted()
                .toList();
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toList();
    }
}
