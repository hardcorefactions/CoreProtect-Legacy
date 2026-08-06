package net.coreprotect.command;

import net.coreprotect.model.Language;
import org.bukkit.command.CommandSender;

public class HelpCommand {
   protected static void runCommand(CommandSender player, boolean permission, String[] args) {
      int resultc = args.length;
      if (permission) {
         if (resultc > 1) {
            String helpcommand_original = args[1];
            String helpcommand = args[1].toLowerCase();
            helpcommand = helpcommand.replaceAll("[^a-zA-Z]", "");
            player.sendMessage(Language.get("coreprotect-help"));
            if (helpcommand.equals("help")) {
               player.sendMessage(Language.get("co-help-displays-a-list-of"));
            } else if (!helpcommand.equals("inspect") && !helpcommand.equals("inspector") && !helpcommand.equals("i")) {
               if (!helpcommand.equals("rollback") && !helpcommand.equals("rollbacks") && !helpcommand.equals("rb") && !helpcommand.equals("ro")) {
                  if (!helpcommand.equals("restore") && !helpcommand.equals("restores") && !helpcommand.equals("re") && !helpcommand.equals("rs")) {
                     if (!helpcommand.equals("lookup") && !helpcommand.equals("lookups") && !helpcommand.equals("l")) {
                        if (!helpcommand.equals("params") && !helpcommand.equals("param") && !helpcommand.equals("parameters") && !helpcommand.equals("parameter")) {
                           if (!helpcommand.equals("purge") && !helpcommand.equals("purges")) {
                              if (helpcommand.equals("version")) {
                                 player.sendMessage(Language.get("co-version-shows-the-version-of"));
                              } else if (!helpcommand.equals("u") && !helpcommand.equals("user") && !helpcommand.equals("users") && !helpcommand.equals("uuser") && !helpcommand.equals("uusers")) {
                                 if (!helpcommand.equals("t") && !helpcommand.equals("time") && !helpcommand.equals("ttime")) {
                                    if (!helpcommand.equals("r") && !helpcommand.equals("radius") && !helpcommand.equals("rradius")) {
                                       if (!helpcommand.equals("a") && !helpcommand.equals("action") && !helpcommand.equals("actions") && !helpcommand.equals("aaction")) {
                                          if (!helpcommand.equals("b") && !helpcommand.equals("block") && !helpcommand.equals("blocks") && !helpcommand.equals("bblock") && !helpcommand.equals("bblocks")) {
                                             if (!helpcommand.equals("e") && !helpcommand.equals("exclude") && !helpcommand.equals("eexclude")) {
                                                player.sendMessage(Language.get("information-for-command-co-help-not", helpcommand_original));
                                             } else {
                                                player.sendMessage(Language.get("co-lookup-e-exclude-exclude-blocks"));
                                                player.sendMessage(Language.get("examples-e-stone-e-notch-e"));
                                                player.sendMessage(Language.get("block-names-http-minecraft-gamepedia-com"));
                                             }
                                          } else {
                                             player.sendMessage(Language.get("co-lookup-b-blocks-restrict-the"));
                                             player.sendMessage(Language.get("examples-b-stone-b-stone-wood"));
                                             player.sendMessage(Language.get("block-names-http-minecraft-gamepedia-com"));
                                          }
                                       } else {
                                          player.sendMessage(Language.get("co-lookup-a-action-restrict-the"));
                                          player.sendMessage(Language.get("examples-a-block-a-block-a"));
                                       }
                                    } else {
                                       player.sendMessage(Language.get("co-lookup-r-radius-specify-a"));
                                       player.sendMessage(Language.get("examples-r-10-only-make-changes"));
                                    }
                                 } else {
                                    player.sendMessage(Language.get("co-lookup-t-time-specify-the"));
                                    player.sendMessage(Language.get("examples-t-2w-5d-7h-2m"));
                                 }
                              } else {
                                 player.sendMessage(Language.get("co-lookup-u-users-specify-the"));
                                 player.sendMessage(Language.get("examples-u-notch-u-notch-enderman"));
                              }
                           } else {
                              player.sendMessage(Language.get("co-purge-t-time-delete-data"));
                              player.sendMessage(Language.get("for-example-co-purge-t-30d"));
                           }
                        } else {
                           player.sendMessage(Language.get("co-lookup-params-perform-the-lookup"));
                           player.sendMessage(Language.get("u-users-specify-the-user-s"));
                           player.sendMessage(Language.get("t-time-specify-the-amount-of"));
                           player.sendMessage(Language.get("r-radius-specify-a-radius-area"));
                           player.sendMessage(Language.get("a-action-restrict-the-rollback-to"));
                           player.sendMessage(Language.get("b-blocks-restrict-the-lookup-to"));
                           player.sendMessage(Language.get("e-exclude-exclude-blocks-users-from"));
                           player.sendMessage(Language.get("please-see-co-help-param-for"));
                        }
                     } else {
                        player.sendMessage(Language.get("co-lookup-params"));
                        player.sendMessage(Language.get("co-l-params-command-shortcut"));
                        player.sendMessage(Language.get("co-lookup-page-use-after-inspecting"));
                        player.sendMessage(Language.get("please-see-co-help-params-for"));
                     }
                  } else {
                     player.sendMessage(Language.get("co-restore-params-perform-the-restore"));
                     player.sendMessage(Language.get("u-users-specify-the-user-s-2"));
                     player.sendMessage(Language.get("t-time-specify-the-amount-of-2"));
                     player.sendMessage(Language.get("r-radius-specify-a-radius-area-2"));
                     player.sendMessage(Language.get("a-action-restrict-the-rollback-to"));
                     player.sendMessage(Language.get("b-blocks-restrict-the-restore-to"));
                     player.sendMessage(Language.get("e-exclude-exclude-blocks-users-from-2"));
                     player.sendMessage(Language.get("please-see-co-help-param-for"));
                  }
               } else {
                  player.sendMessage(Language.get("co-rollback-params-perform-the-rollback"));
                  player.sendMessage(Language.get("u-users-specify-the-user-s-3"));
                  player.sendMessage(Language.get("t-time-specify-the-amount-of-3"));
                  player.sendMessage(Language.get("r-radius-specify-a-radius-area-3"));
                  player.sendMessage(Language.get("a-action-restrict-the-rollback-to"));
                  player.sendMessage(Language.get("b-blocks-restrict-the-rollback-to"));
                  player.sendMessage(Language.get("e-exclude-exclude-blocks-users-from-3"));
                  player.sendMessage(Language.get("please-see-co-help-param-for"));
               }
            } else {
               player.sendMessage(Language.get("with-the-inspector-enabled-you-can"));
               player.sendMessage("* Left-click a block to see who placed that block.");
               player.sendMessage("* Right-click a block to see what adjacent block was removed.");
               player.sendMessage("* Place a block to see what block was removed at the location.");
               player.sendMessage("* Place a block in liquid (etc) to see who placed it.");
               player.sendMessage("* Right-click on a door, chest, etc, to see who last used it.");
               player.sendMessage(Language.get("tip-you-can-use-just-co"));
            }
         } else {
            player.sendMessage(Language.get("coreprotect-help"));
            player.sendMessage(Language.get("co-help-command-display-more-info"));
            player.sendMessage(Language.get("co-inspect-turns-the-block-inspector"));
            player.sendMessage(Language.get("co-rollback-params-rollback-block-data"));
            player.sendMessage(Language.get("co-restore-params-restore-block-data"));
            player.sendMessage(Language.get("co-lookup-params-advanced-block-data"));
            player.sendMessage(Language.get("co-purge-params-delete-old-block"));
            player.sendMessage(Language.get("co-reload-reloads-the-configuration-file"));
            player.sendMessage(Language.get("co-version-displays-the-plugin-version"));
         }
      } else {
         player.sendMessage(Language.get("you-do-not-have-permission-to"));
      }

   }
}
