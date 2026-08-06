package net.coreprotect.command;

import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
import net.coreprotect.thread.CheckUpdate;
import org.bukkit.command.CommandSender;

public class ReloadCommand {
   protected static void runCommand(final CommandSender player, boolean permission, String[] args) {
      if (permission) {
         if (Config.converter_running) {
            player.sendMessage(Language.get("upgrade-in-progress-please-try-again"));
            return;
         }

         if (Config.purge_running) {
            player.sendMessage(Language.get("purge-in-progress-please-try-again"));
            return;
         }

         class BasicThread implements Runnable {
            public void run() {
               try {
                  Config.performInitialization();
                  Language.load();
                  if ((Integer)Config.config.get("check-updates") == 1) {
                     Thread checkUpdateThread = new Thread(new CheckUpdate(false));
                     checkUpdateThread.start();
                  }

                  player.sendMessage(Language.get("configuration-reloaded"));
               } catch (Exception e) {
                  e.printStackTrace();
               }

            }
         }

         Runnable runnable = new BasicThread();
         Thread thread = new Thread(runnable);
         thread.start();
      } else {
         player.sendMessage(Language.get("you-do-not-have-permission-to"));
      }

   }
}
