package net.coreprotect;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import net.coreprotect.consumer.Queue;
import net.coreprotect.database.Database;
import net.coreprotect.database.Lookup;
import net.coreprotect.model.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class CoreProtectAPI extends Queue {
   private static List<Object> parseList(List<Object> list) {
      List<Object> result = new ArrayList();
      if (list != null) {
         for(Object value : list) {
            if (!(value instanceof Material) && !(value instanceof EntityType)) {
               if (value instanceof Integer) {
                  Material material = Functions.getMaterialFromId((Integer)value);
                  result.add(material);
               }
            } else {
               result.add(value);
            }
         }
      }

      return result;
   }

   public int APIVersion() {
      return 5;
   }

   public List<String[]> blockLookup(Block block, int time) {
      return (Integer)Config.config.get("api-enabled") == 1 ? Lookup.block_lookup_api(block, time) : null;
   }

   public boolean hasPlaced(String user, Block block, int time, int offset) {
      boolean match = false;
      if ((Integer)Config.config.get("api-enabled") == 1) {
         int unixtimestamp = (int)(System.currentTimeMillis() / 1000L);
         int stime = unixtimestamp - offset;

         for(String[] value : this.blockLookup(block, time)) {
            ParseResult result = this.parseResult(value);
            if (user.equalsIgnoreCase(result.getPlayer()) && result.getActionId() == 1 && result.getTime() <= stime) {
               match = true;
               break;
            }
         }
      }

      return match;
   }

   public boolean hasRemoved(String user, Block block, int time, int offset) {
      boolean match = false;
      if ((Integer)Config.config.get("api-enabled") == 1) {
         int unixtimestamp = (int)(System.currentTimeMillis() / 1000L);
         int stime = unixtimestamp - offset;

         for(String[] value : this.blockLookup(block, time)) {
            ParseResult result = this.parseResult(value);
            if (user.equalsIgnoreCase(result.getPlayer()) && result.getActionId() == 1 && result.getTime() <= stime) {
               match = true;
               break;
            }
         }
      }

      return match;
   }

   public boolean isEnabled() {
      return Config.config.get("api-enabled") != null && (Integer)Config.config.get("api-enabled") == 1;
   }

   public boolean logChat(Player player, String message) {
      if ((Integer)Config.config.get("api-enabled") == 1 && Functions.checkConfig(player.getWorld(), "player-messages") == 1 && player != null && message != null && message.length() > 0 && !message.startsWith("/")) {
         int time = (int)(System.currentTimeMillis() / 1000L);
         Queue.queuePlayerChat(player, message, time);
         return true;
      } else {
         return false;
      }
   }

   public boolean logCommand(Player player, String command) {
      if ((Integer)Config.config.get("api-enabled") == 1 && Functions.checkConfig(player.getWorld(), "player-commands") == 1 && player != null && command != null && command.length() > 0 && command.startsWith("/")) {
         int time = (int)(System.currentTimeMillis() / 1000L);
         Queue.queuePlayerCommand(player, command, time);
         return true;
      } else {
         return false;
      }
   }

   public boolean logInteraction(String user, Location location) {
      if ((Integer)Config.config.get("api-enabled") == 1 && user != null && location != null && user.length() > 0) {
         Queue.queuePlayerInteraction(user, location.getBlock().getState());
         return true;
      } else {
         return false;
      }
   }

   /** @deprecated */
   @Deprecated
   public boolean logPlacement(String user, Location location, int type, byte data) {
      if ((Integer)Config.config.get("api-enabled") == 1 && user != null && location != null && user.length() > 0) {
         Material material = Material.getMaterial(type);
         Queue.queueBlockPlace(user, (BlockState)location.getBlock().getState(), (Material)material, data);
         return true;
      } else {
         return false;
      }
   }

   public boolean logPlacement(String user, Location location, Material type, byte data) {
      if ((Integer)Config.config.get("api-enabled") == 1 && user != null && location != null && user.length() > 0) {
         Queue.queueBlockPlace(user, (BlockState)location.getBlock().getState(), (Material)type, data);
         return true;
      } else {
         return false;
      }
   }

   /** @deprecated */
   @Deprecated
   public boolean logRemoval(String user, Location location, int type, byte data) {
      if ((Integer)Config.config.get("api-enabled") == 1 && user != null && location != null && user.length() > 0) {
         Material material = Material.getMaterial(type);
         Queue.queueBlockBreak(user, location.getBlock().getState(), material, data);
         return true;
      } else {
         return false;
      }
   }

   public boolean logRemoval(String user, Location location, Material type, byte data) {
      if ((Integer)Config.config.get("api-enabled") == 1 && user != null && location != null && user.length() > 0) {
         Queue.queueBlockBreak(user, location.getBlock().getState(), type, data);
         return true;
      } else {
         return false;
      }
   }

   public ParseResult parseResult(String[] data) {
      return new ParseResult(data);
   }

   public List<String[]> performLookup(int time, List<String> restrict_users, List<String> exclude_users, List<Object> restrict_blocks, List<Object> exclude_blocks, List<Integer> action_list, int radius, Location radius_location) {
      return (Integer)Config.config.get("api-enabled") == 1 ? this.processData(time, radius, radius_location, parseList(restrict_blocks), parseList(exclude_blocks), restrict_users, exclude_users, action_list, 0, 1, -1, -1, false) : null;
   }

   /** @deprecated */
   @Deprecated
   public List<String[]> performLookup(String user, int time, int radius, Location location, List<Object> restrict, List<Object> exclude) {
      return (Integer)Config.config.get("api-enabled") == 1 ? this.processData(user, time, radius, location, parseList(restrict), parseList(exclude), 0, 1, -1, -1, false) : null;
   }

   public List<String[]> performPartialLookup(int time, List<String> restrict_users, List<String> exclude_users, List<Object> restrict_blocks, List<Object> exclude_blocks, List<Integer> action_list, int radius, Location radius_location, int limit_offset, int limit_count) {
      return (Integer)Config.config.get("api-enabled") == 1 ? this.processData(time, radius, radius_location, parseList(restrict_blocks), parseList(exclude_blocks), restrict_users, exclude_users, action_list, 0, 1, limit_offset, limit_count, true) : null;
   }

   /** @deprecated */
   @Deprecated
   public List<String[]> performPartialLookup(String user, int time, int radius, Location location, List<Object> restrict, List<Object> exclude, int limit_offset, int limit_count) {
      return (Integer)Config.config.get("api-enabled") == 1 ? this.processData(user, time, radius, location, parseList(restrict), parseList(exclude), 0, 1, limit_offset, limit_count, true) : null;
   }

   public void performPurge(int time) {
      Server server = CoreProtect.getInstance().getServer();
      server.dispatchCommand(server.getConsoleSender(), "co purge t:" + time + "s");
   }

   public List<String[]> performRestore(int time, List<String> restrict_users, List<String> exclude_users, List<Object> restrict_blocks, List<Object> exclude_blocks, List<Integer> action_list, int radius, Location radius_location) {
      return (Integer)Config.config.get("api-enabled") == 1 ? this.processData(time, radius, radius_location, parseList(restrict_blocks), parseList(exclude_blocks), restrict_users, exclude_users, action_list, 1, 2, -1, -1, false) : null;
   }

   /** @deprecated */
   @Deprecated
   public List<String[]> performRestore(String user, int time, int radius, Location location, List<Object> restrict, List<Object> exclude) {
      return (Integer)Config.config.get("api-enabled") == 1 ? this.processData(user, time, radius, location, parseList(restrict), parseList(exclude), 1, 2, -1, -1, false) : null;
   }

   public List<String[]> performRollback(int time, List<String> restrict_users, List<String> exclude_users, List<Object> restrict_blocks, List<Object> exclude_blocks, List<Integer> action_list, int radius, Location radius_location) {
      return (Integer)Config.config.get("api-enabled") == 1 ? this.processData(time, radius, radius_location, parseList(restrict_blocks), parseList(exclude_blocks), restrict_users, exclude_users, action_list, 0, 2, -1, -1, false) : null;
   }

   /** @deprecated */
   @Deprecated
   public List<String[]> performRollback(String user, int time, int radius, Location location, List<Object> restrict, List<Object> exclude) {
      return (Integer)Config.config.get("api-enabled") == 1 ? this.processData(user, time, radius, location, parseList(restrict), parseList(exclude), 0, 2, -1, -1, false) : null;
   }

   private List<String[]> processData(int time, int radius, Location location, List<Object> restrict_blocks, List<Object> exclude_blocks, List<String> restrict_users, List<String> exclude_users, List<Integer> action_list, int action, int lookup, int offset, int row_count, boolean use_limit) {
      List<String[]> result = new ArrayList();
      List<String> uuids = new ArrayList();
      if (restrict_users == null) {
         restrict_users = new ArrayList();
      }

      if (exclude_users == null) {
         exclude_users = new ArrayList();
      }

      if (action_list == null) {
         action_list = new ArrayList();
      }

      if (action_list.size() == 0 && restrict_blocks.size() > 0) {
         for(Object arg_block : restrict_blocks) {
            if (arg_block instanceof Material) {
               action_list.add(0);
               action_list.add(1);
            } else if (arg_block instanceof EntityType) {
               action_list.add(3);
            }
         }
      }

      if (action_list.size() == 0) {
         action_list.add(0);
         action_list.add(1);
      }

      for(int i = 0; i < action_list.size(); ++i) {
         int actionListItem = (Integer)action_list.get(i);
         if (actionListItem > 3) {
            action_list.remove(i);
         }
      }

      if (restrict_users.size() == 0) {
         restrict_users.add("#global");
      }

      int unixtimestamp = (int)(System.currentTimeMillis() / 1000L);
      int stime = unixtimestamp - time;
      if (radius < 1) {
         radius = -1;
      }

      if (restrict_users.contains("#global") && radius == -1) {
         return null;
      } else if (radius > -1 && location == null) {
         return null;
      } else {
         try {
            Connection connection = Database.getConnection(false);
            if (connection != null) {
               Statement statement = connection.createStatement();
               boolean restrict_world = false;
               if (radius > 0) {
                  restrict_world = true;
               }

               if (location == null) {
                  restrict_world = false;
               }

               Integer[] arg_radius = null;
               if (location != null && radius > 0) {
                  int xmin = location.getBlockX() - radius;
                  int xmax = location.getBlockX() + radius;
                  int zmin = location.getBlockZ() - radius;
                  int zmax = location.getBlockZ() + radius;
                  arg_radius = new Integer[]{radius, xmin, xmax, -1, -1, zmin, zmax, 0};
               }

               if (lookup == 1) {
                  if (location != null) {
                     restrict_world = true;
                  }

                  if (use_limit) {
                     result = Lookup.performPartialLookup(statement, (CommandSender)null, uuids, restrict_users, restrict_blocks, exclude_blocks, exclude_users, action_list, location, arg_radius, stime, offset, row_count, restrict_world, true);
                  } else {
                     result = Lookup.performLookup(statement, (CommandSender)null, uuids, restrict_users, restrict_blocks, exclude_blocks, exclude_users, action_list, location, arg_radius, stime, restrict_world, true);
                  }
               } else {
                  boolean verbose = false;
                  result = Lookup.performRollbackRestore(statement, (CommandSender)null, uuids, restrict_users, (String)null, restrict_blocks, exclude_blocks, exclude_users, action_list, location, arg_radius, stime, restrict_world, false, verbose, action, 0);
               }

               statement.close();
               connection.close();
            }
         } catch (Exception e) {
            e.printStackTrace();
         }

         return result;
      }
   }

   /** @deprecated */
   @Deprecated
   private List<String[]> processData(String user, int time, int radius, Location location, List<Object> restrict_blocks, List<Object> exclude_blocks, int action, int lookup, int offset, int row_count, boolean use_limit) {
      ArrayList<String> restrict_users = new ArrayList();
      if (user != null) {
         restrict_users.add(user);
      }

      return this.processData(time, radius, location, restrict_blocks, exclude_blocks, restrict_users, (List)null, (List)null, action, lookup, offset, row_count, use_limit);
   }

   public void testAPI() {
      System.out.println("[CoreProtect] API Test Successful.");
   }

   public class ParseResult {
      String[] parse = null;

      public ParseResult(String[] data) {
         this.parse = data;
      }

      public int getActionId() {
         return Integer.parseInt(this.parse[7]);
      }

      public String getActionString() {
         int ActionID = Integer.parseInt(this.parse[7]);
         String result = "Unknown";
         if (ActionID == 0) {
            result = "Removal";
         } else if (ActionID == 1) {
            result = "Placement";
         } else if (ActionID == 2) {
            result = "Interaction";
         }

         return result;
      }

      public int getData() {
         return Integer.parseInt(this.parse[6]);
      }

      public String getPlayer() {
         return this.parse[1];
      }

      public int getTime() {
         return Integer.parseInt(this.parse[0]);
      }

      public Material getType() {
         int ActionID = this.getActionId();
         int type = Integer.parseInt(this.parse[5]);
         String dname = "";
         if (ActionID == 3) {
            dname = Functions.getEntityType(type).name();
         } else {
            dname = Functions.getType(type).name().toLowerCase();
            dname = Functions.nameFilter(dname, this.getData());
         }

         return Functions.getType(dname);
      }

      /** @deprecated */
      @Deprecated
      public int getTypeId() {
         return this.getType().getId();
      }

      public int getX() {
         return Integer.parseInt(this.parse[2]);
      }

      public int getY() {
         return Integer.parseInt(this.parse[3]);
      }

      public int getZ() {
         return Integer.parseInt(this.parse[4]);
      }

      public boolean isRolledBack() {
         return Integer.parseInt(this.parse[8]) == 1;
      }

      public String worldName() {
         return Functions.getWorldName(Integer.parseInt(this.parse[9]));
      }
   }
}
