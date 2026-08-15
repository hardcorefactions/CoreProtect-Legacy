package net.coreprotect.command;

import java.util.List;
import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
import org.bukkit.command.CommandSender;

/**
 * /co apply -- turns the sender's pending preview into a real rollback.
 */
public class ApplyCommand {
   protected static void runCommand(CommandSender user, boolean permission, String[] args) {
      try {
         List<Object[]> last = Config.last_rollback.get(user.getName());
         if (last == null) {
            user.sendMessage(Language.get("no-pending-rollback-restore-found"));
            return;
         }

         int time = (Integer) last.get(0)[0];
         args = (String[]) last.get(1);

         // Dropping the marker turns the stored preview into the real thing.
         boolean pending = false;

         for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("#preview")) {
               pending = true;
               args[i] = args[i].replace("#preview", "");
            }
         }

         if (!pending) {
            user.sendMessage(Language.get("no-pending-rollback-restore-found"));
            return;
         }

         Config.last_rollback.remove(user.getName());
         RollbackRestoreCommand.runCommand(user, permission, args, time, RollbackRestoreCommand.storedLocation(last));
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
