package net.coreprotect.listener;

import java.sql.Connection;
import java.sql.Statement;
import net.coreprotect.Functions;
import net.coreprotect.consumer.Queue;
import net.coreprotect.database.Database;
import net.coreprotect.database.Lookup;
import net.coreprotect.model.Config;
import net.coreprotect.model.Language;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;

public class HangingListener extends Queue implements Listener {
   protected static void inspectItemFrame(final BlockState block, final Player player) {
      class BasicThread implements Runnable {
         public void run() {
            try {
               Connection connection = Database.getConnection(false);
               if (connection != null) {
                  Statement statement = connection.createStatement();
                  String blockdata = Lookup.block_lookup(statement, block, player.getName(), 0, 1, 7);
                  if (blockdata.contains("\n")) {
                     for(String b : blockdata.split("\n")) {
                        player.sendMessage(b);
                     }
                  } else if (!blockdata.isEmpty()) {
                     player.sendMessage(blockdata);
                  }

                  statement.close();
                  connection.close();
               } else {
                  player.sendMessage(Language.get("database-busy-please-try-again-later"));
               }
            } catch (Exception e) {
               e.printStackTrace();
            }

         }
      }

      Runnable runnable = new BasicThread();
      Thread thread = new Thread(runnable);
      thread.start();
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onHangingBreak(HangingBreakEvent event) {
      HangingBreakEvent.RemoveCause cause = event.getCause();
      Entity entity = event.getEntity();
      Block block_event = event.getEntity().getLocation().getBlock();
      if ((entity instanceof ItemFrame || entity instanceof Painting) && (cause.equals(RemoveCause.EXPLOSION) || cause.equals(RemoveCause.PHYSICS) || cause.equals(RemoveCause.OBSTRUCTION))) {
         String p = "#explosion";
         Block ab = null;
         if (cause.equals(RemoveCause.PHYSICS)) {
            p = "#physics";
         } else if (cause.equals(RemoveCause.OBSTRUCTION)) {
            p = "#obstruction";
         }

         if (!cause.equals(RemoveCause.EXPLOSION)) {
            Hanging he = (Hanging)entity;
            BlockFace attached = he.getAttachedFace();
            ab = he.getLocation().getBlock().getRelative(attached);
         }

         Material t = Material.AIR;
         int d = 0;
         if (entity instanceof ItemFrame) {
            t = Material.ITEM_FRAME;
            ItemFrame itemframe = (ItemFrame)entity;
            if (itemframe.getItem() != null) {
               d = Functions.block_id(itemframe.getItem().getType());
            }
         } else if (entity instanceof Painting) {
            t = Material.PAINTING;
            Painting painting = (Painting)entity;
            d = Functions.getArtId(painting.getArt().toString(), true);
         }

         if (!event.isCancelled() && Functions.checkConfig(block_event.getWorld(), "natural-break") == 1) {
            Queue.queueNaturalBlockBreak(p, block_event.getState(), ab, t, d);
         }
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   protected void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
      Entity entity = event.getEntity();
      Entity remover = event.getRemover();
      BlockState block_event = event.getEntity().getLocation().getBlock().getState();
      boolean inspecting = false;
      if (event.getRemover() instanceof Player) {
         Player player = (Player)event.getRemover();
         if (Config.inspecting.get(player.getName()) != null && (Boolean)Config.inspecting.get(player.getName())) {
            inspectItemFrame(block_event, player);
            event.setCancelled(true);
            inspecting = true;
         }
      }

      if (entity instanceof ItemFrame || entity instanceof Painting) {
         String p = "#entity";
         if (remover != null) {
            if (remover instanceof Player) {
               Player player = (Player)remover;
               p = player.getName();
            } else if (remover.getType() != null) {
               p = "#" + remover.getType().name().toLowerCase();
            }
         }

         Material t = Material.AIR;
         int d = 0;
         if (entity instanceof ItemFrame) {
            t = Material.ITEM_FRAME;
            ItemFrame itemframe = (ItemFrame)entity;
            if (itemframe.getItem() != null) {
               d = Functions.block_id(itemframe.getItem().getType());
            }
         } else if (entity instanceof Painting) {
            t = Material.PAINTING;
            Painting painting = (Painting)entity;
            d = Functions.getArtId(painting.getArt().toString(), true);
         }

         if (!event.isCancelled() && Functions.checkConfig(block_event.getWorld(), "block-break") == 1 && !inspecting) {
            Queue.queueBlockBreak(p, block_event, t, d);
         }
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   protected void onHangingPlace(HangingPlaceEvent event) {
      Entity entity = event.getEntity();
      Player player = event.getPlayer();
      if (entity instanceof ItemFrame || entity instanceof Painting) {
         Block block_event = event.getEntity().getLocation().getBlock();
         Material t = Material.AIR;
         int d = 0;
         if (entity instanceof ItemFrame) {
            t = Material.ITEM_FRAME;
            d = 0;
         } else if (entity instanceof Painting) {
            t = Material.PAINTING;
            Painting painting = (Painting)entity;
            d = Functions.getArtId(painting.getArt().toString(), true);
         }

         int inspect = 0;
         if (Config.inspecting.get(player) != null && (Boolean)Config.inspecting.get(player)) {
            inspect = 1;
            event.setCancelled(true);
         }

         if (!event.isCancelled() && Functions.checkConfig(block_event.getWorld(), "block-place") == 1 && inspect == 0) {
            Queue.queueBlockPlace(player.getName(), block_event.getState(), t, d);
         }
      }

   }
}
