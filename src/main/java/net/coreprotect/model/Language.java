package net.coreprotect.model;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.coreprotect.CoreProtect;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Loads user-facing text from plugins/CoreProtect/language.yml.
 *
 * A message is a template built from three kinds of token, all written as
 * {NAME} and all resolved by {@link #get(String, Object...)}:
 *
 *   style   -- {PRIMARY}, {SECONDARY}, {TERTIARY}, {ERROR}, {ITALIC}, ...
 *              defined under colors: in the language file
 *   symbol  -- {PREFIX}, {PLUGIN_NAME}, {SEPARATOR}, ...
 *              defined under symbols:, and free to reference style tokens so
 *              the whole plugin can be re-themed by editing two colour values
 *   runtime -- {0}, {1}, ... filled from the varargs at the call site
 *
 * Colour codes are written with &amp; in the file and translated to the section
 * sign on load, so the file stays editable in any text editor.
 *
 * Keys missing from the on-disk file fall back to the copy shipped inside the
 * jar, so adding messages in a later version never breaks an existing install.
 * Every API used here exists in Bukkit 1.8.8.
 */
public class Language {
   private static final String FILE_NAME = "language.yml";
   private static final int MAX_TOKEN_DEPTH = 8;

   private static volatile Map<String, String> messages = Collections.emptyMap();
   private static volatile Map<String, String> defaults = Collections.emptyMap();
   private static volatile Map<String, String> tokens = Collections.emptyMap();

   /**
    * (Re)reads language.yml, writing the bundled copy out first if the operator
    * does not have one yet. Safe to call again for /co reload; readers see
    * either the old maps or the new ones, never a half-built map.
    */
   public static void load() {
      Map<String, String> newDefaults = new HashMap<String, String>();
      Map<String, String> newMessages = new HashMap<String, String>();
      Map<String, String> newTokens = new LinkedHashMap<String, String>();

      try {
         YamlConfiguration bundled = loadBundled();
         if (bundled != null) {
            readMessages(bundled, newDefaults);
         }

         File file = new File(CoreProtect.getInstance().getDataFolder(), FILE_NAME);
         if (!file.exists()) {
            CoreProtect.getInstance().saveResource(FILE_NAME, false);
         }

         YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(file);
         readMessages(onDisk, newMessages);
         readTokens(onDisk, bundled, newTokens);
      } catch (Exception e) {
         e.printStackTrace();
      }

      defaults = newDefaults;
      messages = newMessages;
      tokens = newTokens;
   }

   private static YamlConfiguration loadBundled() {
      try {
         InputStream stream = CoreProtect.getInstance().getResource(FILE_NAME);
         if (stream == null) {
            return null;
         }

         try {
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, "UTF-8"));
         } finally {
            stream.close();
         }
      } catch (Exception e) {
         return null;
      }
   }

   private static void readMessages(ConfigurationSection root, Map<String, String> into) {
      ConfigurationSection section = root.getConfigurationSection("messages");
      if (section == null) {
         return;
      }

      for (String key : section.getKeys(true)) {
         String value = section.getString(key);
         if (value != null) {
            into.put(key, value);
         }
      }
   }

   /**
    * Style and symbol tokens share one namespace so a message does not need to
    * know which section a token came from. On-disk values win; anything the
    * operator has not overridden falls back to the bundled file.
    */
   private static void readTokens(ConfigurationSection onDisk, ConfigurationSection bundled, Map<String, String> into) {
      String[] sections = new String[] { "colors", "symbols" };

      for (String name : sections) {
         if (bundled != null) {
            copyTokens(bundled.getConfigurationSection(name), into);
         }

         copyTokens(onDisk.getConfigurationSection(name), into);
      }
   }

   private static void copyTokens(ConfigurationSection section, Map<String, String> into) {
      if (section == null) {
         return;
      }

      for (String key : section.getKeys(false)) {
         String value = section.getString(key);
         if (value != null) {
            into.put(key.toUpperCase(), value);
         }
      }
   }

   /**
    * Resolves a message key into finished, coloured text.
    *
    * Unknown keys return the key itself in brackets rather than throwing or
    * returning null -- a visible but harmless marker beats a chat message that
    * silently disappears or an exception inside a listener.
    */
   public static String get(String key, Object... args) {
      String template = messages.get(key);
      if (template == null) {
         template = defaults.get(key);
      }

      if (template == null) {
         return "[" + key + "]";
      }

      return ChatColor.translateAlternateColorCodes('&', resolve(template, args, 0));
   }

   /**
    * Single pass over the template. Runtime arguments are inserted verbatim and
    * never rescanned, so a player name containing {PRIMARY} cannot inject
    * colour, and a token that expands to {0} cannot consume an argument.
    */
   private static String resolve(String template, Object[] args, int depth) {
      if (template.indexOf('{') < 0) {
         return template;
      }

      StringBuilder out = new StringBuilder(template.length() + 32);
      int i = 0;

      while (i < template.length()) {
         char c = template.charAt(i);
         if (c != '{') {
            out.append(c);
            ++i;
            continue;
         }

         int close = template.indexOf('}', i + 1);
         if (close < 0) {
            out.append(template, i, template.length());
            break;
         }

         String name = template.substring(i + 1, close);
         String replacement = lookup(name, args, depth);
         if (replacement == null) {
            // Not a token we know: leave the braces alone so the text still
            // reads sensibly instead of vanishing.
            out.append(template, i, close + 1);
         } else {
            out.append(replacement);
         }

         i = close + 1;
      }

      return out.toString();
   }

   private static String lookup(String name, Object[] args, int depth) {
      if (name.length() > 0 && isDigits(name)) {
         try {
            int index = Integer.parseInt(name);
            if (args != null && index >= 0 && index < args.length) {
               Object value = args[index];
               return value == null ? "" : value.toString();
            }
         } catch (NumberFormatException e) {
            return null;
         }

         return "";
      }

      String token = tokens.get(name.toUpperCase());
      if (token == null) {
         return null;
      }

      // Symbols may be built from other tokens (PREFIX is the obvious one).
      // Bounded so a self-referential edit in the language file cannot hang the
      // server thread that is trying to send a message.
      if (depth >= MAX_TOKEN_DEPTH) {
         return token;
      }

      return resolve(token, args, depth + 1);
   }

   private static boolean isDigits(String value) {
      for (int i = 0; i < value.length(); ++i) {
         if (value.charAt(i) < '0' || value.charAt(i) > '9') {
            return false;
         }
      }

      return true;
   }
}
