package net.coreprotect.worldedit;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.logging.AbstractLoggingExtent;
import com.sk89q.worldedit.world.World;
import net.coreprotect.Functions;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;

public class CoreProtectLogger extends AbstractLoggingExtent {
   private final Actor eventActor;
   private final World eventWorld;

   public CoreProtectLogger(Actor actor, World world, Extent extent) {
      super(extent);
      this.eventActor = actor;
      this.eventWorld = world;
   }

   protected void onBlockChange(Vector position, BaseBlock baseBlock) {
      if (this.eventWorld instanceof BukkitWorld) {
         org.bukkit.World world = ((BukkitWorld)this.eventWorld).getWorld();
         if (Functions.checkConfig(world, "worldedit") != 0) {
            Block currentBlock = world.getBlockAt(position.getBlockX(), position.getBlockY(), position.getBlockZ());
            Material oldBlockType = currentBlock.getType();
            ItemStack[] containerData = Functions.getContainerContents(oldBlockType, currentBlock, currentBlock.getLocation());
            BlockState oldBlock = currentBlock.getState();
            BlockState newBlock = currentBlock.getState();
            newBlock.setTypeId(baseBlock.getType());
            newBlock.setRawData((byte)baseBlock.getData());
            WorldEdit.logData(this.eventActor, oldBlock, oldBlockType, newBlock, containerData);
         }
      }
   }
}
