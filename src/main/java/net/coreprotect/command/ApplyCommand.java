package net.coreprotect.command;

import java.util.List;
import net.coreprotect.model.Config;
import org.bukkit.command.CommandSender;

public class ApplyCommand {
   protected static void runCommand(CommandSender user, boolean permission, String[] args) {
      try {
         if (Config.last_rollback.get(user.getName()) != null) {
            List<Object[]> list = (List)Config.last_rollback.get(user.getName());
            int time = (Integer)((Object[])list.get(0))[0];
            args = (String[])list.get(1);
            boolean valid = false;

            for(int i = 0; i < args.length; ++i) {
               if (args[i].equals("#preview")) {
                  valid = true;
                  args[i] = args[i].replaceAll("#preview", "");
               }
            }

            if (!valid) {
               user.sendMessage("§3CoreProtect §f- No pending rollback/restore found.");
            } else {
               Config.last_rollback.remove(user.getName());
               RollbackRestoreCommand.runCommand(user, permission, args, time);
            }
         } else {
            user.sendMessage("§3CoreProtect §f- No pending rollback/restore found.");
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }
}
