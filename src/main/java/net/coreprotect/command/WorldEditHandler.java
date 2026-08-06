package net.coreprotect.command;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import net.coreprotect.worldedit.WorldEdit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WorldEditHandler {
   protected static Integer[] runWorldEditCommand(CommandSender user) {
      Integer[] result = null;

      try {
         WorldEditPlugin wep = WorldEdit.getWorldEdit(user.getServer());
         if (wep != null && user instanceof Player) {
            LocalSession ls = wep.getSession((Player)user);
            World lw = ls.getSelectionWorld();
            if (lw != null) {
               Region region = ls.getSelection(lw);
               if (region != null && lw.getName().equals(((Player)user).getWorld().getName())) {
                  Vector v1 = region.getMinimumPoint();
                  int x1 = v1.getBlockX();
                  int y1 = v1.getBlockY();
                  int z1 = v1.getBlockZ();
                  int w = region.getWidth();
                  int h = region.getHeight();
                  int l = region.getLength();
                  int max = w;
                  if (h > w) {
                     max = h;
                  }

                  if (l > max) {
                     max = l;
                  }

                  int xmax = x1 + (w - 1);
                  int ymax = y1 + (h - 1);
                  int zmax = z1 + (l - 1);
                  result = new Integer[]{max, x1, xmax, y1, ymax, z1, zmax, 1};
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return result;
   }
}
