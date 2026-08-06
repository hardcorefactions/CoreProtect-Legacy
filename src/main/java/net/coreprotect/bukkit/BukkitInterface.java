package net.coreprotect.bukkit;

import java.util.List;
import java.util.Map;
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

public interface BukkitInterface {
   boolean getItemMeta(ItemMeta var1, List<List<Map<String, Object>>> var2, List<Map<String, Object>> var3);

   void setItemMeta(Material var1, List<Map<String, Object>> var2, ItemStack var3, int var4);

   boolean getEntityMeta(LivingEntity var1, List<Object> var2);

   boolean setEntityMeta(Entity var1, Object var2, int var3);

   void processMeta(BlockState var1, List<Object> var2);

   void setPotionMeta(Material var1, List<Map<String, Object>> var2, ItemStack var3);

   Material getEntityMaterial(EntityType var1);

   SkullType getSkullType(int var1);

   int getSkullType(SkullType var1);

   void getEntityAttributes(LivingEntity var1, List<Object> var2);

   void setEntityAttributes(Entity var1, List<Object> var2);

   void getVillagerRiches(Villager var1, List<Object> var2);

   void setVillagerRiches(Villager var1, Object var2);

   void getVillagerRecipes(Villager var1, List<Object> var2);

   void setVillagerRecipes(Villager var1, Object var2);

   boolean isEndCrystal(Material var1);

   void spawnEndCrystal(Location var1, Block var2, int var3);

   void queueEndCrystalPlace(String var1, Block var2, int var3);

   void queueEndCrystalBreak(String var1, Block var2, EnderCrystal var3);

   ItemStack getItemInHand(Player var1);

   EquipmentSlot getEventHand(PlayerInteractEvent var1);

   boolean isHand(PlayerInteractEvent var1);

   boolean isEndCrystalShowingBottom(EnderCrystal var1);

   Material getItemTypeInHand(PlayerInteractEvent var1, Player var2);
}
