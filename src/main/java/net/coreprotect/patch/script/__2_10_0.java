package net.coreprotect.patch.script;

import java.sql.Statement;
import net.coreprotect.model.Config;

public class __2_10_0 {
   protected static boolean patch(Statement statement) {
      try {
         if ((Integer)Config.config.get("use-mysql") == 1) {
            statement.executeUpdate("ALTER TABLE " + Config.prefix + "user ADD COLUMN uuid varchar(64), ADD INDEX(uuid)");
         } else {
            statement.executeUpdate("ALTER TABLE " + Config.prefix + "user ADD COLUMN uuid TEXT;");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS uuid_index ON " + Config.prefix + "user(uuid);");
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return true;
   }
}
