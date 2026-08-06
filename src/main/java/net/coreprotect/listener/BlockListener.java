package net.coreprotect.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.coreprotect.Functions;
import net.coreprotect.consumer.Queue;
import net.coreprotect.database.Database;
import net.coreprotect.database.Lookup;
import net.coreprotect.model.BlockInfo;
import net.coreprotect.model.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.inventory.ItemStack;

public class BlockListener extends Queue implements Listener {
   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onBlockBreak(BlockBreakEvent event) {
      if (!event.isCancelled()) {
         String player = event.getPlayer().getName();
         Block bl = event.getBlock();
         int x = bl.getX();
         int y = bl.getY();
         int z = bl.getZ();
         World world = bl.getWorld();
         Location l1 = new Location(world, (double)(x + 1), (double)y, (double)z);
         Location l2 = new Location(world, (double)(x - 1), (double)y, (double)z);
         Location l3 = new Location(world, (double)x, (double)y, (double)(z + 1));
         Location l4 = new Location(world, (double)x, (double)y, (double)(z - 1));
         Location l5 = new Location(world, (double)x, (double)(y + 1), (double)z);
         Location l6 = new Location(world, (double)x, (double)(y - 1), (double)z);
         int l = 1;
         int m = 7;
         if (Functions.checkConfig(world, "natural-break") == 0) {
            l = 6;
         }

         if (Functions.checkConfig(world, "block-break") == 0) {
            m = 6;
         }

         Block block = bl;
         Material type = bl.getType();

         for(int data = Functions.getData(bl); l < m; ++l) {
            Location lc = l1;
            if (l == 2) {
               lc = l2;
            }

            if (l == 3) {
               lc = l3;
            }

            if (l == 4) {
               lc = l4;
            }

            if (l == 5) {
               lc = l5;
            }

            Block b = block;
            boolean check_down = false;
            Material bt = type;
            int bd = data;
            int log = 1;
            if (l < 6) {
               if (l == 4 && (type.equals(Material.WOODEN_DOOR) || type.equals(Material.SPRUCE_DOOR) || type.equals(Material.BIRCH_DOOR) || type.equals(Material.JUNGLE_DOOR) || type.equals(Material.ACACIA_DOOR) || type.equals(Material.DARK_OAK_DOOR) || type.equals(Material.IRON_DOOR_BLOCK))) {
                  lc = l6;
                  check_down = true;
               }

               Block block_t = world.getBlockAt(lc);
               Material t = block_t.getType();
               if (l == 5 && BlockInfo.falling_block_types.contains(t) && Functions.checkConfig(world, "block-movement") == 1) {
                  int yc = y + 2;

                  for(int topfound = 0; topfound == 0; ++yc) {
                     Block block_up = world.getBlockAt(x, yc, z);
                     Material up = block_up.getType();
                     if (!BlockInfo.falling_block_types.contains(up)) {
                        lc = new Location(world, (double)x, (double)(yc - 1), (double)z);
                        topfound = 1;
                     }
                  }
               }

               if (!BlockInfo.track_any.contains(t)) {
                  if (l != 5 && !check_down) {
                     if (!BlockInfo.track_side.contains(t)) {
                        log = 0;
                     } else if (!type.equals(Material.STONE_BUTTON) && !type.equals(Material.WOOD_BUTTON)) {
                        if (!t.equals(Material.RAILS) && !t.equals(Material.POWERED_RAIL) && !t.equals(Material.DETECTOR_RAIL) && !t.equals(Material.ACTIVATOR_RAIL)) {
                           if (t.equals(Material.BED_BLOCK) && !type.equals(Material.BED_BLOCK)) {
                              log = 0;
                           }
                        } else {
                           int check_data = Functions.getData(block_t);
                           if (l == 1 && check_data != 3) {
                              log = 0;
                           } else if (l == 2 && check_data != 2) {
                              log = 0;
                           } else if (l == 3 && check_data != 4) {
                              log = 0;
                           } else if (l == 4 && check_data != 5) {
                              log = 0;
                           }
                        }
                     } else {
                        Block check = world.getBlockAt(lc);
                        int check_data = Functions.getData(check);
                        if (check_data != l) {
                           log = 0;
                        }
                     }
                  } else if (!BlockInfo.track_top.contains(t)) {
                     log = 0;
                  }

                  if (log == 0) {
                     if (type.equals(Material.PISTON_EXTENSION)) {
                        if (t.equals(Material.PISTON_STICKY_BASE) || t.equals(Material.PISTON_BASE)) {
                           log = 1;
                        }
                     } else if (l == 5 && BlockInfo.falling_block_types.contains(t)) {
                        log = 1;
                     }
                  }
               } else if (t.equals(Material.PISTON_EXTENSION)) {
                  if (!type.equals(Material.PISTON_STICKY_BASE) && !type.equals(Material.PISTON_BASE)) {
                     log = 0;
                  }
               } else {
                  Block check = world.getBlockAt(lc);
                  int check_data = Functions.getData(check);
                  if (check_data != l) {
                     log = 0;
                  }
               }

               if (log == 1) {
                  b = world.getBlockAt(lc);
                  bt = b.getType();
                  bd = Functions.getData(b);
               }
            }

            BlockState b1 = b.getState();
            Material bt1 = bt;
            int bd1 = bd;
            int bn = l;
            if (log == 1 && (bt.equals(Material.SKULL) || bt.equals(Material.WALL_BANNER) || bt.equals(Material.STANDING_BANNER))) {
               try {
                  if (b1 instanceof Banner || b1 instanceof Skull) {
                     Queue.queueAdvancedBreak(player, b1, bt1, bd1, type, bn);
                  }

                  log = 0;
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }

            if (log == 1 && (bt.equals(Material.SIGN_POST) || bt.equals(Material.WALL_SIGN)) && Functions.checkConfig(world, "sign-text") == 1) {
               try {
                  Sign sign = (Sign)b.getState();
                  String line1 = sign.getLine(0);
                  String line2 = sign.getLine(1);
                  String line3 = sign.getLine(2);
                  String line4 = sign.getLine(3);
                  Queue.queueSignText(player, b1, line1, line2, line3, line4, 5);
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }

            if (log == 1) {
               Database.containerBreakCheck(player, block.getType(), block, block.getLocation());
               Functions.iceBreakCheck(b1, player, bt);
               Queue.queueBlockBreak(player, b1, bt, bd, type, l);
            }
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onBlockBurn(BlockBurnEvent event) {
      World world = event.getBlock().getWorld();
      if (!event.isCancelled() && Functions.checkConfig(world, "block-burn") == 1) {
         String player = "#fire";
         Block block = event.getBlock();
         Material type = block.getType();
         int data = Functions.getData(event.getBlock());
         Queue.queueBlockBreak(player, block.getState(), type, data);
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onBlockFromTo(BlockFromToEvent event) {
      Material type = event.getBlock().getType();
      Material type2 = event.getToBlock().getType();
      if (!event.isCancelled()) {
         World world = event.getBlock().getWorld();
         if (Functions.checkConfig(world, "water-flow") == 1 && (type.equals(Material.WATER) || type.equals(Material.STATIONARY_WATER)) || Functions.checkConfig(world, "lava-flow") == 1 && (type.equals(Material.LAVA) || type.equals(Material.STATIONARY_LAVA))) {
            List<Material> flow_break = Arrays.asList(Material.AIR, Material.SAPLING, Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.WEB, Material.LONG_GRASS, Material.DEAD_BUSH, Material.YELLOW_FLOWER, Material.RED_ROSE, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.TORCH, Material.FIRE, Material.REDSTONE_WIRE, Material.CROPS, Material.RAILS, Material.LEVER, Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_ON, Material.STONE_BUTTON, Material.SNOW, Material.DIODE_BLOCK_OFF, Material.DIODE_BLOCK_ON, Material.VINE, Material.COCOA, Material.TRIPWIRE_HOOK, Material.TRIPWIRE, Material.CARROT, Material.POTATO, Material.WOOD_BUTTON, Material.SKULL, Material.REDSTONE_COMPARATOR_OFF, Material.REDSTONE_COMPARATOR_ON, Material.ACTIVATOR_RAIL, Material.CARPET, Material.DOUBLE_PLANT);
            if (flow_break.contains(type2) || (type.equals(Material.WATER) || type.equals(Material.STATIONARY_WATER)) && (type2.equals(Material.LAVA) || type2.equals(Material.STATIONARY_LAVA)) || (type.equals(Material.LAVA) || type.equals(Material.STATIONARY_LAVA)) && (type2.equals(Material.WATER) || type2.equals(Material.STATIONARY_WATER))) {
               String f = "#flow";
               if (!type.equals(Material.WATER) && !type.equals(Material.STATIONARY_WATER)) {
                  if (type.equals(Material.LAVA) || type.equals(Material.STATIONARY_LAVA)) {
                     f = "#lava";
                  }
               } else {
                  f = "#water";
               }

               Block block = event.getBlock();
               int unixtimestamp = (int)(System.currentTimeMillis() / 1000L);
               int x = event.getToBlock().getX();
               int y = event.getToBlock().getY();
               int z = event.getToBlock().getZ();
               int wid = Functions.getWorldId(block.getWorld().getName());
               if (Functions.checkConfig(world, "liquid-tracking") == 1) {
                  String p = Lookup.who_placed_cache(block);
                  if (p.length() > 0) {
                     f = p;
                  }
               }

               Config.lookup_cache.put("" + x + "." + y + "." + z + "." + wid + "", new Object[]{unixtimestamp, f, type});
               Queue.queueBlockPlace(f, (Block)event.getToBlock(), event.getToBlock().getState(), type, Functions.getData(event.getBlock()));
            }
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onBlockIgnite(BlockIgniteEvent event) {
      World world = event.getBlock().getWorld();
      if (!event.isCancelled() && Functions.checkConfig(world, "block-ignite") == 1) {
         Block block = event.getBlock();
         if (block == null) {
            return;
         }

         if (event.getPlayer() == null) {
            Queue.queueBlockPlace("#fire", block.getState(), block.getState(), Material.FIRE);
         } else {
            Player player = event.getPlayer();
            Queue.queueBlockPlace(player.getName(), block.getState(), (BlockState)null, Material.FIRE);
            int unixtimestamp = (int)(System.currentTimeMillis() / 1000L);
            int world_id = Functions.getWorldId(block.getWorld().getName());
            Config.lookup_cache.put("" + block.getX() + "." + block.getY() + "." + block.getZ() + "." + world_id + "", new Object[]{unixtimestamp, player.getName(), block.getType()});
         }
      }

   }

   protected void onBlockPiston(BlockPistonEvent event) {
      List<Block> event_blocks = null;
      if (event instanceof BlockPistonExtendEvent) {
         event_blocks = ((BlockPistonExtendEvent)event).getBlocks();
      } else if (event instanceof BlockPistonRetractEvent) {
         event_blocks = ((BlockPistonRetractEvent)event).getBlocks();
      }

      World world = event.getBlock().getWorld();
      if (Functions.checkConfig(world, "pistons") == 1 && !event.isCancelled()) {
         List<Block> nblocks = new ArrayList();
         List<BlockState> blocks = new ArrayList();

         for(Block block : event_blocks) {
            Block block_relative = block.getRelative(event.getDirection());
            if (Functions.checkConfig(world, "block-movement") == 1) {
               block_relative = Functions.fallingSand(block_relative, block.getState(), "#piston");
            }

            nblocks.add(block_relative);
            blocks.add(block.getState());
         }

         Block b = event.getBlock();
         BlockFace d = event.getDirection();
         Block bm = b.getRelative(d);
         int wid = Functions.getWorldId(bm.getWorld().getName());
         int unixtimestamp = (int)(System.currentTimeMillis() / 1000L);
         int log = 0;

         for(int l = 0; l <= nblocks.size(); ++l) {
            int ll = l - 1;
            Block n = null;
            if (ll == -1) {
               n = bm;
            } else {
               n = (Block)nblocks.get(ll);
            }

            if (n != null) {
               int x = n.getX();
               int y = n.getY();
               int z = n.getZ();
               Material t = n.getType();
               String cords = "" + x + "." + y + "." + z + "." + wid + "." + t.name() + "";
               if (Config.piston_cache.get(cords) == null) {
                  log = 1;
               }

               Config.piston_cache.put(cords, new Object[]{unixtimestamp});
            }
         }

         if (log == 1) {
            String e = "#piston";

            for(BlockState block : blocks) {
               Queue.queueBlockBreak(e, block, block.getType(), Functions.getData(block));
            }

            int c = 0;

            for(Block nblock : nblocks) {
               BlockState block = (BlockState)blocks.get(c);
               Queue.queueBlockPlace(e, (BlockState)nblock.getState(), (Material)block.getType(), Functions.getData(block));
               ++c;
            }
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onBlockPistonExtend(BlockPistonExtendEvent event) {
      this.onBlockPiston(event);
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onBlockPistonRetract(BlockPistonRetractEvent event) {
      this.onBlockPiston(event);
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onBlockPlace(BlockPlaceEvent event) {
      World world = event.getBlockPlaced().getWorld();
      if (!event.isCancelled() && Functions.checkConfig(world, "block-place") == 1) {
         Player player = event.getPlayer();
         Block b = event.getBlockPlaced();
         Block block = b;
         BlockState breplaced = event.getBlockReplacedState();
         Material force_type = null;
         int force_data = -1;
         boolean abort = false;
         Material block_type = b.getType();
         List<Material> stairs = Arrays.asList(Material.WOOD_STAIRS, Material.COBBLESTONE_STAIRS, Material.BRICK_STAIRS, Material.SMOOTH_STAIRS, Material.NETHER_BRICK_STAIRS, Material.SANDSTONE_STAIRS, Material.SPRUCE_WOOD_STAIRS, Material.BIRCH_WOOD_STAIRS, Material.JUNGLE_WOOD_STAIRS, Material.QUARTZ_STAIRS);
         List<Material> dir_blocks = Arrays.asList(Material.PISTON_STICKY_BASE, Material.PISTON_BASE, Material.DIODE_BLOCK_OFF, Material.SKULL, Material.REDSTONE_COMPARATOR_OFF);
         if (!Functions.listContains(BlockInfo.containers, block_type) && !Functions.listContains(dir_blocks, block_type) && !Functions.listContains(stairs, block_type)) {
            if (block_type.equals(Material.FIRE)) {
               ItemStack item = event.getItemInHand();
               if (!item.getType().equals(Material.FIRE)) {
                  abort = true;
               }
            }
         } else {
            Queue.queueBlockPlaceDelayed(player.getName(), b, breplaced, 0);
            abort = true;
         }

         if (!abort) {
            if (Functions.checkConfig(world, "block-movement") == 1) {
               block = Functions.fallingSand(b, (BlockState)null, player.getName());
               if (!block.equals(b)) {
                  force_type = b.getType();
               }
            }

            Queue.queueBlockPlace(player, block.getState(), b, breplaced, force_type, force_data);
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onBlockSpread(BlockSpreadEvent event) {
      if (!event.isCancelled() && Functions.checkConfig(event.getBlock().getWorld(), "vine-growth") == 1) {
         Block block = event.getBlock();
         BlockState blockstate = event.getNewState();
         if (blockstate.getType().equals(Material.VINE)) {
            Queue.queueBlockPlace("#vine", (BlockState)block.getState(), (Material)blockstate.getType(), Functions.getData(blockstate));
         }
      }

   }
}
