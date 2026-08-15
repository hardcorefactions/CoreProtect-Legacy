package net.coreprotect.listener;

import java.util.List;
import net.coreprotect.Functions;
import net.coreprotect.consumer.Queue;
import net.coreprotect.database.Lookup;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class WorldListener extends Queue implements Listener {
   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onLeavesDecay(LeavesDecayEvent event) {
      World world = event.getBlock().getWorld();
      if (!event.isCancelled() && Functions.checkConfig(world, "leaf-decay") == 1) {
         String player = "#decay";
         Block block = event.getBlock();
         Material type = event.getBlock().getType();
         int data = Functions.getData(event.getBlock());
         Queue.queueBlockBreak(player, block.getState(), type, data);
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void OnPortalCreate(PortalCreateEvent event) {
      World world = event.getWorld();
      if (!event.isCancelled() && Functions.checkConfig(world, "portals") == 1) {
         String user = "#portal";

         for(Block block : event.getBlocks()) {
            Material type = block.getType();
            if (type.equals(Material.FIRE)) {
               String result_data = Lookup.who_placed_cache(block);
               if (!result_data.isEmpty()) {
                  user = result_data;
               }
               break;
            }
         }

         for(Block block : event.getBlocks()) {
            Material type = block.getType();
            if (user.equals("#portal") && !type.equals(Material.OBSIDIAN)) {
               Queue.queueBlockPlaceDelayed(user, block, (BlockState)null, 20);
            } else if (type.equals(Material.AIR) || type.equals(Material.FIRE)) {
               Queue.queueBlockPlaceDelayed(user, block, (BlockState)null, 0);
            }
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onStructureGrow(StructureGrowEvent event) {
      TreeType treeType = event.getSpecies();
      String user = "#tree";
      int tree = 1;
      if (treeType != null) {
         List<BlockState> blocks = event.getBlocks();
         if (blocks.size() <= 1) {
            for(BlockState block : blocks) {
               if (block.getType().equals(Material.SAPLING)) {
                  return;
               }
            }
         }

         if (treeType.name().toLowerCase().contains("mushroom")) {
            user = "#mushroom";
            tree = 0;
         }

         if (!event.isCancelled()) {
            World world = event.getWorld();
            if (tree == 1 && Functions.checkConfig(world, "tree-growth") == 1 || tree == 0 && Functions.checkConfig(world, "mushroom-growth") == 1) {
               Player player = event.getPlayer();
               Location location = event.getLocation();
               if (player != null) {
                  user = player.getName();
               }

               Queue.queueStructureGrow(user, world.getBlockAt(location).getState(), blocks);
            }
         }

      }
   }
}
