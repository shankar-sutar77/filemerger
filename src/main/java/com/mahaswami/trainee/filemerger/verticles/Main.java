package com.mahaswami.trainee.filemerger.verticles;

import com.mahaswami.trainee.filemerger.verticles.*;
import io.vertx.core.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.*;;
import io.vertx.config.*;
import io.vertx.core.file.*;
import java.util.function.Supplier;
import java.util.logging.*;


public class Main extends AbstractVerticle {

  private static Logger logger = Logger.getLogger(FileMerger.class.getName());

  public void start() throws Exception {
    logger.info(() ->"Basic verticle started...!!!!");
    try {
      ConfigStoreOptions json = new ConfigStoreOptions()
      .setType("file")
      .setFormat("json")
      .setConfig(new JsonObject().put("path", System.getenv("VERTX_CONFIG_PATH")));
      ConfigRetrieverOptions configOptions = new ConfigRetrieverOptions().addStore(json);
      ConfigRetriever retriever = ConfigRetriever.create(vertx, configOptions);
      retriever.getConfig(ar -> {
        if(ar.succeeded()) {
          //logger.info(() ->"CONFIG SHANKAR===>"+ar.result());
          DeploymentOptions options = new DeploymentOptions().setConfig(ar.result());
          vertx.deployVerticle(new FileMerger(), options, res -> {
            logger.info(() ->"FileMerger===>"+res);
            if (res.succeeded()) {
               vertx.deployVerticle(new Server(), options, test ->{
                 if(test.succeeded()) {
                   logger.info(() ->"Server verticle deployed successfully...!");
                 } else {
                   logger.severe(() -> "Server Failed to START::" + test.cause());
                 }
               });
            } else {
              logger.severe(() -> "FileMerger Deployment failed!");
            }
          });
        } else {
          logger.severe(() -> "configuration not avalable::" + ar.cause());
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
      logger.severe(() -> "Exception In Main VERTICLE::" + e);
    }
  }
}
