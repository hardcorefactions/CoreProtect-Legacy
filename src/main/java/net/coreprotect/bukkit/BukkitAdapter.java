package net.coreprotect.bukkit;

import java.util.List;
import java.util.Map;
import net.coreprotect.model.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BukkitAdapter implements BukkitInterface {
   public static BukkitInterface ADAPTER;
   public static final int BUKKIT_v1_8 = 8;
   public static final int BUKKIT_v1_9 = 9;
   public static final int BUKKIT_v1_10 = 10;
   public static final int BUKKIT_v1_11 = 11;
   public static final int BUKKIT_v1_12 = 12;

   public static void loadAdapter() {
      switch (Config.SPIGOT_VERSION) {
         case 8:
            ADAPTER = new BukkitAdapter();
            break;
         case 9:
         case 10:
            ADAPTER = new Bukkit_v1_9();
            break;
         case 11:
            ADAPTER = new Bukkit_v1_11();
            break;
         case 12:
         default:
            ADAPTER = new Bukkit_v1_12();
      }

   }

   public boolean getItemMeta(ItemMeta itemMeta, List<List<Map<String, Object>>> metadata, List<Map<String, Object>> list) {
      return false;
   }

   public void setItemMeta(Material row_type, List<Map<String, Object>> map, ItemStack itemstack, int item_count) {
   }

   public boolean getEntityMeta(LivingEntity entity, List<Object> info) {
      return false;
   }

   public boolean setEntityMeta(Entity entity, Object value, int count) {
      return false;
   }

   public void processMeta(BlockState block, List<Object> meta) {
   }

   public void setPotionMeta(Material row_type, List<Map<String, Object>> map, ItemStack itemstack) {
   }

   public Material getEntityMaterial(EntityType type) {
      switch (type) {
         case ARMOR_STAND:
            return Material.ARMOR_STAND;
         default:
            return null;
      }
   }

   public SkullType getSkullType(int type) {
      switch (type) {
         case 0:
            return SkullType.SKELETON;
         case 1:
            return SkullType.WITHER;
         case 2:
            return SkullType.ZOMBIE;
         case 3:
            return SkullType.PLAYER;
         case 4:
            return SkullType.CREEPER;
         default:
            return SkullType.SKELETON;
      }
   }

   public int getSkullType(SkullType type) {
      switch (type) {
         case SKELETON:
            return 0;
         case WITHER:
            return 1;
         case ZOMBIE:
            return 2;
         case PLAYER:
            return 3;
         case CREEPER:
            return 4;
         default:
            return 0;
      }
   }

   public void getEntityAttributes(LivingEntity entity, List<Object> attributes) {
   }

   public void setEntityAttributes(Entity entity, List<Object> list) {
   }

   public void getVillagerRiches(Villager villager, List<Object> info) {
   }

   public void setVillagerRiches(Villager villager, Object value) {
   }

   public void getVillagerRecipes(Villager villager, List<Object> info) {
   }

   public void setVillagerRecipes(Villager villager, Object value) {
   }

   public boolean isEndCrystal(Material type) {
      return false;
   }

   public void spawnEndCrystal(Location location, Block block, int row_data) {
   }

   public void queueEndCrystalPlace(String name, Block block, int showingBottom) {
   }

   public void queueEndCrystalBreak(String user, Block block, EnderCrystal crystal) {
   }

   public ItemStack getItemInHand(Player player) {
      return player.getItemInHand();
   }

   public EquipmentSlot getEventHand(PlayerInteractEvent event) {
      return null;
   }

   public boolean isHand(PlayerInteractEvent event) {
      return true;
   }

   public boolean isEndCrystalShowingBottom(EnderCrystal enderCrystal) {
      return false;
   }

   public Material getItemTypeInHand(PlayerInteractEvent event, Player player) {
      return player.getItemInHand().getType().equals(Material.ARMOR_STAND) ? player.getItemInHand().getType() : null;
   }
}
