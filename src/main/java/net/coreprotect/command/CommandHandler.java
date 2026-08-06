package net.coreprotect.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.coreprotect.Functions;
import net.coreprotect.model.BlockInfo;
import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
   private static CommandHandler instance;
   protected static List<Material> natural_blocks;

   public static CommandHandler getInstance() {
      if (instance == null) {
         instance = new CommandHandler();
      }

      return instance;
   }

   protected static List<Integer> parseAction(String[] args_input) {
      String[] args = (String[])args_input.clone();
      List<Integer> result = new ArrayList();
      int count = 0;
      int next = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("a:") && !i.equals("action:")) {
               if (next != 1 && !i.startsWith("a:") && !i.startsWith("action:")) {
                  next = 0;
               } else {
                  result.clear();
                  i = i.replaceAll("action:", "");
                  i = i.replaceAll("a:", "");
                  if (i.startsWith("#")) {
                     i = i.replaceFirst("#", "");
                  }

                  if (!i.equals("broke") && !i.equals("break") && !i.equals("remove") && !i.equals("destroy") && !i.equals("block-break") && !i.equals("block-remove") && !i.equals("-block") && !i.equals("block-")) {
                     if (!i.equals("placed") && !i.equals("place") && !i.equals("block-place") && !i.equals("+block") && !i.equals("block+")) {
                        if (!i.equals("block") && !i.equals("block-change") && !i.equals("change")) {
                           if (!i.equals("click") && !i.equals("clicks") && !i.equals("interact") && !i.equals("interaction") && !i.equals("player-interact") && !i.equals("player-interaction") && !i.equals("player-click")) {
                              if (!i.equals("death") && !i.equals("deaths") && !i.equals("entity-death") && !i.equals("entity-deaths") && !i.equals("kill") && !i.equals("kills") && !i.equals("entity-kill") && !i.equals("entity-kills")) {
                                 if (!i.equals("container") && !i.equals("container-change") && !i.equals("containers") && !i.equals("chest") && !i.equals("transaction") && !i.equals("transactions")) {
                                    if (!i.equals("-container") && !i.equals("container-") && !i.equals("remove-container")) {
                                       if (!i.equals("+container") && !i.equals("container+") && !i.equals("container-add") && !i.equals("add-container")) {
                                          if (i.equals("chat")) {
                                             result.add(6);
                                          } else if (!i.equals("command") && !i.equals("commands")) {
                                             if (!i.equals("login") && !i.equals("+session") && !i.equals("session+") && !i.equals("+connection") && !i.equals("connection+")) {
                                                if (!i.equals("logout") && !i.equals("-session") && !i.equals("session-") && !i.equals("-connection") && !i.equals("connection-")) {
                                                   if (!i.equals("session") && !i.equals("sessions") && !i.equals("connection") && !i.equals("connections")) {
                                                      if (!i.equals("username") && !i.equals("usernames") && !i.equals("user") && !i.equals("users") && !i.equals("name") && !i.equals("names") && !i.equals("uuid") && !i.equals("uuids") && !i.equals("username-change") && !i.equals("username-changes") && !i.equals("name-change") && !i.equals("name-changes")) {
                                                         result.add(-1);
                                                      } else {
                                                         result.add(9);
                                                      }
                                                   } else {
                                                      result.add(8);
                                                   }
                                                } else {
                                                   result.add(8);
                                                   result.add(0);
                                                }
                                             } else {
                                                result.add(8);
                                                result.add(1);
                                             }
                                          } else {
                                             result.add(7);
                                          }
                                       } else {
                                          result.add(4);
                                          result.add(1);
                                       }
                                    } else {
                                       result.add(4);
                                       result.add(0);
                                    }
                                 } else {
                                    result.add(4);
                                 }
                              } else {
                                 result.add(3);
                              }
                           } else {
                              result.add(2);
                           }
                        } else {
                           result.add(0);
                           result.add(1);
                        }
                     } else {
                        result.add(1);
                     }
                  } else {
                     result.add(0);
                  }

                  next = 0;
               }
            } else {
               next = 1;
            }
         }

         ++count;
      }

      return result;
   }

   protected static Location parseCoordinates(Location location, String[] args_input) {
      String[] args = (String[])args_input.clone();
      int count = 0;
      int next = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("c:") && !i.equals("coord:") && !i.equals("coords:") && !i.equals("cord:") && !i.equals("cords:") && !i.equals("coordinate:") && !i.equals("coordinates:") && !i.equals("cordinate:") && !i.equals("cordinates:")) {
               if (next != 2 && !i.startsWith("c:") && !i.startsWith("coord:") && !i.startsWith("coords:") && !i.startsWith("cord:") && !i.startsWith("cords:") && !i.startsWith("coordinate:") && !i.startsWith("coordinates:") && !i.startsWith("cordinate:") && !i.startsWith("cordinates:")) {
                  next = 0;
               } else {
                  i = i.replaceAll("coordinates:", "");
                  i = i.replaceAll("coordinate:", "");
                  i = i.replaceAll("cordinates:", "");
                  i = i.replaceAll("cordinate:", "");
                  i = i.replaceAll("coords:", "");
                  i = i.replaceAll("coord:", "");
                  i = i.replaceAll("cords:", "");
                  i = i.replaceAll("cord:", "");
                  i = i.replaceAll("c:", "");
                  if (i.contains(",")) {
                     String[] i2 = i.split(",");
                     double x = (double)0.0F;
                     double y = (double)0.0F;
                     double z = (double)0.0F;
                     int c_count = 0;

                     for(String coord : i2) {
                        coord = coord.replaceAll("[^0-9.\\-]", "");
                        if (coord.length() > 0 && !coord.equals(".") && !coord.equals("-")) {
                           double parsedCoord = Double.parseDouble(coord);
                           if (c_count == 0) {
                              x = parsedCoord;
                           } else if (c_count == 1) {
                              z = parsedCoord;
                           } else if (c_count == 2) {
                              y = z;
                              z = parsedCoord;
                           }

                           ++c_count;
                        }
                     }

                     if (y < (double)0.0F) {
                        y = (double)0.0F;
                     }

                     if (y > (double)255.0F) {
                        y = (double)255.0F;
                     }

                     if (c_count > 1) {
                        location.setX(x);
                        location.setY(y);
                        location.setZ(z);
                     }
                  }

                  next = 0;
               }
            } else {
               next = 2;
            }
         }

         ++count;
      }

      return location;
   }

   protected static boolean parseCount(String[] args_input) {
      String[] args = (String[])args_input.clone();
      boolean result = false;
      int count = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (i.equals("#count") || i.equals("#sum")) {
               result = true;
            }
         }

         ++count;
      }

      return result;
   }

   protected static List<Object> parseExcluded(CommandSender player, String[] args_input) {
      String[] args = (String[])args_input.clone();
      List<Object> excluded = new ArrayList();
      int count = 0;
      int next = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("e:") && !i.equals("exclude:")) {
               if (next != 5 && !i.startsWith("e:") && !i.startsWith("exclude:")) {
                  next = 0;
               } else {
                  i = i.replaceAll("exclude:", "");
                  i = i.replaceAll("e:", "");
                  if (!i.contains(",")) {
                     if (i.equals("#natural")) {
                        excluded.addAll(natural_blocks);
                     } else {
                        Material i_material = Functions.getType(i);
                        if (i_material != null) {
                           excluded.add(i_material);
                        } else {
                           EntityType i_entity = Functions.getEntityType(i);
                           if (i_entity != null) {
                              excluded.add(i_entity);
                           }
                        }
                     }

                     next = 0;
                  } else {
                     String[] i2 = i.split(",");

                     for(String i3 : i2) {
                        if (i3.equals("#natural")) {
                           excluded.addAll(natural_blocks);
                        } else {
                           Material i3_material = Functions.getType(i3);
                           if (i3_material != null) {
                              excluded.add(i3_material);
                           } else {
                              EntityType i3_entity = Functions.getEntityType(i3);
                              if (i3_entity != null) {
                                 excluded.add(i3_entity);
                              }
                           }
                        }
                     }

                     if (i.endsWith(",")) {
                        next = 5;
                     } else {
                        next = 0;
                     }
                  }
               }
            } else {
               next = 5;
            }
         }

         ++count;
      }

      return excluded;
   }

   protected static List<String> parseExcludedUsers(CommandSender player, String[] args_input) {
      String[] args = (String[])args_input.clone();
      List<String> excluded = new ArrayList();
      int count = 0;
      int next = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("e:") && !i.equals("exclude:")) {
               if (next != 5 && !i.startsWith("e:") && !i.startsWith("exclude:")) {
                  next = 0;
               } else {
                  i = i.replaceAll("exclude:", "");
                  i = i.replaceAll("e:", "");
                  if (!i.contains(",")) {
                     boolean isBlock = false;
                     if (i.equals("#natural")) {
                        isBlock = true;
                     } else {
                        Material i_material = Functions.getType(i);
                        if (i_material != null) {
                           isBlock = true;
                        } else {
                           EntityType i_entity = Functions.getEntityType(i);
                           if (i_entity != null) {
                              isBlock = true;
                           }
                        }
                     }

                     if (!isBlock) {
                        excluded.add(i);
                     }

                     next = 0;
                  } else {
                     String[] i2 = i.split(",");

                     for(String i3 : i2) {
                        boolean isBlock = false;
                        if (i3.equals("#natural")) {
                           isBlock = true;
                        } else {
                           Material i3_material = Functions.getType(i3);
                           if (i3_material != null) {
                              isBlock = true;
                           } else {
                              EntityType i3_entity = Functions.getEntityType(i3);
                              if (i3_entity != null) {
                                 isBlock = true;
                              }
                           }
                        }

                        if (!isBlock) {
                           excluded.add(i3);
                        }
                     }

                     if (i.endsWith(",")) {
                        next = 5;
                     } else {
                        next = 0;
                     }
                  }
               }
            } else {
               next = 5;
            }
         }

         ++count;
      }

      return excluded;
   }

   protected static boolean parseForceGlobal(String[] args_input) {
      String[] args = (String[])args_input.clone();
      boolean result = false;
      int count = 0;
      int next = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("r:") && !i.equals("radius:")) {
               if (next != 2 && !i.startsWith("r:") && !i.startsWith("radius:")) {
                  next = 0;
               } else {
                  i = i.replaceAll("radius:", "");
                  i = i.replaceAll("r:", "");
                  if (!i.equals("#global") && !i.equals("global") && !i.equals("off") && !i.equals("-1") && !i.equals("none") && !i.equals("false")) {
                     if (i.startsWith("#")) {
                        int world_id = Functions.matchWorld(i);
                        if (world_id > 0) {
                           result = true;
                        }
                     }
                  } else {
                     result = true;
                  }

                  next = 0;
               }
            } else {
               next = 2;
            }
         }

         ++count;
      }

      return result;
   }

   protected static Location parseLocation(CommandSender user, String[] args) {
      Location location = null;
      if (user instanceof Player) {
         location = ((Player)user).getLocation();
      } else if (user instanceof BlockCommandSender) {
         location = ((BlockCommandSender)user).getBlock().getLocation();
      }

      if (location != null) {
         location = parseCoordinates(location, args);
      }

      return location;
   }

   protected static int parseNoisy(String[] args_input) {
      String[] args = (String[])args_input.clone();
      int noisy = 0;
      int count = 0;
      if ((Integer)Config.config.get("verbose") == 1) {
         noisy = 1;
      }

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("n") && !i.equals("noisy") && !i.equals("v") && !i.equals("verbose") && !i.equals("#v") && !i.equals("#verbose")) {
               if (i.equals("#silent")) {
                  noisy = 0;
               }
            } else {
               noisy = 1;
            }
         }

         ++count;
      }

      return noisy;
   }

   protected static int parsePreview(String[] args_input) {
      String[] args = (String[])args_input.clone();
      int result = 0;
      int count = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (i.equals("#preview")) {
               result = 1;
            } else if (i.equals("#preview_cancel")) {
               result = 2;
            }
         }

         ++count;
      }

      return result;
   }

   protected static Integer[] parseRadius(String[] args_input, CommandSender user, Location location) {
      String[] args = (String[])args_input.clone();
      Integer[] radius = null;
      int count = 0;
      int next = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("r:") && !i.equals("radius:")) {
               if (next != 2 && !i.startsWith("r:") && !i.startsWith("radius:")) {
                  next = 0;
               } else {
                  i = i.replaceAll("radius:", "");
                  i = i.replaceAll("r:", "");
                  if (!i.equals("#worldedit") && !i.equals("#we")) {
                     if ((!i.startsWith("#") || i.length() <= 1) && !i.equals("global") && !i.equals("off") && !i.equals("-1") && !i.equals("none") && !i.equals("false")) {
                        int rcount = 0;
                        int r_x = 0;
                        int r_y = -1;
                        int r_z = 0;
                        String[] r_dat = new String[]{i};
                        boolean validRadius = false;
                        if (i.contains("x")) {
                           r_dat = i.split("x");
                        }

                        for(String value : r_dat) {
                           String i4 = value.replaceAll("[^0-9.]", "");
                           if (i4.length() > 0 && i4.length() == value.length() && i4.replaceAll("[^0-9]", "").length() > 0) {
                              double a1 = Double.parseDouble(i4);
                              if (rcount == 0) {
                                 r_x = (int)a1;
                                 r_z = (int)a1;
                              } else if (rcount == 1) {
                                 r_y = (int)a1;
                              } else if (rcount == 2) {
                                 r_z = (int)a1;
                              }

                              validRadius = true;
                           }

                           ++rcount;
                        }

                        if (location != null) {
                           int xmin = location.getBlockX() - r_x;
                           int xmax = location.getBlockX() + r_x;
                           int ymin = -1;
                           int ymax = -1;
                           int zmin = location.getBlockZ() - r_z;
                           int zmax = location.getBlockZ() + r_z;
                           if (r_y > -1) {
                              ymin = location.getBlockY() - r_y;
                              ymax = location.getBlockY() + r_y;
                           }

                           int max = r_x;
                           if (r_y > r_x) {
                              max = r_y;
                           }

                           if (r_z > max) {
                              max = r_z;
                           }

                           if (validRadius) {
                              radius = new Integer[]{max, xmin, xmax, ymin, ymax, zmin, zmax, 0};
                           } else {
                              radius = new Integer[]{-1};
                           }
                        }
                     }
                  } else if (user.getServer().getPluginManager().getPlugin("WorldEdit") != null) {
                     Integer[] worldEditResult = WorldEditHandler.runWorldEditCommand(user);
                     if (worldEditResult != null) {
                        radius = worldEditResult;
                     }
                  }

                  next = 0;
               }
            } else {
               next = 2;
            }
         }

         ++count;
      }

      return radius;
   }

   protected static List<Object> parseRestricted(CommandSender player, String[] args_input) {
      String[] args = (String[])args_input.clone();
      List<Object> restricted = new ArrayList();
      int count = 0;
      int next = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("b:") && !i.equals("block:") && !i.equals("blocks:")) {
               if (next != 4 && !i.startsWith("b:") && !i.startsWith("block:") && !i.startsWith("blocks:")) {
                  next = 0;
               } else {
                  i = i.replaceAll("blocks:", "");
                  i = i.replaceAll("block:", "");
                  i = i.replaceAll("b:", "");
                  if (!i.contains(",")) {
                     if (i.equals("#natural")) {
                        restricted.addAll(natural_blocks);
                     } else {
                        Material i_material = Functions.getType(i);
                        if (i_material != null) {
                           restricted.add(i_material);
                        } else {
                           EntityType i_entity = Functions.getEntityType(i);
                           if (i_entity == null) {
                              player.sendMessage(Language.get("sorry-is-an-invalid-block-name", i));
                              player.sendMessage(Language.get("please-view-co-help-blocks"));
                              return null;
                           }

                           restricted.add(i_entity);
                        }
                     }

                     next = 0;
                  } else {
                     String[] i2 = i.split(",");

                     for(String i3 : i2) {
                        if (i3.equals("#natural")) {
                           restricted.addAll(natural_blocks);
                        } else {
                           Material i3_material = Functions.getType(i3);
                           if (i3_material != null) {
                              restricted.add(i3_material);
                           } else {
                              EntityType i3_entity = Functions.getEntityType(i3);
                              if (i3_entity == null) {
                                 player.sendMessage(Language.get("sorry-is-an-invalid-block-name", i3));
                                 player.sendMessage(Language.get("please-view-co-help-blocks"));
                                 return null;
                              }

                              restricted.add(i3_entity);
                           }
                        }
                     }

                     if (i.endsWith(",")) {
                        next = 4;
                     } else {
                        next = 0;
                     }
                  }
               }
            } else {
               next = 4;
            }
         }

         ++count;
      }

      return restricted;
   }

   protected static int parseTime(String[] args_input) {
      String[] args = (String[])args_input.clone();
      int time = 0;
      int count = 0;
      int next = 0;
      double w = (double)0.0F;
      double d = (double)0.0F;
      double h = (double)0.0F;
      double m = (double)0.0F;
      double s = (double)0.0F;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("t:") && !i.equals("time:")) {
               if (next != 1 && !i.startsWith("t:") && !i.startsWith("time:")) {
                  next = 0;
               } else {
                  i = i.replaceAll("time:", "");
                  i = i.replaceAll("t:", "");
                  i = i.replaceAll("y", "y:");
                  i = i.replaceAll("m", "m:");
                  i = i.replaceAll("w", "w:");
                  i = i.replaceAll("d", "d:");
                  i = i.replaceAll("h", "h:");
                  i = i.replaceAll("s", "s:");
                  String[] i2 = i.split(":");

                  for(String i3 : i2) {
                     if (i3.endsWith("w")) {
                        String i4 = i3.replaceAll("[^0-9.]", "");
                        if (i4.length() > 0 && i4.replaceAll("[^0-9]", "").length() > 0) {
                           w = Double.parseDouble(i4);
                        }
                     } else if (i3.endsWith("d")) {
                        String i4 = i3.replaceAll("[^0-9.]", "");
                        if (i4.length() > 0 && i4.replaceAll("[^0-9]", "").length() > 0) {
                           d = Double.parseDouble(i4);
                        }
                     } else if (i3.endsWith("h")) {
                        String i4 = i3.replaceAll("[^0-9.]", "");
                        if (i4.length() > 0 && i4.replaceAll("[^0-9]", "").length() > 0) {
                           h = Double.parseDouble(i4);
                        }
                     } else if (i3.endsWith("m")) {
                        String i4 = i3.replaceAll("[^0-9.]", "");
                        if (i4.length() > 0 && i4.replaceAll("[^0-9]", "").length() > 0) {
                           m = Double.parseDouble(i4);
                        }
                     } else if (i3.endsWith("s")) {
                        String i4 = i3.replaceAll("[^0-9.]", "");
                        if (i4.length() > 0 && i4.replaceAll("[^0-9]", "").length() > 0) {
                           s = Double.parseDouble(i4);
                        }
                     }
                  }

                  double rs = w * (double)7.0F * (double)24.0F * (double)60.0F * (double)60.0F + d * (double)24.0F * (double)60.0F * (double)60.0F + h * (double)60.0F * (double)60.0F + m * (double)60.0F + s;
                  time = (int)rs;
                  next = 0;
               }
            } else {
               next = 1;
            }
         }

         ++count;
      }

      return time;
   }

   protected static String parseTimeString(String[] args_input) {
      String[] args = (String[])args_input.clone();
      String time = "";
      int count = 0;
      int next = 0;
      double w = (double)0.0F;
      double d = (double)0.0F;
      double h = (double)0.0F;
      double m = (double)0.0F;
      double s = (double)0.0F;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("t:") && !i.equals("time:")) {
               if (next != 1 && !i.startsWith("t:") && !i.startsWith("time:")) {
                  next = 0;
               } else {
                  i = i.replaceAll("time:", "");
                  i = i.replaceAll("t:", "");
                  i = i.replaceAll("y", "y:");
                  i = i.replaceAll("m", "m:");
                  i = i.replaceAll("w", "w:");
                  i = i.replaceAll("d", "d:");
                  i = i.replaceAll("h", "h:");
                  i = i.replaceAll("s", "s:");
                  String[] i2 = i.split(":");

                  for(String i3 : i2) {
                     if (i3.endsWith("w")) {
                        String i4 = i3.replaceAll("[^0-9.]", "");
                        if (i4.length() > 0 && i4.replaceAll("[^0-9]", "").length() > 0) {
                           w = Double.parseDouble(i4);
                           time = time + " " + w + " week(s)";
                        }
                     } else if (i3.endsWith("d")) {
                        String i4 = i3.replaceAll("[^0-9.]", "");
                        if (i4.length() > 0 && i4.replaceAll("[^0-9]", "").length() > 0) {
                           d = Double.parseDouble(i4);
                           time = time + " " + d + " day(s)";
                        }
                     } else if (i3.endsWith("h")) {
                        String i4 = i3.replaceAll("[^0-9.]", "");
                        if (i4.length() > 0 && i4.replaceAll("[^0-9]", "").length() > 0) {
                           h = Double.parseDouble(i4);
                           time = time + " " + h + " hour(s)";
                        }
                     } else if (i3.endsWith("m")) {
                        String i4 = i3.replaceAll("[^0-9.]", "");
                        if (i4.length() > 0 && i4.replaceAll("[^0-9]", "").length() > 0) {
                           m = Double.parseDouble(i4);
                           time = time + " " + m + " minute(s)";
                        }
                     } else if (i3.endsWith("s")) {
                        String i4 = i3.replaceAll("[^0-9.]", "");
                        if (i4.length() > 0 && i4.replaceAll("[^0-9]", "").length() > 0) {
                           s = Double.parseDouble(i4);
                           time = time + " " + s + " second(s)";
                        }
                     }
                  }

                  next = 0;
               }
            } else {
               next = 1;
            }
         }

         ++count;
      }

      return time;
   }

   private static void parseUser(List<String> users, String string) {
      string = string.trim();
      if (string.contains(",")) {
         String[] data = string.split(",");

         for(String user : data) {
            validUserCheck(users, user);
         }
      } else {
         validUserCheck(users, string);
      }

   }

   protected static List<String> parseUsers(String[] args_input) {
      String[] args = (String[])args_input.clone();
      List<String> users = new ArrayList();
      int count = 0;
      int next = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (next == 2) {
               if (i.endsWith(",")) {
                  next = 2;
               } else {
                  next = 0;
               }
            } else if (!i.equals("p:") && !i.equals("user:") && !i.equals("u:")) {
               if (next != 1 && !i.startsWith("p:") && !i.startsWith("user:") && !i.startsWith("u:")) {
                  if (!i.endsWith(",") && !i.endsWith(":")) {
                     if (i.contains(":")) {
                        next = 0;
                     } else {
                        parseUser(users, i);
                        next = 0;
                     }
                  } else {
                     next = 2;
                  }
               } else {
                  i = i.replaceAll("user:", "");
                  i = i.replaceAll("p:", "");
                  i = i.replaceAll("u:", "");
                  if (!i.contains(",")) {
                     parseUser(users, i);
                     next = 0;
                  } else {
                     String[] i2 = i.split(",");

                     for(String i3 : i2) {
                        parseUser(users, i3);
                     }

                     if (i.endsWith(",")) {
                        next = 1;
                     } else {
                        next = 0;
                     }
                  }
               }
            } else {
               next = 1;
            }
         }

         ++count;
      }

      return users;
   }

   protected static int parseWorld(String[] args_input) {
      String[] args = (String[])args_input.clone();
      int world_id = 0;
      int count = 0;
      int next = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("r:") && !i.equals("radius:")) {
               if (next != 2 && !i.startsWith("r:") && !i.startsWith("radius:")) {
                  next = 0;
               } else {
                  i = i.replaceAll("radius:", "");
                  i = i.replaceAll("r:", "");
                  if (!i.equals("#worldedit") && !i.equals("#we") && !i.equals("#global") && !i.equals("global") && !i.equals("off") && !i.equals("-1") && !i.equals("none") && !i.equals("false")) {
                     if (i.startsWith("#")) {
                        world_id = Functions.matchWorld(i);
                     }
                  } else {
                     world_id = 0;
                  }

                  next = 0;
               }
            } else {
               next = 2;
            }
         }

         ++count;
      }

      return world_id;
   }

   protected static boolean parseWorldEdit(String[] args_input) {
      String[] args = (String[])args_input.clone();
      boolean result = false;
      int count = 0;
      int next = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("r:") && !i.equals("radius:")) {
               if (next != 2 && !i.startsWith("r:") && !i.startsWith("radius:")) {
                  next = 0;
               } else {
                  i = i.replaceAll("radius:", "");
                  i = i.replaceAll("r:", "");
                  if (i.equals("#worldedit") || i.equals("#we")) {
                     result = true;
                  }

                  next = 0;
               }
            } else {
               next = 2;
            }
         }

         ++count;
      }

      return result;
   }

   protected static String parseWorldName(String[] args_input) {
      String[] args = (String[])args_input.clone();
      String world_name = "";
      int count = 0;
      int next = 0;

      for(String i : args) {
         if (count > 0) {
            i = i.trim().toLowerCase();
            i = i.replaceAll("\\\\", "");
            i = i.replaceAll("'", "");
            if (!i.equals("r:") && !i.equals("radius:")) {
               if (next != 2 && !i.startsWith("r:") && !i.startsWith("radius:")) {
                  next = 0;
               } else {
                  i = i.replaceAll("radius:", "");
                  i = i.replaceAll("r:", "");
                  if (!i.equals("#worldedit") && !i.equals("#we") && !i.equals("#global") && !i.equals("global") && !i.equals("off") && !i.equals("-1") && !i.equals("none") && !i.equals("false")) {
                     if (i.startsWith("#")) {
                        world_name = i.replaceFirst("#", "");
                     }
                  } else {
                     world_name = "";
                  }

                  next = 0;
               }
            } else {
               next = 2;
            }
         }

         ++count;
      }

      return world_name;
   }

   private static void validUserCheck(List<String> users, String user) {
      List<String> bad_users = Arrays.asList("n", "noisy", "v", "verbose", "#v", "#verbose", "#silent", "#preview", "#preview_cancel", "#count", "#sum");
      String check = user.replaceAll("[^a-zA-Z0-9#_]", "");
      if (check.equals(user) && check.length() > 0) {
         if (user.equalsIgnoreCase("#global")) {
            user = "#global";
         }

         if (!bad_users.contains(user.toLowerCase())) {
            users.add(user);
         }
      }

   }

   public boolean onCommand(CommandSender user, Command command, String commandLabel, String[] args) {
      String commandName = command.getName().toLowerCase();
      if (!commandName.equals("core") && !commandName.equals("coreprotect") && !commandName.equals("co")) {
         return false;
      } else {
         int resultc = args.length;
         if (resultc > -1) {
            String corecommand = "help";
            if (resultc > 0) {
               corecommand = args[0].toLowerCase();
            }

            boolean permission = false;
            if (user.isOp()) {
               permission = true;
            }

            if (!permission) {
               if (!user.hasPermission("coreprotect.rollback") || !corecommand.equals("rollback") && !corecommand.equals("rb") && !corecommand.equals("ro") && !corecommand.equals("apply") && !corecommand.equals("cancel")) {
                  if (!user.hasPermission("coreprotect.restore") || !corecommand.equals("restore") && !corecommand.equals("rs") && !corecommand.equals("re") && !corecommand.equals("undo") && !corecommand.equals("apply") && !corecommand.equals("cancel")) {
                     if (!user.hasPermission("coreprotect.inspect") || !corecommand.equals("i") && !corecommand.equals("inspect")) {
                        if (user.hasPermission("coreprotect.help") && corecommand.equals("help")) {
                           permission = true;
                        } else if (user.hasPermission("coreprotect.purge") && corecommand.equals("purge")) {
                           permission = true;
                        } else if (!user.hasPermission("coreprotect.lookup") || !corecommand.equals("l") && !corecommand.equals("lookup") && !corecommand.equals("near")) {
                           if (user.hasPermission("coreprotect.reload") && corecommand.equals("reload")) {
                              permission = true;
                           }
                        } else {
                           permission = true;
                        }
                     } else {
                        permission = true;
                     }
                  } else {
                     permission = true;
                  }
               } else {
                  permission = true;
               }
            }

            if (!corecommand.equals("rollback") && !corecommand.equals("restore") && !corecommand.equals("rb") && !corecommand.equals("rs") && !corecommand.equals("ro") && !corecommand.equals("re")) {
               if (corecommand.equals("apply")) {
                  ApplyCommand.runCommand(user, permission, args);
               } else if (corecommand.equals("cancel")) {
                  CancelCommand.runCommand(user, permission, args);
               } else if (corecommand.equals("undo")) {
                  UndoCommand.runCommand(user, permission, args);
               } else if (corecommand.equals("help")) {
                  HelpCommand.runCommand(user, permission, args);
               } else if (corecommand.equals("purge")) {
                  PurgeCommand.runCommand(user, permission, args);
               } else if (!corecommand.equals("inspect") && !corecommand.equals("i")) {
                  if (!corecommand.equals("lookup") && !corecommand.equals("l")) {
                     if (corecommand.equals("near")) {
                        LookupCommand.runCommand(user, permission, new String[]{"l", "r:5x5"});
                     } else if (corecommand.equals("version")) {
                        VersionCommand.runCommand(user, permission, args);
                     } else if (corecommand.equals("reload")) {
                        ReloadCommand.runCommand(user, permission, args);
                     } else {
                        user.sendMessage(Language.get("command-co-not-found", corecommand));
                     }
                  } else {
                     LookupCommand.runCommand(user, permission, args);
                  }
               } else {
                  InspectCommand.runCommand(user, permission, args);
               }
            } else {
               RollbackRestoreCommand.runCommand(user, permission, args, 0);
            }
         } else {
            user.sendMessage(Language.get("please-use-co-parameters"));
         }

         return true;
      }
   }

   static {
      natural_blocks = BlockInfo.natural_blocks;
   }
}
