package com.vertx.filemerger.verticles;

import io.vertx.core.*;
import io.vertx.core.Future;
import com.hubrick.vertx.s3.client.S3ClientOptions;
import com.hubrick.vertx.s3.client.S3Client;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.json.*;
import com.hubrick.vertx.s3.model.request.AdaptiveUploadRequest;
import java.util.logging.*;

class UploadToS3 {

  private static Logger logger = Logger.getLogger(UploadToS3.class.getName());
  final S3Client s3Client;
  final JsonObject awsConfig;

  public UploadToS3(Vertx vertx, JsonObject config) {
    awsConfig = config;
    final S3ClientOptions clientOptions = new S3ClientOptions()
    .setAwsRegion(config.getString("AwsRegion"))
    .setAwsServiceName(config.getString("AwsServiceName"))
    .setAwsAccessKey(config.getString("AwsAccessKey"))
    .setAwsSecretKey(config.getString("AwsSecretKey"))
    .setGlobalTimeoutMs(30000L)
    .setSignPayload(true);
    s3Client = new S3Client(vertx, clientOptions);
  }

  public void upload(Future<JsonObject> future, Vertx vertx, JsonObject mediaInfo) {
    String inputFileName = mediaInfo.getString("fileName");
    String filePath = "inputs/output/"+inputFileName;
    vertx.fileSystem().open(filePath, new OpenOptions().setRead(true), asyncFile -> {
      if(asyncFile.succeeded()) {
        System.out.println("contentType===>"+ mediaInfo.getString("contentType")+ "/" + inputFileName.split("[.]")[1]);
        AsyncFile file =  asyncFile.result();
        s3Client.adaptiveUpload(
        "ms-trainee",
        "split/shankar/"+inputFileName,
        new AdaptiveUploadRequest(file).withContentType(mediaInfo.getString("contentType")+ "/" + inputFileName.split("[.]")[1]),
        response -> {
          logger.info(() -> "AWS RESPONCE ====> " + response);
          future.complete( new JsonObject().put("fileName", inputFileName)
          .put("awsUrl", awsConfig.getString("s3Path") + inputFileName )
          .put("status","done"));
        },
        error -> {
          logger.severe(() -> "AWS ERROR ====> "+ error);
          future.complete(new JsonObject().put("status","error"));
          error.printStackTrace();
        });
      }
      else {
        logger.severe(() -> "MediaUpload[" + inputFileName + "] Read error while UploadToS3.");
      }
    });
  }
}
