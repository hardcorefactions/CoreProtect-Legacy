package net.coreprotect.worldedit;

import com.sk89q.worldedit.EditSession.Stage;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;
import net.coreprotect.CoreProtect;

public class CoreProtectEditSessionEvent {
   private static boolean initialized = false;

   public static boolean isInitialized() {
      return initialized;
   }

   public static void register() {
      try {
         CoreProtect.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(CoreProtect.getInstance(), new Runnable() {
            public void run() {
               try {
                  com.sk89q.worldedit.WorldEdit.getInstance().getEventBus().register(new CoreProtectEditSessionEvent());
                  CoreProtectEditSessionEvent.initialized = true;
               } catch (Exception var2) {
                  System.out.println("[CoreProtect] Unable to initialize WorldEdit logging.");
               }

            }
         }, 0L);
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   @Subscribe
   public void wrapForLogging(EditSessionEvent event) {
      Actor actor = event.getActor();
      World world = event.getWorld();
      if (actor != null && event.getStage().equals(Stage.BEFORE_CHANGE)) {
         event.setExtent(new CoreProtectLogger(actor, world, event.getExtent()));
      }

   }
}
