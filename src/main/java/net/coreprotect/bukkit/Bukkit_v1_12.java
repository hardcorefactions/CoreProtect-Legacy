package net.coreprotect.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

public class Bukkit_v1_12 extends Bukkit_v1_11 implements BukkitInterface {
   public boolean getItemMeta(ItemMeta itemMeta, List<List<Map<String, Object>>> metadata, List<Map<String, Object>> list) {
      if (itemMeta instanceof PotionMeta) {
         PotionMeta meta = (PotionMeta)itemMeta.clone();
         PotionMeta sub_meta = meta.clone();
         meta.setColor((Color)null);
         meta.clearCustomEffects();
         list.add(meta.serialize());
         if (sub_meta.hasColor()) {
            list.add(sub_meta.getColor().serialize());
         }

         metadata.add(list);
         if (sub_meta.hasCustomEffects()) {
            for(PotionEffect effect : sub_meta.getCustomEffects()) {
               list = new ArrayList<>();
               list.add(effect.serialize());
               metadata.add(list);
            }
         }

         return true;
      } else {
         return super.getItemMeta(itemMeta, metadata, list);
      }
   }

   public void setPotionMeta(Material row_type, List<Map<String, Object>> map, ItemStack itemstack) {
      if (map.size() > 1 && row_type.equals(Material.POTION)) {
         PotionMeta subMeta = (PotionMeta)itemstack.getItemMeta();
         Color color = Color.deserialize((Map)map.get(1));
         subMeta.setColor(color);
         itemstack.setItemMeta(subMeta);
      }

   }
}
