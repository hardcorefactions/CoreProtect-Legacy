package net.coreprotect.thread;

import java.util.Iterator;
import java.util.Map;
import net.coreprotect.model.Config;

public class CacheCleanUp implements Runnable {
   public void run() {
      while(Config.server_running) {
         try {
            for(int c = 0; c < 4; ++c) {
               Thread.sleep(5000L);
               int scan_time = 30;
               Map<String, Object[]> cache = Config.lookup_cache;
               switch (c) {
                  case 1:
                     cache = Config.break_cache;
                     break;
                  case 2:
                     cache = Config.piston_cache;
                     break;
                  case 3:
                     cache = Config.entity_cache;
                     scan_time = 3600;
               }

               int timestamp = (int)(System.currentTimeMillis() / 1000L) - scan_time;
               Iterator<Map.Entry<String, Object[]>> it = cache.entrySet().iterator();

               while(it.hasNext()) {
                  try {
                     Map.Entry<String, Object[]> entry = (Map.Entry)it.next();
                     Object[] data = entry.getValue();
                     int time = (Integer)data[0];
                     if (time < timestamp) {
                        try {
                           it.remove();
                        } catch (Exception ignored) {
                        }
                     }
                  } catch (Exception var11) {
                     break;
                  }
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

   }
}
