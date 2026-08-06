package net.coreprotect.bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.coreprotect.consumer.Queue;
import net.coreprotect.database.Logger;
import net.coreprotect.database.Lookup;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;

public class Bukkit_v1_9 extends BukkitAdapter implements BukkitInterface {
   public Material getEntityMaterial(EntityType type) {
      switch (type) {
         case ARMOR_STAND:
            return Material.ARMOR_STAND;
         case ENDER_CRYSTAL:
            return Material.END_CRYSTAL;
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
         case 5:
            return SkullType.DRAGON;
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
         case DRAGON:
            return 5;
         default:
            return 0;
      }
   }

   public void getEntityAttributes(LivingEntity entity, List<Object> attributes) {
      if (entity instanceof Attributable) {
         Attributable attributable = entity;

         for(Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = attributable.getAttribute(attribute);
            if (attributeInstance != null) {
               List<Object> attributeData = new ArrayList();
               List<Object> attributeModifiers = new ArrayList();
               attributeData.add(attributeInstance.getAttribute());
               attributeData.add(attributeInstance.getBaseValue());

               for(AttributeModifier modifier : attributeInstance.getModifiers()) {
                  attributeModifiers.add(modifier.serialize());
               }

               attributeData.add(attributeModifiers);
               attributes.add(attributeData);
            }
         }
      }

   }

   public void setEntityAttributes(Entity entity, List<Object> list) {
      if (entity instanceof Attributable && list.size() >= 6) {
         Attributable attributable = (Attributable)entity;

         for(Object value : (List)list.get(5)) {
            List<Object> attributeData = (List)value;
            Attribute attribute = (Attribute)attributeData.get(0);
            Double baseValue = (Double)attributeData.get(1);
            List<Object> attributeModifiers = (List)attributeData.get(2);
            AttributeInstance entityAttribute = attributable.getAttribute(attribute);
            if (entityAttribute != null) {
               entityAttribute.setBaseValue(baseValue);

               for(AttributeModifier modifier : entityAttribute.getModifiers()) {
                  entityAttribute.removeModifier(modifier);
               }

               for(Object modifier : attributeModifiers) {
                  Map<String, Object> serializedModifier = (Map)modifier;
                  entityAttribute.addModifier(AttributeModifier.deserialize(serializedModifier));
               }
            }
         }
      }

   }

   public void getVillagerRiches(Villager villager, List<Object> info) {
      info.add(villager.getRiches());
   }

   public void setVillagerRiches(Villager villager, Object value) {
      int set = (Integer)value;
      villager.setRiches(set);
   }

   public void getVillagerRecipes(Villager villager, List<Object> info) {
      List<Object> recipes = new ArrayList();

      for(MerchantRecipe merchantRecipe : villager.getRecipes()) {
         List<Object> recipe = new ArrayList();
         List<Object> ingredients = new ArrayList();
         List<Object> itemMap = new ArrayList();
         ItemStack item = merchantRecipe.getResult().clone();
         List<List<Map<String, Object>>> metadata = Logger.getItemMeta(item, item.getType(), 0);
         item.setItemMeta((ItemMeta)null);
         itemMap.add(item.serialize());
         itemMap.add(metadata);
         recipe.add(itemMap);
         recipe.add(merchantRecipe.getUses());
         recipe.add(merchantRecipe.getMaxUses());
         recipe.add(merchantRecipe.hasExperienceReward());

         for(ItemStack ingredient : merchantRecipe.getIngredients()) {
            List<Object> var13 = new ArrayList();
            item = ingredient.clone();
            metadata = Logger.getItemMeta(item, item.getType(), 0);
            item.setItemMeta((ItemMeta)null);
            var13.add(item.serialize());
            var13.add(metadata);
            ingredients.add(var13);
         }

         recipe.add(ingredients);
         recipes.add(recipe);
      }

      info.add(recipes);
   }

   public void setVillagerRecipes(Villager villager, Object value) {
      List<MerchantRecipe> merchantRecipes = new ArrayList();

      for(Object recipes : (List)value) {
         List<Object> recipe = (List)recipes;
         List<Object> itemMap = (List)recipe.get(0);
         ItemStack result = ItemStack.deserialize((Map)itemMap.get(0));
         List<List<Map<String, Object>>> metadata = (List)itemMap.get(1);
         Object[] populatedStack = Lookup.populateItemStack(result, metadata);
         result = (ItemStack)populatedStack[1];
         int uses = (Integer)recipe.get(1);
         int maxUses = (Integer)recipe.get(2);
         boolean experienceReward = (Boolean)recipe.get(3);
         List<ItemStack> merchantIngredients = new ArrayList();

         for(Object ingredient : (List)recipe.get(4)) {
            List<Object> ingredientMap = (List)ingredient;
            ItemStack item = ItemStack.deserialize((Map)ingredientMap.get(0));
            List<List<Map<String, Object>>> itemMetaData = (List)ingredientMap.get(1);
            populatedStack = Lookup.populateItemStack(item, itemMetaData);
            item = (ItemStack)populatedStack[1];
            merchantIngredients.add(item);
         }

         MerchantRecipe merchantRecipe = new MerchantRecipe(result, uses, maxUses, experienceReward);
         merchantRecipe.setIngredients(merchantIngredients);
         merchantRecipes.add(merchantRecipe);
      }

      if (merchantRecipes.size() > 0) {
         villager.setRecipes(merchantRecipes);
      }

   }

   public boolean isEndCrystal(Material type) {
      return type.equals(Material.END_CRYSTAL);
   }

   public void spawnEndCrystal(Location location, Block block, int row_data) {
      Entity entity = block.getLocation().getWorld().spawnEntity(location, EntityType.ENDER_CRYSTAL);
      EnderCrystal enderCrystal = (EnderCrystal)entity;
      enderCrystal.setShowingBottom(row_data != 0);
      entity.teleport(location);
   }

   public void queueEndCrystalPlace(String name, Block block, int showingBottom) {
      Queue.queueBlockPlace(name, block.getState(), block, block.getState(), Material.END_CRYSTAL, showingBottom, 1);
   }

   public void queueEndCrystalBreak(String user, Block block, EnderCrystal crystal) {
      Queue.queueBlockBreak(user, block.getState(), Material.END_CRYSTAL, crystal.isShowingBottom() ? 1 : 0);
   }

   public ItemStack getItemInHand(Player player) {
      return player.getInventory().getItemInMainHand();
   }

   public EquipmentSlot getEventHand(PlayerInteractEvent event) {
      return event.getHand();
   }

   public boolean isHand(PlayerInteractEvent event) {
      return event.getHand().equals(EquipmentSlot.HAND);
   }

   public boolean isEndCrystalShowingBottom(EnderCrystal enderCrystal) {
      return enderCrystal.isShowingBottom();
   }

   public Material getItemTypeInHand(PlayerInteractEvent event, Player player) {
      List<Material> entityBlockTypes = Arrays.asList(Material.ARMOR_STAND, Material.END_CRYSTAL);
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      ItemStack offHand = player.getInventory().getItemInOffHand();
      if (event.getHand().equals(EquipmentSlot.HAND) && mainHand != null && entityBlockTypes.contains(mainHand.getType())) {
         return mainHand.getType();
      } else {
         return event.getHand().equals(EquipmentSlot.OFF_HAND) && offHand != null && entityBlockTypes.contains(offHand.getType()) ? offHand.getType() : null;
      }
   }
}
