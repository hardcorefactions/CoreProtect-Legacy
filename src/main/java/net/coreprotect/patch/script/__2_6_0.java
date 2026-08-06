package net.coreprotect.patch.script;

import java.sql.Statement;
import net.coreprotect.model.Config;

public class __2_6_0 {
   protected static boolean patch(Statement statement) {
      try {
         if ((Integer)Config.config.get("use-mysql") == 1) {
            statement.executeUpdate("START TRANSACTION");
            statement.executeUpdate("CREATE TEMPORARY TABLE " + Config.prefix + "version_tmp(rowid int(8), time int(10), version varchar(16)) ENGINE=InnoDB");
            statement.executeUpdate("INSERT INTO " + Config.prefix + "version_tmp SELECT rowid,time,version FROM " + Config.prefix + "version;");
            statement.executeUpdate("DROP TABLE " + Config.prefix + "version;");
            statement.executeUpdate("CREATE TABLE " + Config.prefix + "version(rowid int(8) NOT NULL AUTO_INCREMENT,PRIMARY KEY(rowid),time int(10),version varchar(16)) ENGINE=InnoDB");
            statement.executeUpdate("INSERT INTO " + Config.prefix + "version SELECT rowid,time,version FROM " + Config.prefix + "version_tmp;");
            statement.executeUpdate("DROP TEMPORARY TABLE " + Config.prefix + "version_tmp;");
            statement.executeUpdate("COMMIT");
         } else {
            statement.executeUpdate("BEGIN TRANSACTION");
            statement.executeUpdate("CREATE TEMPORARY TABLE " + Config.prefix + "version_tmp (time INTEGER, version TEXT);");
            statement.executeUpdate("INSERT INTO " + Config.prefix + "version_tmp SELECT time,version FROM " + Config.prefix + "version;");
            statement.executeUpdate("DROP TABLE " + Config.prefix + "version;");
            statement.executeUpdate("CREATE TABLE " + Config.prefix + "version (time INTEGER, version TEXT);");
            statement.executeUpdate("INSERT INTO " + Config.prefix + "version SELECT time,version FROM " + Config.prefix + "version_tmp;");
            statement.executeUpdate("DROP TABLE " + Config.prefix + "version_tmp;");
            statement.executeUpdate("COMMIT TRANSACTION");
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return true;
   }
}
