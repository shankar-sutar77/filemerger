package com.vertx.filemerger.verticles;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

import java.util.logging.Logger;


public class Main extends AbstractVerticle {

    private static Logger logger = Logger.getLogger(FileMerger.class.getName());

    public void start() throws Exception {
        logger.info(() -> "Basic verticle started...!!!!");
        try {
            ConfigStoreOptions configStoreOptions = new ConfigStoreOptions()
                    .setType("file")
                    .setFormat("json")
                    .setConfig(new JsonObject().put("path", System.getenv("VERTX_CONFIG_PATH")));

            ConfigRetrieverOptions configOptions = new ConfigRetrieverOptions().addStore(configStoreOptions);

            ConfigRetriever retriever = ConfigRetriever.create(vertx, configOptions);

            retriever.getConfig(ar -> {
                if (ar.succeeded()) {
                    //logger.info(() ->"CONFIG SHANKAR===>"+ar.result());
                    DeploymentOptions options = new DeploymentOptions().setConfig(ar.result());
                    vertx.deployVerticle(new FileMerger(), options, res -> {
                        logger.info(() -> "FileMerger===>" + res);
                        if (res.succeeded()) {
                            vertx.deployVerticle(new Server(), options, test -> {
                                if (test.succeeded()) {
                                    logger.info(() -> "Server verticle deployed successfully...!");
                                } else {
                                    logger.severe(() -> "Server Failed to START::" + test.cause());
                                }
                            });
                        } else {
                            logger.severe(() -> "FileMerger Deployment failed!");
                        }
                    });
                } else {
                    logger.severe(() -> "configuration not available::" + ar.cause());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(() -> "Exception In Main VERTICLE::" + e);
        }
    }
}
