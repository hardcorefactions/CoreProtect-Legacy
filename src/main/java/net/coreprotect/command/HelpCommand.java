package net.coreprotect.command;

import org.bukkit.command.CommandSender;

public class HelpCommand {
   protected static void runCommand(CommandSender player, boolean permission, String[] args) {
      int resultc = args.length;
      if (permission) {
         if (resultc > 1) {
            String helpcommand_original = args[1];
            String helpcommand = args[1].toLowerCase();
            helpcommand = helpcommand.replaceAll("[^a-zA-Z]", "");
            player.sendMessage("§f----- §3CoreProtect Help §f-----");
            if (helpcommand.equals("help")) {
               player.sendMessage("§3/co help §f- Displays a list of all commands.");
            } else if (!helpcommand.equals("inspect") && !helpcommand.equals("inspector") && !helpcommand.equals("i")) {
               if (!helpcommand.equals("rollback") && !helpcommand.equals("rollbacks") && !helpcommand.equals("rb") && !helpcommand.equals("ro")) {
                  if (!helpcommand.equals("restore") && !helpcommand.equals("restores") && !helpcommand.equals("re") && !helpcommand.equals("rs")) {
                     if (!helpcommand.equals("lookup") && !helpcommand.equals("lookups") && !helpcommand.equals("l")) {
                        if (!helpcommand.equals("params") && !helpcommand.equals("param") && !helpcommand.equals("parameters") && !helpcommand.equals("parameter")) {
                           if (!helpcommand.equals("purge") && !helpcommand.equals("purges")) {
                              if (helpcommand.equals("version")) {
                                 player.sendMessage("§3/co version §f- Shows the version of CoreProtect you're using.");
                              } else if (!helpcommand.equals("u") && !helpcommand.equals("user") && !helpcommand.equals("users") && !helpcommand.equals("uuser") && !helpcommand.equals("uusers")) {
                                 if (!helpcommand.equals("t") && !helpcommand.equals("time") && !helpcommand.equals("ttime")) {
                                    if (!helpcommand.equals("r") && !helpcommand.equals("radius") && !helpcommand.equals("rradius")) {
                                       if (!helpcommand.equals("a") && !helpcommand.equals("action") && !helpcommand.equals("actions") && !helpcommand.equals("aaction")) {
                                          if (!helpcommand.equals("b") && !helpcommand.equals("block") && !helpcommand.equals("blocks") && !helpcommand.equals("bblock") && !helpcommand.equals("bblocks")) {
                                             if (!helpcommand.equals("e") && !helpcommand.equals("exclude") && !helpcommand.equals("eexclude")) {
                                                player.sendMessage("§fInformation for command \"§3/co help " + helpcommand_original + "§f\" not found.");
                                             } else {
                                                player.sendMessage("§3/co lookup e:<exclude> §f- Exclude blocks/users.");
                                                player.sendMessage("§7§oExamples: [e:stone], [e:Notch], e:[stone,Notch]");
                                                player.sendMessage("§7§oBlock Names: http://minecraft.gamepedia.com/Blocks");
                                             }
                                          } else {
                                             player.sendMessage("§3/co lookup b:<blocks> §f- Restrict the lookup to certain blocks.");
                                             player.sendMessage("§7§oExamples: [b:stone], [b:stone,wood,bedrock]");
                                             player.sendMessage("§7§oBlock Names: http://minecraft.gamepedia.com/Blocks");
                                          }
                                       } else {
                                          player.sendMessage("§3/co lookup a:<action> §f- Restrict the lookup to a certain action.");
                                          player.sendMessage("§7§oExamples: [a:block], [a:+block], [a:-block] [a:click], [a:container], [a:kill], [a:chat], [a:command], [a:session], [a:username]");
                                       }
                                    } else {
                                       player.sendMessage("§3/co lookup r:<radius> §f- Specify a radius area.");
                                       player.sendMessage("§7§oExamples: [r:10] (Only make changes within 10 blocks of you)");
                                    }
                                 } else {
                                    player.sendMessage("§3/co lookup t:<time> §f- Specify the amount of time to lookup.");
                                    player.sendMessage("§7§oExamples: [t:2w,5d,7h,2m,10s], [t:5d2h], [t:2.50h]");
                                 }
                              } else {
                                 player.sendMessage("§3/co lookup u:<users> §f- Specify the user(s) to lookup.");
                                 player.sendMessage("§7§oExamples: [u:Notch], [u:Notch,#enderman]");
                              }
                           } else {
                              player.sendMessage("§3/co purge t:<time> §f- Delete data older than specified time.");
                              player.sendMessage("§7§oFor example, \"/co purge t:30d\" will delete all data older than one month, and only keep the last 30 days of data.");
                           }
                        } else {
                           player.sendMessage("§3/co lookup §7<params> §f- Perform the lookup.");
                           player.sendMessage("§3| §7u:<users> §f- Specify the user(s) to lookup.");
                           player.sendMessage("§3| §7t:<time> §f- Specify the amount of time to lookup.");
                           player.sendMessage("§3| §7r:<radius> §f- Specify a radius area to limit the lookup to.");
                           player.sendMessage("§3| §7a:<action> §f- Restrict the rollback to a certain action.");
                           player.sendMessage("§3| §7b:<blocks> §f- Restrict the lookup to certain block types.");
                           player.sendMessage("§3| §7e:<exclude> §f- Exclude blocks/users from the lookup.");
                           player.sendMessage("§7§oPlease see \"/co help <param>\" for detailed parameter info.");
                        }
                     } else {
                        player.sendMessage("§3/co lookup <params>");
                        player.sendMessage("§3/co l <params> §f- Command shortcut.");
                        player.sendMessage("§3/co lookup <page> §f- Use after inspecting a block to view logs.");
                        player.sendMessage("§7§oPlease see \"/co help params\" for detailed parameters.");
                     }
                  } else {
                     player.sendMessage("§3/co restore §7<params> §f- Perform the restore.");
                     player.sendMessage("§3| §7u:<users> §f- Specify the user(s) to restore.");
                     player.sendMessage("§3| §7t:<time> §f- Specify the amount of time to restore.");
                     player.sendMessage("§3| §7r:<radius> §f- Specify a radius area to limit the restore to.");
                     player.sendMessage("§3| §7a:<action> §f- Restrict the rollback to a certain action.");
                     player.sendMessage("§3| §7b:<blocks> §f- Restrict the restore to certain block types.");
                     player.sendMessage("§3| §7e:<exclude> §f- Exclude blocks/users from the restore.");
                     player.sendMessage("§7§oPlease see \"/co help <param>\" for detailed parameter info.");
                  }
               } else {
                  player.sendMessage("§3/co rollback §7<params> §f- Perform the rollback.");
                  player.sendMessage("§3| §7u:<users> §f- Specify the user(s) to rollback.");
                  player.sendMessage("§3| §7t:<time> §f- Specify the amount of time to rollback.");
                  player.sendMessage("§3| §7r:<radius> §f- Specify a radius area to limit the rollback to.");
                  player.sendMessage("§3| §7a:<action> §f- Restrict the rollback to a certain action.");
                  player.sendMessage("§3| §7b:<blocks> §f- Restrict the rollback to certain block types.");
                  player.sendMessage("§3| §7e:<exclude> §f- Exclude blocks/users from the rollback.");
                  player.sendMessage("§7§oPlease see \"/co help <param>\" for detailed parameter info.");
               }
            } else {
               player.sendMessage("§3With the inspector enabled, you can do the following:");
               player.sendMessage("* Left-click a block to see who placed that block.");
               player.sendMessage("* Right-click a block to see what adjacent block was removed.");
               player.sendMessage("* Place a block to see what block was removed at the location.");
               player.sendMessage("* Place a block in liquid (etc) to see who placed it.");
               player.sendMessage("* Right-click on a door, chest, etc, to see who last used it.");
               player.sendMessage("§7§oTip: You can use just \"/co i\" for quicker access.");
            }
         } else {
            player.sendMessage("§f----- §3CoreProtect Help §f-----");
            player.sendMessage("§3/co help §7<command> §f- Display more info for that command.");
            player.sendMessage("§3/co §7inspect §f- Turns the block inspector on or off.");
            player.sendMessage("§3/co §7rollback §3<params> §f- Rollback block data.");
            player.sendMessage("§3/co §7restore §3<params> §f- Restore block data.");
            player.sendMessage("§3/co §7lookup §3<params> §f- Advanced block data lookup.");
            player.sendMessage("§3/co §7purge §3<params> §f- Delete old block data.");
            player.sendMessage("§3/co §7reload §f- Reloads the configuration file.");
            player.sendMessage("§3/co §7version §f- Displays the plugin version.");
         }
      } else {
         player.sendMessage("§3CoreProtect §f- You do not have permission to do that.");
      }

   }
}
