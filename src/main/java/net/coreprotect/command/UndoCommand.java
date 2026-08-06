package net.coreprotect.command;

import java.util.List;
import net.coreprotect.model.Config;
import org.bukkit.command.CommandSender;

public class UndoCommand {
   protected static void runCommand(CommandSender user, boolean permission, String[] args) {
      try {
         if (Config.last_rollback.get(user.getName()) != null) {
            List<Object[]> list = (List)Config.last_rollback.get(user.getName());
            int time = (Integer)((Object[])list.get(0))[0];
            args = (String[])list.get(1);

            for(String arg : args) {
               if (arg.equals("#preview")) {
                  CancelCommand.runCommand(user, permission, args);
                  return;
               }
            }

            boolean valid = true;
            if (!args[0].equals("rollback") && !args[0].equals("rb") && !args[0].equals("ro")) {
               if (!args[0].equals("restore") && !args[0].equals("rs") && !args[0].equals("re")) {
                  valid = false;
               } else {
                  args[0] = "rollback";
               }
            } else {
               args[0] = "restore";
            }

            if (valid) {
               Config.last_rollback.remove(user.getName());
               RollbackRestoreCommand.runCommand(user, permission, args, time);
            }
         } else {
            user.sendMessage("§3CoreProtect §f- No previous rollback/restore found.");
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }
}
