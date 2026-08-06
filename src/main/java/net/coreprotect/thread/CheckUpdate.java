package net.coreprotect.thread;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import net.coreprotect.CoreProtect;
import net.coreprotect.Functions;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class CheckUpdate implements Runnable {
   private boolean startup = true;
   private static String latestVersion = null;

   public CheckUpdate(boolean startup) {
      this.startup = startup;
   }

   public static String latestVersion() {
      return latestVersion;
   }

   public void run() {
      try {
         int status = 0;
         HttpURLConnection connection = null;
         String version = Functions.getPluginVersion();

         try {
            URL url = new URL("https://api.curseforge.com/servermods/files?projectIds=37375");
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("X-API-Key", "dd6940ea59515bc48f617e3cd12d923e5eb7dab4");
            connection.setRequestProperty("User-Agent", "CoreProtect/v" + version + " (by Intelli)");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.connect();
            status = connection.getResponseCode();
         } catch (Exception var11) {
         }

         if (status == 200) {
            try {
               BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
               String response = reader.readLine();
               JSONArray array = (JSONArray)JSONValue.parse(response);
               if (array.size() > 0) {
                  String remoteVersion = ((String)((JSONObject)array.get(array.size() - 1)).get("name")).replaceAll("[^0-9.]", "");
                  if (remoteVersion.contains(".")) {
                     Thread.sleep(2000L);
                     boolean newVersion = Functions.newVersion(version, remoteVersion);
                     if (newVersion) {
                        latestVersion = remoteVersion;
                        if (this.startup) {
                           System.out.println("--------------------");
                           System.out.println("[CoreProtect] Version " + remoteVersion + " is now available.");
                           System.out.println("[CoreProtect] Download: Type \"/co version\" in-game.");
                           System.out.println("--------------------");
                           System.out.println("[Sponsor] Unlimited MC Hosting: www.hosthorde.com");
                           System.out.println("--------------------");
                        }
                     } else {
                        latestVersion = null;
                     }
                  }
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         }

         try {
            int port = CoreProtect.getInstance().getServer().getPort();
            String stats = port + ":" + version;
            URL url = new URL("http://stats.coreprotect.net/u/?data=" + stats);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("User-Agent", "CoreProtect");
            connection.setConnectTimeout(5000);
            connection.connect();
            connection.getResponseCode();
            connection.disconnect();
         } catch (Exception var9) {
         }
      } catch (Exception e) {
         System.err.println("[CoreProtect] An error occurred while checking for updates.");
         e.printStackTrace();
      }

   }
}
