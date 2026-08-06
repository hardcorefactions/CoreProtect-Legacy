package net.coreprotect.patch.script;

import java.sql.Statement;
import net.coreprotect.model.Config;
import net.coreprotect.patch.Patch;

public class __2_5_0 {
   protected static boolean patch(Statement statement) {
      try {
         if ((Integer)Config.config.get("use-mysql") == 1) {
            try {
               statement.executeUpdate("ALTER TABLE " + Config.prefix + "sign MODIFY line_1 VARCHAR(100)");
               statement.executeUpdate("ALTER TABLE " + Config.prefix + "sign MODIFY line_2 VARCHAR(100)");
               statement.executeUpdate("ALTER TABLE " + Config.prefix + "sign MODIFY line_3 VARCHAR(100)");
               statement.executeUpdate("ALTER TABLE " + Config.prefix + "sign MODIFY line_4 VARCHAR(100)");
               statement.executeUpdate("ALTER TABLE " + Config.prefix + "user MODIFY user VARCHAR(32)");
            } catch (Exception e) {
               e.printStackTrace();
            }

            if (!Patch.continuePatch()) {
               return false;
            }
         }

         statement.executeUpdate("ALTER TABLE " + Config.prefix + "block ADD COLUMN meta BLOB");
      } catch (Exception e) {
         e.printStackTrace();
      }

      return true;
   }
}
