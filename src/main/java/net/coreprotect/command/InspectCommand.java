package net.coreprotect.command;

import net.coreprotect.model.Config;
import org.bukkit.command.CommandSender;

public class InspectCommand {
   protected static void runCommand(CommandSender player, boolean permission, String[] args) {
      if (permission) {
         int command = -1;
         if (Config.inspecting.get(player.getName()) == null) {
            Config.inspecting.put(player.getName(), false);
         }

         if (args.length > 1) {
            String action = args[1];
            if (action.equalsIgnoreCase("on")) {
               command = 1;
            } else if (action.equalsIgnoreCase("off")) {
               command = 0;
            }
         }

         if (!(Boolean)Config.inspecting.get(player.getName())) {
            if (command == 0) {
               player.sendMessage("§3CoreProtect §f- Inspector already disabled.");
            } else {
               player.sendMessage("§3CoreProtect §f- Inspector now enabled.");
               Config.inspecting.put(player.getName(), true);
            }
         } else if (command == 1) {
            player.sendMessage("§3CoreProtect §f- Inspector already enabled.");
         } else {
            player.sendMessage("§3CoreProtect §f- Inspector now disabled.");
            Config.inspecting.put(player.getName(), false);
         }
      } else {
         player.sendMessage("§3CoreProtect §f- You do not have permission to do that.");
      }

   }
}
