package net.coreprotect.model;

public class BlockModel {
   private int type;
   private int data;

   public BlockModel(int type, int data) {
      this.type = type;
      this.data = data;
   }

   public int getData() {
      return this.data;
   }

   public int getType() {
      return this.type;
   }

   public void setData(int data) {
      this.data = data;
   }

   public void setType(int type) {
      this.type = type;
   }
}
