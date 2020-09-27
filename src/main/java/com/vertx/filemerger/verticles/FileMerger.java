/* eventBus Table
1.media.process.fragments
1.1 Input Params
1.1.1 filename.
1.1.2 fragmentsCount
1.1.3 fragmentNumber
1.1.4 fragmentContent
1.2 Send Reply => No
2.test.delete.status
2.1 Output Params
2.1.1 filename
2.1.2 fragmentsCount
2.2 SendReply => No
3.media.delete
3.1 Input Params
3.1.1 filename
3.2 SendReply => YES
3.2.1 filename
*/
package com.vertx.filemerger.verticles;

import java.util.logging.*;
import io.vertx.core.*;
import io.vertx.core.Future;
import io.vertx.core.json.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.*;
import io.vertx.ext.sql.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.shareddata.*;


interface GenericCallback<T> {
  void perform(T result);
}

public class FileMerger extends AbstractVerticle {

  private static Logger logger = Logger.getLogger(FileMerger.class.getName());
  private Database database;
  private int fileMergeCount = 0;
  private UploadToS3 s3Client = null;
  private EventBus eventBus;
  private ResultSet resultset;
  private SharedData sd;
  private String tempPath;
  private String outputPath;
  private int receviedFragmentsCount = 0;
  private Boolean isAlreadyUploaded = false;
  private int target;

  void initialize() {
    eventBus = vertx.eventBus();
    sd =  vertx.sharedData();
    s3Client = new UploadToS3(vertx, config().getJsonObject("awsconfig"));
    database = new Database(vertx, config());
    JsonObject configPath = config().getJsonObject("filesPath");
    tempPath = configPath.getString("tempPath");
    outputPath = configPath.getString("outputPath");
  }

  public void start() {
    try {
      this.initialize();
      this.generateMediaProceessHandler();
      this.generateMediaDeleteHandler();
    } catch(Exception e) {
      logger.severe(() -> "ERROR::" + e);
    }
  }//end start

  void generateMediaProceessHandler() {

    eventBus.consumer("media.process.fragments", message -> {
      JsonObject fragmentInfo = new JsonObject(message.body().toString());
      String fileName = fragmentInfo.getString("fileName");
      int fragmentNumber = fragmentInfo.getInteger("fragmentNumber");
      logger.info(() -> "FRAGMENT RECEVIED ---> " + fileName + "_" +fragmentNumber);

      sd.getLock(fileName, res -> {
        if(res.succeeded()) {
          Lock lock = res.result();
          this.selectOrInsertMedia(lock, fragmentInfo, selectResult -> {
            this.createFragment(fragmentInfo, createFragment -> {
              if(createFragment) {
                this.updateFragments(lock, selectResult, fragmentInfo, updateResult -> {
                  if(isAllFragmentsReceived(fragmentInfo)) {
                    this.performingFileMerge(fragmentInfo, mergeFileName -> {
                      this.uploadToS3(fragmentInfo, awsResult -> {
                          if(awsResult.getString("status").equals("error")){
                            this.sendAcknowledgement(fragmentInfo.put("mediaStatus","error"));
                          } else {
                            this.updateAwsUrlAndStatus(awsResult);
                            this.sendAcknowledgement(fragmentInfo.put("mediaStatus","uploaded"));
                          }
                      });
                    });
                  }
                });
              }
            });
          });
        } else {
          logger.severe(() -> "ERROR IN LOCK ::" + res.cause());
        }
      });
    });
  }

  Boolean isAllFragmentsReceived(JsonObject fragmentInfo) {
    int totalFragments = fragmentInfo.getInteger("fragmentsCount");
    JsonArray currentFragments = fragmentInfo.getJsonArray("currentFragments");
    if( totalFragments == currentFragments.size()) {
      return true;
    } else {
      return false;
    }
  }

  void sendAcknowledgement(JsonObject fragmentInfo) {
    String fileName = fragmentInfo.getString("fileName");
    String mediaStatus = fragmentInfo.getString("mediaStatus");
    final String textHandlerID = fragmentInfo.getString("textHandlerID");
    JsonObject acknowledgement = new JsonObject()
    .put("fileName", fileName)
    .put("mediaStatus", mediaStatus);
    logger.info(() -> "SENT sendAcknowledgement.................!");
    vertx.eventBus().publish(textHandlerID, acknowledgement.toString());
  }

  void generateMediaDeleteHandler() {
    eventBus.consumer("media.delete", message -> {
      String fileName = message.body().toString();
      database.updateQuery("delete from media where filename='" + fileName + "'", delete -> {
        if(delete){
          logger.info(() -> "File " + fileName + " Deleted from database...!");
          message.reply(new JsonObject().put("status",true)
          .put("filename",fileName));
        } else {
          logger.severe(() -> "File " + fileName + " Not Deleted from database...!");
          message.reply(new JsonObject().put("status",false));
        }
      });
    });
  }


  void selectOrInsertMedia(Lock lock, JsonObject fragmentInfo, GenericCallback<ResultSet> callback) {
    String fileName = fragmentInfo.getString("fileName");
    String selectFileName = "select *from media where filename='"+ fileName +"'";
    database.selectQuery(selectFileName, resultset -> {
      logger.info(() -> "SELCTED MEDIA::" + fileName);
      if(resultset.getNumRows() == 0) {
        this.insertMedia(lock, fragmentInfo);
      } else {
        callback.perform(resultset);
      }
    });
  }

  void insertMedia(Lock lock, JsonObject fragmentInfo) {

    String fileName = fragmentInfo.getString("fileName");
    int fragmentCount = fragmentInfo.getInteger("fragmentsCount");
    int fragmentNumber = fragmentInfo.getInteger("fragmentNumber");
    String contentType = fragmentInfo.getString("contentType");
    String sender = fragmentInfo.getString("sender");
    String receiver = fragmentInfo.getString("receiver");
    Buffer fragmentContent = Buffer.buffer(fragmentInfo.getBinary("fragmentContent"));

    JsonArray fragmentsArray = new JsonArray().add(fragmentNumber);
    String insertQuery = "insert into media(filename,total_count,fragments,file_size,content_type,status,s3_url,created_at,updated_at,receiver,sender) "+
    "values('" + fileName + "','" + fragmentCount + "','" + fragmentsArray + "','"
    + 0 +"','" + contentType + "','pending','" + "S3 url',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'"+ receiver +"','" +sender+ "')";

    this.createFragment(fragmentInfo, create -> {
      logger.info(() -> "FIRST FRAGMENT CREATED.");
      database.insertQuery(insertQuery, insert -> {
        lock.release();
      });
    });
  }

  void performingFileMerge(JsonObject fragmentInfo, GenericCallback<String> callback) {
    logger.info(() -> "Performing merging.....!");
    String fileName = fragmentInfo.getString("fileName");
    int fragmentCount = fragmentInfo.getInteger("fragmentsCount");
    JsonArray currentFragments = fragmentInfo.getJsonArray("currentFragments");
    if(currentFragments.contains(0)) {
      target = 0;
    } else {
      target = 1;
    }
    vertx.fileSystem().open(outputPath + fileName, new OpenOptions(), asyncFile -> {
      AsyncFile file = asyncFile.result();
      for(int index = target ; index <= fragmentCount; index++) {
        vertx.fileSystem().readFile(tempPath + fileName + "_" + index, tempFile -> {
          if(tempFile.succeeded()) {
            file.write(tempFile.result());
            fileMergeCount++;
            if(fileMergeCount == fragmentCount){
              fileMergeCount = 0;
              logger.info(() -> "FILE "+ fileName + " Merged Complete.");
              callback.perform(fileName);
            }
          }
        });
      }
    });
  }

  void uploadToS3(JsonObject fragmentInfo, GenericCallback<JsonObject> callback) {
    Future<JsonObject> futureAwsURL = Future.future();
    s3Client.upload(futureAwsURL, vertx, fragmentInfo);
    futureAwsURL.setHandler( getawsURL -> {
      callback.perform(getawsURL.result());
    });
  }

  void updateAwsUrlAndStatus(JsonObject awsResult) {
    String fileName = awsResult.getString("fileName");
    String awsUrl = awsResult.getString("awsUrl");
    vertx.fileSystem().props(outputPath + fileName , props -> {
      if(props.succeeded()) {
        String updateStatus = "update media set status='done'"
        + ", file_size = '"+props.result().size()+"', s3_url='" + awsUrl + "' where filename = '"+ fileName +"'";
        database.updateQuery(updateStatus, updateAwsUrl -> {
          logger.info(() -> "File " + fileName + " successfully Uploaded to AWS cloud.");
        });
      } else {
        logger.severe(() -> "FILE PROPS READ ERROR::" + props.cause());
      }
    });
  }

  void createFragment(JsonObject fragmentInfo, GenericCallback<Boolean> callback) {
    String fileName = fragmentInfo.getString("fileName");
    int fragmentNumber = fragmentInfo.getInteger("fragmentNumber");
    Buffer fragmentContent = Buffer.buffer(fragmentInfo.getBinary("fragmentContent"));

    vertx.fileSystem().writeFile(tempPath + fileName+"_"+fragmentNumber, fragmentContent, asyncFile -> {
      if(asyncFile.succeeded()) {
        logger.info(() -> "FRAGMENT CREATED = " +fileName + "_" + fragmentNumber);
        callback.perform(true);
      } else {
        logger.severe(() -> "ERROR IN CREATE FRAGMENT = " + fileName + "_" + fragmentNumber);
        callback.perform(false);
      }
    });
  }

  void updateFragments(Lock lock, ResultSet resultset, JsonObject fragmentInfo, GenericCallback<JsonObject> callback) {
    try {
      String fileName = fragmentInfo.getString("fileName");
      int fragmentCount = fragmentInfo.getInteger("fragmentsCount");
      int fragmentNumber = fragmentInfo.getInteger("fragmentNumber");
      String status = resultset.getRows().get(0).getString("status");
      String receiver = fragmentInfo.getString("receiver");
      String mediaReceiver = resultset.getRows().get(0).getString("receiver");
      if(status.equals("done") && receiver.equals(mediaReceiver)) {
        lock.release();
        this.sendAcknowledgement(fragmentInfo.put("mediaStatus", "done"));
      } else {
        JsonArray currentFragments = new JsonArray(resultset.getRows().get(0).getString("fragments"));
        if(fragmentCount == 1) {
          callback.perform(fragmentInfo.put("currentFragments",currentFragments));
        } else {
          if(currentFragments.contains(fragmentNumber) == false ) {
            currentFragments.add(fragmentNumber);
            String updateFragments = "update media set fragments='" + currentFragments + "' where filename='" + fileName + "'";
            database.updateQuery(updateFragments, updtae -> {
              lock.release();
              logger.info(() -> "AVALABLE FRAGMENTS SIZE ===> " + currentFragments.size());
              logger.info(() -> "MEDIA UPDATED " + fileName + " WITH FRAGMENT NUMBER = " + fragmentNumber);
              if(currentFragments.size() == fragmentCount) {
                logger.info(() -> "ALL FRAGMENTS RECEVIED ==> " + fileName);
                callback.perform(fragmentInfo.put("currentFragments",currentFragments));
              }
            });
          } else {
            lock.release();
          }
        }
      }
    } catch(Exception e) {
      lock.release();
      logger.severe(() -> "ERROR IN UPDATE FRAGMENT::" + e);
    }
  }
}
