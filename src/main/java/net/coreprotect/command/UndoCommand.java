package net.coreprotect.command;

import java.util.List;
import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
import org.bukkit.command.CommandSender;

/**
 * /co undo -- re-runs the sender's last rollback or restore as its opposite.
 */
public class UndoCommand {
   protected static void runCommand(CommandSender user, boolean permission, String[] args) {
      try {
         List<Object[]> last = Config.last_rollback.get(user.getName());
         if (last == null) {
            user.sendMessage(Language.get("no-previous-rollback-restore-found"));
            return;
         }

         int time = (Integer) last.get(0)[0];
         args = (String[]) last.get(1);

         // A pending preview is cancelled rather than inverted.
         for (String arg : args) {
            if (arg.equals("#preview")) {
               CancelCommand.runCommand(user, permission, args);
               return;
            }
         }

         String command = args[0];
         if (command.equals("rollback") || command.equals("rb") || command.equals("ro")) {
            args[0] = "restore";
         } else if (command.equals("restore") || command.equals("rs") || command.equals("re")) {
            args[0] = "rollback";
         } else {
            return;
         }

         Config.last_rollback.remove(user.getName());
         RollbackRestoreCommand.runCommand(user, permission, args, time, RollbackRestoreCommand.storedLocation(last));
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
