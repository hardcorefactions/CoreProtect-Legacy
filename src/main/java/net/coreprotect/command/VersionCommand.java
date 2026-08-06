package net.coreprotect.command;

import net.coreprotect.CoreProtect;
import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
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

      player.sendMessage(Language.get("coreprotect"));
      player.sendMessage(Language.get("version-coreprotect-v", pdfFile.getVersion(), versionCheck));
      if ((Integer)Config.config.get("use-mysql") == 1) {
         player.sendMessage(Language.get("storage-using-mysql"));
      } else {
         player.sendMessage(Language.get("storage-using-sqlite"));
      }

      player.sendMessage(Language.get("download-http-coreprotect-net-download"));
      player.sendMessage(Language.get("sponsor-http-hosthorde-com"));
   }
}
