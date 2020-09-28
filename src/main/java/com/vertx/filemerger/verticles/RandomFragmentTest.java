/* eventBus Table
1.media.process.fragments
1.1 Output Params
1.1.1 filename.
1.1.2 fragmentsCount
1.1.3 fragmentNumber
1.1.4 fragmentContent
1.2 Send Reply => No
2.test.delete.status
2.1 Input Params
2.1.1 filename
2.1.2 fragmentsCount
2.2 SendReply => No
3.media.delete
3.1 Output Params
3.1.1 filename
*/

package com.vertx.filemerger.verticles;

import java.util.Random;
import java.util.ArrayList;
import java.util.Base64;
import java.io.*;
import io.vertx.core.*;
import io.vertx.core.json.*;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.*;
import io.vertx.core.logging.*;

public class MyTest extends AbstractVerticle {
  int start = 0;
  int fileSize = 0;
  int count = 0;
  int randomFragment = 0;
  int currentFragments;
  int totalFragments;
  int total_count = 0;
  final int chunckSize = 256 * 1000;
  LocalMap<String, String>  status_map;
  String imagesPath;
  String fragmentsPath;

  public void start()throws Exception {

    JsonObject configPath = config().getJsonObject("filesPath");
    imagesPath = configPath.getString("imagesPath");
    fragmentsPath = configPath.getString("fragmentsPath");

    status_map = vertx.sharedData().getLocalMap("mymap1");
    File[] files = new File(imagesPath).listFiles();
    Random random = new Random();
    vertx.setPeriodic(1, id -> {

      ArrayList<File> fileList = new ArrayList<File>();
      File file = files[random.nextInt(files.length)];
      String fileName = file.getName();

      if(status_map.get(fileName) == null) {
        status_map.put(fileName,"true");
        vertx.fileSystem().readFile(file.getPath(), content -> {
          if(content.succeeded()) {
            Buffer buffer = Buffer.buffer(content.result().getBytes());
            fileSize = buffer.length();
            total_count = (fileSize % chunckSize) == 0 ? (fileSize / chunckSize) : (fileSize / chunckSize) + 1;
            totalFragments += total_count;
            status_map.put(fileName + "total_count", String.valueOf(total_count));
            for(int index = 1; index <= total_count ;index ++) {
              vertx.fileSystem().open(fragmentsPath + fileName + "_" +index, new OpenOptions(), asyncFile ->{
                if(asyncFile.succeeded()) {
                  int end = 0;
                  int fragment;
                  if(fileSize < chunckSize) {
                    end = fileSize;
                  }
                  else {
                    if((fragment = (fileSize - start)) >= chunckSize) {
                      end = chunckSize;
                    }
                    else {
                      end = fragment;
                    }
                  }
                  asyncFile.result().write(buffer.slice(start, start + end));
                  count ++;
                  start += chunckSize;
                  if(count == total_count) {
                    // System.out.println("FILE DEVIDED...!");
                    status_map.put(fileName + "fragmented", "fragmented");
                    start = 0;
                    count = 0;
                  }
                }
              });
            }
          }
        });
      }//end if
      else {
        if(status_map.get(fileName + "fragmented") != null) {
          randomFragment = random.nextInt(Integer.parseInt(status_map.get(fileName+"total_count"))) + 1;
          if(status_map.get(fileName + "_" + randomFragment) == null ) {
            status_map.put(fileName + "_" +randomFragment,"added");
            status_map.put(fileName + "_currentFragmentCount", String.valueOf(++currentFragments));
            this.sendFragments(fileName, randomFragment);
          }
        }
      }
    });
  }//end AbstractVerticle

  void sendFragments(String fileName, int randomFragment) {
    vertx.fileSystem().readFile(fragmentsPath + fileName+"_"+randomFragment, content -> {
      if(content.succeeded()) {
        // System.out.println("FRAGMENT SENT ----->" + fileName + "_" + randomFragment);
        JsonObject fragmentInfo = new JsonObject()
        .put("fileName",fileName)
        .put("fragmentsCount", Integer.parseInt(status_map.get(fileName + "total_count")))
        .put("fragmentNumber",randomFragment)
        .put("contentType","image")
        .put("fragmentContent",content.result().getBytes());
        vertx.eventBus().send("media.process.fragments",fragmentInfo.encode());
      }
    });
  }
}
