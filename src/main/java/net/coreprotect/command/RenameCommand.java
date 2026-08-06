package net.coreprotect.command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import net.coreprotect.database.Database;
import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RenameCommand extends Config {
   protected static void runCommand(final CommandSender player, boolean permission, String[] args) {
      int resultc = args.length;
      if (Config.converter_running) {
         player.sendMessage(Language.get("upgrade-in-progress-please-try-again"));
      } else if (Config.purge_running) {
         player.sendMessage(Language.get("purge-in-progress-please-try-again"));
      } else {
         if (permission) {
            if (player instanceof Player) {
               player.sendMessage(Language.get("this-command-must-be-used-via"));
               return;
            }

            if (resultc <= 2) {
               player.sendMessage(Language.get("please-use-co-rename-args"));
               return;
            }

            String rename_command = args[1].toLowerCase();
            if (!rename_command.equals("world")) {
               player.sendMessage(Language.get("please-use-co-rename-args"));
               return;
            }

            if (resultc > 3) {
               final String old_world = args[2];
               final String new_world = args[3];

               class BasicThread implements Runnable {
                  public void run() {
                     try {
                        Connection connection = Database.getConnection(false);
                        if (connection == null) {
                           player.sendMessage(Language.get("database-busy-please-try-again-later"));
                           return;
                        }

                        int wid = -1;
                        PreparedStatement preparedStmt = connection.prepareStatement("SELECT rowid FROM " + Config.prefix + "world WHERE world LIKE ?");
                        preparedStmt.setString(1, old_world);

                        ResultSet rs;
                        for(rs = preparedStmt.executeQuery(); rs.next(); wid = rs.getInt("rowid")) {
                        }

                        rs.close();
                        if (wid == -1) {
                           player.sendMessage(Language.get("world-not-found", old_world));
                           connection.close();
                           return;
                        }

                        preparedStmt = connection.prepareStatement("UPDATE " + Config.prefix + "world SET world = ? WHERE rowid='" + wid + "'");
                        preparedStmt.setString(1, new_world);
                        preparedStmt.executeUpdate();
                        Statement statement = connection.createStatement();
                        statement.close();
                        connection.close();
                        player.sendMessage(Language.get("world-renamed-to", old_world, new_world));
                     } catch (Exception e) {
                        e.printStackTrace();
                     }

                  }
               }

               Runnable runnable = new BasicThread();
               Thread thread = new Thread(runnable);
               thread.start();
            } else {
               player.sendMessage(Language.get("please-use-co-rename-world-old"));
            }
         } else {
            player.sendMessage(Language.get("you-do-not-have-permission-to"));
         }

      }
   }
}
