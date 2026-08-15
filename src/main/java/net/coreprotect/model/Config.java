package net.coreprotect.model;

import java.io.File;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.coreprotect.CoreProtect;
import net.coreprotect.Functions;
import net.coreprotect.bukkit.BukkitAdapter;
import net.coreprotect.command.CommandHandler;
import net.coreprotect.consumer.Consumer;
import net.coreprotect.consumer.Queue;
import net.coreprotect.database.Database;
import net.coreprotect.patch.Patch;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Config extends Queue {
   public static int SPIGOT_VERSION = 0;
   public static final String driver = "com.mysql.jdbc.Driver";
   public static final String sqlite = "plugins/CoreProtect/database.db";
   public static String host = "127.0.0.1";
   public static int port = 3306;
   public static String database = "database";
   public static String username = "root";
   public static String password = "";
   public static String prefix = "co_";
   // Spun on across threads (see Consumer/Lookup); must be volatile.
   public static volatile boolean server_running = false;
   public static volatile boolean converter_running = false;
   public static volatile boolean purge_running = false;
   /**
    * Guards the four id counters below. They are allocated from check-then-act
    * paths in Functions that the main thread, the consumer thread and rollback
    * threads all reach, and a race there hands the same id to two names.
    */
   public static final Object ID_LOCK = new Object();
   public static volatile int world_id = 0;
   public static volatile int material_id = 0;
   public static volatile int entity_id = 0;
   public static volatile int art_id = 0;
   public static final Map<String, Integer> worlds = Collections.synchronizedMap(new HashMap<>());
   public static final Map<Integer, String> worlds_reversed = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Integer> materials = Collections.synchronizedMap(new HashMap<>());
   public static final Map<Integer, String> materials_reversed = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Integer> entities = Collections.synchronizedMap(new HashMap<>());
   public static final Map<Integer, String> entities_reversed = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Integer> art = Collections.synchronizedMap(new HashMap<>());
   public static final Map<Integer, String> art_reversed = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Integer> config = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, int[]> rollback_hash = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Boolean> inspecting = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Object[]> lookup_cache = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Object[]> break_cache = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Object[]> piston_cache = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Object[]> entity_cache = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Boolean> blacklist = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Integer> logging_chest = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, List<ItemStack[]>> old_container = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, List<ItemStack[]>> force_containers = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Integer> lookup_type = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Integer> lookup_page = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, String> lookup_command = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, List<Object>> lookup_blist = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, List<Object>> lookup_elist = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, List<String>> lookup_e_userlist = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, List<String>> lookup_ulist = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, List<Integer>> lookup_alist = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Integer[]> lookup_radius = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, String> lookup_time = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Integer> lookup_rows = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, String> uuid_cache = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, String> uuid_cache_reversed = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Integer> player_id_cache = Collections.synchronizedMap(new HashMap<>());
   public static final Map<Integer, String> player_id_cache_reversed = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, List<Object[]>> last_rollback = Collections.synchronizedMap(new HashMap<>());
   public static final Map<String, Boolean> active_rollbacks = Collections.synchronizedMap(new HashMap<>());
   public static final Map<UUID, Object[]> entity_block_mapper = Collections.synchronizedMap(new HashMap<>());
   public static ConcurrentHashMap<String, String> language = new ConcurrentHashMap<>();
   public static final List<String> databaseTables = new ArrayList<>();

   private static void checkPlayers(Connection connection) {
      player_id_cache.clear();

      for (Player player : CoreProtect.getInstance().getServer().getOnlinePlayers()) {
         if (player_id_cache.get(player.getName().toLowerCase()) == null) {
            Database.loadUserID(connection, player.getName(), player.getUniqueId().toString());
         }
      }

   }

   private static void loadBlacklist() {
      try {
         Config.blacklist.clear();
         String blacklist = "plugins/CoreProtect/blacklist.txt";
         boolean exists = (new File(blacklist)).exists();
         if (exists) {
            RandomAccessFile blfile = new RandomAccessFile(blacklist, "rw");
            long blc = blfile.length();
            if (blc > 0L) {
               while (blfile.getFilePointer() < blfile.length()) {
                  String blacklist_user = blfile.readLine().replace(" ", "").toLowerCase();
                  if (!blacklist_user.isEmpty()) {
                     Config.blacklist.put(blacklist_user, true);
                  }
               }
            }

            blfile.close();
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   private static void loadConfig() {
      try {
         String confighead = "#CoreProtect Config\n";
         String noisy = "\n# If enabled, extra data is displayed when doing rollbacks and restores.\n# If disabled, you can manually trigger it in-game by adding \"#verbose\"\n# to the end of your rollback statement.\nverbose: true\n";
         String mysql = "\n# MySQL is optional and not required.\n# If you prefer to use MySQL, enable the following and fill out the fields.\nuse-mysql: false\ntable-prefix: co_\nmysql-host: 127.0.0.1\nmysql-port: 3306\nmysql-database: database\nmysql-username: root\nmysql-password: \n";
         String update = "\n# If enabled, CoreProtect will check for updates when your server starts up.\n# If an update is available, you'll be notified via your server console.\ncheck-updates: true\n";
         String api = "\n# If enabled, other plugins will be able to utilize the CoreProtect API.\napi-enabled: true\n";
         String defaultradius = "\n# If no radius is specified in a rollback or restore, this value will be\n# used as the radius. Set to \"0\" to disable automatically adding a radius.\ndefault-radius: 10\n";
         String maxradius = "\n# The maximum radius that can be used in a command. Set to \"0\" to disable.\n# To run a rollback or restore without a radius, you can use \"r:#global\".\nmax-radius: 100\n";
         String purgemin = "\n# The minimum age of data that a player can purge with \"/co purge\".\n# Uses the same time format as commands, for example \"30d\", \"12h\" or \"4w\".\n# Set to \"0\" to allow purging data of any age.\npurge-minimum-time: 30d\n";
         String purgeminconsole = "\n# As above, but for purges run from the console or a command block.\npurge-minimum-time-console: 24h\n";
         String sqlitewal = "\n# Puts SQLite into write-ahead logging mode, which lets lookups and rollbacks\n# read the database while block logging keeps writing to it. Without this the\n# two block each other, and whichever loses has its work silently dropped.\n# Only disable this if the database file lives on a network share, where WAL\n# is not supported. Has no effect when use-mysql is enabled.\nsqlite-wal: true\n";
         String sqlitebusytimeout = "\n# How long, in milliseconds, a SQLite query waits for the database to become\n# available before giving up. Set to \"0\" to fail immediately.\nsqlite-busy-timeout: 5000\n";
         String rollbackitems = "\n# If enabled, items taken from containers (etc) will be included in rollbacks.\nrollback-items: true\n";
         String rollbackentities = "\n# If enabled, entities, such as killed animals, will be included in rollbacks.\nrollback-entities: true\n";
         String skipgenericdata = "\n# If enabled, generic data, like zombies burning in daylight, won't be logged.\nskip-generic-data: true\n";
         String blockplace = "\n# Logs blocks placed by players.\nblock-place: true\n";
         String blockbreak = "\n# Logs blocks broken by players.\nblock-break: true\n";
         String naturalbreak = "\n# Logs blocks that break off of other blocks; for example, a sign or torch\n# falling off of a dirt block that a player breaks. This is required for\n# beds/doors to properly rollback.\nnatural-break: true\n";
         String blockmovement = "\n# Properly track block movement, such as sand or gravel falling.\nblock-movement: true\n";
         String pistons = "\n# Properly track blocks moved by pistons.\npistons: true\n";
         String blockburn = "\n# Logs blocks that burn up in a fire.\nblock-burn: true\n";
         String blockignite = "\n# Logs when a block naturally ignites, such as from fire spreading.\nblock-ignite: true\n";
         String explosions = "\n# Logs explosions, such as TNT and Creepers.\nexplosions: true\n";
         String entitychange = "\n# Track when an entity changes a block, such as an Enderman destroying blocks.\nentity-change: true\n";
         String entitykills = "\n# Logs killed entities, such as killed cows and enderman.\nentity-kills: true\n";
         String signtext = "\n# Logs text on signs. If disabled, signs will be blank when rolled back.\nsign-text: true\n";
         String buckets = "\n# Logs lava and water sources placed/removed by players who are using buckets.\nbuckets: true\n";
         String leafdecay = "\n# Logs natural tree leaf decay.\nleaf-decay: true\n";
         String treegrowth = "\n# Logs tree growth. Trees are linked to the player who planted the sappling.\ntree-growth: true\n";
         String mushroomgrowth = "\n# Logs mushroom growth.\nmushroom-growth: true\n";
         String vinegrowth = "\n# Logs natural vine growth.\nvine-growth: true\n";
         String portals = "\n# Logs when portals such as Nether portals generate naturally.\nportals: true\n";
         String waterflow = "\n# Logs water flow. If water destroys other blocks, such as torches,\n# this allows it to be properly rolled back.\nwater-flow: true\n";
         String lavaflow = "\n# Logs lava flow. If lava destroys other blocks, such as torches,\n# this allows it to be properly rolled back.\nlava-flow: true\n";
         String liquidtracking = "\n# Allows liquid to be properly tracked and linked to players.\n# For example, if a player places water which flows and destroys torches,\n# it can all be properly restored by rolling back that single player.\nliquid-tracking: true\n";
         String itemlogging = "\n# Track item transactions, such as when a player takes items from a\n# chest, furnace, or dispenser. Necessary for any item based rollbacks.\nitem-transactions: true\n";
         String playerinteract = "\n# Track player interactions, such as when a player opens a door, presses\n# a button, or opens a chest. Player interactions can't be rolled back.\nplayer-interactions: true\n";
         String playermessages = "\n# Logs messages that players send in the chat.\nplayer-messages: true\n";
         String playercommands = "\n# Logs all commands used by players.\nplayer-commands: true\n";
         String playersessions = "\n# Logs the logins and logouts of players.\nplayer-sessions: true\n";
         String usernamechanges = "\n# Logs when a player changes their Minecraft username.\nusername-changes: true\n";
         String worldedit = "\n# Logs changes made via the plugin \"WorldEdit\" if it's in use on your server.\nworldedit: true\n";
         config.clear();
         File config_file = new File("plugins/CoreProtect/config.yml");
         boolean exists = config_file.exists();
         if (!exists) {
            config_file.createNewFile();
         }

         File dir = new File("plugins/CoreProtect");
         String[] children = dir.list();
         if (children != null) {
            for (String element : children) {
                if (!element.startsWith(".") && element.endsWith(".yml") && !element.equalsIgnoreCase("language.yml")) {
                  try {
                     String key = element.replaceAll(".yml", "-");
                     if (key.equals("config-")) {
                        key = "";
                     }

                     RandomAccessFile configfile = new RandomAccessFile("plugins/CoreProtect/" + element, "rw");
                     long config_length = configfile.length();
                     if (config_length > 0L) {
                        while (configfile.getFilePointer() < configfile.length()) {
                           String line = configfile.readLine();
                           if (line.contains(":") && !line.startsWith("#")) {
                              line = line.replaceFirst(":", "§ ");
                              String[] i2 = line.split("§");
                              String option = i2[0].trim().toLowerCase();
                              if (key.isEmpty()) {
                                 if (option.equals("verbose")) {
                                    String setting = i2[1].trim().toLowerCase();
                                    if (setting.startsWith("t")) {
                                       config.put(key + "verbose", 1);
                                    } else if (setting.startsWith("f")) {
                                       config.put("verbose", 0);
                                    }
                                 }

                                 if (option.equals("use-mysql")) {
                                    String setting = i2[1].trim().toLowerCase();
                                    if (setting.startsWith("t")) {
                                       config.put("use-mysql", 1);
                                    } else if (setting.startsWith("f")) {
                                       config.put("use-mysql", 0);
                                    }
                                 }

                                 if (option.equals("table-prefix")) {
                                    prefix = i2[1].trim();
                                 }

                                 if (option.equals("mysql-host")) {
                                    host = i2[1].trim();
                                 }

                                 if (option.equals("mysql-port")) {
                                    String setting = i2[1].trim();
                                    setting = setting.replaceAll("[^0-9]", "");
                                    if (setting.isEmpty()) {
                                       setting = "0";
                                    }

                                    port = Integer.parseInt(setting);
                                 }

                                 if (option.equals("mysql-database")) {
                                    database = i2[1].trim();
                                 }

                                 if (option.equals("mysql-username")) {
                                    username = i2[1].trim();
                                 }

                                 if (option.equals("mysql-password")) {
                                    password = i2[1].trim();
                                 }

                                 if (option.equals("check-updates")) {
                                    String setting = i2[1].trim().toLowerCase();
                                    if (setting.startsWith("t")) {
                                       config.put("check-updates", 1);
                                    } else if (setting.startsWith("f")) {
                                       config.put("check-updates", 0);
                                    }
                                 }

                                 if (option.equals("api-enabled")) {
                                    String setting = i2[1].trim().toLowerCase();
                                    if (setting.startsWith("t")) {
                                       config.put("api-enabled", 1);
                                    } else if (setting.startsWith("f")) {
                                       config.put("api-enabled", 0);
                                    }
                                 }

                                 if (option.equals("default-radius")) {
                                    String setting = i2[1].trim();
                                    setting = setting.replaceAll("[^0-9]", "");
                                    if (setting.isEmpty()) {
                                       setting = "0";
                                    }

                                    config.put("default-radius", Integer.parseInt(setting));
                                 }

                                 if (option.equals("max-radius")) {
                                    String setting = i2[1].trim();
                                    setting = setting.replaceAll("[^0-9]", "");
                                    if (setting.isEmpty()) {
                                       setting = "0";
                                    }

                                    config.put("max-radius", Integer.parseInt(setting));
                                 }

                                 if (option.equals("purge-minimum-time") || option.equals("purge-minimum-time-console")) {
                                    String setting = i2[1].trim().toLowerCase();
                                    int parsed = CommandHandler.parseTime(new String[]{"", "t:" + setting.replaceAll("\\\\s+", "")});
                                    if (parsed <= 0 && !setting.startsWith("0")) {
                                       // A bare number carries no unit, so it would parse
                                       // as 0 and silently remove the limit. Keep the
                                       // default and say so rather than doing that.
                                       parsed = option.equals("purge-minimum-time") ? 2592000 : 86400;
                                       System.out.println("[CoreProtect] Invalid " + option + " value \"" + setting + "\"; using the default. Specify a unit, such as 30d, 12h or 4w.");
                                    }

                                    config.put(option, parsed);
                                 }

                                 if (option.equals("sqlite-wal")) {
                                    String setting = i2[1].trim().toLowerCase();
                                    if (setting.startsWith("t")) {
                                       config.put("sqlite-wal", 1);
                                    } else if (setting.startsWith("f")) {
                                       config.put("sqlite-wal", 0);
                                    }
                                 }

                                 if (option.equals("sqlite-busy-timeout")) {
                                    String setting = i2[1].trim();
                                    setting = setting.replaceAll("[^0-9]", "");
                                    if (setting.isEmpty()) {
                                       setting = "5000";
                                    }

                                    config.put("sqlite-busy-timeout", Integer.parseInt(setting));
                                 }

                                 if (option.equals("rollback-items")) {
                                    String setting = i2[1].trim().toLowerCase();
                                    if (setting.startsWith("t")) {
                                       config.put("rollback-items", 1);
                                    } else if (setting.startsWith("f")) {
                                       config.put("rollback-items", 0);
                                    }
                                 }

                                 if (option.equals("rollback-entities")) {
                                    String setting = i2[1].trim().toLowerCase();
                                    if (setting.startsWith("t")) {
                                       config.put("rollback-entities", 1);
                                    } else if (setting.startsWith("f")) {
                                       config.put("rollback-entities", 0);
                                    }
                                 }
                              }

                              if (option.equals("skip-generic-data")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "skip-generic-data", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "skip-generic-data", 0);
                                 }
                              }

                              if (option.equals("block-place")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "block-place", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "block-place", 0);
                                 }
                              }

                              if (option.equals("block-break")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "block-break", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "block-break", 0);
                                 }
                              }

                              if (option.equals("natural-break")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "natural-break", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "natural-break", 0);
                                 }
                              }

                              if (option.equals("block-movement")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "block-movement", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "block-movement", 0);
                                 }
                              }

                              if (option.equals("pistons")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "pistons", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "pistons", 0);
                                 }
                              }

                              if (option.equals("block-burn")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "block-burn", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "block-burn", 0);
                                 }
                              }

                              if (option.equals("block-ignite")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "block-ignite", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "block-ignite", 0);
                                 }
                              }

                              if (option.equals("explosions")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "explosions", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "explosions", 0);
                                 }
                              }

                              if (option.equals("entity-change")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "entity-change", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "entity-change", 0);
                                 }
                              }

                              if (option.equals("entity-kills")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "entity-kills", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "entity-kills", 0);
                                 }
                              }

                              if (option.equals("sign-text")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "sign-text", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "sign-text", 0);
                                 }
                              }

                              if (option.equals("buckets")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "buckets", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "buckets", 0);
                                 }
                              }

                              if (option.equals("leaf-decay")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "leaf-decay", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "leaf-decay", 0);
                                 }
                              }

                              if (option.equals("tree-growth")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "tree-growth", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "tree-growth", 0);
                                 }
                              }

                              if (option.equals("mushroom-growth")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "mushroom-growth", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "mushroom-growth", 0);
                                 }
                              }

                              if (option.equals("vine-growth")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "vine-growth", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "vine-growth", 0);
                                 }
                              }

                              if (option.equals("portals")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "portals", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "portals", 0);
                                 }
                              }

                              if (option.equals("water-flow")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "water-flow", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "water-flow", 0);
                                 }
                              }

                              if (option.equals("lava-flow")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "lava-flow", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "lava-flow", 0);
                                 }
                              }

                              if (option.equals("liquid-tracking")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "liquid-tracking", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "liquid-tracking", 0);
                                 }
                              }

                              if (option.equals("item-transactions")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "item-transactions", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "item-transactions", 0);
                                 }
                              }

                              if (option.equals("player-interactions")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "player-interactions", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "player-interactions", 0);
                                 }
                              }

                              if (option.equals("player-messages")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "player-messages", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "player-messages", 0);
                                 }
                              }

                              if (option.equals("player-commands")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "player-commands", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "player-commands", 0);
                                 }
                              }

                              if (option.equals("player-sessions")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "player-sessions", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "player-sessions", 0);
                                 }
                              }

                              if (option.equals("username-changes")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "username-changes", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "username-changes", 0);
                                 }
                              }

                              if (option.equals("worldedit")) {
                                 String setting = i2[1].trim().toLowerCase();
                                 if (setting.startsWith("t")) {
                                    config.put(key + "worldedit", 1);
                                 } else if (setting.startsWith("f")) {
                                    config.put(key + "worldedit", 0);
                                 }
                              }
                           }
                        }
                     }

                     if (key.isEmpty()) {
                        if (config_length < 1L) {
                           configfile.write(confighead.getBytes());
                        }

                        if (config.get("verbose") == null) {
                           config.put("verbose", 1);
                           configfile.seek(configfile.length());
                           configfile.write(noisy.getBytes());
                        }

                        if (config.get("use-mysql") == null) {
                           config.put("use-mysql", 0);
                           configfile.seek(configfile.length());
                           configfile.write(mysql.getBytes());
                        }

                        if (config.get("check-updates") == null) {
                           config.put("check-updates", 1);
                           configfile.seek(configfile.length());
                           configfile.write(update.getBytes());
                        }

                        if (config.get("api-enabled") == null) {
                           config.put("api-enabled", 1);
                           configfile.seek(configfile.length());
                           configfile.write(api.getBytes());
                        }

                        if (config.get("default-radius") == null) {
                           config.put("default-radius", 10);
                           configfile.seek(configfile.length());
                           configfile.write(defaultradius.getBytes());
                        }

                        if (config.get("max-radius") == null) {
                           config.put("max-radius", 100);
                           configfile.seek(configfile.length());
                           configfile.write(maxradius.getBytes());
                        }

                        if (config.get("purge-minimum-time") == null) {
                           config.put("purge-minimum-time", 2592000);
                           configfile.seek(configfile.length());
                           configfile.write(purgemin.getBytes());
                        }

                        if (config.get("purge-minimum-time-console") == null) {
                           config.put("purge-minimum-time-console", 86400);
                           configfile.seek(configfile.length());
                           configfile.write(purgeminconsole.getBytes());
                        }

                        if (config.get("sqlite-wal") == null) {
                           config.put("sqlite-wal", 1);
                           configfile.seek(configfile.length());
                           configfile.write(sqlitewal.getBytes());
                        }

                        if (config.get("sqlite-busy-timeout") == null) {
                           config.put("sqlite-busy-timeout", 5000);
                           configfile.seek(configfile.length());
                           configfile.write(sqlitebusytimeout.getBytes());
                        }

                        if (config.get("rollback-items") == null) {
                           config.put("rollback-items", 1);
                           configfile.seek(configfile.length());
                           configfile.write(rollbackitems.getBytes());
                        }

                        if (config.get("rollback-entities") == null) {
                           config.put("rollback-entities", 1);
                           configfile.seek(configfile.length());
                           configfile.write(rollbackentities.getBytes());
                        }

                        if (config.get("skip-generic-data") == null) {
                           config.put("skip-generic-data", 1);
                           configfile.seek(configfile.length());
                           configfile.write(skipgenericdata.getBytes());
                        }

                        if (config.get("block-place") == null) {
                           config.put("block-place", 1);
                           configfile.seek(configfile.length());
                           configfile.write(blockplace.getBytes());
                        }

                        if (config.get("block-break") == null) {
                           config.put("block-break", 1);
                           configfile.seek(configfile.length());
                           configfile.write(blockbreak.getBytes());
                        }

                        if (config.get("natural-break") == null) {
                           config.put("natural-break", 1);
                           configfile.seek(configfile.length());
                           configfile.write(naturalbreak.getBytes());
                        }

                        if (config.get("block-movement") == null) {
                           config.put("block-movement", 1);
                           configfile.seek(configfile.length());
                           configfile.write(blockmovement.getBytes());
                        }

                        if (config.get("pistons") == null) {
                           config.put("pistons", 1);
                           configfile.seek(configfile.length());
                           configfile.write(pistons.getBytes());
                        }

                        if (config.get("block-burn") == null) {
                           config.put("block-burn", 1);
                           configfile.seek(configfile.length());
                           configfile.write(blockburn.getBytes());
                        }

                        if (config.get("block-ignite") == null) {
                           config.put("block-ignite", 1);
                           configfile.seek(configfile.length());
                           configfile.write(blockignite.getBytes());
                        }

                        if (config.get("explosions") == null) {
                           config.put("explosions", 1);
                           configfile.seek(configfile.length());
                           configfile.write(explosions.getBytes());
                        }

                        if (config.get("entity-change") == null) {
                           config.put("entity-change", 1);
                           configfile.seek(configfile.length());
                           configfile.write(entitychange.getBytes());
                        }

                        if (config.get("entity-kills") == null) {
                           config.put("entity-kills", 1);
                           configfile.seek(configfile.length());
                           configfile.write(entitykills.getBytes());
                        }

                        if (config.get("sign-text") == null) {
                           config.put("sign-text", 1);
                           configfile.seek(configfile.length());
                           configfile.write(signtext.getBytes());
                        }

                        if (config.get("buckets") == null) {
                           config.put("buckets", 1);
                           configfile.seek(configfile.length());
                           configfile.write(buckets.getBytes());
                        }

                        if (config.get("leaf-decay") == null) {
                           config.put("leaf-decay", 1);
                           configfile.seek(configfile.length());
                           configfile.write(leafdecay.getBytes());
                        }

                        if (config.get("tree-growth") == null) {
                           config.put("tree-growth", 1);
                           configfile.seek(configfile.length());
                           configfile.write(treegrowth.getBytes());
                        }

                        if (config.get("mushroom-growth") == null) {
                           config.put("mushroom-growth", 1);
                           configfile.seek(configfile.length());
                           configfile.write(mushroomgrowth.getBytes());
                        }

                        if (config.get("vine-growth") == null) {
                           config.put("vine-growth", 1);
                           configfile.seek(configfile.length());
                           configfile.write(vinegrowth.getBytes());
                        }

                        if (config.get("portals") == null) {
                           config.put("portals", 1);
                           configfile.seek(configfile.length());
                           configfile.write(portals.getBytes());
                        }

                        if (config.get("water-flow") == null) {
                           config.put("water-flow", 1);
                           configfile.seek(configfile.length());
                           configfile.write(waterflow.getBytes());
                        }

                        if (config.get("lava-flow") == null) {
                           config.put("lava-flow", 1);
                           configfile.seek(configfile.length());
                           configfile.write(lavaflow.getBytes());
                        }

                        if (config.get("liquid-tracking") == null) {
                           config.put("liquid-tracking", 1);
                           configfile.seek(configfile.length());
                           configfile.write(liquidtracking.getBytes());
                        }

                        if (config.get("item-transactions") == null) {
                           config.put("item-transactions", 1);
                           configfile.seek(configfile.length());
                           configfile.write(itemlogging.getBytes());
                        }

                        if (config.get("player-interactions") == null) {
                           config.put("player-interactions", 1);
                           configfile.seek(configfile.length());
                           configfile.write(playerinteract.getBytes());
                        }

                        if (config.get("player-messages") == null) {
                           config.put("player-messages", 1);
                           configfile.seek(configfile.length());
                           configfile.write(playermessages.getBytes());
                        }

                        if (config.get("player-commands") == null) {
                           config.put("player-commands", 1);
                           configfile.seek(configfile.length());
                           configfile.write(playercommands.getBytes());
                        }

                        if (config.get("player-sessions") == null) {
                           config.put("player-sessions", 1);
                           configfile.seek(configfile.length());
                           configfile.write(playersessions.getBytes());
                        }

                        if (config.get("username-changes") == null) {
                           config.put("username-changes", 1);
                           configfile.seek(configfile.length());
                           configfile.write(usernamechanges.getBytes());
                        }

                        if (config.get("worldedit") == null) {
                           config.put("worldedit", 1);
                           configfile.seek(configfile.length());
                           configfile.write(worldedit.getBytes());
                        }
                     }

                     configfile.close();
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               }
            }
         }

         if ((Integer)config.get("use-mysql") == 0) {
            prefix = "co_";
         }

         loadBlacklist();
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public static void loadDatabase() {
      if ((Integer)config.get("use-mysql") == 0) {
         try {
            File tempFile = File.createTempFile("CoreProtect_" + System.currentTimeMillis(), ".tmp");
            tempFile.setExecutable(true);
            if (!tempFile.canExecute()) {
               File tempFolder = new File("cache");
               boolean exists = tempFolder.exists();
               if (!exists) {
                  tempFolder.mkdir();
               }

               System.setProperty("java.io.tmpdir", "cache");
            }

            tempFile.delete();
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      if (server_running) {
         Consumer.resetConnection = true;
      }

      Functions.createDatabaseTables(prefix, false);
   }

   private static void loadTypes(Statement statement) {
      try {
         materials.clear();
         materials_reversed.clear();
         material_id = 0;
         String query = "SELECT id,material FROM " + prefix + "material_map";
         ResultSet rs = statement.executeQuery(query);

         while (rs.next()) {
            int id = rs.getInt("id");
            String material = rs.getString("material");
            materials.put(material, id);
            materials_reversed.put(id, material);
            if (id > material_id) {
               material_id = id;
            }
         }

         rs.close();
         Config.art.clear();
         art_reversed.clear();
         art_id = 0;
         query = "SELECT id,art FROM " + prefix + "art_map";
         rs = statement.executeQuery(query);

         while (rs.next()) {
            int id = rs.getInt("id");
            String art = rs.getString("art");
            Config.art.put(art, id);
            art_reversed.put(id, art);
            if (id > art_id) {
               art_id = id;
            }
         }

         rs.close();
         entities.clear();
         entities_reversed.clear();
         entity_id = 0;
         query = "SELECT id,entity FROM " + prefix + "entity_map";
         rs = statement.executeQuery(query);

         while (rs.next()) {
            int id = rs.getInt("id");
            String entity = rs.getString("entity");
            entities.put(entity, id);
            entities_reversed.put(id, entity);
            if (id > entity_id) {
               entity_id = id;
            }
         }

         rs.close();
      } catch (Exception e) {
         e.printStackTrace();
      }

      BlockInfo.loadData();
   }

   private static void loadWorlds(Statement statement) {
      try {
         Config.worlds.clear();
         worlds_reversed.clear();
         world_id = 0;
         String query = "SELECT id,world FROM " + prefix + "world";
         ResultSet rs = statement.executeQuery(query);

         while (rs.next()) {
            int id = rs.getInt("id");
            String world = rs.getString("world");
            Config.worlds.put(world, id);
            worlds_reversed.put(id, world);
            if (id > world_id) {
               world_id = id;
            }
         }

         for (World world : CoreProtect.getInstance().getServer().getWorlds()) {
            String worldname = world.getName();
            if (Config.worlds.get(worldname) == null) {
               int id = world_id + 1;
               Config.worlds.put(worldname, id);
               worlds_reversed.put(id, worldname);
               world_id = id;
               Queue.queueWorldInsert(id, worldname);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public static boolean performInitialization() {
      try {
         BukkitAdapter.loadAdapter();
         loadConfig();
         loadDatabase();
         Connection connection = Database.getConnection(true);
         Statement statement = connection.createStatement();
         checkPlayers(connection);
         loadWorlds(statement);
         loadTypes(statement);
         if (Functions.checkWorldEdit()) {
            Functions.loadWorldEdit();
         }

         server_running = true;
         boolean validVersion = Patch.versionCheck(statement);
         statement.close();
         connection.close();
         return validVersion;
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
   }
}
