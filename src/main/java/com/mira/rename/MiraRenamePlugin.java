package com.mira.rename;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.*;

public final class MiraRenamePlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private NamespacedKey tagKey;
    private NamespacedKey tokenKey;
    private NamespacedKey nameKey;
    private final Map<UUID, String> awaiting = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        tagKey = new NamespacedKey(this, "rename_tag");
        tokenKey = new NamespacedKey(this, "token_id");
        nameKey = new NamespacedKey(this, "prepared_name");

        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand command = getCommand("mirarename");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
        getLogger().info("MiraRename v" + getPluginMeta().getVersion() + " enabled.");
    }

    public ItemStack createTag() {
        Material material = Material.matchMaterial(getConfig().getString("tag.material", "NAME_TAG"));
        if (material == null || material.isAir()) material = Material.NAME_TAG;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(c(getConfig().getString("tag.display-name", "&d&lItem Name Tag")));
        meta.lore(getConfig().getStringList("tag.lore").stream().map(this::c).toList());
        meta.setMaxStackSize(1);
        meta.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(tokenKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() == null) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!isTag(item)) return;

        event.setCancelled(true);
        if (!player.hasPermission("mirarename.use")) {
            msg(player, "messages.no-permission", "&cYou cannot use item name tags.");
            return;
        }

        String token = tokenId(item);
        if (token == null) return;
        awaiting.put(player.getUniqueId(), token);
        msg(player, "messages.prompt", "&eType the new item name in chat. Use &fcancel &eto stop.");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.1f);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String token = awaiting.remove(player.getUniqueId());
        if (token == null) return;
        event.setCancelled(true);

        String raw = PLAIN.serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(this, () -> {
            if (raw.equalsIgnoreCase("cancel")) {
                msg(player, "messages.cancelled", "&cItem rename cancelled.");
                return;
            }

            int max = Math.max(1, Math.min(128, getConfig().getInt("tag.max-name-length", 64)));
            if (raw.isBlank() || raw.length() > max || raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0) {
                player.sendMessage(c("&cName must be between 1 and " + max + " characters."));
                return;
            }

            ItemStack tag = findToken(player, token);
            if (tag == null) {
                player.sendMessage(c("&cThat name tag is no longer in your inventory."));
                return;
            }

            String legacyName = normalizeLegacy(raw);
            ItemMeta meta = tag.getItemMeta();
            meta.displayName(c(legacyName));
            meta.getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, legacyName);
            tag.setItemMeta(meta);
            player.updateInventory();
            player.sendMessage(c(getConfig().getString("messages.armed",
                    "&aName tag prepared as &f%name%&a. Click it onto the item you want to rename.")
                    .replace("%name%", legacyName)));
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.3f);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onApply(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack cursor = event.getCursor();
        ItemStack target = event.getCurrentItem();
        if (!isTag(cursor) || target == null || target.getType().isAir()) return;

        String name = preparedName(cursor);
        if (name == null || name.isBlank()) return;

        event.setCancelled(true);
        if (target.getType() == Material.AIR || isTag(target)) {
            msg(player, "messages.invalid", "&cThat item cannot be renamed.");
            return;
        }

        ItemStack renamed = target.clone();
        boolean applied = renameMiraItem(renamed, name);
        if (!applied) {
            ItemMeta meta = renamed.getItemMeta();
            if (meta == null) {
                msg(player, "messages.invalid", "&cThat item cannot be renamed.");
                return;
            }
            meta.displayName(c(name));
            renamed.setItemMeta(meta);
        }

        event.setCurrentItem(renamed);
        consumeCursor(event, cursor);
        player.updateInventory();
        player.sendMessage(c(getConfig().getString("messages.applied", "&aItem renamed to &f%name%&a.")
                .replace("%name%", name)));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        awaiting.remove(event.getPlayer().getUniqueId());
    }

    private boolean renameMiraItem(ItemStack item, String name) {
        Plugin miraItems = getServer().getPluginManager().getPlugin("MiraItems");
        if (miraItems == null || !miraItems.isEnabled()) return false;
        try {
            Method itemsMethod = miraItems.getClass().getMethod("items");
            Object service = itemsMethod.invoke(miraItems);
            Method rename = service.getClass().getMethod("renamePreservingIdentity", ItemStack.class, String.class);
            Object result = rename.invoke(service, item, name);
            return result instanceof Boolean b && b;
        } catch (ReflectiveOperationException ex) {
            getLogger().warning("Could not use MiraItems rename bridge: " + ex.getMessage());
            return false;
        }
    }

    private void consumeCursor(InventoryClickEvent event, ItemStack cursor) {
        if (cursor.getAmount() <= 1) event.setCursor(null);
        else {
            ItemStack next = cursor.clone();
            next.setAmount(cursor.getAmount() - 1);
            event.setCursor(next);
        }
    }

    private ItemStack findToken(Player player, String token) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && token.equals(tokenId(item))) return item;
        }
        return null;
    }

    private boolean isTag(ItemStack item) {
        return item != null && !item.getType().isAir() && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(tagKey, PersistentDataType.BYTE);
    }

    private String tokenId(ItemStack item) {
        if (!isTag(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(tokenKey, PersistentDataType.STRING);
    }

    private String preparedName(ItemStack item) {
        if (!isTag(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
    }

    private Component c(String value) {
        return LEGACY.deserialize(normalizeLegacy(value));
    }

    private String normalizeLegacy(String value) {
        if (value == null) return "";
        // Support the standard ampersand legacy syntax players expect:
        // &0-&9, &a-&f, &k-&o and &r. Section-sign input is normalized too.
        return value.replace('§', '&');
    }

    private void msg(Player player, String path, String fallback) {
        player.sendMessage(c(getConfig().getString(path, fallback)));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(c("&7Usage: /mirarename give <player> [amount]"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(c("&cThat player is not online."));
            return true;
        }
        int amount = 1;
        if (args.length >= 3) {
            try { amount = Math.max(1, Math.min(2304, Integer.parseInt(args[2]))); }
            catch (NumberFormatException ex) {
                sender.sendMessage(c("&cAmount must be a number."));
                return true;
            }
        }

        int given = 0;
        for (int i = 0; i < amount; i++) {
            Map<Integer, ItemStack> overflow = target.getInventory().addItem(createTag());
            if (!overflow.isEmpty()) break;
            given++;
        }
        sender.sendMessage(c("&aGave &f" + given + " &aitem name tag" + (given == 1 ? "" : "s") + " to &f" + target.getName() + "&a."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("give");
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
