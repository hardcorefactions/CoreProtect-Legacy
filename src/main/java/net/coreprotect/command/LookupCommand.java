package net.coreprotect.command;

import java.sql.Connection;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import net.coreprotect.CoreProtect;
import net.coreprotect.Functions;
import net.coreprotect.database.Database;
import net.coreprotect.database.Lookup;
import net.coreprotect.model.Config;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class LookupCommand {
   protected static void runCommand(final CommandSender player, boolean permission, String[] args) {
      int resultc = args.length;
      final Location lo = CommandHandler.parseLocation(player, args);
      List<String> arg_users = CommandHandler.parseUsers(args);
      Integer[] arg_radius = CommandHandler.parseRadius(args, player, lo);
      int arg_noisy0 = CommandHandler.parseNoisy(args);
      List<String> arg_exclude_users0 = CommandHandler.parseExcludedUsers(player, args);
      List<Object> arg_exclude0 = CommandHandler.parseExcluded(player, args);
      List<Object> arg_blocks0 = CommandHandler.parseRestricted(player, args);
      String ts0 = CommandHandler.parseTimeString(args);
      int rbseconds = CommandHandler.parseTime(args);
      int arg_wid0 = CommandHandler.parseWorld(args);
      List<Integer> arg_action0 = CommandHandler.parseAction(args);
      final boolean count = CommandHandler.parseCount(args);
      boolean worldedit = CommandHandler.parseWorldEdit(args);
      boolean page_lookup = false;
      if (arg_blocks0 != null && arg_exclude0 != null && arg_exclude_users0 != null) {
         int arg_excluded0 = arg_exclude0.size();
         int arg_restricted0 = arg_blocks0.size();
         if (arg_action0.size() == 0 && arg_blocks0.size() > 0) {
            for(Object arg_block : arg_blocks0) {
               if (arg_block instanceof Material) {
                  arg_action0.add(0);
                  arg_action0.add(1);
               } else if (arg_block instanceof EntityType) {
                  arg_action0.add(3);
               }
            }
         }

         if (arg_wid0 == -1) {
            String world_name = CommandHandler.parseWorldName(args);
            player.sendMessage("§3CoreProtect §f- World \"" + world_name + "\" not found.");
         } else {
            int type0 = 0;
            if (Config.lookup_type.get(player.getName()) != null) {
               type0 = (Integer)Config.lookup_type.get(player.getName());
            }

            if (type0 == 0 && resultc > 1) {
               type0 = 4;
            } else if (resultc > 2) {
               type0 = 4;
            } else if (resultc > 1) {
               page_lookup = true;
               String dat = args[1];
               if (dat.contains(":")) {
                  String[] split = dat.split(":");
                  String check1 = split[0].replaceAll("[^a-zA-Z_]", "");
                  String check2 = "";
                  if (split.length > 1) {
                     check2 = split[1].replaceAll("[^a-zA-Z_]", "");
                  }

                  if (check1.length() > 0 || check2.length() > 0) {
                     type0 = 4;
                     page_lookup = false;
                  }
               } else {
                  String check1 = dat.replaceAll("[^a-zA-Z_]", "");
                  if (check1.length() > 0) {
                     type0 = 4;
                     page_lookup = false;
                  }
               }
            }

            if (arg_action0.contains(6) || arg_action0.contains(7) || arg_action0.contains(8) || arg_action0.contains(9)) {
               page_lookup = true;
            }

            if (permission || page_lookup && player.hasPermission("coreprotect.inspect")) {
               if (Config.converter_running) {
                  player.sendMessage("§3CoreProtect §f- Upgrade in progress. Please try again later.");
               } else if (Config.purge_running) {
                  player.sendMessage("§3CoreProtect §f- Purge in progress. Please try again later.");
               } else if (resultc < 2) {
                  player.sendMessage("§3CoreProtect §f- Please use \"/co l <params>\".");
               } else if (arg_action0.contains(-1)) {
                  player.sendMessage("§3CoreProtect §f- That is not a valid action.");
               } else if (worldedit && arg_radius == null) {
                  player.sendMessage("§3CoreProtect §f- WorldEdit selection not found.");
               } else if (arg_radius != null && arg_radius[0] == -1) {
                  player.sendMessage("§3CoreProtect §f- Please enter a valid radius.");
               } else {
                  boolean allPermission = false;
                  if (player.isOp()) {
                     allPermission = true;
                  }

                  if (!allPermission) {
                     if (!page_lookup && (arg_action0.size() == 0 || arg_action0.contains(0) || arg_action0.contains(1)) && !player.hasPermission("coreprotect.lookup.block")) {
                        player.sendMessage("§3CoreProtect §f- You do not have permission to do that.");
                        return;
                     }

                     if (arg_action0.contains(2) && !player.hasPermission("coreprotect.lookup.click")) {
                        player.sendMessage("§3CoreProtect §f- You do not have permission to do that.");
                        return;
                     }

                     if (arg_action0.contains(3) && !player.hasPermission("coreprotect.lookup.kill")) {
                        player.sendMessage("§3CoreProtect §f- You do not have permission to do that.");
                        return;
                     }

                     if (arg_action0.contains(4) && !player.hasPermission("coreprotect.lookup.container")) {
                        player.sendMessage("§3CoreProtect §f- You do not have permission to do that.");
                        return;
                     }

                     if (arg_action0.contains(6) && !player.hasPermission("coreprotect.lookup.chat")) {
                        player.sendMessage("§3CoreProtect §f- You do not have permission to do that.");
                        return;
                     }

                     if (arg_action0.contains(7) && !player.hasPermission("coreprotect.lookup.command")) {
                        player.sendMessage("§3CoreProtect §f- You do not have permission to do that.");
                        return;
                     }

                     if (arg_action0.contains(8) && !player.hasPermission("coreprotect.lookup.session")) {
                        player.sendMessage("§3CoreProtect §f- You do not have permission to do that.");
                        return;
                     }

                     if (arg_action0.contains(9) && !player.hasPermission("coreprotect.lookup.username")) {
                        player.sendMessage("§3CoreProtect §f- You do not have permission to do that.");
                        return;
                     }
                  }

                  if (arg_action0.contains(6) || arg_action0.contains(7) || arg_action0.contains(8) || arg_action0.contains(9)) {
                     if (!arg_action0.contains(8) && (arg_radius != null || arg_wid0 > 0 || worldedit)) {
                        player.sendMessage("§3CoreProtect §f- \"r:\" can't be used with that action.");
                        return;
                     }

                     if (arg_blocks0.size() > 0) {
                        player.sendMessage("§3CoreProtect §f- \"b:\" can't be used with that action.");
                        return;
                     }

                     if (arg_exclude0.size() > 0) {
                        player.sendMessage("§3CoreProtect §f- \"e:\" can't be used with that action.");
                        return;
                     }
                  }

                  if (resultc > 2) {
                     String bid = args[1];
                     if (bid.equalsIgnoreCase("type") || bid.equalsIgnoreCase("id")) {
                        type0 = 6;
                     }
                  }

                  final int type = type0;

                  if (rbseconds > 0 || page_lookup || type != 4 || arg_blocks0.size() <= 0 && arg_users.size() <= 0) {
                     if (type == 1) {
                        boolean default_re = true;
                        int p0 = 0;
                        int re0 = 7;
                        if (resultc > 1) {
                           String pages = args[1];
                           if (pages.contains(":")) {
                              String[] data = pages.split(":");
                              pages = data[0];
                              String results = "";
                              if (data.length > 1) {
                                 results = data[1];
                              }

                              results = results.replaceAll("[^0-9]", "");
                              if (results.length() > 0) {
                                 int r = Integer.parseInt(results);
                                 if (r > 0) {
                                    re0 = r;
                                    default_re = false;
                                 }
                              }
                           }

                           pages = pages.replaceAll("[^0-9]", "");
                           if (pages.length() > 0) {
                              int pa = Integer.parseInt(pages);
                              if (pa > 0) {
                                 p0 = pa;
                              }
                           }
                        }

                        if (p0 <= 0) {
                           p0 = 1;
                        }

                        String lcommand = (String)Config.lookup_command.get(player.getName());
                        String[] data = lcommand.split("\\.");
                        int x = Integer.parseInt(data[0]);
                        int y = Integer.parseInt(data[1]);
                        int z = Integer.parseInt(data[2]);
                        int wid = Integer.parseInt(data[3]);
                        int x2 = Integer.parseInt(data[4]);
                        int y2 = Integer.parseInt(data[5]);
                        int z2 = Integer.parseInt(data[6]);
                        if (default_re) {
                           re0 = Integer.parseInt(data[7]);
                        }

                        String bc = x + "." + y + "." + z + "." + wid + "." + x2 + "." + y2 + "." + z2 + "." + re0;
                        Config.lookup_command.put(player.getName(), bc);
                        String world = Functions.getWorldName(wid);
                        double dx = (double)0.5F * (double)(x + x2);
                        double dy = (double)0.5F * (double)(y + y2);
                        double dz = (double)0.5F * (double)(z + z2);
                        final Location location = new Location(CoreProtect.getInstance().getServer().getWorld(world), dx, dy, dz);

                        final int p = p0;
                        final int re = re0;

                        class BasicThread implements Runnable {
                           public void run() {
                              try {
                                 Connection connection = Database.getConnection(false);
                                 if (connection != null) {
                                    Statement statement = connection.createStatement();
                                    String blockdata = Lookup.chest_transactions(statement, location, player.getName(), p, re);
                                    if (blockdata.contains("\n")) {
                                       for(String b : blockdata.split("\n")) {
                                          player.sendMessage(b);
                                       }
                                    } else {
                                       player.sendMessage(blockdata);
                                    }

                                    statement.close();
                                    connection.close();
                                 } else {
                                    player.sendMessage("§3CoreProtect §f- Database busy. Please try again later.");
                                 }
                              } catch (Exception e) {
                                 e.printStackTrace();
                              }

                           }
                        }

                        Runnable runnable = new BasicThread();
                        Thread thread = new Thread(runnable);
                        thread.start();
                     } else if (type != 2 && type != 3 && type != 7) {
                        if (type != 4 && type != 5) {
                           if (type == 6) {
                              String bid = args[2];
                              bid = bid.replaceAll("[^0-9]", "");
                              if (bid.length() > 0) {
                                 int b = Integer.parseInt(bid);
                                 if (b > 0) {
                                    String bname = Functions.block_name_lookup(b);
                                    if (bname.length() > 0) {
                                       player.sendMessage("§3CoreProtect §f- The name of block ID #" + b + " is \"" + bname + "\".");
                                    } else {
                                       player.sendMessage("§3CoreProtect §f- No data found for block ID #" + b + ".");
                                    }
                                 } else {
                                    player.sendMessage("§3CoreProtect §f- Please use \"/co lookup type <ID>\".");
                                 }
                              } else {
                                 player.sendMessage("§3CoreProtect §f- Please use \"/co lookup type <ID>\".");
                              }
                           } else {
                              player.sendMessage("§3CoreProtect §f- Please use \"/co l <params>\".");
                           }
                        } else {
                           boolean default_re = true;
                           int pa0 = 1;
                           int re0 = 4;
                           if (arg_action0.contains(6) || arg_action0.contains(7) || arg_action0.contains(9)) {
                              re0 = 7;
                           }

                           if (type == 5 && resultc > 1) {
                              String pages = args[1];
                              if (pages.contains(":")) {
                                 String[] data = pages.split(":");
                                 pages = data[0];
                                 String results = "";
                                 if (data.length > 1) {
                                    results = data[1];
                                 }

                                 results = results.replaceAll("[^0-9]", "");
                                 if (results.length() > 0) {
                                    int r = Integer.parseInt(results);
                                    if (r > 0) {
                                       re0 = r;
                                       default_re = false;
                                    }
                                 }
                              }

                              pages = pages.replaceAll("[^0-9]", "");
                              if (pages.length() > 0) {
                                 int p = Integer.parseInt(pages);
                                 if (p > 0) {
                                    pa0 = p;
                                 }
                              }
                           }

                           int g = 1;
                           if (arg_users.contains("#global") && arg_radius == null) {
                              g = 0;
                           }

                           if (g == 1 && (page_lookup || arg_blocks0.size() > 0 || arg_users.size() > 0 || arg_users.size() == 0 && arg_radius != null)) {
                              int max_radius = (Integer)Config.config.get("max-radius");
                              if (arg_radius != null) {
                                 int radius_value = arg_radius[0];
                                 if (radius_value > max_radius && max_radius > 0) {
                                    player.sendMessage("§3CoreProtect §f- The maximum lookup radius is " + max_radius + ".");
                                    player.sendMessage("§3CoreProtect §f- Don't specify a radius to do a global lookup.");
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

                              int cs = -1;
                              int x0 = 0;
                              int y0 = 0;
                              int z0 = 0;
                              int wid0 = 0;
                              if (type == 5) {
                                 String lcommand = (String)Config.lookup_command.get(player.getName());
                                 String[] data = lcommand.split("\\.");
                                 x0 = Integer.parseInt(data[0]);
                                 y0 = Integer.parseInt(data[1]);
                                 z0 = Integer.parseInt(data[2]);
                                 wid0 = Integer.parseInt(data[3]);
                                 cs = Integer.parseInt(data[4]);
                                 arg_noisy0 = Integer.parseInt(data[5]);
                                 arg_excluded0 = Integer.parseInt(data[6]);
                                 arg_restricted0 = Integer.parseInt(data[7]);
                                 arg_wid0 = Integer.parseInt(data[8]);
                                 if (default_re) {
                                    re0 = Integer.parseInt(data[9]);
                                 }

                                 rollbackusers = (List)Config.lookup_ulist.get(player.getName());
                                 arg_blocks0 = (List)Config.lookup_blist.get(player.getName());
                                 arg_exclude0 = (List)Config.lookup_elist.get(player.getName());
                                 arg_exclude_users0 = (List)Config.lookup_e_userlist.get(player.getName());
                                 arg_action0 = (List)Config.lookup_alist.get(player.getName());
                                 arg_radius = (Integer[])Config.lookup_radius.get(player.getName());
                                 ts0 = (String)Config.lookup_time.get(player.getName());
                                 rbseconds = 1;
                              } else {
                                 if (lo != null) {
                                    x0 = lo.getBlockX();
                                    z0 = lo.getBlockZ();
                                    wid0 = Functions.getWorldId(lo.getWorld().getName());
                                 }

                                 if (rollbackusers.size() == 1 && rollbackusers.contains("#global") && arg_action0.contains(9)) {
                                    player.sendMessage("§3CoreProtect §f- Please use \"/co l a:username u:<user>\".");
                                    return;
                                 }

                                 if (rollbackusers.contains("#container")) {
                                    if (arg_action0.contains(6) || arg_action0.contains(7) || arg_action0.contains(8) || arg_action0.contains(9)) {
                                       player.sendMessage("§3CoreProtect §f- \"#container\" is an invalid username.");
                                       return;
                                    }

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
                                       player.sendMessage("§3CoreProtect §f- Please inspect a valid container first.");
                                       return;
                                    }

                                    if (!player.hasPermission("coreprotect.lookup.container") && !allPermission) {
                                       player.sendMessage("§3CoreProtect §f- You do not have permission to do that.");
                                       return;
                                    }

                                    String lcommand = (String)Config.lookup_command.get(player.getName());
                                    String[] data = lcommand.split("\\.");
                                    x0 = Integer.parseInt(data[0]);
                                    y0 = Integer.parseInt(data[1]);
                                    z0 = Integer.parseInt(data[2]);
                                    wid0 = Integer.parseInt(data[3]);
                                    arg_action0.add(5);
                                    arg_radius = null;
                                    arg_wid0 = 0;
                                 }
                              }

                              final List<String> rollbackusers2 = rollbackusers;
                              int unixtimestamp = (int)(System.currentTimeMillis() / 1000L);
                              if (cs == -1) {
                                 if (rbseconds <= 0) {
                                    cs = 0;
                                 } else {
                                    cs = unixtimestamp - rbseconds;
                                 }
                              }

                              final int pa = pa0;
                              final int re = re0;
                              final int x = x0;
                              final int y = y0;
                              final int z = z0;
                              final int wid = wid0;
                              final int arg_noisy = arg_noisy0;
                              final int arg_excluded = arg_excluded0;
                              final int arg_restricted = arg_restricted0;
                              final int arg_wid = arg_wid0;
                              final List<Object> arg_blocks = arg_blocks0;
                              final List<Object> arg_exclude = arg_exclude0;
                              final List<String> arg_exclude_users = arg_exclude_users0;
                              final List<Integer> arg_action = arg_action0;
                              final String ts = ts0;

                              final int stime = cs;
                              final Integer[] radius = arg_radius;

                              try {
                                 player.sendMessage("§3CoreProtect §f- Lookup searching. Please wait...");

                                 class BasicThread2 implements Runnable {
                                    public void run() {
                                       try {
                                          List<String> uuid_list = new ArrayList();
                                          Location location = lo;
                                          boolean exists = false;
                                          String bc = x + "." + y + "." + z + "." + wid + "." + stime + "." + arg_noisy + "." + arg_excluded + "." + arg_restricted + "." + arg_wid + "." + re;
                                          Config.lookup_command.put(player.getName(), bc);
                                          Config.lookup_page.put(player.getName(), pa);
                                          Config.lookup_time.put(player.getName(), ts);
                                          Config.lookup_type.put(player.getName(), 5);
                                          Config.lookup_elist.put(player.getName(), arg_exclude);
                                          Config.lookup_e_userlist.put(player.getName(), arg_exclude_users);
                                          Config.lookup_blist.put(player.getName(), arg_blocks);
                                          Config.lookup_ulist.put(player.getName(), rollbackusers2);
                                          Config.lookup_alist.put(player.getName(), arg_action);
                                          Config.lookup_radius.put(player.getName(), radius);
                                          Connection connection = Database.getConnection(false);
                                          if (connection != null) {
                                             Statement statement = connection.createStatement();
                                             String baduser = "";

                                             for(String check : rollbackusers2) {
                                                if ((check.equals("#global") || check.equals("#container")) && !arg_action.contains(9)) {
                                                   exists = true;
                                                } else {
                                                   exists = Lookup.playerExists(connection, check);
                                                   if (!exists) {
                                                      baduser = check;
                                                      break;
                                                   }

                                                   if (arg_action.contains(9) && Config.uuid_cache.get(check.toLowerCase()) != null) {
                                                      String uuid = (String)Config.uuid_cache.get(check.toLowerCase());
                                                      uuid_list.add(uuid);
                                                   }
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
                                                player.sendMessage("§3CoreProtect §f- User \"" + baduser + "\" not found.");
                                             } else {
                                                List<String> user_list = new ArrayList();
                                                if (!arg_action.contains(9)) {
                                                   user_list = rollbackusers2;
                                                }

                                                int unixtimestamp = (int)(System.currentTimeMillis() / 1000L);
                                                boolean restrict_world = false;
                                                if (radius != null) {
                                                   restrict_world = true;
                                                }

                                                if (location == null) {
                                                   restrict_world = false;
                                                }

                                                if (arg_wid > 0) {
                                                   restrict_world = true;
                                                   location = new Location(CoreProtect.getInstance().getServer().getWorld(Functions.getWorldName(arg_wid)), (double)x, (double)y, (double)z);
                                                } else if (location != null) {
                                                   location = new Location(CoreProtect.getInstance().getServer().getWorld(Functions.getWorldName(wid)), (double)x, (double)y, (double)z);
                                                }

                                                int row_max = pa * re;
                                                int page_start = row_max - re;
                                                int rows = 0;
                                                boolean check_rows = true;
                                                if (type == 5 && pa > 1) {
                                                   rows = (Integer)Config.lookup_rows.get(player.getName());
                                                   if (page_start < rows) {
                                                      check_rows = false;
                                                   }
                                                }

                                                if (check_rows) {
                                                   rows = Lookup.countLookupRows(statement, player, uuid_list, user_list, arg_blocks, arg_exclude, arg_exclude_users, arg_action, location, radius, stime, restrict_world, true);
                                                   Config.lookup_rows.put(player.getName(), rows);
                                                }

                                                if (count) {
                                                   String row_format = NumberFormat.getInstance().format((long)rows);
                                                   player.sendMessage("§3CoreProtect §f- " + row_format + " row(s) found.");
                                                } else if (page_start >= rows) {
                                                   if (rows > 0) {
                                                      player.sendMessage("§3CoreProtect §f- No results found for that page.");
                                                   } else {
                                                      player.sendMessage("§3CoreProtect §f- No results found.");
                                                   }
                                                } else {
                                                   String arrows = "                      ";
                                                   if (rows > re) {
                                                      int total_pages = (int)Math.ceil((double)rows / ((double)re + (double)0.0F));
                                                      String page_back = "«";
                                                      String page_next = "»";
                                                      if (pa > 1 && pa < total_pages) {
                                                         (new StringBuilder()).append(page_back).append(" | ").append(page_next).toString();
                                                      } else if (pa > 1) {
                                                         (new StringBuilder()).append("    ").append(page_back).toString();
                                                      } else {
                                                         (new StringBuilder()).append("    ").append(page_next).toString();
                                                      }
                                                   }

                                                   arrows = "";
                                                   List<String[]> lookup_list = Lookup.performPartialLookup(statement, player, uuid_list, user_list, arg_blocks, arg_exclude, arg_exclude_users, arg_action, location, radius, stime, page_start, re, restrict_world, true);
                                                   player.sendMessage("§f----- §3CoreProtect Lookup Results §f-----" + arrows);
                                                   if (!arg_action.contains(6) && !arg_action.contains(7)) {
                                                      if (arg_action.contains(8)) {
                                                         for(String[] data : lookup_list) {
                                                            String time = data[0];
                                                            String dplayer = data[1];
                                                            int wid = Integer.parseInt(data[2]);
                                                            int x = Integer.parseInt(data[3]);
                                                            int y = Integer.parseInt(data[4]);
                                                            int z = Integer.parseInt(data[5]);
                                                            int action = Integer.parseInt(data[6]);
                                                            double time_since = (double)unixtimestamp - Double.parseDouble(time);
                                                            time_since /= (double)60.0F;
                                                            time_since /= (double)60.0F;
                                                            String timeago = (new DecimalFormat("0.00")).format(time_since);
                                                            String action_string = "in";
                                                            if (action == 0) {
                                                               action_string = "out";
                                                            }

                                                            String world = Functions.getWorldName(wid);
                                                            double time_length = (double)timeago.replaceAll("[^0-9]", "").length() * (double)1.5F;
                                                            int padding = (int)(time_length + (double)12.5F);
                                                            String left_padding = StringUtils.leftPad("", padding, ' ');
                                                            player.sendMessage("§7" + timeago + "/h ago §f- §3" + dplayer + " §flogged §3" + action_string + "§f.");
                                                            player.sendMessage("§f" + left_padding + "§7^ §o(x" + x + "/y" + y + "/z" + z + "/" + world + ")");
                                                         }
                                                      } else if (arg_action.contains(9)) {
                                                         for(String[] data : lookup_list) {
                                                            String time = data[0];
                                                            String user = (String)Config.uuid_cache_reversed.get(data[1]);
                                                            String username = data[2];
                                                            double time_since = (double)unixtimestamp - Double.parseDouble(time);
                                                            time_since /= (double)60.0F;
                                                            time_since /= (double)60.0F;
                                                            String timeago = (new DecimalFormat("0.00")).format(time_since);
                                                            player.sendMessage("§7" + timeago + "/h ago §f- §3" + user + " §flogged in as §3" + username + "§f.");
                                                         }
                                                      } else {
                                                         for(String[] data : lookup_list) {
                                                            String string_amount = "";
                                                            int drb = Integer.parseInt(data[8]);
                                                            String rbd = "";
                                                            if (drb == 1) {
                                                               rbd = "§m";
                                                            }

                                                            int amount = 0;
                                                            String time = data[0];
                                                            String dplayer = data[1];
                                                            int x = Integer.parseInt(data[2]);
                                                            int y = Integer.parseInt(data[3]);
                                                            int z = Integer.parseInt(data[4]);
                                                            String dtype = data[5];
                                                            int ddata = Integer.parseInt(data[6]);
                                                            int daction = Integer.parseInt(data[7]);
                                                            int wid = Integer.parseInt(data[9]);
                                                            String a = "placed";
                                                            String tag = "§f-";
                                                            if (arg_action.contains(4) || arg_action.contains(5)) {
                                                               amount = Integer.parseInt(data[10]);
                                                               string_amount = "x" + amount + " ";
                                                               a = "added";
                                                            }

                                                            if (daction == 0) {
                                                               a = "removed";
                                                            } else if (daction == 2) {
                                                               a = "clicked";
                                                            } else if (daction == 3) {
                                                               a = "killed";
                                                            }

                                                            double time_since = (double)unixtimestamp - Double.parseDouble(time);
                                                            time_since /= (double)60.0F;
                                                            time_since /= (double)60.0F;
                                                            String timeago = (new DecimalFormat("0.00")).format(time_since);
                                                            double time_length = (double)timeago.replaceAll("[^0-9]", "").length() * (double)1.5F;
                                                            int padding = (int)(time_length + (double)12.5F);
                                                            String left_padding = StringUtils.leftPad("", padding, ' ');
                                                            String world = Functions.getWorldName(wid);
                                                            String dname = "";
                                                            boolean isPlayer = false;
                                                            if (daction == 3) {
                                                               int dTypeInt = Integer.parseInt(dtype);
                                                               if (dTypeInt == 0) {
                                                                  if (Config.player_id_cache_reversed.get(ddata) == null) {
                                                                     Database.loadUserName(connection, ddata);
                                                                  }

                                                                  dname = (String)Config.player_id_cache_reversed.get(ddata);
                                                                  isPlayer = true;
                                                               } else {
                                                                  dname = Functions.getEntityType(dTypeInt).name();
                                                               }
                                                            } else {
                                                               dname = Functions.getTypeName(Integer.parseInt(dtype)).toLowerCase();
                                                               dname = Functions.nameFilter(dname, ddata);
                                                            }

                                                            if (dname.length() > 0 && !isPlayer) {
                                                               dname = "minecraft:" + dname.toLowerCase() + "";
                                                            }

                                                            if (dname.contains("minecraft:")) {
                                                               String[] block_name_split = dname.split(":");
                                                               dname = block_name_split[1];
                                                            }

                                                            player.sendMessage("§7" + timeago + "/h ago " + tag + " §3" + rbd + "" + dplayer + " §f" + rbd + "" + a + " " + string_amount + "§3" + rbd + "" + dname + "§f.");
                                                            player.sendMessage("§f" + left_padding + "§7^ §o(x" + x + "/y" + y + "/z" + z + "/" + world + ")");
                                                         }
                                                      }
                                                   } else {
                                                      for(String[] data : lookup_list) {
                                                         String time = data[0];
                                                         String dplayer = data[1];
                                                         String message = data[2];
                                                         double time_since = (double)unixtimestamp - Double.parseDouble(time);
                                                         time_since /= (double)60.0F;
                                                         time_since /= (double)60.0F;
                                                         String timeago = (new DecimalFormat("0.00")).format(time_since);
                                                         player.sendMessage("§7" + timeago + "/h ago §f- §3" + dplayer + ": §f" + message + "");
                                                      }
                                                   }

                                                   if (rows > re) {
                                                      int total_pages = (int)Math.ceil((double)rows / ((double)re + (double)0.0F));
                                                      if (arg_action.contains(6) || arg_action.contains(7) || arg_action.contains(9)) {
                                                         player.sendMessage("-----");
                                                      }

                                                      player.sendMessage("§fPage " + pa + "/" + total_pages + ". View older data by typing \"§3/co l <page>§f\".");
                                                   }
                                                }
                                             }

                                             statement.close();
                                             connection.close();
                                          } else {
                                             player.sendMessage("§3CoreProtect §f- Database busy. Please try again later.");
                                          }
                                       } catch (Exception e) {
                                          e.printStackTrace();
                                       }

                                    }
                                 }

                                 Runnable runnable = new BasicThread2();
                                 Thread thread = new Thread(runnable);
                                 thread.start();
                              } catch (Exception e) {
                                 e.printStackTrace();
                              }
                           } else {
                              player.sendMessage("§3CoreProtect §f- Please use \"/co l <params>\".");
                           }
                        }
                     } else {
                        boolean default_re = true;
                        int page0 = 1;
                        int re0 = 7;
                        if (resultc > 1) {
                           String pages = args[1];
                           if (pages.contains(":")) {
                              String[] data = pages.split(":");
                              pages = data[0];
                              String results = "";
                              if (data.length > 1) {
                                 results = data[1];
                              }

                              results = results.replaceAll("[^0-9]", "");
                              if (results.length() > 0) {
                                 int r = Integer.parseInt(results);
                                 if (r > 0) {
                                    re0 = r;
                                    default_re = false;
                                 }
                              }
                           }

                           pages = pages.replaceAll("[^0-9]", "");
                           if (pages.length() > 0) {
                              int p = Integer.parseInt(pages);
                              if (p > 0) {
                                 page0 = p;
                              }
                           }
                        }

                        String lcommand = (String)Config.lookup_command.get(player.getName());
                        String[] data = lcommand.split("\\.");
                        int x = Integer.parseInt(data[0]);
                        int y = Integer.parseInt(data[1]);
                        int z = Integer.parseInt(data[2]);
                        int wid = Integer.parseInt(data[3]);
                        int lookup_type = Integer.parseInt(data[4]);
                        if (default_re) {
                           re0 = Integer.parseInt(data[5]);
                        }

                        String bc = x + "." + y + "." + z + "." + wid + "." + lookup_type + "." + re0;
                        Config.lookup_command.put(player.getName(), bc);
                        String world = Functions.getWorldName(wid);
                        final Block fblock = CoreProtect.getInstance().getServer().getWorld(world).getBlockAt(x, y, z);
                        final BlockState fblockstate = fblock.getState();

                        final int page = page0;
                        final int re = re0;

                        class BasicThread implements Runnable {
                           public void run() {
                              try {
                                 Connection connection = Database.getConnection(false);
                                 if (connection != null) {
                                    Statement statement = connection.createStatement();
                                    String blockdata = null;
                                    if (type == 7) {
                                       blockdata = Lookup.interaction_lookup(statement, fblock, player.getName(), 0, page, re);
                                    } else {
                                       blockdata = Lookup.block_lookup(statement, fblockstate, player.getName(), 0, page, re);
                                    }

                                    if (blockdata.contains("\n")) {
                                       for(String b : blockdata.split("\n")) {
                                          player.sendMessage(b);
                                       }
                                    } else if (blockdata.length() > 0) {
                                       player.sendMessage(blockdata);
                                    }

                                    statement.close();
                                    connection.close();
                                 } else {
                                    player.sendMessage("§3CoreProtect §f- Database busy. Please try again later.");
                                 }
                              } catch (Exception e) {
                                 e.printStackTrace();
                              }

                           }
                        }

                        Runnable runnable = new BasicThread();
                        Thread thread = new Thread(runnable);
                        thread.start();
                     }

                  } else {
                     player.sendMessage("§3CoreProtect §f- Please specify the amount of time to lookup.");
                  }
               }
            } else {
               player.sendMessage("§3CoreProtect §f- You do not have permission to do that.");
            }
         }
      }
   }
}
