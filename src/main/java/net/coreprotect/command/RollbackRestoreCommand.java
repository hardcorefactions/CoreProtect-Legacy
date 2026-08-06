package net.coreprotect.command;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import net.coreprotect.CoreProtect;
import net.coreprotect.Functions;
import net.coreprotect.database.Database;
import net.coreprotect.database.Lookup;
import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class RollbackRestoreCommand {
   protected static void runCommand(final CommandSender player, boolean permission, final String[] args, int force_seconds) {
      Location lo0 = CommandHandler.parseLocation(player, args);
      final List<String> arg_uuids = new ArrayList();
      List<String> arg_users = CommandHandler.parseUsers(args);
      Integer[] arg_radius = CommandHandler.parseRadius(args, player, lo0);
      final int arg_noisy = CommandHandler.parseNoisy(args);
      final List<Object> arg_exclude = CommandHandler.parseExcluded(player, args);
      final List<String> arg_exclude_users = CommandHandler.parseExcludedUsers(player, args);
      final List<Object> arg_blocks = CommandHandler.parseRestricted(player, args);
      final String ts = CommandHandler.parseTimeString(args);
      int rbseconds = CommandHandler.parseTime(args);
      int arg_wid0 = CommandHandler.parseWorld(args);
      final List<Integer> arg_action = CommandHandler.parseAction(args);
      boolean count = CommandHandler.parseCount(args);
      boolean worldedit = CommandHandler.parseWorldEdit(args);
      boolean forceglobal = CommandHandler.parseForceGlobal(args);
      int preview0 = CommandHandler.parsePreview(args);
      String corecommand = args[0].toLowerCase();
      if (arg_blocks != null && arg_exclude != null && arg_exclude_users != null) {
         if (arg_action.size() == 0 && arg_blocks.size() > 0) {
            for(Object arg_block : arg_blocks) {
               if (arg_block instanceof Material) {
                  arg_action.add(0);
                  arg_action.add(1);
               } else if (arg_block instanceof EntityType) {
                  arg_action.add(3);
               }
            }
         }

         if (count) {
            LookupCommand.runCommand(player, permission, args);
         } else if (Config.converter_running) {
            player.sendMessage(Language.get("upgrade-in-progress-please-try-again"));
         } else if (Config.purge_running) {
            player.sendMessage(Language.get("purge-in-progress-please-try-again"));
         } else if (arg_wid0 == -1) {
            String world_name = CommandHandler.parseWorldName(args);
            player.sendMessage(Language.get("world-not-found", world_name));
         } else if (preview0 > 0 && !(player instanceof Player)) {
            player.sendMessage(Language.get("you-can-only-preview0-rollbacks-in"));
         } else if (arg_action.contains(-1)) {
            player.sendMessage(Language.get("that-is-not-a-valid-action"));
         } else if (worldedit && arg_radius == null) {
            player.sendMessage(Language.get("worldedit-selection-not-found"));
         } else if (arg_radius != null && arg_radius[0] == -1) {
            player.sendMessage(Language.get("please-enter-a-valid-radius"));
         } else if (Config.active_rollbacks.get(player.getName()) != null) {
            player.sendMessage(Language.get("a-rollback-restore-is-already-in"));
         } else {
            if (preview0 > 1 && force_seconds <= 0) {
               preview0 = 1;
            }

            if (!permission) {
               player.sendMessage(Language.get("you-do-not-have-permission-to"));
            } else {
               int a = 0;
               if (corecommand.equals("restore") || corecommand.equals("rs") || corecommand.equals("re")) {
                  a = 1;
               }

               final int final_action = a;
               int default_radius = (Integer)Config.config.get("default-radius");
               if ((player instanceof Player || player instanceof BlockCommandSender) && arg_radius == null && default_radius > 0 && !forceglobal) {
                  int xmin = lo0.getBlockX() - default_radius;
                  int xmax = lo0.getBlockX() + default_radius;
                  int zmin = lo0.getBlockZ() - default_radius;
                  int zmax = lo0.getBlockZ() + default_radius;
                  arg_radius = new Integer[]{default_radius, xmin, xmax, -1, -1, zmin, zmax, 0};
               }

               int g = 1;
               if (arg_users.contains("#global") && arg_radius == null) {
                  g = 0;
               }

               if (arg_users.size() == 0 && arg_wid0 > 0) {
                  if (a == 0) {
                     player.sendMessage(Language.get("you-did-not-specify-a-rollback"));
                  } else {
                     player.sendMessage(Language.get("you-did-not-specify-a-restore"));
                  }

                  return;
               }

               if (g == 1 && (arg_users.size() > 0 || arg_users.size() == 0 && arg_radius != null)) {
                  int max_radius = (Integer)Config.config.get("max-radius");
                  if (arg_radius != null) {
                     int radius_value = arg_radius[0];
                     if (radius_value > max_radius && max_radius > 0) {
                        player.sendMessage(Language.get("the-maximum-radius-is", corecommand.toLowerCase(), max_radius));
                        player.sendMessage(Language.get("use-r-global-to-do-a", corecommand.toLowerCase()));
                        return;
                     }
                  }

                  if (arg_action.size() > 0) {
                     if (arg_action.contains(4)) {
                        if (arg_users.contains("#global") || arg_users.size() == 0) {
                           player.sendMessage(Language.get("to-use-that-action-please-specify"));
                           return;
                        }

                        if (preview0 > 0) {
                           player.sendMessage(Language.get("you-can-t-preview0-container-transactions"));
                           return;
                        }
                     } else if (!arg_action.contains(0) && !arg_action.contains(1) && !arg_action.contains(3)) {
                        if (a == 0) {
                           player.sendMessage(Language.get("that-action-can-t-be-used"));
                        } else {
                           player.sendMessage(Language.get("that-action-can-t-be-used-2"));
                        }

                        return;
                     }
                  }

                  if (arg_users.size() == 0) {
                     arg_users.add("#global");
                  }

                  List<String> rollbackusers = arg_users;
                  int c = 0;

                  for(String ruser : arg_users) {
                     for(Player p : CoreProtect.getInstance().getServer().matchPlayer(ruser)) {
                        if (p.getName().equalsIgnoreCase(ruser)) {
                           rollbackusers.set(c, p.getName());
                        }
                     }

                     ++c;
                  }

                  int wid = 0;
                  int x = 0;
                  int y = 0;
                  int z = 0;
                  if (rollbackusers.contains("#container")) {
                     boolean valid = false;
                     if (Config.lookup_type.get(player.getName()) != null) {
                        int lookup_type = (Integer)Config.lookup_type.get(player.getName());
                        if (lookup_type == 1) {
                           valid = true;
                        } else if (lookup_type == 5 && ((List)Config.lookup_ulist.get(player.getName())).contains("#container")) {
                           valid = true;
                        }
                     }

                     if (!valid) {
                        player.sendMessage(Language.get("please-inspect-a-valid-container-first"));
                        return;
                     }

                     if (preview0 > 0) {
                        player.sendMessage(Language.get("you-can-t-preview0-container-transactions"));
                        return;
                     }

                     String lcommand = (String)Config.lookup_command.get(player.getName());
                     String[] data = lcommand.split("\\.");
                     x = Integer.parseInt(data[0]);
                     y = Integer.parseInt(data[1]);
                     z = Integer.parseInt(data[2]);
                     wid = Integer.parseInt(data[3]);
                     arg_action.add(5);
                     arg_radius = null;
                     arg_wid0 = 0;
                     lo0 = new Location(CoreProtect.getInstance().getServer().getWorld(Functions.getWorldName(wid)), (double)x, (double)y, (double)z);
                     Block block = lo0.getBlock();
                     if (block.getState() instanceof Chest) {
                        BlockFace[] block_sides = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

                        for(BlockFace face : block_sides) {
                           if (block.getRelative(face, 1).getState() instanceof Chest) {
                              Block relative = block.getRelative(face, 1);
                              int x2 = relative.getX();
                              int z2 = relative.getZ();
                              double new_x = (double)(x + x2) / (double)2.0F;
                              double new_z = (double)(z + z2) / (double)2.0F;
                              lo0.setX(new_x);
                              lo0.setZ(new_z);
                              break;
                           }
                        }
                     }
                  }

                  final Location lo = lo0;
                  final int arg_wid = arg_wid0;
                  final int preview = preview0;
                  final List<String> rollbackusers2 = rollbackusers;
                  if (rbseconds > 0) {
                     int unixtimestamp = (int)(System.currentTimeMillis() / 1000L);
                     int seconds = unixtimestamp - rbseconds;
                     if (force_seconds > 0) {
                        seconds = force_seconds;
                     }

                     final int stime = seconds;
                     final Integer[] radius = arg_radius;

                     try {
                        Config.active_rollbacks.put(player.getName(), true);

                        class BasicThread2 implements Runnable {
                           public void run() {
                              try {
                                 int action = final_action;
                                 Location location = lo;
                                 Connection connection = Database.getConnection(false);
                                 if (connection != null) {
                                    Statement statement = connection.createStatement();
                                    String baduser = "";
                                    boolean exists = false;

                                    for(String check : rollbackusers2) {
                                       if (!check.equals("#global") && !check.equals("#container")) {
                                          exists = Lookup.playerExists(connection, check);
                                          if (!exists) {
                                             baduser = check;
                                             break;
                                          }
                                       } else {
                                          exists = true;
                                       }
                                    }

                                    if (exists) {
                                       for(String check : arg_exclude_users) {
                                          if (!check.equals("#global")) {
                                             exists = Lookup.playerExists(connection, check);
                                             if (!exists) {
                                                baduser = check;
                                                break;
                                             }
                                          } else {
                                             baduser = "#global";
                                             exists = false;
                                          }
                                       }
                                    }

                                    if (!exists) {
                                       player.sendMessage(Language.get("user-not-found", baduser));
                                    } else {
                                       boolean restrict_world = false;
                                       if (radius != null) {
                                          restrict_world = true;
                                       }

                                       if (location == null) {
                                          restrict_world = false;
                                       }

                                       if (arg_wid > 0) {
                                          restrict_world = true;
                                          location = new Location(CoreProtect.getInstance().getServer().getWorld(Functions.getWorldName(arg_wid)), (double)0.0F, (double)0.0F, (double)0.0F);
                                       }

                                       boolean verbose = false;
                                       if (arg_noisy == 1) {
                                          verbose = true;
                                       }

                                       String users = "";

                                       for(String value : rollbackusers2) {
                                          if (users.length() == 0) {
                                             users = "" + value + "";
                                          } else {
                                             users = users + ", " + value;
                                          }
                                       }

                                       if (users.equals("#global") && restrict_world) {
                                          users = "#" + location.getWorld().getName();
                                       }

                                       if (preview == 2) {
                                          player.sendMessage(Language.get("cancelling-preview"));
                                       } else if (preview == 1) {
                                          player.sendMessage(Language.get("preview-started-on", users));
                                       } else if (action == 0) {
                                          player.sendMessage(Language.get("rollback-started-on", users));
                                       } else {
                                          player.sendMessage(Language.get("restore-started-on", users));
                                       }

                                       if (arg_action.contains(5)) {
                                          Lookup.performContainerRollbackRestore(statement, player, arg_uuids, rollbackusers2, ts, arg_blocks, arg_exclude, arg_exclude_users, arg_action, location, radius, stime, restrict_world, false, verbose, action);
                                       } else {
                                          Lookup.performRollbackRestore(statement, player, arg_uuids, rollbackusers2, ts, arg_blocks, arg_exclude, arg_exclude_users, arg_action, location, radius, stime, restrict_world, false, verbose, action, preview);
                                          if (preview < 2) {
                                             List<Object[]> list = new ArrayList();
                                             list.add(new Object[]{stime});
                                             list.add(args);
                                             Config.last_rollback.put(player.getName(), list);
                                          }
                                       }
                                    }

                                    statement.close();
                                    connection.close();
                                 } else {
                                    player.sendMessage(Language.get("database-busy-please-try-again-later"));
                                 }
                              } catch (Exception e) {
                                 e.printStackTrace();
                              }

                              if (Config.active_rollbacks.get(player.getName()) != null) {
                                 Config.active_rollbacks.remove(player.getName());
                              }

                           }
                        }

                        Runnable runnable = new BasicThread2();
                        Thread thread = new Thread(runnable);
                        thread.start();
                     } catch (Exception e) {
                        e.printStackTrace();
                     }
                  } else if (a == 0) {
                     player.sendMessage(Language.get("please-specify-the-amount-of-time-2"));
                  } else {
                     player.sendMessage(Language.get("please-specify-the-amount-of-time-3"));
                  }
               } else if (a == 0) {
                  player.sendMessage(Language.get("you-did-not-specify-a-rollback-2"));
               } else {
                  player.sendMessage(Language.get("you-did-not-specify-a-restore-2"));
               }
            }

         }
      }
   }
}
