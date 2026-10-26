package net.coreprotect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.coreprotect.database.Lookup;
import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Makes the item name in a container transaction line interactive: hovering it
 * shows the logged item's tooltip (name, enchantments, lore, ...), clicking it
 * runs "/co copyitem <row>" for viewers allowed to take a copy. The two halves
 * are switched by item-hover and item-copy in config.yml.
 *
 * Callers put {@link #MARKER} where the item name goes in a message, so the
 * name can be cut out of the coloured text without the language file having
 * to know anything about chat components.
 */
public final class ItemLink {
   public static final String MARKER = "\u0000";
   public static final String COPY_PERMISSION = "coreprotect.copyitem";

   // A tooltip larger than this (written books, mostly) risks pushing the chat
   // packet past what the client accepts, which disconnects the viewer.
   private static final int MAX_NBT_LENGTH = 30000;

   private static volatile boolean nmsResolved = false;
   private static Method asNMSCopy;
   private static Method saveNbt;
   private static Class<?> nbtCompound;

   private ItemLink() {
   }

   public static boolean hoverEnabled() {
      Integer setting = Config.config.get("item-hover");
      return setting != null && setting == 1;
   }

   public static boolean copyEnabled() {
      Integer setting = Config.config.get("item-copy");
      return setting != null && setting == 1;
   }

   public static boolean canCopy(CommandSender sender) {
      return copyEnabled() && sender instanceof Player && (sender.isOp() || sender.hasPermission(COPY_PERMISSION));
   }

   /** Whether a lookup for this viewer needs the logged items rebuilt at all. */
   public static boolean wanted(CommandSender viewer) {
      return viewer instanceof Player && (hoverEnabled() || canCopy(viewer));
   }

   /**
    * Rebuilds a logged container item from its row. Returns null when the
    * material no longer resolves, in which case the line is sent as plain text.
    */
   public static ItemStack buildItem(int type, int data, int amount, byte[] metadata) {
      Material material = Functions.getType(type);
      if (material == null || material == Material.AIR) {
         return null;
      }

      ItemStack item = new ItemStack(material, Math.max(amount, 1), (short)data);
      return (ItemStack)Lookup.populateItemStack(item, metadata)[1];
   }

   /** Plain text, split on newlines the same way the old String results were. */
   public static List<BaseComponent[]> lines(String text) {
      List<BaseComponent[]> result = new ArrayList<>();
      for (String line : text.split("\n")) {
         result.add(TextComponent.fromLegacyText(line));
      }

      return result;
   }

   /** As {@link #lines(String)}, with the marker in any line replaced by the item. */
   public static List<BaseComponent[]> lines(String text, String name, ItemStack item, long row, boolean copy) {
      List<BaseComponent[]> result = new ArrayList<>();
      for (String line : text.split("\n")) {
         result.add(line(line, name, item, row, copy));
      }

      return result;
   }

   public static BaseComponent[] line(String line, String name, ItemStack item, long row, boolean copy) {
      int split = line.indexOf(MARKER);
      if (split < 0) {
         return TextComponent.fromLegacyText(line);
      }

      String before = line.substring(0, split);
      String after = line.substring(split + MARKER.length());
      boolean hover = item != null && hoverEnabled();
      copy = copy && item != null;
      if (!hover && !copy) {
         return TextComponent.fromLegacyText(before + name + after);
      }

      // Each legacy fragment becomes its own component, so the formatting in
      // force where the name starts has to be carried into the pieces after it.
      String color = ChatColor.getLastColors(before);
      HoverEvent hoverEvent = hoverEvent(item, hover, copy);
      ClickEvent clickEvent = copy ? new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/co copyitem " + row) : null;
      List<BaseComponent> parts = new ArrayList<>();
      Collections.addAll(parts, TextComponent.fromLegacyText(before));

      for (BaseComponent part : TextComponent.fromLegacyText(color + name)) {
         part.setHoverEvent(hoverEvent);
         part.setClickEvent(clickEvent);
         parts.add(part);
      }

      Collections.addAll(parts, TextComponent.fromLegacyText(color + after));
      return parts.toArray(new BaseComponent[0]);
   }

   public static void send(CommandSender to, List<BaseComponent[]> lines) {
      for (BaseComponent[] line : lines) {
         send(to, line);
      }
   }

   public static void send(CommandSender to, BaseComponent[] line) {
      if (to instanceof Player) {
         ((Player)to).spigot().sendMessage(line);
      } else {
         to.sendMessage(BaseComponent.toLegacyText(line));
      }
   }

   private static HoverEvent hoverEvent(ItemStack item, boolean hover, boolean copy) {
      if (hover) {
         // The client's own tooltip, exactly as it looks in an inventory.
         String nbt = itemNbt(item);
         if (nbt != null) {
            return new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[]{new TextComponent(nbt)});
         }
      }

      List<String> text = new ArrayList<>();
      if (hover) {
         text.addAll(describe(item));
      }

      if (copy) {
         text.add(Language.get("item-copy-hint"));
      }

      StringBuilder joined = new StringBuilder();
      for (String line : text) {
         if (joined.length() > 0) {
            joined.append('\n');
         }

         joined.append(line);
      }

      return new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(joined.toString()));
   }

   /** Fallback tooltip for when the server internals could not be reached. */
   private static List<String> describe(ItemStack item) {
      List<String> text = new ArrayList<>();
      ItemMeta meta = item.getItemMeta();
      if (meta != null && meta.hasDisplayName()) {
         text.add(meta.getDisplayName());
      } else {
         text.add(ChatColor.WHITE + item.getType().name().toLowerCase());
      }

      for (Map.Entry<Enchantment, Integer> enchant : item.getEnchantments().entrySet()) {
         text.add(ChatColor.GRAY + enchant.getKey().getName().toLowerCase() + " " + enchant.getValue());
      }

      if (meta != null && meta.hasLore()) {
         for (String lore : meta.getLore()) {
            text.add(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + lore);
         }
      }

      return text;
   }

   /**
    * The item's NBT in the form a SHOW_ITEM hover expects. Read through
    * CraftItemStack by reflection, since the server package is versioned;
    * null means the hover falls back to {@link #describe(ItemStack)}.
    */
   private static String itemNbt(ItemStack item) {
      resolveNms();
      if (asNMSCopy == null) {
         return null;
      }

      try {
         Object nmsItem = asNMSCopy.invoke(null, item);
         Object tag = saveNbt.invoke(nmsItem, nbtCompound.newInstance());
         String nbt = tag.toString();
         return nbt.length() > MAX_NBT_LENGTH ? null : nbt;
      } catch (Exception e) {
         return null;
      }
   }

   private static synchronized void resolveNms() {
      if (nmsResolved) {
         return;
      }

      try {
         String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
         String version = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
         Method copy = Class.forName(craftPackage + ".inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);
         Class<?> compound = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");
         Method save = copy.getReturnType().getMethod("save", compound);
         nbtCompound = compound;
         saveNbt = save;
         asNMSCopy = copy;
      } catch (Exception e) {
         System.out.println("[CoreProtect] Item tooltips unavailable on this server version; using a text summary instead.");
      }

      nmsResolved = true;
   }
}
