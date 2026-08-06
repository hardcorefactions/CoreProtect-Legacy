package net.coreprotect.command;

import net.coreprotect.CoreProtect;
import net.coreprotect.model.Config;
import net.coreprotect.thread.CheckUpdate;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

public class VersionCommand {
   protected static void runCommand(CommandSender player, boolean permission, String[] args) {
      PluginDescriptionFile pdfFile = CoreProtect.getInstance().getDescription();
      String versionCheck = "";
      if ((Integer)Config.config.get("check-updates") == 1) {
         String latestVersion = CheckUpdate.latestVersion();
         if (latestVersion != null) {
            versionCheck = " (Latest Version: v" + latestVersion + ")";
         }
      }

      player.sendMessage("§f----- §3CoreProtect §f-----");
      player.sendMessage("§3Version: §fCoreProtect v" + pdfFile.getVersion() + "." + versionCheck);
      if ((Integer)Config.config.get("use-mysql") == 1) {
         player.sendMessage("§3Storage: §fUsing MySQL.");
      } else {
         player.sendMessage("§3Storage: §fUsing SQLite.");
      }

      player.sendMessage("§3Download: §fhttp://coreprotect.net/download/");
      player.sendMessage("§3Sponsor: §fhttp://hosthorde.com");
   }
}
