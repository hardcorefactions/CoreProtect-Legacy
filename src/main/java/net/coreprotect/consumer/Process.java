package net.coreprotect.consumer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.coreprotect.Functions;
import net.coreprotect.database.Database;
import net.coreprotect.database.Logger;
import net.coreprotect.database.Lookup;
import net.coreprotect.model.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public class Process {
   private static Connection connection = null;
   private static int lastConnection = 0;

   private static void validateConnection() {
      try {
         if (connection != null) {
            int timeSinceLastConnection = (int)(System.currentTimeMillis() / 1000L) - lastConnection;
            if (timeSinceLastConnection > 900 || connection.isClosed() || !Config.server_running || Consumer.resetConnection) {
               connection.close();
               connection = null;
               Consumer.resetConnection = false;
            }
         }

         if (connection == null && Config.server_running) {
            connection = Database.getConnection(false);
            lastConnection = (int)(System.currentTimeMillis() / 1000L);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public static void processConsumer(int process_id) {
      label172: {
         try {
            validateConnection();
            if (connection != null) {
               Consumer.is_paused = true;
               Statement statement = connection.createStatement();
               ArrayList<Object[]> consumer_data = (ArrayList)Consumer.consumer.get(process_id);
               Map<Integer, String[]> users = (Map)Consumer.consumer_users.get(process_id);
               Map<Integer, Object> blocks = (Map)Consumer.consumer_object.get(process_id);
               Database.beginTransaction(statement);

               for(Map.Entry<Integer, String[]> entry : users.entrySet()) {
                  String[] user_data = (String[])entry.getValue();
                  String user = user_data[0];
                  String uuid = user_data[1];
                  if (Config.player_id_cache.get(user.toLowerCase()) == null) {
                     Database.loadUserID(connection, user, uuid);
                  }
               }

               Database.commitTransaction(statement);
               PreparedStatement preparedStmt_signs = Database.prepareStatement(connection, 0, false);
               PreparedStatement preparedStmt_blocks = Database.prepareStatement(connection, 1, false);
               PreparedStatement preparedStmt_skulls = Database.prepareStatement(connection, 2, true);
               PreparedStatement preparedStmt_containers = Database.prepareStatement(connection, 3, false);
               PreparedStatement preparedStmt_worlds = Database.prepareStatement(connection, 4, false);
               PreparedStatement preparedStmt_chat = Database.prepareStatement(connection, 5, false);
               PreparedStatement preparedStmt_command = Database.prepareStatement(connection, 6, false);
               PreparedStatement preparedStmt_session = Database.prepareStatement(connection, 7, false);
               PreparedStatement preparedStmt_entities = Database.prepareStatement(connection, 8, true);
               PreparedStatement preparedStmt_materials = Database.prepareStatement(connection, 9, false);
               PreparedStatement preparedStmt_art = Database.prepareStatement(connection, 10, false);
               PreparedStatement preparedStmt_entity = Database.prepareStatement(connection, 11, false);
               Database.beginTransaction(statement);

               for(Object[] data : consumer_data) {
                  if (data != null) {
                     int id = (Integer)data[0];
                     int action = (Integer)data[1];
                     Material block_type = (Material)data[2];
                     int block_data = (Integer)data[3];
                     Material replace_type = (Material)data[4];
                     int replace_data = (Integer)data[5];
                     int force_data = (Integer)data[6];
                     if (users.get(id) != null && blocks.get(id) != null) {
                        String user = ((String[])users.get(id))[0];
                        Object object = blocks.get(id);

                        try {
                           switch (action) {
                              case 0:
                                 processBlockBreak(preparedStmt_blocks, preparedStmt_skulls, process_id, id, block_type, block_data, replace_type, force_data, user, object);
                                 break;
                              case 1:
                                 processBlockPlace(preparedStmt_blocks, preparedStmt_skulls, block_type, block_data, replace_type, replace_data, force_data, user, object);
                                 break;
                              case 2:
                                 processSignText(preparedStmt_signs, process_id, id, force_data, user, object);
                                 break;
                              case 3:
                                 processContainerBreak(preparedStmt_containers, process_id, id, user, object);
                                 break;
                              case 4:
                                 processPlayerInteraction(preparedStmt_blocks, user, object);
                                 break;
                              case 5:
                                 processContainerTransaction(preparedStmt_containers, process_id, id, force_data, user, object);
                                 break;
                              case 6:
                                 processStructureGrowth(statement, preparedStmt_blocks, process_id, id, user, object);
                                 break;
                              case 7:
                                 processRollbackUpdate(statement, process_id, id, force_data, 0);
                                 break;
                              case 8:
                                 processRollbackUpdate(statement, process_id, id, force_data, 1);
                                 break;
                              case 9:
                                 processWorldInsert(preparedStmt_worlds, user, force_data);
                                 break;
                              case 10:
                                 processSignUpdate(statement, object, block_data, force_data);
                                 break;
                              case 11:
                                 processSkullUpdate(statement, object, force_data);
                                 break;
                              case 12:
                                 processPlayerChat(preparedStmt_chat, process_id, id, force_data, user);
                                 break;
                              case 13:
                                 processPlayerCommand(preparedStmt_command, process_id, id, force_data, user);
                                 break;
                              case 14:
                                 processPlayerLogin(connection, preparedStmt_session, process_id, id, object, block_data, replace_data, force_data, user);
                                 break;
                              case 15:
                                 processPlayerLogout(preparedStmt_session, object, force_data, user);
                                 break;
                              case 16:
                                 processEntityKill(preparedStmt_blocks, preparedStmt_entities, process_id, id, object, user);
                                 break;
                              case 17:
                                 processEntitySpawn(statement, object, force_data);
                                 break;
                              case 18:
                                 processHangingRemove(object, force_data);
                                 break;
                              case 19:
                                 processHangingSpawn(object, block_type, block_data, force_data);
                                 break;
                              case 20:
                                 processNaturalBlockBreak(statement, preparedStmt_blocks, process_id, id, user, object, block_type, block_data);
                                 break;
                              case 21:
                                 processMaterialInsert(preparedStmt_materials, user, force_data);
                                 break;
                              case 22:
                                 processMaterialInsert(preparedStmt_art, user, force_data);
                                 break;
                              case 23:
                                 processMaterialInsert(preparedStmt_entity, user, force_data);
                                 break;
                              case 24:
                                 processPlayerKill(preparedStmt_blocks, id, object, user);
                           }
                        } catch (Exception e) {
                           e.printStackTrace();
                        }

                        users.remove(id);
                        blocks.remove(id);
                     }
                  }
               }

               Database.commitTransaction(statement);
               consumer_data.clear();
               preparedStmt_signs.close();
               preparedStmt_blocks.close();
               preparedStmt_skulls.close();
               preparedStmt_containers.close();
               preparedStmt_worlds.close();
               preparedStmt_chat.close();
               preparedStmt_command.close();
               preparedStmt_session.close();
               preparedStmt_entities.close();
               preparedStmt_materials.close();
               preparedStmt_art.close();
               preparedStmt_entity.close();
               statement.close();
               break label172;
            }
         } catch (Exception e) {
            e.printStackTrace();
            break label172;
         } finally {
            validateConnection();
         }

         return;
      }

      Consumer.is_paused = false;
   }

   private static void processBlockBreak(PreparedStatement preparedStmt, PreparedStatement preparedStmt_skulls, int process_id, int id, Material block_type, int block_data, Material replace_type, int force_data, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         List<Object> meta = Functions.processMeta(block);
         if (block instanceof Skull) {
            Logger.log_skull_break(preparedStmt, preparedStmt_skulls, user, block);
         } else {
            Logger.log_break(preparedStmt, user, block.getLocation(), Functions.block_id(block_type), block_data, meta);
            if (force_data == 5 && (block_type.equals(Material.WOODEN_DOOR) || block_type.equals(Material.SPRUCE_DOOR) || block_type.equals(Material.BIRCH_DOOR) || block_type.equals(Material.JUNGLE_DOOR) || block_type.equals(Material.ACACIA_DOOR) || block_type.equals(Material.DARK_OAK_DOOR) || block_type.equals(Material.IRON_DOOR_BLOCK)) && !replace_type.equals(Material.WOODEN_DOOR) && !replace_type.equals(Material.SPRUCE_DOOR) && !replace_type.equals(Material.BIRCH_DOOR) && !replace_type.equals(Material.JUNGLE_DOOR) && !replace_type.equals(Material.ACACIA_DOOR) && !replace_type.equals(Material.DARK_OAK_DOOR) && !replace_type.equals(Material.IRON_DOOR_BLOCK)) {
               int d = block_data;
               if (block_data < 9) {
                  d = block_data + 8;
               }

               Location location = block.getLocation();
               location.setY(location.getY() + (double)1.0F);
               Logger.log_break(preparedStmt, user, location, Functions.block_id(block_type), d, (List)null);
            }
         }
      }

   }

   private static void processBlockPlace(PreparedStatement preparedStmt, PreparedStatement preparedStmt_skulls, Material block_type, int block_data, Material replace_type, int replace_data, int force_data, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         List<Object> meta = Functions.processMeta(block);
         if (block_type.equals(Material.SKULL)) {
            Logger.log_skull_place(preparedStmt, preparedStmt_skulls, user, block, Functions.block_id(replace_type), replace_data);
         } else Logger.log_place(preparedStmt, user, block, Functions.block_id(replace_type), replace_data, block_type, block_data, force_data == 1, meta);
      }

   }

   private static void processContainerBreak(PreparedStatement preparedStmt, int process_id, int id, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Map<Integer, ItemStack[]> containers = (Map)Consumer.consumer_containers.get(process_id);
         if (containers.get(id) != null) {
            ItemStack[] container = (ItemStack[])containers.get(id);
            Logger.log_container_break(preparedStmt, user, block.getLocation(), block.getType(), container);
            containers.remove(id);
         }
      }

   }

   private static void processContainerTransaction(PreparedStatement preparedStmt, int process_id, int id, int force_data, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Map<Integer, Object> inventories = (Map)Consumer.consumer_inventories.get(process_id);
         if (inventories.get(id) != null) {
            Object inventory = inventories.get(id);
            String logging_chest_id = user.toLowerCase() + "." + block.getX() + "." + block.getY() + "." + block.getZ();
            if (Config.logging_chest.get(logging_chest_id) != null) {
               int current_chest = (Integer)Config.logging_chest.get(logging_chest_id);
               if (Config.old_container.get(logging_chest_id) == null) {
                  return;
               }

               int force_size = 0;
               if (Config.force_containers.get(logging_chest_id) != null) {
                  force_size = ((List)Config.force_containers.get(logging_chest_id)).size();
               }

               if (current_chest == force_data || force_size > 0) {
                  Logger.log_container(preparedStmt, user, block.getType(), inventory, block.getLocation());
                  List<ItemStack[]> old = (List)Config.old_container.get(logging_chest_id);
                  if (old.isEmpty()) {
                     Config.old_container.remove(logging_chest_id);
                     Config.logging_chest.remove(logging_chest_id);
                  }
               }
            }

            inventories.remove(id);
         }
      }

   }

   private static void processEntityKill(PreparedStatement preparedStmt, PreparedStatement preparedStmt_entities, int process_id, int id, Object object, String user) {
      if (object instanceof Object[]) {
         BlockState block = (BlockState)((Object[])object)[0];
         EntityType type = (EntityType)((Object[])object)[1];
         Map<Integer, List<Object>> object_lists = (Map)Consumer.consumer_object_list.get(process_id);
         if (object_lists.get(id) != null) {
            List<Object> object_list = (List)object_lists.get(id);
            int entityId = Functions.getEntityId(type);
            Logger.log_entity_kill(preparedStmt, preparedStmt_entities, user, block, object_list, entityId);
            object_lists.remove(id);
         }
      }

   }

   private static void processEntitySpawn(Statement statement, Object object, int row_id) {
      if (object instanceof Object[]) {
         BlockState block = (BlockState)((Object[])object)[0];
         EntityType type = (EntityType)((Object[])object)[1];
         String query = "SELECT data FROM " + Config.prefix + "entity WHERE rowid='" + row_id + "' LIMIT 0, 1";
         List<Object> data = Database.getEntityData(statement, block, query);
         Functions.spawnEntity(block, type, data);
      }

   }

   private static void processHangingRemove(Object object, int delay) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Functions.removeHanging(block, delay);
      }

   }

   private static void processHangingSpawn(Object object, Material type, int data, int delay) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Functions.spawnHanging(block, type, data, delay);
      }

   }

   private static void processMaterialInsert(PreparedStatement preparedStmt, String name, int material_id) {
      Database.insertMaterial(preparedStmt, material_id, name);
   }

   private static void processNaturalBlockBreak(Statement statement, PreparedStatement preparedStmt, int process_id, int id, String user, Object object, Material block_type, int block_data) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Map<Integer, List<BlockState>> block_lists = (Map)Consumer.consumer_block_list.get(process_id);
         if (block_lists.get(id) != null) {
            for(BlockState list_block : block_lists.get(id)) {
               String removed = Lookup.who_removed_cache(list_block);
               if (!removed.isEmpty()) {
                  user = removed;
               }
            }

            block_lists.remove(id);
            Logger.log_break(preparedStmt, user, block.getLocation(), Functions.block_id(block_type), block_data, (List)null);
         }
      }

   }

   private static void processPlayerChat(PreparedStatement preparedStmt, int process_id, int id, int time, String user) {
      Map<Integer, String> strings = (Map)Consumer.consumer_strings.get(process_id);
      if (strings.get(id) != null) {
         String message = (String)strings.get(id);
         Logger.log_chat(preparedStmt, time, user, message);
         strings.remove(id);
      }

   }

   private static void processPlayerCommand(PreparedStatement preparedStmt, int process_id, int id, int time, String user) {
      Map<Integer, String> strings = (Map)Consumer.consumer_strings.get(process_id);
      if (strings.get(id) != null) {
         String message = (String)strings.get(id);
         Logger.log_command(preparedStmt, time, user, message);
         strings.remove(id);
      }

   }

   private static void processPlayerInteraction(PreparedStatement preparedStmt, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Logger.log_interact(preparedStmt, user, block);
      }

   }

   private static void processPlayerKill(PreparedStatement preparedStmt, int id, Object object, String user) {
      if (object instanceof Object[]) {
         BlockState block = (BlockState)((Object[])object)[0];
         String player = (String)((Object[])object)[1];
         Logger.log_player_kill(preparedStmt, user, block, player);
      }

   }

   private static void processPlayerLogin(Connection connection, PreparedStatement preparedStmt, int process_id, int id, Object object, int configSessions, int configUsernames, int time, String user) {
      if (object instanceof BlockState) {
         Map<Integer, String> strings = (Map)Consumer.consumer_strings.get(process_id);
         if (strings.get(id) != null) {
            String uuid = (String)strings.get(id);
            BlockState block = (BlockState)object;
            Logger.log_username(connection, user, uuid, configUsernames, time);
            if (configSessions == 1) {
               Logger.log_session(preparedStmt, user, block, time, 1);
            }

            strings.remove(id);
         }
      }

   }

   private static void processPlayerLogout(PreparedStatement preparedStmt, Object object, int time, String user) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Logger.log_session(preparedStmt, user, block, time, 0);
      }

   }

   private static void processRollbackUpdate(Statement statement, int process_id, int id, int action, int table) {
      Map<Integer, List<Object[]>> update_lists = (Map)Consumer.consumer_object_array_list.get(process_id);
      if (update_lists.get(id) != null) {
         for(Object[] list_row : update_lists.get(id)) {
            int rowid = (Integer)list_row[0];
            int rolled_back = (Integer)list_row[9];
            if (rolled_back == action) {
               Database.performUpdate(statement, rowid, action, table);
            }
         }

         update_lists.remove(id);
      }

   }

   private static void processSignText(PreparedStatement preparedStmt, int process_id, int id, int force_data, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Map<Integer, String[]> signs = (Map)Consumer.consumer_signs.get(process_id);
         if (signs.get(id) != null) {
            String[] sign_text = (String[])signs.get(id);
            Logger.sign_text(preparedStmt, user, block, sign_text[0], sign_text[1], sign_text[2], sign_text[3], force_data);
            signs.remove(id);
         }
      }

   }

   private static void processSignUpdate(Statement statement, Object object, int action, int time) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         int x = block.getX();
         int y = block.getY();
         int z = block.getZ();
         int wid = Functions.getWorldId(block.getWorld().getName());
         String query = "";
         if (action == 0) {
            query = "SELECT line_1, line_2, line_3, line_4 FROM " + Config.prefix + "sign WHERE wid='" + wid + "' AND x='" + x + "' AND z='" + z + "' AND y='" + y + "' AND time < '" + time + "' ORDER BY rowid DESC LIMIT 0, 1";
         } else {
            query = "SELECT line_1, line_2, line_3, line_4 FROM " + Config.prefix + "sign WHERE wid='" + wid + "' AND x='" + x + "' AND z='" + z + "' AND y='" + y + "' AND time >= '" + time + "' ORDER BY rowid ASC LIMIT 0, 1";
         }

         Database.getSignData(statement, block, query);
         Functions.updateBlock(block);
      }

   }

   private static void processSkullUpdate(Statement statement, Object object, int row_id) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         String query = "SELECT type,data,rotation,owner FROM " + Config.prefix + "skull WHERE rowid='" + row_id + "' LIMIT 0, 1";
         Database.getSkullData(statement, block, query);
         Functions.updateBlock(block);
      }

   }

   private static void processStructureGrowth(Statement statement, PreparedStatement preparedStmt, int process_id, int id, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Map<Integer, List<BlockState>> block_lists = (Map)Consumer.consumer_block_list.get(process_id);
         if (block_lists.get(id) != null) {
            List<BlockState> block_list = (List)block_lists.get(id);
            String result_data = Lookup.who_placed(statement, block);
            if (!result_data.isEmpty()) {
               user = result_data;
            }

            for(BlockState list_block : block_list) {
               if (list_block.getY() >= block.getY()) {
                  Logger.log_place(preparedStmt, user, list_block, 0, 0, (Material)null, -1, false, (List)null);
               }
            }

            block_lists.remove(id);
         }
      }

   }

   private static void processWorldInsert(PreparedStatement preparedStmt, String world, int world_id) {
      Database.insertWorld(preparedStmt, world_id, world);
   }
}
