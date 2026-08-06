package net.coreprotect.listener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.coreprotect.CoreProtect;
import net.coreprotect.Functions;
import net.coreprotect.bukkit.BukkitAdapter;
import net.coreprotect.consumer.Queue;
import net.coreprotect.database.Database;
import net.coreprotect.model.BlockInfo;
import net.coreprotect.model.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;

public class EntityListener extends Queue implements Listener {
   @EventHandler
   public void onCreatureSpawn(CreatureSpawnEvent event) {
      if (!event.isCancelled()) {
         if (event.getEntityType().equals(EntityType.ARMOR_STAND)) {
            World world = event.getEntity().getWorld();
            Location location = event.getEntity().getLocation();
            String key = world.getName() + "-" + location.getBlockX() + "-" + location.getBlockY() + "-" + location.getBlockZ();
            Iterator<Map.Entry<UUID, Object[]>> it = Config.entity_block_mapper.entrySet().iterator();

            while(it.hasNext()) {
               Map.Entry<UUID, Object[]> pair = (Map.Entry)it.next();
               UUID uuid = (UUID)pair.getKey();
               Object[] data = pair.getValue();
               if ((data[0].equals(key) || data[1].equals(key)) && Functions.getEntityMaterial(event.getEntityType()).equals(data[2])) {
                  Player player = CoreProtect.getInstance().getServer().getPlayer(uuid);
                  Queue.queueBlockPlace(player.getName(), location.getBlock().getState(), location.getBlock(), location.getBlock().getState(), (Material)data[2], (int)event.getEntity().getLocation().getYaw(), 1);
                  it.remove();
               }
            }
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onEntityBlockForm(EntityBlockFormEvent event) {
      World world = event.getBlock().getWorld();
      if (!event.isCancelled() && Functions.checkConfig(world, "entity-change") == 1) {
         Entity entity = event.getEntity();
         Block block = event.getBlock();
         BlockState newState = event.getNewState();
         String e = "";
         if (entity instanceof Snowman) {
            e = "#snowman";
         }

         if (e.length() > 0) {
            Queue.queueBlockPlace(e, (BlockState)block.getState(), (Material)newState.getType(), Functions.getData(newState));
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onEntityChangeBlock(EntityChangeBlockEvent event) {
      World world = event.getBlock().getWorld();
      if (!event.isCancelled() && Functions.checkConfig(world, "entity-change") == 1) {
         Entity entity = event.getEntity();
         Block block = event.getBlock();
         Material newtype = event.getTo();
         Material type = event.getBlock().getType();
         int data = Functions.getData(event.getBlock());
         String e = "";
         if (entity instanceof Enderman) {
            e = "#enderman";
         } else if (entity instanceof EnderDragon) {
            e = "#enderdragon";
         } else if (entity instanceof Wither) {
            e = "#wither";
         } else if (entity instanceof Silverfish && newtype.equals(Material.AIR)) {
            e = "#silverfish";
         }

         if (e.length() > 0) {
            if (newtype.equals(Material.AIR)) {
               Queue.queueBlockBreak(e, block.getState(), type, data);
            } else {
               Queue.queueBlockPlace(e, block);
            }
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      Entity damager = event.getDamager();
      if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof ArmorStand || event.getEntity() instanceof EnderCrystal) {
         boolean inspecting = false;
         String user = "#entity";
         if (damager != null) {
            Entity entity = event.getEntity();
            Block block = entity.getLocation().getBlock();
            if (damager instanceof Player) {
               Player player = (Player)damager;
               user = player.getName();
               if (Config.inspecting.get(player.getName()) != null && (Boolean)Config.inspecting.get(player.getName())) {
                  HangingListener.inspectItemFrame(block.getState(), player);
                  event.setCancelled(true);
                  inspecting = true;
               }
            } else if (damager instanceof Arrow) {
               Arrow arrow = (Arrow)damager;
               ProjectileSource source = arrow.getShooter();
               if (source instanceof Player) {
                  Player player = (Player)source;
                  user = player.getName();
               }
            } else if (damager instanceof TNTPrimed) {
               user = "#tnt";
            } else if (damager instanceof Minecart) {
               String name = damager.getType().name();
               if (name.contains("TNT")) {
                  user = "#tnt";
               }
            } else if (damager instanceof Creeper) {
               user = "#creeper";
            } else if (!(damager instanceof EnderDragon) && !(damager instanceof EnderDragonPart)) {
               if (!(damager instanceof Wither) && !(damager instanceof WitherSkull)) {
                  if (damager.getType() != null) {
                     user = "#" + damager.getType().name().toLowerCase();
                  }
               } else {
                  user = "#wither";
               }
            } else {
               user = "#enderdragon";
            }

            if (!event.isCancelled() && Functions.checkConfig(entity.getWorld(), "block-break") == 1 && !inspecting) {
               if (entity instanceof ItemFrame) {
                  ItemFrame frame = (ItemFrame)event.getEntity();
                  int data = 0;
                  if (frame.getItem() != null) {
                     data = Functions.block_id(frame.getItem().getType());
                  }

                  Queue.queueBlockBreak(user, block.getState(), Material.ITEM_FRAME, data);
                  Queue.queueBlockPlace(user, (BlockState)block.getState(), (Material)Material.ITEM_FRAME, 0);
               } else if (entity instanceof ArmorStand) {
                  Database.containerBreakCheck(user, Material.ARMOR_STAND, entity, block.getLocation());
                  Queue.queueBlockBreak(user, block.getState(), Material.ARMOR_STAND, (int)entity.getLocation().getYaw());
               } else if (entity instanceof EnderCrystal) {
                  EnderCrystal crystal = (EnderCrystal)event.getEntity();
                  BukkitAdapter.ADAPTER.queueEndCrystalBreak(user, block, crystal);
               }
            }
         }
      }

   }

   @EventHandler
   public void onEntityDeath(EntityDeathEvent event) {
      LivingEntity entity = event.getEntity();
      if (entity != null) {
         if (Functions.checkConfig(entity.getWorld(), "entity-kills") == 1) {
            EntityDamageEvent damage = entity.getLastDamageCause();
            if (damage != null) {
               String e = "";
               boolean skip = true;
               if (Functions.checkConfig(entity.getWorld(), "skip-generic-data") == 0 || !(entity instanceof Zombie) && !(entity instanceof Skeleton)) {
                  skip = false;
               }

               if (damage instanceof EntityDamageByEntityEvent) {
                  EntityDamageByEntityEvent attack = (EntityDamageByEntityEvent)damage;
                  Entity attacker = attack.getDamager();
                  if (attacker instanceof Player) {
                     Player player = (Player)attacker;
                     e = player.getName();
                  } else if (attacker instanceof Arrow) {
                     Arrow arrow = (Arrow)attacker;
                     ProjectileSource shooter = arrow.getShooter();
                     if (shooter instanceof Player) {
                        Player player = (Player)shooter;
                        e = player.getName();
                     } else if (shooter instanceof LivingEntity) {
                        EntityType entityType = ((LivingEntity)shooter).getType();
                        if (entityType != null) {
                           String name = entityType.name().toLowerCase();
                           e = "#" + name;
                        }
                     }
                  } else if (attacker instanceof ThrownPotion) {
                     ThrownPotion potion = (ThrownPotion)attacker;
                     ProjectileSource shooter = potion.getShooter();
                     if (shooter instanceof Player) {
                        Player player = (Player)shooter;
                        e = player.getName();
                     } else if (shooter instanceof LivingEntity) {
                        EntityType entityType = ((LivingEntity)shooter).getType();
                        if (entityType != null) {
                           String name = entityType.name().toLowerCase();
                           e = "#" + name;
                        }
                     }
                  } else if (attacker.getType().name() != null) {
                     e = "#" + attacker.getType().name().toLowerCase();
                  }
               } else {
                  EntityDamageEvent.DamageCause cause = damage.getCause();
                  if (cause.equals(DamageCause.FIRE)) {
                     e = "#fire";
                  } else if (cause.equals(DamageCause.FIRE_TICK)) {
                     if (!skip) {
                        e = "#fire";
                     }
                  } else if (cause.equals(DamageCause.LAVA)) {
                     e = "#lava";
                  } else if (cause.equals(DamageCause.BLOCK_EXPLOSION)) {
                     e = "#explosion";
                  }
               }

               EntityType entity_type = entity.getType();
               if (e.length() == 0 && !skip) {
                  if (!(entity instanceof Player) && entity_type.name() != null) {
                     e = "#" + entity_type.name().toLowerCase();
                  } else if (entity instanceof Player) {
                     e = entity.getName();
                  }
               }

               if (e.startsWith("#wither")) {
                  e = "#wither";
               }

               if (e.startsWith("#enderdragon")) {
                  e = "#enderdragon";
               }

               if (e.startsWith("#primedtnt") || e.startsWith("#tnt")) {
                  e = "#tnt";
               }

               if (e.startsWith("#lightning")) {
                  e = "#lightning";
               }

               if (e.length() > 0) {
                  List<Object> data = new ArrayList();
                  List<Object> age = new ArrayList();
                  List<Object> tame = new ArrayList();
                  List<Object> attributes = new ArrayList();
                  List<Object> info = new ArrayList();
                  if (entity instanceof Ageable) {
                     Ageable ageable = (Ageable)entity;
                     age.add(ageable.getAge());
                     age.add(ageable.getAgeLock());
                     age.add(ageable.isAdult());
                     age.add(ageable.canBreed());
                     age.add((Object)null);
                  }

                  if (entity instanceof Tameable) {
                     Tameable tameable = (Tameable)entity;
                     tame.add(tameable.isTamed());
                     if (tameable.isTamed() && tameable.getOwner() != null) {
                        tame.add(tameable.getOwner().getName());
                     }
                  }

                  BukkitAdapter.ADAPTER.getEntityAttributes(entity, attributes);
                  if (entity instanceof Creeper) {
                     Creeper creeper = (Creeper)entity;
                     info.add(creeper.isPowered());
                  } else if (entity instanceof Enderman) {
                     Enderman enderman = (Enderman)entity;
                     info.add(enderman.getCarriedMaterial().toItemStack().serialize());
                  } else if (entity instanceof IronGolem) {
                     IronGolem irongolem = (IronGolem)entity;
                     info.add(irongolem.isPlayerCreated());
                  } else if (entity instanceof Ocelot) {
                     Ocelot ocelot = (Ocelot)entity;
                     info.add(ocelot.getCatType());
                     info.add(ocelot.isSitting());
                  } else if (entity instanceof Pig) {
                     Pig pig = (Pig)entity;
                     info.add(pig.hasSaddle());
                  } else if (entity instanceof Sheep) {
                     Sheep sheep = (Sheep)entity;
                     info.add(sheep.isSheared());
                     info.add(sheep.getColor());
                  } else if (entity instanceof Skeleton) {
                     info.add((Object)null);
                  } else if (entity instanceof Slime) {
                     Slime slime = (Slime)entity;
                     info.add(slime.getSize());
                  } else if (entity instanceof Villager) {
                     Villager villager = (Villager)entity;
                     info.add(villager.getProfession());
                     BukkitAdapter.ADAPTER.getVillagerRiches(villager, info);
                     BukkitAdapter.ADAPTER.getVillagerRecipes(villager, info);
                  } else if (entity instanceof Wolf) {
                     Wolf wolf = (Wolf)entity;
                     info.add(wolf.isSitting());
                     info.add(wolf.getCollarColor());
                  } else if (!BukkitAdapter.ADAPTER.getEntityMeta(entity, info) && entity instanceof Zombie) {
                     Zombie zombie = (Zombie)entity;
                     info.add(zombie.isBaby());
                     info.add((Object)null);
                     info.add((Object)null);
                  }

                  data.add(age);
                  data.add(tame);
                  data.add(info);
                  data.add(entity.isCustomNameVisible());
                  data.add(entity.getCustomName());
                  data.add(attributes);
                  if (!(entity instanceof Player)) {
                     Queue.queueEntityKill(e, entity.getLocation(), data, entity_type);
                  } else {
                     Queue.queuePlayerKill(e, entity.getLocation(), entity.getName());
                  }
               }
            }
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   protected void onEntityExplode(EntityExplodeEvent event) {
      Entity entity = event.getEntity();
      World world = event.getLocation().getWorld();
      String user = "#explosion";
      if (entity instanceof TNTPrimed) {
         user = "#tnt";
      } else if (entity instanceof Minecart) {
         String name = entity.getType().name();
         if (name.contains("TNT")) {
            user = "#tnt";
         }
      } else if (entity instanceof Creeper) {
         user = "#creeper";
      } else if (!(entity instanceof EnderDragon) && !(entity instanceof EnderDragonPart)) {
         if (!(entity instanceof Wither) && !(entity instanceof WitherSkull)) {
            if (entity instanceof EnderCrystal) {
               user = "#endercrystal";
            }
         } else {
            user = "#wither";
         }
      } else {
         user = "#enderdragon";
      }

      int log = 0;
      if (Functions.checkConfig(world, "explosions") == 1) {
         log = 1;
      }

      if ((user.equals("#enderdragon") || user.equals("#wither")) && Functions.checkConfig(world, "entity-change") == 0) {
         log = 0;
      }

      if (!event.isCancelled() && log == 1) {
         List<Block> b = event.blockList();
         List<Block> nb = new ArrayList();
         if (Functions.checkConfig(world, "natural-break") == 1) {
            for(Block block : b) {
               int x = block.getX();
               int y = block.getY();
               int z = block.getZ();
               Location l1 = new Location(world, (double)(x + 1), (double)y, (double)z);
               Location l2 = new Location(world, (double)(x - 1), (double)y, (double)z);
               Location l3 = new Location(world, (double)x, (double)y, (double)(z + 1));
               Location l4 = new Location(world, (double)x, (double)y, (double)(z - 1));
               Location l5 = new Location(world, (double)x, (double)(y + 1), (double)z);
               int l = 1;

               for(int m = 6; l < m; ++l) {
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

                  Block block_t = world.getBlockAt(lc);
                  Material t = block_t.getType();
                  if (BlockInfo.track_any.contains(t) || BlockInfo.track_top.contains(t) || BlockInfo.track_side.contains(t)) {
                     Block bl = world.getBlockAt(lc);
                     nb.add(bl);
                  }
               }
            }
         }

         for(Block block : b) {
            if (!nb.contains(block)) {
               nb.add(block);
            }
         }

         for(Block block : nb) {
            Material blockType = block.getType();
            BlockState blockState = block.getState();
            if ((blockType.equals(Material.SIGN_POST) || blockType.equals(Material.WALL_SIGN)) && Functions.checkConfig(world, "sign-text") == 1) {
               try {
                  Sign sign = (Sign)blockState;
                  String line1 = sign.getLine(0);
                  String line2 = sign.getLine(1);
                  String line3 = sign.getLine(2);
                  String line4 = sign.getLine(3);
                  Queue.queueSignText(user, blockState, line1, line2, line3, line4, 5);
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }

            Database.containerBreakCheck(user, blockType, block, block.getLocation());
            Queue.queueBlockBreak(user, blockState, blockType, Functions.getData(block));
         }
      }

   }
}
