package net.coreprotect.command;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import net.coreprotect.Functions;
import net.coreprotect.consumer.Consumer;
import net.coreprotect.database.Database;
import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
import net.coreprotect.patch.Patch;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PurgeCommand extends Consumer {
   protected static void runCommand(final CommandSender player, boolean permission, String[] args) {
      int resultc = args.length;
      final int seconds = CommandHandler.parseTime(args);
      if (Config.converter_running) {
         player.sendMessage(Language.get("upgrade-in-progress-please-try-again"));
      } else if (Config.purge_running) {
         player.sendMessage(Language.get("purge-in-progress-please-try-again"));
      } else if (!permission) {
         player.sendMessage(Language.get("you-do-not-have-permission-to"));
      } else if (resultc <= 1) {
         player.sendMessage(Language.get("please-use-co-purge-t-time"));
      } else if (seconds <= 0) {
         player.sendMessage(Language.get("please-use-co-purge-t-time"));
      } else if (player instanceof Player && seconds < (Integer)Config.config.get("purge-minimum-time")) {
         player.sendMessage(Language.get("purge-minimum-age", Functions.formatDuration((Integer)Config.config.get("purge-minimum-time"))));
      } else if (seconds < (Integer)Config.config.get("purge-minimum-time-console")) {
         player.sendMessage(Language.get("purge-minimum-age-console", Functions.formatDuration((Integer)Config.config.get("purge-minimum-time-console"))));
      } else {
         boolean optimizeCheckValue = false;

         for(String arg : args) {
            if (arg.trim().equalsIgnoreCase("#optimize")) {
               optimizeCheckValue = true;
               break;
            }
         }

         final boolean optimizeCheck = optimizeCheckValue;

         class BasicThread implements Runnable {
            public void run() {
               try {
                  int timestamp = (int)(System.currentTimeMillis() / 1000L);
                  int ptime = timestamp - seconds;
                  long removed = 0L;
                  Connection connection = null;

                  for(int i = 0; i <= 5; ++i) {
                     connection = Database.getConnection(false);
                     if (connection != null) {
                        break;
                     }

                     Thread.sleep(1000L);
                  }

                  if (connection == null) {
                     Functions.messageOwnerAndUser(player, Language.get("database-busy-please-try-again-later-2"));
                     return;
                  }

                  Functions.messageOwnerAndUser(player, Language.get("data-purge-started-this-may-take"));
                  Functions.messageOwnerAndUser(player, Language.get("do-not-restart-your-server-until"));
                  Config.purge_running = true;

                  while(!PurgeCommand.pause_success) {
                     Thread.sleep(1L);
                  }

                  Consumer.is_paused = true;
                  String query = "";
                  PreparedStatement preparedStmt = null;
                  boolean abort = false;
                  String purge_prefix = "tmp_" + Config.prefix;
                  if ((Integer)Config.config.get("use-mysql") == 0) {
                     query = "ATTACH DATABASE '" + Config.sqlite + ".tmp' AS tmp_db";
                     preparedStmt = connection.prepareStatement(query);
                     preparedStmt.execute();
                     preparedStmt.close();
                     purge_prefix = "tmp_db." + Config.prefix;
                  }

                  Integer[] last_version = Patch.getLastVersion(connection);
                  boolean newVersion = Functions.newVersion(last_version, Functions.getPluginVersion());
                  if (newVersion) {
                     Functions.messageOwnerAndUser(player, Language.get("purge-failed-please-try-again-later"));
                     Consumer.is_paused = false;
                     Config.purge_running = false;
                     return;
                  }

                  if ((Integer)Config.config.get("use-mysql") == 0) {
                     for(String table : Config.databaseTables) {
                        try {
                           query = "DROP TABLE IF EXISTS " + purge_prefix + table + "";
                           preparedStmt = connection.prepareStatement(query);
                           preparedStmt.execute();
                           preparedStmt.close();
                        } catch (Exception e) {
                           e.printStackTrace();
                        }
                     }

                     Functions.createDatabaseTables(purge_prefix, true);
                  }

                  List<String> purge_tables = Arrays.asList("sign", "container", "skull", "session", "chat", "command", "entity", "block");

                  for(String table : Config.databaseTables) {
                     String tableName = table.replaceAll("_", " ");
                     Functions.messageOwnerAndUser(player, Language.get("processing-data", tableName));
                     if ((Integer)Config.config.get("use-mysql") == 0) {
                        String columns = "";
                        ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM " + purge_prefix + table);
                        ResultSetMetaData resultSetMetaData = rs.getMetaData();
                        int columnCount = resultSetMetaData.getColumnCount();

                        for(int i = 1; i <= columnCount; ++i) {
                           String name = resultSetMetaData.getColumnName(i);
                           if (columns.length() == 0) {
                              columns = name;
                           } else {
                              columns = columns + "," + name;
                           }
                        }

                        rs.close();
                        boolean error = false;

                        try {
                           String time_limit = "";
                           if (purge_tables.contains(table)) {
                              time_limit = " WHERE time >= '" + ptime + "'";
                           }

                           query = "INSERT INTO " + purge_prefix + table + " SELECT " + columns + " FROM " + Config.prefix + table + time_limit;
                           preparedStmt = connection.prepareStatement(query);
                           preparedStmt.execute();
                           preparedStmt.close();
                        } catch (Exception e) {
                           error = true;
                           e.printStackTrace();
                        }

                        if (error) {
                           Functions.messageOwnerAndUser(player, Language.get("unable-to-process-data", tableName));
                           Functions.messageOwnerAndUser(player, Language.get("attempting-to-repair-this-may-take"));

                           try {
                              query = "DELETE FROM " + purge_prefix + table;
                              preparedStmt = connection.prepareStatement(query);
                              preparedStmt.execute();
                              preparedStmt.close();
                           } catch (Exception e) {
                              e.printStackTrace();
                           }

                           try {
                              query = "REINDEX " + Config.prefix + table;
                              preparedStmt = connection.prepareStatement(query);
                              preparedStmt.execute();
                              preparedStmt.close();
                           } catch (Exception e) {
                              e.printStackTrace();
                           }

                           try {
                              String index = " NOT INDEXED";
                              query = "INSERT INTO " + purge_prefix + table + " SELECT " + columns + " FROM " + Config.prefix + table + index;
                              preparedStmt = connection.prepareStatement(query);
                              preparedStmt.execute();
                              preparedStmt.close();
                           } catch (Exception e) {
                              e.printStackTrace();
                              abort = true;
                              break;
                           }

                           if (purge_tables.contains(table)) {
                              try {
                                 query = "DELETE FROM " + purge_prefix + table + " WHERE time < '" + ptime + "'";
                                 preparedStmt = connection.prepareStatement(query);
                                 preparedStmt.execute();
                                 preparedStmt.close();
                              } catch (Exception e) {
                                 e.printStackTrace();
                              }
                           }
                        }

                        int old_count = 0;

                        try {
                           query = "SELECT COUNT(*) as count FROM " + Config.prefix + table + " LIMIT 0, 1";
                           preparedStmt = connection.prepareStatement(query);

                           ResultSet resultSet;
                           for(resultSet = preparedStmt.executeQuery(); resultSet.next(); old_count = resultSet.getInt("count")) {
                           }

                           resultSet.close();
                           preparedStmt.close();
                        } catch (Exception e) {
                           e.printStackTrace();
                        }

                        int new_count = 0;

                        try {
                           query = "SELECT COUNT(*) as count FROM " + purge_prefix + table + " LIMIT 0, 1";
                           preparedStmt = connection.prepareStatement(query);

                           ResultSet resultSet;
                           for(resultSet = preparedStmt.executeQuery(); resultSet.next(); new_count = resultSet.getInt("count")) {
                           }

                           resultSet.close();
                           preparedStmt.close();
                        } catch (Exception e) {
                           e.printStackTrace();
                        }

                        removed += (long)(old_count - new_count);
                     }

                     if ((Integer)Config.config.get("use-mysql") == 1) {
                        try {
                           if (purge_tables.contains(table)) {
                              query = "DELETE FROM " + Config.prefix + table + " WHERE time < '" + ptime + "'";
                              preparedStmt = connection.prepareStatement(query);
                              preparedStmt.execute();
                              removed += (long)preparedStmt.getUpdateCount();
                              preparedStmt.close();
                           }
                        } catch (Exception e) {
                           e.printStackTrace();
                        }
                     }
                  }

                  if ((Integer)Config.config.get("use-mysql") == 1 && optimizeCheck) {
                     Functions.messageOwnerAndUser(player, Language.get("optimizing-database-please-wait"));

                     for(String table : Config.databaseTables) {
                        query = "OPTIMIZE LOCAL TABLE " + Config.prefix + table + "";
                        preparedStmt = connection.prepareStatement(query);
                        preparedStmt.execute();
                        preparedStmt.close();
                     }
                  }

                  connection.close();
                  if (abort) {
                     if ((Integer)Config.config.get("use-mysql") == 0) {
                        (new File(Config.sqlite + ".tmp")).delete();
                     }

                     Config.loadDatabase();
                     Functions.messageOwnerAndUser(player, Language.get("purge-failed-database-may-be-corrupt"));
                     Consumer.is_paused = false;
                     Config.purge_running = false;
                     return;
                  }

                  if ((Integer)Config.config.get("use-mysql") == 0) {
                     (new File(Config.sqlite)).delete();
                     (new File(Config.sqlite + ".tmp")).renameTo(new File(Config.sqlite));
                     Functions.messageOwnerAndUser(player, Language.get("indexing-database-please-wait"));
                  }

                  Config.loadDatabase();
                  Functions.messageOwnerAndUser(player, Language.get("data-purge-successful"));
                  Functions.messageOwnerAndUser(player, NumberFormat.getInstance().format(removed) + " row(s) of data deleted.");
               } catch (Exception e) {
                  Functions.messageOwnerAndUser(player, Language.get("purge-failed-please-try-again-later"));
                  e.printStackTrace();
               }

               Consumer.is_paused = false;
               Config.purge_running = false;
            }
         }

         Runnable runnable = new BasicThread();
         Thread thread = new Thread(runnable);
         thread.start();
      }
   }
}
