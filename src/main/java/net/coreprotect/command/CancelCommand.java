package net.coreprotect.command;

import java.util.List;
import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
import org.bukkit.command.CommandSender;

public class CancelCommand {
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
                  args[i] = args[i].replace("#preview", "#preview_cancel");
               }
            }

            if (!valid) {
               user.sendMessage(Language.get("no-pending-rollback-restore-found"));
            } else {
               Config.last_rollback.remove(user.getName());
               RollbackRestoreCommand.runCommand(user, permission, args, time);
            }
         } else {
            user.sendMessage(Language.get("no-pending-rollback-restore-found"));
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }
}
