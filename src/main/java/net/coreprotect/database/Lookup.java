package net.coreprotect.database;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.coreprotect.CoreProtect;
import net.coreprotect.Functions;
import net.coreprotect.bukkit.BukkitAdapter;
import net.coreprotect.consumer.Queue;
import net.coreprotect.model.BlockInfo;
import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

public class Lookup extends Queue {
   // Both of these were allocated fresh inside the rollback row loop, on the main
   // thread, once per row. They are constant; all of these Materials exist as far
   // back as 1.8.8, so class init is safe on every supported server version.
   private static final List<Material> UPDATE_STATE = Collections.unmodifiableList(Arrays.asList(Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.TORCH, Material.REDSTONE_WIRE, Material.BURNING_FURNACE, Material.LEVER, Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_ON, Material.GLOWSTONE, Material.JACK_O_LANTERN, Material.DIODE_BLOCK_OFF, Material.DIODE_BLOCK_ON, Material.REDSTONE_LAMP_ON, Material.BEACON, Material.REDSTONE_COMPARATOR_OFF, Material.REDSTONE_COMPARATOR_ON, Material.DAYLIGHT_DETECTOR, Material.REDSTONE_BLOCK, Material.HOPPER, Material.ACTIVATOR_RAIL));
   private static final List<Material> UNSAFE_BLOCKS = Collections.unmodifiableList(Arrays.asList(Material.LAVA, Material.FIRE));

   /**
    * Java deserialisation is pure computation with no Bukkit dependency, so it is
    * done on the rollback thread rather than inside the main-thread chunk task.
    *
    * Returns null on a malformed blob. This is a deliberate behaviour change: the
    * old inline deserialisation sat outside the per-row try/catch, so a single
    * corrupt blob threw out to the chunk task's handler, flagged the chunk as
    * failed and aborted the entire rollback. Now that row is simply treated as
    * having no metadata and the rollback continues.
    */
   private static List<Object> deserializeMeta(byte[] meta) {
      if (meta == null) {
         return null;
      }

      try {

          try (ObjectInputStream ins = new ObjectInputStream(new ByteArrayInputStream(meta))) {
              return (List) ins.readObject();
          }
      } catch (Exception e) {
         return null;
      }
   }

   public static String block_lookup(Statement statement, BlockState block, String user, int offset, int page, int limit) {
      String result = "";

      try {
         if (block == null) {
            return result;
         }

         boolean found = false;
         int x = block.getX();
         int y = block.getY();
         int z = block.getZ();
         int time = (int)(System.currentTimeMillis() / 1000L);
         int wid = Functions.getWorldId(block.getWorld().getName());
         int check_time = 0;
         int count = 0;
         int row_max = page * limit;
         int page_start = row_max - limit;
         if (offset > 0) {
            check_time = time - offset;
         }

         String blockName = "air";
         if (block != null && block.getType() != null) {
            blockName = block.getType().name().toLowerCase();
         }

         String query = "SELECT COUNT(*) as count from " + Config.prefix + "block WHERE wid = '" + wid + "' AND x = '" + x + "' AND z = '" + z + "' AND y = '" + y + "' AND action IN(0,1) AND time >= '" + check_time + "' LIMIT 0, 1";

         ResultSet rs;
         for (rs = statement.executeQuery(query); rs.next(); count = rs.getInt("count")) {
         }

         rs.close();
         int total_pages = (int)Math.ceil((double)count / ((double)limit + 0.0D));
         query = "SELECT time,user,action,type,data,rolled_back FROM " + Config.prefix + "block WHERE wid = '" + wid + "' AND x = '" + x + "' AND z = '" + z + "' AND y = '" + y + "' AND action IN(0,1) AND time >= '" + check_time + "' ORDER BY rowid DESC LIMIT " + page_start + ", " + limit + "";

         String result_user;
         String timeago;
         String a2;
         String rbd;
         String dname;
         for (rs = statement.executeQuery(query); rs.next(); result = result + Language.get("lookup-row", timeago, rbd, result_user, a2, dname)) {
            int result_userid = rs.getInt("user");
            int result_action = rs.getInt("action");
            int result_type = rs.getInt("type");
            int result_data = rs.getInt("data");
            int result_time = rs.getInt("time");
            int result_rolled_back = rs.getInt("rolled_back");
            if (Config.player_id_cache_reversed.get(result_userid) == null) {
               Database.loadUserName(statement.getConnection(), result_userid);
            }

            result_user = Config.player_id_cache_reversed.get(result_userid);
            double time_since = (double)time - ((double)result_time + 0.0D);
            time_since /= 60.0D;
            time_since /= 60.0D;
            timeago = (new DecimalFormat("0.00")).format(time_since);
            if (!found) {
               result = Language.get("lookup-header", x, y, z);
            }

            found = true;
            a2 = "placed";
            if (result_action == 0) {
               a2 = "removed";
            } else if (result_action == 2) {
               a2 = "clicked";
            } else if (result_action == 3) {
               a2 = "killed";
            }

            rbd = "";
            if (result_rolled_back == 1) {
               rbd = Language.get("rolled-back-marker");
            }

            dname = "";
            if (result_action == 3) {
               dname = Functions.getEntityType(result_type).name();
            } else {
               dname = Functions.nameFilter(Functions.getTypeName(result_type).toLowerCase(), result_data);
               dname = "minecraft:" + dname.toLowerCase();
            }

            if (!dname.isEmpty()) {
               dname = "" + dname + "";
            }

            if (dname.contains("minecraft:")) {
               String[] block_name_split = dname.split(":");
               dname = block_name_split[1];
            }
         }

         rs.close();
         if (found) {
            if (count > limit) {
               String n = Language.get("lookup-divider");
               n = n + Language.get("lookup-page-footer", page, total_pages);
               result = result + n;
            }
         } else if (!found) {
            if (row_max > count && count > 0) {
               result = Language.get("no-block-data-for-page");
            } else {
               result = Language.get("no-block-data-for-location");
               if (!blockName.equals("air") && !blockName.equals("cave_air")) {
                  result = Language.get("no-block-data-at", block.getType().name().toLowerCase());
               }
            }
         }

         String bc = x + "." + y + "." + z + "." + wid + ".0." + limit;
         Config.lookup_page.put(user, page);
         Config.lookup_type.put(user, 2);
         Config.lookup_command.put(user, bc);
      } catch (Exception e) {
         e.printStackTrace();
      }

      return result;
   }

   public static List<String[]> block_lookup_api(Block block, int offset) {
      List<String[]> result = new ArrayList<>();

      try {
         if (block == null) {
            return result;
         }

         int x = block.getX();
         int y = block.getY();
         int z = block.getZ();
         int time = (int)(System.currentTimeMillis() / 1000L);
         int wid = Functions.getWorldId(block.getWorld().getName());
         int check_time = 0;
         if (offset > 0) {
            check_time = time - offset;
         }

         Connection connection = Database.getConnection(false);
         if (connection == null) {
            return result;
         }

         Statement statement = connection.createStatement();
         String query = "SELECT time,user,action,type,data,rolled_back FROM " + Config.prefix + "block WHERE wid = '" + wid + "' AND x = '" + x + "' AND z = '" + z + "' AND y = '" + y + "' AND time > '" + check_time + "' ORDER BY rowid DESC";
         ResultSet rs = statement.executeQuery(query);

         while (rs.next()) {
            int result_time = rs.getInt("time");
            int result_userid = rs.getInt("user");
            int result_action = rs.getInt("action");
            int result_type = rs.getInt("type");
            int result_data = rs.getInt("data");
            int result_rolled_back = rs.getInt("rolled_back");
            if (Config.player_id_cache_reversed.get(result_userid) == null) {
               Database.loadUserName(connection, result_userid);
            }

            String result_user = Config.player_id_cache_reversed.get(result_userid);
            String line = result_time + "," + result_user + "," + x + "." + y + "." + z + "," + result_type + "," + result_data + "," + result_action + "," + result_rolled_back + "," + wid + ",";
            String[] ldata = Functions.toStringArray(line);
            result.add(ldata);
         }

         rs.close();
         statement.close();
         connection.close();
      } catch (Exception e) {
         e.printStackTrace();
      }

      return result;
   }

   public static String chest_transactions(Statement statement, Location l, String lookup_user, int page, int limit) {
      String result = "";

      try {
         if (l == null) {
            return result;
         }

         boolean found = false;
         int x = (int)Math.floor(l.getX());
         int y = (int)Math.floor(l.getY());
         int z = (int)Math.floor(l.getZ());
         int x2 = (int)Math.ceil(l.getX());
         int y2 = (int)Math.ceil(l.getY());
         int z2 = (int)Math.ceil(l.getZ());
         int time = (int)(System.currentTimeMillis() / 1000L);
         int wid = Functions.getWorldId(l.getWorld().getName());
         int count = 0;
         int row_max = page * limit;
         int page_start = row_max - limit;
         String query = "SELECT COUNT(*) as count from " + Config.prefix + "container WHERE wid = '" + wid + "' AND (x = '" + x + "' OR x = '" + x2 + "') AND (z = '" + z + "' OR z = '" + z2 + "') AND y = '" + y + "' LIMIT 0, 1";

         ResultSet rs;
         for (rs = statement.executeQuery(query); rs.next(); count = rs.getInt("count")) {
         }

         rs.close();
         int total_pages = (int)Math.ceil((double)count / ((double)limit + 0.0D));
         query = "SELECT time,user,action,type,data,amount,rolled_back FROM " + Config.prefix + "container WHERE wid = '" + wid + "' AND (x = '" + x + "' OR x = '" + x2 + "') AND (z = '" + z + "' OR z = '" + z2 + "') AND y = '" + y + "' ORDER BY rowid DESC LIMIT " + page_start + ", " + limit + "";

         int result_amount;
         String result_user;
         String timeago;
         String a2;
         String rbd;
         String dname;
         for (rs = statement.executeQuery(query); rs.next(); result = result + Language.get("lookup-row-container", timeago, rbd, result_user, a2, result_amount, dname)) {
            int result_userid = rs.getInt("user");
            int result_action = rs.getInt("action");
            int result_type = rs.getInt("type");
            int result_data = rs.getInt("data");
            int result_time = rs.getInt("time");
            result_amount = rs.getInt("amount");
            int result_rolled_back = rs.getInt("rolled_back");
            if (Config.player_id_cache_reversed.get(result_userid) == null) {
               Database.loadUserName(statement.getConnection(), result_userid);
            }

            result_user = Config.player_id_cache_reversed.get(result_userid);
            double time_since = (double)time - ((double)result_time + 0.0D);
            time_since /= 60.0D;
            time_since /= 60.0D;
            timeago = (new DecimalFormat("0.00")).format(time_since);
            if (!found) {
               result = Language.get("lookup-header-container", x, y, z);
            }

            found = true;
            a2 = "added";
            if (result_action == 0) {
               a2 = "removed";
            }

            rbd = "";
            if (result_rolled_back == 1) {
               rbd = Language.get("rolled-back-marker");
            }

            dname = Functions.getTypeName(result_type).toLowerCase();
            dname = Functions.nameFilter(dname, result_data);
            if (!dname.isEmpty()) {
               dname = "minecraft:" + dname.toLowerCase() + "";
            }

            if (dname.contains("minecraft:")) {
               String[] block_name_split = dname.split(":");
               dname = block_name_split[1];
            }
         }

         rs.close();
         if (found) {
            if (count > limit) {
               String n = Language.get("lookup-divider");
               n = n + Language.get("lookup-page-footer", page, total_pages);
               result = result + n;
            }
         } else if (!found) {
            if (row_max > count && count > 0) {
               result = Language.get("no-container-data-for-page");
            } else {
               result = Language.get("no-container-data-for-location");
            }
         }

         String bc = x + "." + y + "." + z + "." + wid + "." + x2 + "." + y2 + "." + z2 + "." + limit;
         Config.lookup_type.put(lookup_user, 1);
         Config.lookup_page.put(lookup_user, page);
         Config.lookup_command.put(lookup_user, bc);
      } catch (Exception e) {
         e.printStackTrace();
      }

      return result;
   }

   private static List<String[]> convertRawLookup(Statement statement, List<Object[]> list) {
      List<String[]> new_list = new ArrayList<>();
      if (list == null) {
         return null;
      } else {
         for (Object[] map : list) {
            int new_length = map.length - 1;
            String[] results = new String[new_length];

            for (int i2 = 0; i2 < map.length; ++i2) {
               try {
                  int new_id = i2 - 1;
                  if (i2 == 2) {
                     if (map[i2] instanceof Integer) {
                        int user_id = (Integer)map[i2];
                        if (Config.player_id_cache_reversed.get(user_id) == null) {
                           Database.loadUserName(statement.getConnection(), user_id);
                        }

                        String user_result = Config.player_id_cache_reversed.get(user_id);
                        results[new_id] = user_result;
                     } else {
                        results[new_id] = (String)map[i2];
                     }
                  } else if (i2 > 0) {
                     if (map[i2] instanceof Integer) {
                        results[new_id] = map[i2].toString();
                     } else if (map[i2] instanceof String) {
                        results[new_id] = (String)map[i2];
                     }
                  }
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }

            new_list.add(results);
         }

         return new_list;
      }
   }

   public static int countLookupRows(Statement statement, CommandSender user, List<String> check_uuids, List<String> check_users, List<Object> restrict_list, List<Object> exclude_list, List<String> exclude_user_list, List<Integer> action_list, Location location, Integer[] radius, int check_time, boolean restrict_world, boolean lookup) {
      int rows = 0;

      // The is_paused check-then-set that used to wrap this was not a mutex --
      // two threads could both pass the check, and Process cleared the flag
      // regardless of who set it. SQLite concurrency is handled by the WAL and
      // busy_timeout pragmas in Database instead.
      try {
         ResultSet rs;
         for (rs = rawLookupResultSet(statement, user, check_uuids, check_users, restrict_list, exclude_list, exclude_user_list, action_list, location, radius, check_time, -1, -1, restrict_world, lookup, true); rs.next(); rows = rs.getInt("count")) {
         }

         rs.close();
      } catch (Exception e) {
         e.printStackTrace();
         return -1;
      }

      return rows;
   }

   public static void finishRollbackRestore(CommandSender user, Location location, List<String> check_users, List<Object> restrict_list, List<Object> exclude_list, List<String> exclude_user_list, List<Integer> action_list, String time_string, int file, int seconds, int item_count, int block_count, int entity_count, int rollback_type, Integer[] radius, boolean verbose, boolean restrict_world, int preview) {
      try {
         if (preview == 2) {
            user.sendMessage(Language.get("preview-cancelled"));
            return;
         }

         user.sendMessage("-----");
         StringBuilder users = new StringBuilder();

         for (String value : check_users) {
            if (users.length() == 0) {
               users = new StringBuilder("" + value + "");
            } else {
               users.append(", ").append(value);
            }
         }

         if (users.toString().equals("#global") && restrict_world) {
            users = new StringBuilder("#" + location.getWorld().getName());
         }

         if (preview > 0) {
            user.sendMessage(Language.get("preview-completed-for", users.toString()));
         } else if (rollback_type == 1) {
            user.sendMessage(Language.get("restore-completed-for", users.toString()));
         } else if (rollback_type == 0) {
            user.sendMessage(Language.get("rollback-completed-for", users.toString()));
         }

         if (preview == 1) {
            user.sendMessage(Language.get("time", time_string));
         } else if (rollback_type == 1) {
            user.sendMessage(Language.get("restored", time_string));
         } else if (rollback_type == 0) {
            user.sendMessage(Language.get("rolled-back", time_string));
         }

         if (radius != null) {
            int worldedit = radius[7];
            if (worldedit == 0) {
               int rad = radius[0];
               user.sendMessage(Language.get("radius-block-s", rad));
            } else {
               user.sendMessage(Language.get("radius-worldedit"));
            }
         }

         if (restrict_world && radius == null && location != null) {
            user.sendMessage(Language.get("limited-to-world", location.getWorld().getName()));
         }

         if (action_list.contains(4)) {
            if (action_list.contains(0)) {
               user.sendMessage(Language.get("limited-to-action-container"));
            } else if (action_list.contains(1)) {
               user.sendMessage(Language.get("limited-to-action-container-2"));
            }
         } else if (action_list.contains(0) && action_list.contains(1)) {
            user.sendMessage(Language.get("limited-to-action-block-change"));
         } else if (action_list.contains(0)) {
            user.sendMessage(Language.get("limited-to-action-block-break"));
         } else if (action_list.contains(1)) {
            user.sendMessage(Language.get("limited-to-action-block-place"));
         } else if (action_list.contains(3)) {
            user.sendMessage(Language.get("limited-to-action-entity-kill"));
         }

         if (!restrict_list.isEmpty()) {
            StringBuilder r = new StringBuilder();
            int rc = 0;

            for (Object rt : restrict_list) {
               String value_name = "";
               if (rt instanceof Material) {
                  value_name = ((Material)rt).name().toLowerCase();
               } else if (rt instanceof EntityType) {
                  value_name = ((EntityType)rt).name().toLowerCase();
               }

               if (rc == 0) {
                  r = new StringBuilder("" + value_name + "");
               } else {
                  r.append(", ").append(value_name);
               }

               ++rc;
            }

            user.sendMessage(Language.get("limited-to-block-type-s", r.toString()));
         }

         if (!exclude_list.isEmpty()) {
            StringBuilder e = new StringBuilder();
            int ec = 0;

            for (Object et : exclude_list) {
               String value_name = "";
               if (et instanceof Material) {
                  value_name = ((Material)et).name().toLowerCase();
               } else if (et instanceof EntityType) {
                  value_name = ((EntityType)et).name().toLowerCase();
               }

               if (ec == 0) {
                  e = new StringBuilder("" + value_name + "");
               } else {
                  e.append(", ").append(value_name);
               }

               ++ec;
            }

            user.sendMessage(Language.get("excluded-block-type-s", e.toString()));
         }

         if (!exclude_user_list.isEmpty()) {
            StringBuilder e = new StringBuilder();
            int ec = 0;

            for (String et : exclude_user_list) {
               if (ec == 0) {
                  e = new StringBuilder("" + et + "");
               } else {
                  e.append(", ").append(et);
               }

               ++ec;
            }

            user.sendMessage(Language.get("excluded-user-s", e.toString()));
         }

         if (action_list.contains(5)) {
            user.sendMessage(Language.get("approx-item-s-changed", block_count));
         } else if (preview == 0) {
            if (item_count > 0) {
               user.sendMessage(Language.get("approx-item-s-changed", item_count));
            }

            if (entity_count > 0) {
               if (entity_count == 1) {
                  user.sendMessage(Language.get("approx-entity-changed", entity_count));
               } else {
                  user.sendMessage(Language.get("approx-entities-changed", entity_count));
               }
            }

            user.sendMessage(Language.get("approx-block-s-changed", block_count));
         } else if (preview > 0) {
            user.sendMessage(Language.get("approx-block-s-to-change", block_count));
         }

         if (verbose && preview == 0 && file > -1) {
            user.sendMessage(Language.get("modified-chunk-s", file));
         }

         if (preview == 0) {
            user.sendMessage(Language.get("time-taken-second-s", seconds));
         }

         user.sendMessage("-----");
         if (preview > 0) {
            user.sendMessage(Language.get("please-select-co-apply-or-co"));
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public static String interaction_lookup(Statement statement, Block block, String user, int offset, int page, int limit) {
      String result = "";

      try {
         if (block == null) {
            return result;
         }

         boolean found = false;
         int x = block.getX();
         int y = block.getY();
         int z = block.getZ();
         int time = (int)(System.currentTimeMillis() / 1000L);
         int wid = Functions.getWorldId(block.getWorld().getName());
         int check_time = 0;
         int count = 0;
         int row_max = page * limit;
         int page_start = row_max - limit;
         if (offset > 0) {
            check_time = time - offset;
         }

         String query = "SELECT COUNT(*) as count from " + Config.prefix + "block WHERE wid = '" + wid + "' AND x = '" + x + "' AND z = '" + z + "' AND y = '" + y + "' AND action='2' AND time >= '" + check_time + "' LIMIT 0, 1";

         ResultSet rs;
         for (rs = statement.executeQuery(query); rs.next(); count = rs.getInt("count")) {
         }

         rs.close();
         int total_pages = (int)Math.ceil((double)count / ((double)limit + 0.0D));
         query = "SELECT time,user,action,type,data,rolled_back FROM " + Config.prefix + "block WHERE wid = '" + wid + "' AND x = '" + x + "' AND z = '" + z + "' AND y = '" + y + "' AND action='2' AND time >= '" + check_time + "' ORDER BY rowid DESC LIMIT " + page_start + ", " + limit + "";

         String result_user;
         String timeago;
         String a2;
         String rbd;
         String dname;
         for (rs = statement.executeQuery(query); rs.next(); result = result + Language.get("lookup-row", timeago, rbd, result_user, a2, dname)) {
            int result_userid = rs.getInt("user");
            int result_action = rs.getInt("action");
            int result_type = rs.getInt("type");
            int result_data = rs.getInt("data");
            int result_time = rs.getInt("time");
            int result_rolled_back = rs.getInt("rolled_back");
            if (Config.player_id_cache_reversed.get(result_userid) == null) {
               Database.loadUserName(statement.getConnection(), result_userid);
            }

            result_user = Config.player_id_cache_reversed.get(result_userid);
            double time_since = (double)time - ((double)result_time + 0.0D);
            time_since /= 60.0D;
            time_since /= 60.0D;
            timeago = (new DecimalFormat("0.00")).format(time_since);
            if (!found) {
               result = Language.get("lookup-header-interaction", x, y, z);
            }

            found = true;
            a2 = "placed";
            if (result_action == 0) {
               a2 = "removed";
            } else if (result_action == 2) {
               a2 = "clicked";
            }

            rbd = "";
            if (result_rolled_back == 1) {
               rbd = Language.get("rolled-back-marker");
            }

            dname = Functions.getTypeName(result_type).toLowerCase();
            dname = Functions.nameFilter(dname, result_data);
            if (!dname.isEmpty()) {
               dname = "minecraft:" + dname.toLowerCase() + "";
            }

            if (dname.contains("minecraft:")) {
               String[] block_name_split = dname.split(":");
               dname = block_name_split[1];
            }
         }

         rs.close();
         if (found) {
            if (count > limit) {
               String n = Language.get("lookup-divider");
               n = n + Language.get("lookup-page-footer", page, total_pages);
               result = result + n;
            }
         } else if (!found) {
            if (row_max > count && count > 0) {
               result = Language.get("no-interaction-data-for-page");
            } else {
               result = Language.get("no-interaction-data-for-location");
            }
         }

         String bc = x + "." + y + "." + z + "." + wid + ".2." + limit;
         Config.lookup_page.put(user, page);
         Config.lookup_type.put(user, 7);
         Config.lookup_command.put(user, bc);
      } catch (Exception e) {
         e.printStackTrace();
      }

      return result;
   }

   public static void modifyContainerItems(Material type, Object container, int slot, ItemStack itemstack, int action) {
      try {
         ItemStack[] contents = null;
         if (type.equals(Material.ARMOR_STAND)) {
            EntityEquipment equipment = (EntityEquipment)container;
            if (equipment != null) {
               contents = equipment.getArmorContents();
               if (action == 1) {
                  itemstack.setAmount(1);
               } else {
                  itemstack.setType(Material.AIR);
                  itemstack.setAmount(0);
               }

               if (slot >= 0) {
                  contents[slot] = itemstack;
               }

               equipment.setArmorContents(contents);
            }
         } else {
            Inventory inventory = (Inventory)container;
            if (inventory != null) {
               int count = 0;
               int amount = itemstack.getAmount();
               itemstack.setAmount(1);

               for (; count < amount; ++count) {
                  if (action == 1) {
                     inventory.addItem(new ItemStack[]{itemstack});
                  } else {
                     inventory.removeItem(new ItemStack[]{itemstack});
                  }
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public static void performContainerRollbackRestore(Statement statement, CommandSender user, List<String> check_uuids, List<String> check_users, String time_string, List<Object> restrict_list, List<Object> exclude_list, List<String> exclude_user_list, List<Integer> action_list, final Location location, Integer[] radius, int check_time, boolean restrict_world, boolean lookup, boolean verbose, final int rollback_type) {
      try {
         long time1 = System.currentTimeMillis();
         final List<Object[]> lookup_list = performLookupRaw(statement, user, check_uuids, check_users, restrict_list, exclude_list, exclude_user_list, action_list, location, radius, check_time, -1, -1, restrict_world, lookup);
         if (lookup_list == null) {
            if (user != null) {
               user.sendMessage(Language.get("database-query-failed"));
            }

            return;
         }

         if (rollback_type == 1) {
            Collections.reverse(lookup_list);
         }

         String user_string = "#server";
         if (user != null) {
            user_string = user.getName();
         }

         Queue.queueContainerRollbackUpdate(user_string, location, lookup_list, rollback_type);
         final String final_user_string = user_string;
         Config.rollback_hash.put(user_string, new int[]{0, 0, 0, 0});
         CoreProtect.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(CoreProtect.getInstance(), () -> {
            try {
               int[] rollback_hash_data = Config.rollback_hash.get(final_user_string);
               int item_count = rollback_hash_data[0];
               int entity_count = rollback_hash_data[2];
               Block block = location.getBlock();
               if (!block.getWorld().isChunkLoaded(block.getChunk())) {
                  block.getWorld().loadChunk(block.getChunk());
               }

               Object container = null;
               Material type = block.getType();
               if (BlockInfo.containers.contains(type)) {
                  container = Functions.getContainerInventory(block.getState(), false);
               } else {
                  for (Entity entity : block.getChunk().getEntities()) {
                     if (entity instanceof ArmorStand && entity.getLocation().getBlockX() == location.getBlockX() && entity.getLocation().getBlockY() == location.getBlockY() && entity.getLocation().getBlockZ() == location.getBlockZ()) {
                        type = Material.ARMOR_STAND;
                        container = Functions.getEntityEquipment((LivingEntity)entity);
                     }
                  }
               }

               int modify_count = 0;
               if (container != null) {
                  for (Object[] row : lookup_list) {
                     int row_type_raw = (Integer)row[6];
                     int row_data = (Integer)row[7];
                     int row_action = (Integer)row[8];
                     int row_rolled_back = (Integer)row[9];
                     int row_amount = (Integer)row[11];
                     byte[] row_metadata = (byte[])row[12];
                     Material row_type = Functions.getType(row_type_raw);
                     if (rollback_type == 0 && row_rolled_back == 0 || rollback_type == 1 && row_rolled_back == 1) {
                        modify_count += row_amount;
                        int action = 0;
                        if (rollback_type == 0 && row_action == 0) {
                           action = 1;
                        }

                        if (rollback_type == 1 && row_action == 1) {
                           action = 1;
                        }

                        ItemStack itemstack = new ItemStack(row_type, row_amount, (short)row_data);
                        Object[] populatedStack = Lookup.populateItemStack(itemstack, row_metadata);
                        int slot = (Integer)populatedStack[0];
                        itemstack = (ItemStack)populatedStack[1];
                        Lookup.modifyContainerItems(type, container, slot, itemstack, action);
                     }
                  }
               }

               Config.rollback_hash.put(final_user_string, new int[]{item_count, modify_count, entity_count, 1});
            } catch (Exception e) {
               e.printStackTrace();
            }

         }, 0L);
         int[] rollback_hash_data = Config.rollback_hash.get(user_string);
         int next = rollback_hash_data[3];
         int sleep_time = 0;

         while (next == 0) {
            sleep_time += 5;
            Thread.sleep(5L);
            rollback_hash_data = Config.rollback_hash.get(final_user_string);
            next = rollback_hash_data[3];
            if (sleep_time > 300000) {
               System.out.println("[CoreProtect] Rollback or restore aborted.");
               break;
            }
         }

         rollback_hash_data = Config.rollback_hash.get(final_user_string);
         int block_count = rollback_hash_data[1];
         long time2 = System.currentTimeMillis();
         int seconds = (int)((time2 - time1) / 1000L);
         if (user != null) {
            int file = -1;
            if (block_count > 0) {
               file = 1;
            }

            int item_count = 0;
            int entity_count = 0;
            finishRollbackRestore(user, location, check_users, restrict_list, exclude_list, exclude_user_list, action_list, time_string, file, seconds, item_count, block_count, entity_count, rollback_type, radius, verbose, restrict_world, 0);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public static List<String[]> performLookup(Statement statement, CommandSender user, List<String> check_uuids, List<String> check_users, List<Object> restrict_list, List<Object> exclude_list, List<String> exclude_user_list, List<Integer> action_list, Location location, Integer[] radius, int check_time, boolean restrict_world, boolean lookup) {
      List<String[]> new_list = new ArrayList<>();

      try {
         List<Object[]> lookup_list = performLookupRaw(statement, user, check_uuids, check_users, restrict_list, exclude_list, exclude_user_list, action_list, location, radius, check_time, -1, -1, restrict_world, lookup);
         new_list = convertRawLookup(statement, lookup_list);
      } catch (Exception e) {
         e.printStackTrace();
      }

      return new_list;
   }

   public static List<Object[]> performLookupRaw(Statement statement, CommandSender user, List<String> check_uuids, List<String> check_users, List<Object> restrict_list, List<Object> exclude_list, List<String> exclude_user_list, List<Integer> action_list, Location location, Integer[] radius, int check_time, int limit_offset, int limit_count, boolean restrict_world, boolean lookup) {
      List<Object[]> list = new ArrayList<>();
      List<Integer> invalid_rollback_actions = new ArrayList<>();
      invalid_rollback_actions.add(2);
      if (Config.config.get("rollback-entities") == 0 && !action_list.contains(3)) {
         invalid_rollback_actions.add(3);
      }

      // Returns null when the query itself failed, as distinct from an empty
      // list meaning "nothing matched". Previously a failure was swallowed here
      // and the empty list was returned, so a rollback whose SELECT lost a
      // SQLITE_BUSY race ran over zero rows and still reported success.
      try {
         ResultSet rs = rawLookupResultSet(statement, user, check_uuids, check_users, restrict_list, exclude_list, exclude_user_list, action_list, location, radius, check_time, limit_offset, limit_count, restrict_world, lookup, false);

         while (rs.next()) {
            if (!action_list.contains(6) && !action_list.contains(7)) {
               if (action_list.contains(8)) {
                  int result_id = rs.getInt("id");
                  int result_time = rs.getInt("time");
                  int result_userid = rs.getInt("user");
                  int result_wid = rs.getInt("wid");
                  int result_x = rs.getInt("x");
                  int result_y = rs.getInt("y");
                  int result_z = rs.getInt("z");
                  int result_action = rs.getInt("action");
                  Object[] data_array = new Object[]{result_id, result_time, result_userid, result_wid, result_x, result_y, result_z, result_action};
                  list.add(data_array);
               } else if (action_list.contains(9)) {
                  int result_id = rs.getInt("id");
                  int result_time = rs.getInt("time");
                  String result_uuid = rs.getString("uuid");
                  String result_user = rs.getString("user");
                  Object[] data_array = new Object[]{result_id, result_time, result_uuid, result_user};
                  list.add(data_array);
               } else {
                  int result_amount = 0;
                  byte[] result_meta = null;
                  int result_id = rs.getInt("id");
                  int result_userid = rs.getInt("user");
                  int result_action = rs.getInt("action");
                  int result_type = rs.getInt("type");
                  int result_data = rs.getInt("data");
                  int result_rolled_back = rs.getInt("rolled_back");
                  int result_time = rs.getInt("time");
                  int result_x = rs.getInt("x");
                  int result_y = rs.getInt("y");
                  int result_z = rs.getInt("z");
                  int result_wid = rs.getInt("wid");
                  if (!action_list.contains(4) && !action_list.contains(5)) {
                     result_meta = rs.getBytes("meta");
                  } else {
                     result_amount = rs.getInt("amount");
                     result_meta = rs.getBytes("metadata");
                  }

                  boolean valid = lookup || !invalid_rollback_actions.contains(result_action);

                   if (valid) {
                      Object[] data_array;
                      if (!action_list.contains(4) && !action_list.contains(5)) {
                          data_array = new Object[]{result_id, result_time, result_userid, result_x, result_y, result_z, result_type, result_data, result_action, result_rolled_back, result_wid, result_meta};
                      } else {
                          data_array = new Object[]{result_id, result_time, result_userid, result_x, result_y, result_z, result_type, result_data, result_action, result_rolled_back, result_wid, result_amount, result_meta};
                      }
                      list.add(data_array);
                  }
               }
            } else {
               int result_id = rs.getInt("id");
               int result_time = rs.getInt("time");
               int result_userid = rs.getInt("user");
               String result_message = rs.getString("message");
               Object[] data_array = new Object[]{result_id, result_time, result_userid, result_message};
               list.add(data_array);
            }
         }

         rs.close();
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }

      return list;
   }

   public static List<String[]> performPartialLookup(Statement statement, CommandSender user, List<String> check_uuids, List<String> check_users, List<Object> restrict_list, List<Object> exclude_list, List<String> exclude_user_list, List<Integer> action_list, Location location, Integer[] radius, int check_time, int limit_offset, int limit_count, boolean restrict_world, boolean lookup) {
      List<String[]> new_list = new ArrayList<>();

      try {
         List<Object[]> lookup_list = performLookupRaw(statement, user, check_uuids, check_users, restrict_list, exclude_list, exclude_user_list, action_list, location, radius, check_time, limit_offset, limit_count, restrict_world, lookup);
         new_list = convertRawLookup(statement, lookup_list);
      } catch (Exception e) {
         e.printStackTrace();
      }

      return new_list;
   }

   public static List<String[]> performRollbackRestore(Statement statement, final CommandSender user, List<String> check_uuids, List<String> check_users, String time_string, List<Object> restrict_list, List<Object> exclude_list, List<String> exclude_user_list, List<Integer> action_list, Location location, Integer[] radius, int check_time, boolean restrict_world, boolean lookup, boolean verbose, final int rollback_type, final int preview) {
      new ArrayList();

      try {
         long time1 = System.currentTimeMillis();
         List<Object[]> lookup_list = new ArrayList<>();
         if (!action_list.contains(4) && !action_list.contains(5) && !check_users.contains("#container")) {
            lookup_list = performLookupRaw(statement, user, check_uuids, check_users, restrict_list, exclude_list, exclude_user_list, action_list, location, radius, check_time, -1, -1, restrict_world, lookup);
         }

         if (lookup_list == null) {
            return null;
         } else {
            boolean rollbackItems = false;
            List<Object> itemRestrictList = new ArrayList<>(restrict_list);
            List<Object> itemExcludeList = new ArrayList<>(exclude_list);
            if (action_list.contains(1)) {
               for (Object value : restrict_list) {
                  if (value instanceof Material && !exclude_list.contains(value) && BlockInfo.containers.contains(value)) {
                     rollbackItems = true;
                     itemRestrictList.clear();
                     itemExcludeList.clear();
                     break;
                  }
               }
            }

            List<Object[]> item_list = new ArrayList<>();
            if (Config.config.get("rollback-items") == 1 && !check_users.contains("#container") && (action_list.isEmpty() || action_list.contains(4) || rollbackItems) && preview == 0) {
                List<Integer> item_action_list = new ArrayList<>(action_list);
               if (!item_action_list.contains(4)) {
                  item_action_list.add(4);
               }

               item_list = performLookupRaw(statement, user, check_uuids, check_users, itemRestrictList, itemExcludeList, exclude_user_list, item_action_list, location, radius, check_time, -1, -1, restrict_world, lookup);
               if (item_list == null) {
                  // Half a rollback is worse than none: the blocks would come
                  // back without the container contents that belong in them.
                  return null;
               }
            }

            TreeMap<String, Integer> chunk_list = new TreeMap<>();
            final HashMap<String, ArrayList<Object[]>> data_list = new HashMap<>();
            final HashMap<String, ArrayList<Object[]>> item_data_list = new HashMap<>();

            for (int list_c = 0; list_c < 2; ++list_c) {
               List<Object[]> scan_list = lookup_list;
               if (list_c == 1) {
                  scan_list = item_list;
               }

               for (Object[] result : scan_list) {
                  int user_id = (Integer)result[2];
                  int chunk_x = (Integer)result[3] >> 4;
                  int chunk_z = (Integer)result[5] >> 4;
                  if (chunk_list.get(chunk_x + "." + chunk_z) == null) {
                     int distance = 0;
                     if (location != null) {
                        distance = (int)Math.sqrt(Math.pow((double)((Integer)result[3] - location.getBlockX()), 2.0D) + Math.pow((double)((Integer)result[5] - location.getBlockZ()), 2.0D));
                     }

                     chunk_list.put(chunk_x + "." + chunk_z, distance);
                  }

                  if (Config.player_id_cache_reversed.get(user_id) == null) {
                     Database.loadUserName(statement.getConnection(), user_id);
                  }

                  HashMap<String, ArrayList<Object[]>> modify_list = data_list;
                  if (list_c == 1) {
                     modify_list = item_data_list;
                  }

                  if (modify_list.get(chunk_x + "." + chunk_z) == null) {
                     data_list.put(chunk_x + "." + chunk_z, new ArrayList<>());
                     item_data_list.put(chunk_x + "." + chunk_z, new ArrayList<>());
                  }

                  modify_list.get(chunk_x + "." + chunk_z).add(result);
               }
            }

            if (rollback_type == 1) {
               Iterator<Map.Entry<String, ArrayList<Object[]>>> it = data_list.entrySet().iterator();

               while (it.hasNext()) {
                  Collections.reverse(it.next().getValue());
               }

               it = item_data_list.entrySet().iterator();

               while (it.hasNext()) {
                  Collections.reverse(it.next().getValue());
               }
            }

            int file = 0;
            String user_string = "#server";
            if (user != null) {
               user_string = user.getName();
               if (verbose && preview == 0) {
                  user.sendMessage(Language.get("found-chunk-s-to-modify", chunk_list.size()));
               }
            }

            Config.rollback_hash.put(user_string, new int[]{0, 0, 0, 0});
            final String final_user_string = user_string;
            // rolled_back used to be written for the whole range before the first
            // chunk was even scheduled, so an abort left the database claiming
            // work that never happened -- and the container path gates on that
            // flag, so those rows could then be neither rolled back nor restored.
            // Chunks are recorded as they finish instead.
            List<String> completed_chunks = new ArrayList<>();
            boolean aborted = false;

            for (Map.Entry<String, Integer> entry : Functions.entriesSortedByValues(chunk_list)) {
               ++file;
               int item_count = 0;
               int block_count = 0;
               int entity_count = 0;
               int[] rollback_hash_data = Config.rollback_hash.get(final_user_string);
               item_count = rollback_hash_data[0];
               block_count = rollback_hash_data[1];
               entity_count = rollback_hash_data[2];
               String chunk_key = entry.getKey();
               String[] chunk_cords = chunk_key.split("\\.");
               final int final_chunk_x = Integer.parseInt(chunk_cords[0]);
               final int final_chunk_z = Integer.parseInt(chunk_cords[1]);
               // Meta is deserialised one chunk ahead rather than for every row
               // in the rollback up front, so only the chunk in flight holds a
               // live object graph. Still off the main thread, which was the
               // point of moving it out of the chunk task.
               ArrayList<Object[]> chunk_rows = data_list.get(chunk_key);
               if (chunk_rows != null) {
                  for (Object[] chunk_row : chunk_rows) {
                     if (chunk_row[11] instanceof byte[]) {
                        chunk_row[11] = deserializeMeta((byte[])chunk_row[11]);
                     }
                  }
               }

               Config.rollback_hash.put(final_user_string, new int[]{item_count, block_count, entity_count, 0});
               CoreProtect.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(CoreProtect.getInstance(), () -> {
                  long chunk_start_ns = System.nanoTime();
                  // Counters live for the whole chunk instead of being read back
                  // out of a synchronizedMap and rewritten into a fresh int[] on
                  // every single row. Nothing else mutates this entry while the
                  // chunk task is running -- the rollback thread only touches it
                  // between chunks, gated on the completion flag.
                  int[] chunk_counts = Config.rollback_hash.get(final_user_string);
                  int item_count1 = chunk_counts[0];
                  int block_count1 = chunk_counts[1];
                  int entity_count1 = chunk_counts[2];

                  try {
                     boolean clearInventories = (Integer) Config.config.get("rollback-items") == 1;

                      ArrayList<Object[]> data = data_list.get(final_chunk_x + "." + final_chunk_z);
                     ArrayList<Object[]> item_data = item_data_list.get(final_chunk_x + "." + final_chunk_z);
                     Map<String, Integer> hanging_delay = new HashMap<>();

                     for (Object[] row : data) {
                        int unixtimestamp = (int)(System.currentTimeMillis() / 1000L);
                        int row_time = (Integer)row[1];
                        int row_userid = (Integer)row[2];
                        int row_x = (Integer)row[3];
                        int row_y = (Integer)row[4];
                        int row_z = (Integer)row[5];
                        int row_type_raw = (Integer)row[6];
                        int row_data = (Integer)row[7];
                        int row_action = (Integer)row[8];
                        int row_rolled_back = (Integer)row[9];
                        int row_wid = (Integer)row[10];
                        // Already deserialised on the rollback thread.
                        List<Object> meta = (List)row[11];
                        Material row_type = Functions.getType(row_type_raw);
                        String row_user = Config.player_id_cache_reversed.get(row_userid);
                        int old_type_raw = row_type_raw;
                        Material old_type_material = Functions.getType(row_type_raw);
                        if (row_action == 1 && rollback_type == 0) {
                           row_type = Material.AIR;
                           row_type_raw = 0;
                        } else if (row_action == 0 && rollback_type == 1) {
                           row_type = Material.AIR;
                           row_type_raw = 0;
                        } else if (row_action == 4 && rollback_type == 0) {
                           row_type = null;
                           row_type_raw = 0;
                        } else if (row_action == 3 && rollback_type == 1) {
                           row_type = null;
                           row_type_raw = 0;
                        }

                        if (preview > 0) {
                           if (row_action != 3) {
                              Player player = (Player)user;
                              String world = Functions.getWorldName(row_wid);
                              if (world.isEmpty()) {
                                 continue;
                              }

                              Location location1 = new Location(CoreProtect.getInstance().getServer().getWorld(world), (double)row_x, (double)row_y, (double)row_z);
                              if (preview == 2) {
                                 Block block = location1.getBlock();
                                 Material block_type = block.getType();
                                 byte block_data = Functions.getData(block);
                                 if (!block_type.equals(Material.PAINTING) && !block_type.equals(Material.ITEM_FRAME) && !block_type.equals(Material.ARMOR_STAND)) {
                                    Functions.sendBlockChange(player, location1, block_type, block_data);
                                    ++block_count1;
                                 }
                              } else if (!row_type.equals(Material.PAINTING) && !row_type.equals(Material.ITEM_FRAME) && !row_type.equals(Material.ARMOR_STAND)) {
                                 Functions.sendBlockChange(player, location1, row_type, (byte)row_data);
                                 ++block_count1;
                              }
                           }
                        } else if (row_action == 3) {
                           String world = Functions.getWorldName(row_wid);
                           if (world.isEmpty()) {
                              continue;
                           }

                           Block block = CoreProtect.getInstance().getServer().getWorld(world).getBlockAt(row_x, row_y, row_z);
                           if (!CoreProtect.getInstance().getServer().getWorld(world).isChunkLoaded(block.getChunk())) {
                              CoreProtect.getInstance().getServer().getWorld(world).loadChunk(block.getChunk());
                           }

                           if (row_type_raw > 0) {
                              if (row_rolled_back == 0) {
                                 EntityType entity_type = Functions.getEntityType(row_type_raw);
                                 Lookup.queueEntitySpawn(row_user, block.getState(), entity_type, row_data);
                                 ++entity_count1;
                              }
                           } else if (old_type_raw > 0 && row_rolled_back == 1) {
                              boolean removed = false;
                              int entity_id = -1;
                              String entity_name = Functions.getEntityType(old_type_raw).name();
                              String token = "" + row_x + "." + row_y + "." + row_z + "." + row_wid + "." + entity_name + "";
                              Object[] cached_entity = Config.entity_cache.get(token);
                              if (cached_entity != null) {
                                 entity_id = (Integer)cached_entity[1];
                              }

                              int xmin = row_x - 5;
                              int xmax = row_x + 5;
                              int ymin = row_y - 1;
                              int ymax = row_y + 1;
                              int zmin = row_z - 5;
                              int zmax = row_z + 5;

                              for (Entity e : block.getChunk().getEntities()) {
                                 if (entity_id > -1) {
                                    int id = e.getEntityId();
                                    if (id == entity_id) {
                                       ++entity_count1;
                                       removed = true;
                                       e.remove();
                                       break;
                                    }
                                 } else if (e.getType().equals(Functions.getEntityType(old_type_raw))) {
                                    Location el = e.getLocation();
                                    int e_x = el.getBlockX();
                                    int e_y = el.getBlockY();
                                    int e_z = el.getBlockZ();
                                    if (e_x >= xmin && e_x <= xmax && e_y >= ymin && e_y <= ymax && e_z >= zmin && e_z <= zmax) {
                                       ++entity_count1;
                                       removed = true;
                                       e.remove();
                                       break;
                                    }
                                 }
                              }

                              if (!removed && entity_id > -1) {
                                 for (Entity e : block.getWorld().getLivingEntities()) {
                                    int id = e.getEntityId();
                                    if (id == entity_id) {
                                       ++entity_count1;
                                       removed = true;
                                       e.remove();
                                       break;
                                    }
                                 }
                              }
                           }
                        } else {
                           List<Material> update_state = Lookup.UPDATE_STATE;
                           String world = Functions.getWorldName(row_wid);
                           if (world.isEmpty()) {
                              continue;
                           }

                           Block block = CoreProtect.getInstance().getServer().getWorld(world).getBlockAt(row_x, row_y, row_z);
                           if (!CoreProtect.getInstance().getServer().getWorld(world).isChunkLoaded(block.getChunk())) {
                              CoreProtect.getInstance().getServer().getWorld(world).loadChunk(block.getChunk());
                           }

                           boolean change_block = true;
                           boolean count_block = true;
                           Material ctype = block.getType();
                           int cdata = Functions.getData(block);
                           if (row_rolled_back == 1 && rollback_type == 0) {
                              count_block = false;
                           }

                           if (row_type.equals(ctype) && !old_type_material.equals(Material.PAINTING) && !old_type_material.equals(Material.ITEM_FRAME) && !old_type_material.equals(Material.ARMOR_STAND) && !BukkitAdapter.ADAPTER.isEndCrystal(old_type_material)) {
                              if (row_data == cdata) {
                                 change_block = false;
                              }

                              count_block = false;
                           } else if (!ctype.equals(Material.AIR)) {
                              count_block = true;
                           }

                           if (count_block) {
                              List<Material> c1 = Arrays.asList(Material.GRASS, Material.WATER, Material.LAVA);
                              List<Material> c2 = Arrays.asList(Material.DIRT, Material.STATIONARY_WATER, Material.STATIONARY_LAVA);
                              int c = 0;

                              for (Material cv1 : c1) {
                                 Material cv2 = (Material)c2.get(c);
                                 if (row_type.equals(cv1) && ctype.equals(cv2) || row_type.equals(cv2) && ctype.equals(cv1)) {
                                    count_block = false;
                                 }

                                 ++c;
                              }
                           }

                           try {
                              if (change_block) {
                                 if (!row_type.equals(Material.AIR) || !old_type_material.equals(Material.PAINTING) && !old_type_material.equals(Material.ITEM_FRAME)) {
                                    if (row_type.equals(Material.DOUBLE_PLANT) || row_type.equals(Material.AIR) && old_type_material.equals(Material.DOUBLE_PLANT)) {
                                       if (row_data < 8) {
                                          int top_data = 8;
                                          if (row_data == 0 || row_data == 4) {
                                             top_data = 9;
                                          }

                                          Block block_above = CoreProtect.getInstance().getServer().getWorld(world).getBlockAt(row_x, row_y + 1, row_z);
                                          Functions.setTypeAndData(block, row_type, (byte)row_data, false);
                                          Functions.setTypeAndData(block_above, row_type, (byte)top_data, false);
                                       }
                                    } else if (!row_type.equals(Material.PAINTING) && !row_type.equals(Material.ITEM_FRAME)) {
                                       if (row_type.equals(Material.ARMOR_STAND)) {
                                          Location location1 = block.getLocation();
                                          location1.setX(location1.getX() + 0.5D);
                                          location1.setZ(location1.getZ() + 0.5D);
                                          location1.setYaw((float)row_data);
                                          boolean exists = false;

                                          for (Entity entity : block.getChunk().getEntities()) {
                                             if (entity instanceof ArmorStand && entity.getLocation().getBlockX() == location1.getBlockX() && entity.getLocation().getBlockY() == location1.getBlockY() && entity.getLocation().getBlockZ() == location1.getBlockZ()) {
                                                exists = true;
                                             }
                                          }

                                          if (!exists) {
                                             Entity entity = block.getLocation().getWorld().spawnEntity(location1, EntityType.ARMOR_STAND);
                                             entity.teleport(location1);
                                          }
                                       } else if (BukkitAdapter.ADAPTER.isEndCrystal(row_type)) {
                                          Location location1 = block.getLocation();
                                          location1.setX(location1.getX() + 0.5D);
                                          location1.setZ(location1.getZ() + 0.5D);
                                          boolean exists = false;

                                          for (Entity entity : block.getChunk().getEntities()) {
                                             if (entity instanceof EnderCrystal && entity.getLocation().getBlockX() == location1.getBlockX() && entity.getLocation().getBlockY() == location1.getBlockY() && entity.getLocation().getBlockZ() == location1.getBlockZ()) {
                                                exists = true;
                                             }
                                          }

                                          if (!exists) {
                                             BukkitAdapter.ADAPTER.spawnEndCrystal(location1, block, row_data);
                                          }
                                       } else if (row_type.equals(Material.AIR) && BukkitAdapter.ADAPTER.isEndCrystal(old_type_material)) {
                                          for (Entity entity : block.getChunk().getEntities()) {
                                             if (entity instanceof EnderCrystal && entity.getLocation().getBlockX() == row_x && entity.getLocation().getBlockY() == row_y && entity.getLocation().getBlockZ() == row_z) {
                                                entity.remove();
                                             }
                                          }
                                       } else if (rollback_type != 0 || row_action != 0 || !row_type.equals(Material.AIR)) {
                                          if (!row_type.equals(Material.AIR) && !row_type.equals(Material.TNT)) {
                                             if (row_type.equals(Material.MOB_SPAWNER)) {
                                                try {
                                                   Functions.setTypeAndData(block, row_type, (byte)0, false);
                                                   CreatureSpawner mobSpawner = (CreatureSpawner)block.getState();
                                                   mobSpawner.setSpawnedType(Functions.getSpawnerType(row_data));
                                                   if (count_block) {
                                                      ++block_count1;
                                                   }
                                                } catch (Exception ignored) {
                                                }
                                             } else if (row_type.equals(Material.SKULL)) {
                                                block.setType(row_type, false);
                                                Lookup.queueSkullUpdate(row_user, block.getState(), row_data);
                                                if (count_block) {
                                                   ++block_count1;
                                                }
                                             } else if (!row_type.equals(Material.SIGN_POST) && !row_type.equals(Material.WALL_SIGN)) {
                                                if (BlockInfo.shulker_boxes.contains(row_type)) {
                                                   Functions.setTypeAndData(block, row_type, (byte)row_data, false);
                                                   if (count_block) {
                                                      ++block_count1;
                                                   }

                                                   if (meta != null) {
                                                      Inventory inventory = Functions.getContainerInventory(block.getState(), false);

                                                      for (Object value : meta) {
                                                         if (value instanceof Map) {
                                                            Map<Integer, Object> itemMap = (Map)value;
                                                            ItemStack item = ItemStack.deserialize((Map)itemMap.get(0));
                                                            List<List<Map<String, Object>>> metadata = (List)itemMap.get(1);
                                                            Object[] populatedStack = Lookup.populateItemStack(item, metadata);
                                                            item = (ItemStack)populatedStack[1];
                                                            Lookup.modifyContainerItems(item.getType(), inventory, 0, item, 1);
                                                         }
                                                      }
                                                   }
                                                } else if (row_type.equals(Material.COMMAND)) {
                                                   Functions.setTypeAndData(block, row_type, (byte)row_data, false);
                                                   if (count_block) {
                                                      ++block_count1;
                                                   }

                                                   if (meta != null) {
                                                      CommandBlock command_block = (CommandBlock)block.getState();

                                                      for (Object value : meta) {
                                                         if (value instanceof String) {
                                                            String string = (String)value;
                                                            command_block.setCommand(string);
                                                            command_block.update();
                                                         }
                                                      }
                                                   }
                                                } else if (!row_type.equals(Material.WALL_BANNER) && !row_type.equals(Material.STANDING_BANNER)) {
                                                   if (update_state.contains(row_type)) {
                                                      Functions.setTypeAndData(block, row_type, (byte)row_data, true);
                                                      if (count_block) {
                                                         ++block_count1;
                                                      }
                                                   } else if (row_type != ctype && BlockInfo.containers.contains(row_type) && BlockInfo.containers.contains(ctype)) {
                                                      block.setType(Material.AIR);
                                                      Functions.setTypeAndData(block, row_type, (byte)row_data, false);
                                                      if (count_block) {
                                                         ++block_count1;
                                                      }
                                                   } else {
                                                      if (BlockInfo.containers.contains(row_type)) {
                                                         block.setType(row_type);
                                                         Functions.setData(block, (byte)row_data);
                                                      } else {
                                                         Functions.setTypeAndData(block, row_type, (byte)row_data, false);
                                                      }

                                                      if (count_block) {
                                                         ++block_count1;
                                                      }
                                                   }
                                                } else {
                                                   Functions.setTypeAndData(block, row_type, (byte)row_data, false);
                                                   if (count_block) {
                                                      ++block_count1;
                                                   }

                                                   if (meta != null) {
                                                      Banner banner = (Banner)block.getState();

                                                      for (Object value : meta) {
                                                         if (value instanceof DyeColor) {
                                                            banner.setBaseColor((DyeColor)value);
                                                         } else if (value instanceof Map) {
                                                            Pattern pattern = new Pattern((Map)value);
                                                            banner.addPattern(pattern);
                                                         }
                                                      }

                                                      banner.update();
                                                   }
                                                }
                                             } else {
                                                Functions.setTypeAndData(block, row_type, (byte)row_data, false);
                                                Lookup.queueSignUpdate(row_user, block.getState(), rollback_type, row_time);
                                                if (count_block) {
                                                   ++block_count1;
                                                }
                                             }
                                          } else {
                                             if (clearInventories) {
                                                if (BlockInfo.containers.contains(ctype)) {
                                                   Inventory inventory = Functions.getContainerInventory(block.getState(), false);
                                                   if (inventory != null) {
                                                      inventory.clear();
                                                   }
                                                } else if (BlockInfo.containers.contains(Material.ARMOR_STAND) && old_type_material.equals(Material.ARMOR_STAND)) {
                                                   for (Entity entity : block.getChunk().getEntities()) {
                                                      if (entity instanceof ArmorStand && entity.getLocation().getBlockX() == row_x && entity.getLocation().getBlockY() == row_y && entity.getLocation().getBlockZ() == row_z) {
                                                         EntityEquipment equipment = Functions.getEntityEquipment((LivingEntity)entity);
                                                         if (equipment != null) {
                                                            equipment.clear();
                                                         }

                                                         Location location1 = entity.getLocation();
                                                         location1.setY(location1.getY() - 1.0D);
                                                         entity.teleport(location1);
                                                         entity.remove();
                                                      }
                                                   }
                                                }
                                             }

                                             Functions.setTypeAndData(block, row_type, (byte)row_data, false);
                                             if (count_block) {
                                                ++block_count1;
                                             }
                                          }
                                       }
                                    } else {
                                       int delay = Functions.getHangingDelay(hanging_delay, row_wid, row_x, row_y, row_z);
                                       Lookup.queueHangingSpawn(row_user, block.getState(), row_type, row_data, delay);
                                    }
                                 } else {
                                    int delay = Functions.getHangingDelay(hanging_delay, row_wid, row_x, row_y, row_z);
                                    Lookup.queueHangingRemove(row_user, block.getState(), delay);
                                 }
                              }
                           } catch (Exception e) {
                              e.printStackTrace();
                           }

                           if (!row_type.equals(Material.AIR) && change_block && !row_user.isEmpty()) {
                              Config.lookup_cache.put("" + row_x + "." + row_y + "." + row_z + "." + row_wid + "", new Object[]{unixtimestamp, row_user, row_type});
                           }
                        }
                     }

                     hanging_delay.clear();
                     Object container = null;
                     Material container_type = null;
                     boolean container_init = false;
                     int last_x = 0;
                     int last_y = 0;
                     int last_z = 0;
                     int last_wid = 0;

                     for (Object[] row : item_data) {
                        int row_x = (Integer)row[3];
                        int row_y = (Integer)row[4];
                        int row_z = (Integer)row[5];
                        int row_type_raw = (Integer)row[6];
                        int row_data = (Integer)row[7];
                        int row_action = (Integer)row[8];
                        int row_rolled_back = (Integer)row[9];
                        int row_wid = (Integer)row[10];
                        int row_amount = (Integer)row[11];
                        byte[] row_metadata = (byte[])row[12];
                        Material row_type = Functions.getType(row_type_raw);
                        if (rollback_type == 0 && row_rolled_back == 0 || rollback_type == 1 && row_rolled_back == 1) {
                           if (!container_init || row_x != last_x || row_y != last_y || row_z != last_z || row_wid != last_wid) {
                              container = null;
                              String world = Functions.getWorldName(row_wid);
                              Block block = CoreProtect.getInstance().getServer().getWorld(world).getBlockAt(row_x, row_y, row_z);
                              if (!CoreProtect.getInstance().getServer().getWorld(world).isChunkLoaded(block.getChunk())) {
                                 CoreProtect.getInstance().getServer().getWorld(world).loadChunk(block.getChunk());
                              }

                              if (BlockInfo.containers.contains(block.getType())) {
                                 container = Functions.getContainerInventory(block.getState(), false);
                                 container_type = block.getType();
                              } else if (BlockInfo.containers.contains(Material.ARMOR_STAND)) {
                                 for (Entity entity : block.getChunk().getEntities()) {
                                    if (entity instanceof ArmorStand && entity.getLocation().getBlockX() == row_x && entity.getLocation().getBlockY() == row_y && entity.getLocation().getBlockZ() == row_z) {
                                       container = Functions.getEntityEquipment((LivingEntity)entity);
                                       container_type = Material.ARMOR_STAND;
                                    }
                                 }
                              }

                              last_x = row_x;
                              last_y = row_y;
                              last_z = row_z;
                              last_wid = row_wid;
                           }

                           if (container != null) {
                              int action = 0;
                              if (rollback_type == 0 && row_action == 0) {
                                 action = 1;
                              }

                              if (rollback_type == 1 && row_action == 1) {
                                 action = 1;
                              }

                              ItemStack itemstack = new ItemStack(row_type, row_amount, (short)row_data);
                              Object[] populatedStack = Lookup.populateItemStack(itemstack, row_metadata);
                              int slot = (Integer)populatedStack[0];
                              itemstack = (ItemStack)populatedStack[1];
                              Lookup.modifyContainerItems(container_type, container, slot, itemstack, action);
                              item_count1 += row_amount;
                           }

                           container_init = true;
                        }
                     }

                     Config.rollback_hash.put(final_user_string, new int[]{item_count1, block_count1, entity_count1, 1});
                     if (user instanceof Player && preview == 0) {
                        Player player = (Player)user;
                        Location location1 = player.getLocation();
                        Chunk chunk = location1.getChunk();
                        if (chunk.getX() == final_chunk_x && chunk.getZ() == final_chunk_z) {
                           List<Material> unsafe_blocks = Lookup.UNSAFE_BLOCKS;
                           int player_x = location1.getBlockX();
                           int player_y = location1.getBlockY();
                           int player_z = location1.getBlockZ();
                           int check_y = player_y - 1;
                           boolean safe_block = false;

                           for (boolean place_safe = false; !safe_block; ++check_y) {
                              int above = check_y + 1;
                              if (above > 256) {
                                 above = 256;
                              }

                              Block block_type1 = location1.getWorld().getBlockAt(player_x, check_y, player_z);
                              Block block_type2 = location1.getWorld().getBlockAt(player_x, above, player_z);
                              Material type1 = block_type1.getType();
                              Material type2 = block_type2.getType();
                              if (!Functions.solidBlock(type1) && !Functions.solidBlock(type2)) {
                                 if (unsafe_blocks.contains(type1)) {
                                    place_safe = true;
                                 } else {
                                    safe_block = true;
                                    if (place_safe) {
                                       int below = check_y - 1;
                                       Block block_below = location1.getWorld().getBlockAt(player_x, below, player_z);
                                       if (unsafe_blocks.contains(block_below.getType())) {
                                          block_type1.setType(Material.DIRT);
                                          ++check_y;
                                       }
                                    }
                                 }
                              }

                              if (check_y >= 256) {
                                 safe_block = true;
                              }

                              if (safe_block && check_y > player_y) {
                                 if (check_y > 256) {
                                    check_y = 256;
                                 }

                                 location1.setY((double)check_y);
                                 player.teleport(location1);
                                 player.sendMessage(Language.get("teleported-you-to-safety"));
                                 if (place_safe) {
                                    player.sendMessage(Language.get("placed-a-dirt-block-under-you"));
                                 }
                              }
                           }
                        }
                     }
                  } catch (Exception e) {
                     e.printStackTrace();
                     Config.rollback_hash.put(final_user_string, new int[]{item_count1, block_count1, entity_count1, 2});
                  } finally {
                     // This whole body runs on the main thread inside a single
                     // tick, so anything past one tick (50ms) is stall the server
                     // has to absorb. Report it, otherwise a watchdog kill leaves
                     // nothing behind to say which chunk was responsible.
                     long chunk_ms = (System.nanoTime() - chunk_start_ns) / 1000000L;
                     if (chunk_ms >= 50L) {
                        ArrayList<Object[]> d = data_list.get(final_chunk_x + "." + final_chunk_z);
                        ArrayList<Object[]> i = item_data_list.get(final_chunk_x + "." + final_chunk_z);
                        System.out.println("[CoreProtect] Slow rollback chunk " + final_chunk_x + "," + final_chunk_z + ": " + chunk_ms + "ms for " + (d == null ? 0 : d.size()) + " block row(s), " + (i == null ? 0 : i.size()) + " container row(s).");
                     }
                  }

               }, 0L);
               rollback_hash_data = Config.rollback_hash.get(final_user_string);
               int next = rollback_hash_data[3];
               int sleep_time = 0;
               int abort = 0;

               while (next == 0) {
                  if (preview == 1) {
                     ++sleep_time;
                     Thread.sleep(1L);
                  } else {
                     sleep_time += 5;
                     Thread.sleep(5L);
                  }

                  rollback_hash_data = Config.rollback_hash.get(final_user_string);
                  next = rollback_hash_data[3];
                  if (sleep_time > 300000) {
                     abort = 1;
                     break;
                  }
               }

               if (abort == 1 || next == 2) {
                  System.out.println("[CoreProtect] Rollback or restore aborted at chunk " + chunk_key + " (" + file + " of " + chunk_list.size() + "). Chunks already completed have been recorded; re-run the command to finish the rest.");
                  aborted = true;
                  break;
               }

               completed_chunks.add(chunk_key);
               // The chunk is done with its metadata; drop it so a large rollback
               // does not accumulate every deserialised blob it has ever touched.
               if (chunk_rows != null) {
                  for (Object[] chunk_row : chunk_rows) {
                     chunk_row[11] = null;
                  }
               }

               rollback_hash_data = Config.rollback_hash.get(final_user_string);
               item_count = rollback_hash_data[0];
               block_count = rollback_hash_data[1];
               entity_count = rollback_hash_data[2];
               Config.rollback_hash.put(final_user_string, new int[]{item_count, block_count, entity_count, 0});
               if (verbose && user != null && preview == 0) {
                  user.sendMessage(Language.get("modified-chunk-s-2", file, chunk_list.size()));
               }
            }

            if (preview == 0) {
               List<Object[]> applied_blocks = new ArrayList<>();
               List<Object[]> applied_items = new ArrayList<>();

               for (String completed : completed_chunks) {
                  ArrayList<Object[]> completed_blocks = data_list.get(completed);
                  if (completed_blocks != null) {
                     applied_blocks.addAll(completed_blocks);
                  }

                  ArrayList<Object[]> completed_items = item_data_list.get(completed);
                  if (completed_items != null) {
                     applied_items.addAll(completed_items);
                  }
               }

               Queue.queueRollbackUpdate(user_string, location, applied_blocks, rollback_type);
               Queue.queueContainerRollbackUpdate(user_string, location, applied_items, rollback_type);
            }

            int[] rollback_hash_data = Config.rollback_hash.get(final_user_string);
            int item_count = rollback_hash_data[0];
            int block_count = rollback_hash_data[1];
            int entity_count = rollback_hash_data[2];
            long time2 = System.currentTimeMillis();
            int seconds = (int)((time2 - time1) / 1000L);
            if (user != null) {
               finishRollbackRestore(user, location, check_users, restrict_list, exclude_list, exclude_user_list, action_list, time_string, file, seconds, item_count, block_count, entity_count, rollback_type, radius, verbose, restrict_world, preview);
               // An abort used to fall through to the completion banner with no
               // sign anything was wrong, so a rollback that stopped a tenth of
               // the way in still read as finished.
               if (aborted) {
                  user.sendMessage(Language.get("rollback-restore-aborted", file, chunk_list.size()));
               }
            }

             return convertRawLookup(statement, lookup_list);
         }
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   public static boolean playerExists(Connection connection, String user) {
      try {
         int id = -1;
         String uuid = null;
         if (Config.player_id_cache.get(user.toLowerCase()) != null) {
            return true;
         }

         String query = "SELECT rowid as id, uuid FROM " + Config.prefix + "user WHERE user LIKE ? LIMIT 0, 1";
         PreparedStatement preparedStmt = connection.prepareStatement(query);
         preparedStmt.setString(1, user);

         ResultSet rs;
         for (rs = preparedStmt.executeQuery(); rs.next(); uuid = rs.getString("uuid")) {
            id = rs.getInt("id");
         }

         rs.close();
         preparedStmt.close();
         if (id > -1) {
            if (uuid != null) {
               Config.uuid_cache.put(user.toLowerCase(), uuid);
               Config.uuid_cache_reversed.put(uuid, user);
            }

            Config.player_id_cache.put(user.toLowerCase(), id);
            Config.player_id_cache_reversed.put(id, user);
            return true;
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return false;
   }

   public static Object[] populateItemStack(ItemStack itemstack, List<List<Map<String, Object>>> list) {
      int slot = 0;

      try {
         Material row_type = itemstack.getType();
         int item_count = 0;
         FireworkEffect.Builder effect_builder = FireworkEffect.builder();

         for (List<Map<String, Object>> map : list) {
            Map<String, Object> mapData = (Map)map.get(0);
            if (mapData.get("slot") != null) {
               slot = (Integer)mapData.get("slot");
            } else if (item_count == 0) {
               ItemMeta meta = Functions.deserializeItemMeta(itemstack.getItemMeta().getClass(), (Map)map.get(0));
               itemstack.setItemMeta(meta);
               BukkitAdapter.ADAPTER.setPotionMeta(row_type, map, itemstack);
            } else if (!row_type.equals(Material.LEATHER_HELMET) && !row_type.equals(Material.LEATHER_CHESTPLATE) && !row_type.equals(Material.LEATHER_LEGGINGS) && !row_type.equals(Material.LEATHER_BOOTS)) {
               if (row_type.equals(Material.POTION)) {
                  for (Map<String, Object> l : map) {
                     PotionMeta meta = (PotionMeta)itemstack.getItemMeta();
                     PotionEffect effect = new PotionEffect(l);
                     meta.addCustomEffect(effect, true);
                     itemstack.setItemMeta(meta);
                  }
               } else if (row_type.equals(Material.BANNER)) {
                  for (Map<String, Object> l : map) {
                     BannerMeta meta = (BannerMeta)itemstack.getItemMeta();
                     Pattern pattern = new Pattern(l);
                     meta.addPattern(pattern);
                     itemstack.setItemMeta(meta);
                  }
               } else if (!row_type.equals(Material.FIREWORK) && !row_type.equals(Material.FIREWORK_CHARGE)) {
                  BukkitAdapter.ADAPTER.setItemMeta(row_type, map, itemstack, item_count);
               } else if (item_count == 1) {
                  for (Map<String, Object> l : map) {
                     boolean hasFlicker = (Boolean)l.get("flicker");
                     boolean hasTrail = (Boolean)l.get("trail");
                     effect_builder.flicker(hasFlicker);
                     effect_builder.trail(hasTrail);
                  }
               } else if (item_count == 2) {
                  for (Map<String, Object> l : map) {
                     Color color = Color.deserialize(l);
                     effect_builder.withColor(color);
                  }
               } else if (item_count == 3) {
                  for (Map<String, Object> l : map) {
                     Color color = Color.deserialize(l);
                     effect_builder.withFade(color);
                  }

                  FireworkEffect effect = effect_builder.build();
                  if (row_type.equals(Material.FIREWORK)) {
                     FireworkMeta meta = (FireworkMeta)itemstack.getItemMeta();
                     meta.addEffect(effect);
                     itemstack.setItemMeta(meta);
                  } else if (row_type.equals(Material.FIREWORK_CHARGE)) {
                     FireworkEffectMeta meta = (FireworkEffectMeta)itemstack.getItemMeta();
                     meta.setEffect(effect);
                     itemstack.setItemMeta(meta);
                  }

                  effect_builder = FireworkEffect.builder();
                  item_count = 0;
               }
            } else {
               for (Map<String, Object> l : map) {
                  LeatherArmorMeta meta = (LeatherArmorMeta)itemstack.getItemMeta();
                  Color color = Color.deserialize(l);
                  meta.setColor(color);
                  itemstack.setItemMeta(meta);
               }
            }

            ++item_count;
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return new Object[]{slot, itemstack};
   }

   private static Object[] populateItemStack(ItemStack itemstack, byte[] metadata) {
      // Items with no metadata now store NULL rather than a serialised empty
      // list, so this is the ordinary case rather than an error.
      if (metadata == null || metadata.length == 0) {
         return new Object[]{0, itemstack};
      }

      try {
         ByteArrayInputStream bais = new ByteArrayInputStream(metadata);
         ObjectInputStream ins = new ObjectInputStream(bais);
         List<List<Map<String, Object>>> list = (List)ins.readObject();
         return populateItemStack(itemstack, list);
      } catch (Exception e) {
         e.printStackTrace();
         return new Object[]{0, itemstack};
      }
   }

   // Throws rather than returning null on failure. Swallowing here meant the
   // caller could not tell a broken query from an empty result, which is how a
   // failed rollback ended up reporting success.
   private static ResultSet rawLookupResultSet(Statement statement, CommandSender user, List<String> check_uuids, List<String> check_users, List<Object> restrict_list, List<Object> exclude_list, List<String> exclude_user_list, List<Integer> action_list, Location location, Integer[] radius, int check_time, int limit_offset, int limit_count, boolean restrict_world, boolean lookup, boolean count) throws Exception {
      ResultSet rs;

      {
         List<Integer> valid_actions = Arrays.asList(0, 1, 2, 3);
         if (radius != null) {
            restrict_world = true;
         }

         boolean valid_action = false;
         String query_extra = "";
         String query_limit = "";
         String query_table = "block";
         String action = "";
         String exclude = "";
         String restrict = "";
         String users = "";
         String uuids = "";
         String exclude_users = "";
         String index = "";
         if (!check_uuids.isEmpty()) {
            StringBuilder list = new StringBuilder();

            for (String value : check_uuids) {
               if (list.length() == 0) {
                  list = new StringBuilder("'" + value + "'");
               } else {
                  list.append(",'").append(value).append("'");
               }
            }

            uuids = list.toString();
         }

         if (!check_users.contains("#global")) {
            StringBuilder list = new StringBuilder();

            for (String value : check_users) {
               if (!value.equals("#container")) {
                  if (Config.player_id_cache.get(value.toLowerCase()) == null) {
                     Database.loadUserID(statement.getConnection(), value, null);
                  }

                  int userid = Config.player_id_cache.get(value.toLowerCase());
                  if (list.length() == 0) {
                     list = new StringBuilder("" + userid + "");
                  } else {
                     list.append(",").append(userid);
                  }
               }
            }

            users = list.toString();
         }

         if (!restrict_list.isEmpty()) {
            StringBuilder list = new StringBuilder();

            for (Object value : restrict_list) {
               String value_name = "";
               if (value instanceof Material) {
                  value_name = ((Material)value).name();
                  if (list.length() == 0) {
                     list = new StringBuilder("" + Functions.block_id(value_name, false) + "");
                  } else {
                     list.append(",").append(Functions.block_id(value_name, false));
                  }
               } else if (value instanceof EntityType) {
                  value_name = ((EntityType)value).name();
                  if (list.length() == 0) {
                     list = new StringBuilder("" + Functions.getEntityId(value_name, false) + "");
                  } else {
                     list.append(",").append(Functions.getEntityId(value_name, false));
                  }
               }
            }

            restrict = list.toString();
         }

         if (!exclude_list.isEmpty()) {
            StringBuilder list = new StringBuilder();

            for (Object value : exclude_list) {
               String value_name = "";
               if (value instanceof Material) {
                  value_name = ((Material)value).name();
                  if (list.length() == 0) {
                     list = new StringBuilder("" + Functions.block_id(value_name, false) + "");
                  } else {
                     list.append(",").append(Functions.block_id(value_name, false));
                  }
               } else if (value instanceof EntityType) {
                  value_name = ((EntityType)value).name();
                  if (list.length() == 0) {
                     list = new StringBuilder("" + Functions.getEntityId(value_name, false) + "");
                  } else {
                     list.append(",").append(Functions.getEntityId(value_name, false));
                  }
               }
            }

            exclude = list.toString();
         }

         if (!exclude_user_list.isEmpty()) {
            StringBuilder list = new StringBuilder();

            for (String value : exclude_user_list) {
               if (Config.player_id_cache.get(value.toLowerCase()) == null) {
                  Database.loadUserID(statement.getConnection(), value, null);
               }

               int userid = Config.player_id_cache.get(value.toLowerCase());
               if (list.length() == 0) {
                  list = new StringBuilder("" + userid + "");
               } else {
                  list.append(",").append(userid);
               }
            }

            exclude_users = list.toString();
         }

         if (!action_list.isEmpty()) {
            StringBuilder list = new StringBuilder();

            for (Integer value : action_list) {
               if (valid_actions.contains(value)) {
                  if (list.length() == 0) {
                     list = new StringBuilder("" + value + "");
                  } else {
                     list.append(",").append(value);
                  }
               }
            }

            action = list.toString();
         }

         for (Integer value : action_list) {
            if (valid_actions.contains(value)) {
               valid_action = true;
            }
         }

         if (restrict_world) {
            int wid = Functions.getWorldId(location.getWorld().getName());
            query_extra = query_extra + " wid=" + wid + " AND";
         }

         if (radius != null) {
            int xmin = radius[1];
            int xmax = radius[2];
            int ymin = radius[3];
            int ymax = radius[4];
            int zmin = radius[5];
            int zmax = radius[6];
            String query_y = "";
            if (ymin > -1 && ymax > -1) {
               query_y = " y >= '" + ymin + "' AND y <= '" + ymax + "' AND";
            }

            query_extra = query_extra + " x >= '" + xmin + "' AND x <= '" + xmax + "' AND z >= '" + zmin + "' AND z <= '" + zmax + "' AND" + query_y;
         } else if (action_list.contains(5)) {
            int wid = Functions.getWorldId(location.getWorld().getName());
            int x = (int)Math.floor(location.getX());
            int z = (int)Math.floor(location.getZ());
            int x2 = (int)Math.ceil(location.getX());
            int z2 = (int)Math.ceil(location.getZ());
            query_extra = query_extra + " wid=" + wid + " AND (x = '" + x + "' OR x = '" + x2 + "') AND (z = '" + z + "' OR z = '" + z2 + "') AND y = '" + location.getBlockY() + "' AND";
         }

         if (valid_action) {
            query_extra = query_extra + " action IN(" + action + ") AND";
         }

         if (!restrict.isEmpty()) {
            query_extra = query_extra + " type IN(" + restrict + ") AND";
         }

         if (!exclude.isEmpty()) {
            query_extra = query_extra + " type NOT IN(" + exclude + ") AND";
         }

         if (!uuids.isEmpty()) {
            query_extra = query_extra + " uuid IN(" + uuids + ") AND";
         }

         if (!users.isEmpty()) {
            query_extra = query_extra + " user IN(" + users + ") AND";
         }

         if (!exclude_users.isEmpty()) {
            query_extra = query_extra + " user NOT IN(" + exclude_users + ") AND";
         }

         if (check_time > 0) {
            query_extra = query_extra + " time > '" + check_time + "' AND";
         }

         if (!query_extra.isEmpty()) {
            query_extra = query_extra.substring(0, query_extra.length() - 4);
         }

         if (query_extra.isEmpty()) {
            query_extra = " 1";
         }

         if (limit_offset > -1 && limit_count > -1) {
            query_limit = " LIMIT " + limit_offset + ", " + limit_count + "";
         }

         String rows = "rowid as id,time,user,wid,x,y,z,action,type,data,meta,rolled_back";
         String query_order = " ORDER BY rowid DESC";
         if (lookup) {
            query_order = " ORDER BY time DESC";
         }

         if (!action_list.contains(4) && !action_list.contains(5)) {
            if (!action_list.contains(6) && !action_list.contains(7)) {
               if (action_list.contains(8)) {
                  query_table = "session";
                  rows = "rowid as id,time,user,wid,x,y,z,action";
               } else if (action_list.contains(9)) {
                  query_table = "username_log";
                  rows = "rowid as id,time,uuid,user";
               }
            } else {
               query_table = "chat";
               rows = "rowid as id,time,user,message";
               if (action_list.contains(7)) {
                  query_table = "command";
               }
            }
         } else {
            query_table = "container";
            rows = "rowid as id,time,user,wid,x,y,z,action,type,data,rolled_back,amount,metadata";
         }

         if (count) {
            rows = "COUNT(*) as count";
            query_limit = " LIMIT 0, 1";
            query_order = "";
         }

         if (Config.config.get("use-mysql") == 1) {
            if ((radius == null || !users.isEmpty() || !restrict.isEmpty()) && !users.isEmpty()) {
            }
         } else if (query_table.equals("block")) {
            if (!restrict.isEmpty() || !exclude.isEmpty()) {
               index = "INDEXED BY block_type_index ";
            }

            if (!users.isEmpty() || !exclude_users.isEmpty()) {
               index = "INDEXED BY block_user_index ";
            }

            if (radius != null || action_list.contains(5) || index.isEmpty() && restrict_world) {
               index = "INDEXED BY block_index ";
            }
         }

         String query = "SELECT " + rows + " FROM " + Config.prefix + query_table + " " + index + "WHERE" + query_extra + query_order + query_limit + "";
         rs = statement.executeQuery(query);
      }

      return rs;
   }

   public static String who_placed(Statement statement, BlockState block) {
      String result = "";

      try {
         if (block == null) {
            return result;
         }

         int x = block.getX();
         int y = block.getY();
         int z = block.getZ();
         int time = (int)(System.currentTimeMillis() / 1000L);
         int wid = Functions.getWorldId(block.getWorld().getName());
         String query = "SELECT user,type FROM " + Config.prefix + "block WHERE wid = '" + wid + "' AND x = '" + x + "' AND z = '" + z + "' AND y = '" + y + "' AND rolled_back = '0' AND action='1' ORDER BY rowid DESC LIMIT 0, 1";
         ResultSet rs = statement.executeQuery(query);

         while (rs.next()) {
            int result_userid = rs.getInt("user");
            int result_type = rs.getInt("type");
            if (Config.player_id_cache_reversed.get(result_userid) == null) {
               Database.loadUserName(statement.getConnection(), result_userid);
            }

            result = Config.player_id_cache_reversed.get(result_userid);
            if (!result.isEmpty()) {
               Material result_material = Functions.getType(result_type);
               Config.lookup_cache.put("" + x + "." + y + "." + z + "." + wid + "", new Object[]{time, result, result_material});
            }
         }

         rs.close();
      } catch (Exception e) {
         e.printStackTrace();
      }

      return result;
   }

   public static String who_placed_cache(Block block) {
      String result = "";

      try {
         if (block == null) {
            return result;
         }

         int x = block.getX();
         int y = block.getY();
         int z = block.getZ();
         int wid = Functions.getWorldId(block.getWorld().getName());
         String cords = "" + x + "." + y + "." + z + "." + wid + "";
         Object[] data = Config.lookup_cache.get(cords);
         if (data != null) {
            result = (String)data[1];
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return result;
   }

   public static String who_removed_cache(BlockState block) {
      String result = "";

      try {
         if (block != null) {
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();
            int wid = Functions.getWorldId(block.getWorld().getName());
            String cords = "" + x + "." + y + "." + z + "." + wid + "";
            Object[] data = Config.break_cache.get(cords);
            if (data != null) {
               result = (String)data[1];
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return result;
   }
}
