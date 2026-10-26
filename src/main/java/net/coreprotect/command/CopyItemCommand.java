package net.coreprotect.command;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import net.coreprotect.CoreProtect;
import net.coreprotect.Functions;
import net.coreprotect.ItemLink;
import net.coreprotect.database.Database;
import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /co copyitem <row> -- gives the sender an exact copy of the item logged in a
 * container transaction row. Run by clicking an item name in a lookup; the
 * permission is checked again here, since anyone can type the command.
 */
public class CopyItemCommand {
   protected static void runCommand(final CommandSender user, boolean permission, String[] args) {
      if (!ItemLink.copyEnabled()) {
         user.sendMessage(Language.get("item-copy-disabled"));
         return;
      }

      if (!(user instanceof Player)) {
         user.sendMessage(Language.get("item-copy-players-only"));
         return;
      }

      if (!ItemLink.canCopy(user)) {
         user.sendMessage(Language.get("you-do-not-have-permission-to"));
         return;
      }

      final long row;
      try {
         row = args.length > 1 ? Long.parseLong(args[1]) : -1L;
      } catch (NumberFormatException e) {
         user.sendMessage(Language.get("item-copy-usage"));
         return;
      }

      if (row < 0L) {
         user.sendMessage(Language.get("item-copy-usage"));
         return;
      }

      final Player player = (Player)user;
      Thread thread = new Thread(() -> {
         ItemStack item = null;
         String name = "";

         try {
            Connection connection = Database.getConnection(false);
            if (connection == null) {
               player.sendMessage(Language.get("database-busy-please-try-again-later"));
               return;
            }

            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT type,data,amount,metadata FROM " + Config.prefix + "container WHERE rowid = '" + row + "' LIMIT 0, 1");
            if (rs.next()) {
               int type = rs.getInt("type");
               int data = rs.getInt("data");
               item = ItemLink.buildItem(type, data, rs.getInt("amount"), rs.getBytes("metadata"));
               name = Functions.nameFilter(Functions.getTypeName(type).toLowerCase(), data);
            }

            rs.close();
            statement.close();
            connection.close();
         } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(Language.get("database-query-failed"));
            return;
         }

         if (item == null) {
            player.sendMessage(Language.get("item-copy-not-found"));
            return;
         }

         final ItemStack copy = item;
         final String copy_name = name;
         CoreProtect.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(CoreProtect.getInstance(), () -> {
            if (!player.isOnline()) {
               return;
            }

            // Whatever does not fit is dropped at the player's feet rather than lost.
            for (ItemStack leftover : player.getInventory().addItem(copy).values()) {
               player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }

            player.sendMessage(Language.get("item-copy-given", copy.getAmount(), copy_name));
         });
      });
      thread.start();
   }
}
