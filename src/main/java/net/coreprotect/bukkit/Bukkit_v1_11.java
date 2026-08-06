package net.coreprotect.bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.coreprotect.database.Logger;
import net.coreprotect.model.BlockInfo;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;

public class Bukkit_v1_11 extends Bukkit_v1_9 implements BukkitInterface {
   public Bukkit_v1_11() {
      BlockInfo.shulker_boxes = Arrays.asList(Material.BLACK_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.GREEN_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.LIME_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.SILVER_SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX);
      BlockInfo.containers = Arrays.asList(Material.DISPENSER, Material.CHEST, Material.FURNACE, Material.BURNING_FURNACE, Material.BREWING_STAND, Material.TRAPPED_CHEST, Material.HOPPER, Material.DROPPER, Material.ARMOR_STAND, Material.BLACK_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.GREEN_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.LIME_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.SILVER_SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX);
      BlockInfo.interact_blocks = Arrays.asList(Material.SPRUCE_DOOR, Material.BIRCH_DOOR, Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE, Material.JUNGLE_FENCE_GATE, Material.DARK_OAK_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.DISPENSER, Material.NOTE_BLOCK, Material.CHEST, Material.FURNACE, Material.BURNING_FURNACE, Material.WOODEN_DOOR, Material.LEVER, Material.STONE_BUTTON, Material.DIODE_BLOCK_OFF, Material.DIODE_BLOCK_ON, Material.TRAP_DOOR, Material.FENCE_GATE, Material.BREWING_STAND, Material.WOOD_BUTTON, Material.ANVIL, Material.TRAPPED_CHEST, Material.REDSTONE_COMPARATOR_OFF, Material.REDSTONE_COMPARATOR_ON, Material.HOPPER, Material.DROPPER, Material.BLACK_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.GREEN_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.LIME_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.SILVER_SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX);
   }

   public boolean getItemMeta(ItemMeta itemMeta, List<List<Map<String, Object>>> metadata, List<Map<String, Object>> list) {
      if (itemMeta instanceof MapMeta) {
         MapMeta meta = (MapMeta)itemMeta.clone();
         MapMeta sub_meta = meta.clone();
         meta.setColor((Color)null);
         list.add(meta.serialize());
         metadata.add(list);
         if (sub_meta.hasColor()) {
            list = new ArrayList();
            list.add(sub_meta.getColor().serialize());
            metadata.add(list);
         }

         return true;
      } else {
         return false;
      }
   }

   public void setItemMeta(Material row_type, List<Map<String, Object>> map, ItemStack itemstack, int item_count) {
      if (row_type.equals(Material.MAP)) {
         for(Map<String, Object> l : map) {
            MapMeta meta = (MapMeta)itemstack.getItemMeta();
            Color color = Color.deserialize(l);
            meta.setColor(color);
            itemstack.setItemMeta(meta);
         }
      }

   }

   public boolean getEntityMeta(LivingEntity entity, List<Object> info) {
      if (entity instanceof ZombieVillager) {
         ZombieVillager zombieVillager = (ZombieVillager)entity;
         info.add(zombieVillager.isBaby());
         info.add(zombieVillager.getVillagerProfession());
      } else {
         if (!(entity instanceof AbstractHorse)) {
            return false;
         }

         AbstractHorse abstractHorse = (AbstractHorse)entity;
         info.add((Object)null);
         info.add((Object)null);
         info.add(abstractHorse.getDomestication());
         info.add(abstractHorse.getJumpStrength());
         info.add(abstractHorse.getMaxDomestication());
         info.add((Object)null);
         info.add((Object)null);
         if (entity instanceof Horse) {
            Horse horse = (Horse)entity;
            ItemStack armor = horse.getInventory().getArmor();
            if (armor != null) {
               info.add(armor.serialize());
            } else {
               info.add((Object)null);
            }

            ItemStack saddle = horse.getInventory().getSaddle();
            if (saddle != null) {
               info.add(saddle.serialize());
            } else {
               info.add((Object)null);
            }

            info.add(horse.getColor());
            info.add(horse.getStyle());
         } else if (entity instanceof ChestedHorse) {
            ChestedHorse chestedHorse = (ChestedHorse)entity;
            info.add(chestedHorse.isCarryingChest());
            if (entity instanceof Llama) {
               Llama llama = (Llama)entity;
               ItemStack decor = llama.getInventory().getDecor();
               if (decor != null) {
                  info.add(decor.serialize());
               } else {
                  info.add((Object)null);
               }

               info.add(llama.getColor());
            }
         }
      }

      return true;
   }

   public boolean setEntityMeta(Entity entity, Object value, int count) {
      if (entity instanceof ZombieVillager) {
         ZombieVillager zombieVillager = (ZombieVillager)entity;
         if (count == 0) {
            boolean set = (Boolean)value;
            zombieVillager.setBaby(set);
         } else if (count == 1) {
            Villager.Profession set = (Villager.Profession)value;
            zombieVillager.setVillagerProfession(set);
         }
      } else {
         if (!(entity instanceof AbstractHorse)) {
            return false;
         }

         AbstractHorse abstractHorse = (AbstractHorse)entity;
         if (count == 0 && value != null) {
            boolean set = (Boolean)value;
            if (entity instanceof ChestedHorse) {
               ChestedHorse chestedHorse = (ChestedHorse)entity;
               chestedHorse.setCarryingChest(set);
            }
         } else if (count == 1 && value != null) {
            Horse.Color set = (Horse.Color)value;
            if (entity instanceof Horse) {
               Horse horse = (Horse)entity;
               horse.setColor(set);
            }
         } else if (count == 2) {
            int set = (Integer)value;
            abstractHorse.setDomestication(set);
         } else if (count == 3) {
            double set = (Double)value;
            abstractHorse.setJumpStrength(set);
         } else if (count == 4) {
            int set = (Integer)value;
            abstractHorse.setMaxDomestication(set);
         } else if (count == 5 && value != null) {
            Horse.Style set = (Horse.Style)value;
            Horse horse = (Horse)entity;
            horse.setStyle(set);
         }

         if (entity instanceof Horse) {
            Horse horse = (Horse)entity;
            if (count == 7) {
               if (value != null) {
                  ItemStack set = ItemStack.deserialize((Map)value);
                  horse.getInventory().setArmor(set);
               }
            } else if (count == 8) {
               if (value != null) {
                  ItemStack set = ItemStack.deserialize((Map)value);
                  horse.getInventory().setSaddle(set);
               }
            } else if (count == 9) {
               Horse.Color set = (Horse.Color)value;
               horse.setColor(set);
            } else if (count == 10) {
               Horse.Style set = (Horse.Style)value;
               horse.setStyle(set);
            }
         } else if (entity instanceof ChestedHorse) {
            if (count == 7) {
               ChestedHorse chestedHorse = (ChestedHorse)entity;
               boolean set = (Boolean)value;
               chestedHorse.setCarryingChest(set);
            }

            if (entity instanceof Llama) {
               Llama llama = (Llama)entity;
               if (count == 8) {
                  if (value != null) {
                     ItemStack set = ItemStack.deserialize((Map)value);
                     llama.getInventory().setDecor(set);
                  }
               } else if (count == 9) {
                  Llama.Color set = (Llama.Color)value;
                  llama.setColor(set);
               }
            }
         }
      }

      return true;
   }

   public void processMeta(BlockState block, List<Object> meta) {
      if (block instanceof ShulkerBox) {
         ShulkerBox shulkerBox = (ShulkerBox)block;
         ItemStack[] inventory = shulkerBox.getInventory().getStorageContents();
         int slot = 0;

         for(ItemStack itemStack : inventory) {
            if (itemStack != null && !itemStack.getType().equals(Material.AIR)) {
               Map<Integer, Object> itemMap = new HashMap();
               ItemStack item = itemStack.clone();
               List<List<Map<String, Object>>> metadata = Logger.getItemMeta(item, item.getType(), slot);
               item.setItemMeta((ItemMeta)null);
               itemMap.put(0, item.serialize());
               itemMap.put(1, metadata);
               meta.add(itemMap);
            }

            ++slot;
         }
      }

   }
}
