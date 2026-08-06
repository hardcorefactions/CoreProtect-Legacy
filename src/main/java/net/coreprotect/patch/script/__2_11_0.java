package net.coreprotect.patch.script;

import java.sql.Statement;
import net.coreprotect.model.Config;
import org.bukkit.Art;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class __2_11_0 {
   protected static boolean patch(Statement statement) {
      try {
         if ((Integer)Config.config.get("use-mysql") == 1) {
            statement.executeUpdate("START TRANSACTION");
         } else {
            statement.executeUpdate("BEGIN TRANSACTION");
         }

         for(Art artType : Art.values()) {
            Integer type = artType.getId();
            String name = artType.toString().toLowerCase();
            statement.executeUpdate("INSERT INTO " + Config.prefix + "art_map (id, art) VALUES ('" + type + "', '" + name + "')");
            Config.art.put(name, type);
            Config.art_reversed.put(type, name);
            if (type > Config.art_id) {
               Config.art_id = type;
            }
         }

         for(EntityType entityType : EntityType.values()) {
            Integer type = (int)entityType.getTypeId();
            String name = entityType.toString().toLowerCase();
            statement.executeUpdate("INSERT INTO " + Config.prefix + "entity_map (id, entity) VALUES ('" + type + "', '" + name + "')");
            Config.entities.put(name, type);
            Config.entities_reversed.put(type, name);
            if (type > Config.entity_id) {
               Config.entity_id = type;
            }
         }

         for(Material material : Material.values()) {
            Integer type = material.getId();
            String name = material.toString().toLowerCase();
            statement.executeUpdate("INSERT INTO " + Config.prefix + "material_map (id, material) VALUES ('" + type + "', '" + name + "')");
            Config.materials.put(name, type);
            Config.materials_reversed.put(type, name);
            if (type > Config.material_id) {
               Config.material_id = type;
            }
         }

         if ((Integer)Config.config.get("use-mysql") == 1) {
            statement.executeUpdate("COMMIT");
         } else {
            statement.executeUpdate("COMMIT TRANSACTION");
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return true;
   }
}
