package com.mahaswami.trainee.filemerger.verticles;

import java.util.Random;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.file.FileSystem;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.shareddata.SharedData;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.logging.*;

public class SubbuTest extends AbstractVerticle{
	final int chunkSize = 25 * 1000; //256kB
	Logger logger = LoggerFactory.getLogger(SubbuTest.class);
	String path = "inputs/images";

	@Override
	public void start(){
		vertx.fileSystem().readDir(path, res -> {
			if (res.succeeded()) {
				int count = 0;
				Future futur = Future.future();
				while(count < res.result().size()){
					String filePath = res.result().get(count);
					System.out.println(filePath);
					String fileName = filePath.split("/")[filePath.split("/").length - 1];
					createFragments(futur, fileName);
					if(count == res.result().size() - 1){
						futur.complete();
					}
					count ++;
				}
				futur.setHandler(ar -> {
					sendFragments(path);
				});
			}else{
				logger.error("failed in reading the current directory");
			}
		});
		vertx.eventBus().consumer("filemerger.merge.completed", message -> {
			logger.info(message.body());
		});
		vertx.eventBus().consumer("filemerger.aws.updated", message -> {
			logger.info(message.body());
		});
	}

	void createFragments(Future futur, String file){
		FileSystem fs = vertx.fileSystem();
		fs.readFile("inputs/images/" + file, result -> {
			if (result.succeeded()) {
				double fileSize = result.result().length();
				int chunks = (int) Math.ceil(fileSize/chunkSize);
				int chunk = 1;
				int fileLength = fileSize > chunkSize ? chunkSize + chunkSize*(chunk - 1) : (int) fileSize;
				while(chunk <= chunks){
					int count = chunk;
					Buffer content = result.result().slice(chunkSize*(chunk - 1), fileLength);
					String fragmentPath = "inputs/fragments/" + file + "-" + String.valueOf(chunks) + "-" + chunk;
					fs.writeFile(fragmentPath, content, res -> {
						if (res.succeeded()) {
							logger.info("File written" + count);
						} else {
							System.err.println("failed to write the fragment: " + res.cause());
						}
					});
					if(fileLength + chunkSize > fileSize){
						fileLength = (int) fileSize;
					}else{
						fileLength = fileLength + chunkSize;
					}
					chunk ++;
				}
			} else {
				System.err.println("Failed to read the file: " + result.cause());
			}
		});
	}

	void sendFragments(String path){
		FileSystem fs = vertx.fileSystem();
		EventBus eventBus = vertx.eventBus();
		String dirPath = "inputs/fragments/"; //path + "/fragments/";
		SharedData sd = vertx.sharedData();
		LocalMap<String, String> fragments = sd.getLocalMap("fragmentsMap");
		vertx.setPeriodic(1, id -> {
			fs.readDir(dirPath, res -> {
				if (res.succeeded()) {
					try{
						Random random = new Random();
						String fragmentPath = res.result().get(random.nextInt(res.result().size()));
						String fragmentName = fragmentPath.split("/")[fragmentPath.split("/").length - 1];
						if(!fragments.containsKey(fragmentName)){
							fragments.put(fragmentName, fragmentName);
							fs.readFile(dirPath + fragmentName, result -> {
								if (result.succeeded()) {
									JsonObject fragment = new JsonObject();
									int fragmentNum = Integer.parseInt(fragmentName.split("-")[fragmentName.split("-").length - 1]);
									int fragmentsCount = Integer.parseInt(fragmentName.split("-")[fragmentName.split("-").length - 2]);
									String fileName = fragmentName.replace("-" + fragmentsCount + "-" + fragmentNum, "");
									Buffer content = result.result();
									fragment.put("fileName", fileName);
									fragment.put("fragmentNumber", fragmentNum);
									fragment.put("fragmentsCount", fragmentsCount);
									fragment.put("contentType", "image");
									fragment.put("fragmentContent", content.getBytes());
									eventBus.publish("media.process.fragments", fragment.toString());
								} else {
									logger.error("failed to read the fail/directory ..." + result.cause());
								}
							});
						}else if(fragments.size() == res.result().size()){
							vertx.cancelTimer(id);
							logger.error("number of fragments sent from the tester: " + fragments.size());
						}
					}catch(IndexOutOfBoundsException ex){
						logger.error("No such file or directory..");
						vertx.cancelTimer(id);
					}catch(Exception ex){
						logger.error("Unknown exception " + ex.getMessage());
						vertx.cancelTimer(id);
					}
				} else {
					logger.info("failed to read the directory: " + res.cause());
					vertx.cancelTimer(id);
				}
			});
		});
	}
}
