package dev.advancementplus.command;

import dev.advancementplus.AdvancementPlusPlugin;
import dev.advancementplus.advancement.AdvancementContext;
import io.papermc.paper.advancement.AdvancementDisplay;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
            message(sender, "<#ED4245>No permission.</#ED4245>");
            return true;
        }

        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            sendStatus(sender);
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            plugin.reloadPlugin();
            message(sender, "<#57F287>Advancements reloaded.</#57F287>");
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
        message(sender, "<gray>AdvancementPlus</gray> <dark_gray>›</dark_gray> <#2b98fd>" + plugin.getPluginMeta().getVersion() + "</#2b98fd>");
        message(sender, "<gray>Loaded advancements:</gray> <white>" + plugin.loadedAdvancementCount() + "</white>");
        message(sender, "<gray>Progress messages:</gray> <white>" + enabledText(plugin.advancementConfig().progress().enabled()) + "</white>");
        message(sender, "<gray>Completion messages:</gray> <white>" + enabledText(plugin.advancementConfig().completion().enabled()) + "</white>");
        message(sender, "<gray>Broadcast audience:</gray> <white>" + plugin.advancementConfig().broadcast().audience() + "</white>");
        message(sender, "<gray>Format engine:</gray> <white>" + plugin.advancementConfig().format().engine() + "</white>");

        for (World world : Bukkit.getWorlds()) {
            Boolean gamerule = world.getGameRuleValue(GameRules.SHOW_ADVANCEMENT_MESSAGES);
            message(sender, "<dark_gray>World " + world.getName() + " showAdvancementMessages=" + gamerule + "</dark_gray>");
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
            message(sender, "<#f5c542>No advancements matched.</#f5c542>");
            return;
        }

        int maxPage = Math.max(1, (int) Math.ceil(advancements.size() / (double) PAGE_SIZE));
        page = Math.min(page, maxPage);
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, advancements.size());

        message(sender, "<gray>Advancements</gray> <dark_gray>›</dark_gray> <#2b98fd>page " + page + "/" + maxPage + "</#2b98fd> <dark_gray>(" + advancements.size() + ")</dark_gray>");
        for (Advancement advancement : advancements.subList(start, end)) {
            AdvancementDisplay display = advancement.getDisplay();
            String label = display == null ? AdvancementContext.humanTitle(advancement.getKey()) : plain.serialize(display.title());
            String frame = display == null ? "no-display" : display.frame().name().toLowerCase(Locale.ROOT);
            message(sender, "<dark_gray>" + advancement.getKey() + "</dark_gray> <gray>›</gray> <#8ecbff>" + label + "</#8ecbff> <dark_gray>(" + frame + ", criteria=" + advancement.getCriteria().size() + ")</dark_gray>");
        }
    }

    private void handleInspect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            message(sender, "<gray>Usage:</gray> <#2b98fd>/advancementplus inspect <namespace:path></#2b98fd>");
            return;
        }

        NamespacedKey key = NamespacedKey.fromString(args[1].toLowerCase(Locale.ROOT));
        if (key == null) {
            message(sender, "<#ED4245>Invalid advancement key:</#ED4245> <white>" + args[1] + "</white>");
            return;
        }

        Advancement advancement = Bukkit.getAdvancement(key);
        if (advancement == null) {
            message(sender, "<#ED4245>Unknown advancement:</#ED4245> <white>" + key + "</white>");
            return;
        }

        AdvancementDisplay display = advancement.getDisplay();
        message(sender, "<gray>Advancement</gray> <dark_gray>›</dark_gray> <#2b98fd>" + advancement.getKey() + "</#2b98fd>");
        message(sender, "<gray>Title:</gray> <#8ecbff>" + (display == null ? AdvancementContext.humanTitle(key) : plain.serialize(display.title())) + "</#8ecbff>");
        message(sender, "<gray>Display:</gray> <white>" + (display == null ? "none" : "yes") + "</white>");
        if (display != null) {
            message(sender, "<gray>Frame:</gray> <white>" + display.frame().name().toLowerCase(Locale.ROOT) + "</white>");
            message(sender, "<gray>Hidden:</gray> <white>" + display.isHidden() + "</white>");
            message(sender, "<gray>Announces to chat:</gray> <white>" + display.doesAnnounceToChat() + "</white>");
            message(sender, "<gray>Description:</gray> <white>" + plain.serialize(display.description()) + "</white>");
        }
        message(sender, "<gray>Root:</gray> <white>" + advancement.getRoot().getKey() + "</white>");
        message(sender, "<gray>Parent:</gray> <white>" + (advancement.getParent() == null ? "none" : advancement.getParent().getKey()) + "</white>");
        message(sender, "<gray>Criteria:</gray> <white>" + advancement.getCriteria().size() + " " + advancement.getCriteria() + "</white>");
        message(sender, "<gray>Requirement groups:</gray> <white>" + advancement.getRequirements().getRequirements().size() + "</white>");

        if (sender instanceof Player player) {
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            message(sender, "<gray>Your progress:</gray> <#2b98fd>" + progress.getAwardedCriteria().size() + "/"
                    + Math.max(1, advancement.getCriteria().size()) + "</#2b98fd> <dark_gray>done=" + progress.isDone() + "</dark_gray>");
            message(sender, "<dark_gray>Awarded: " + progress.getAwardedCriteria() + "</dark_gray>");
            message(sender, "<dark_gray>Remaining: " + progress.getRemainingCriteria() + "</dark_gray>");
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        message(sender, "<gray>Usage:</gray> <#2b98fd>/" + label + " <status|reload|list|inspect></#2b98fd>");
    }

    private void message(CommandSender sender, String body) {
        sender.sendMessage(miniMessage.deserialize("<#2b98fd>AdvancementPlus</#2b98fd> <dark_gray>›</dark_gray> " + body));
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
