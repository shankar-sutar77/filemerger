//import com.vertx.filemerger.DatabaseConnection;
//import com.vertx.filemerger.S3Upload;
//import io.vertx.core.AbstractVerticle;
//import io.vertx.core.Future;
//import io.vertx.core.buffer.Buffer;
//import io.vertx.core.eventbus.MessageConsumer;
//import io.vertx.core.file.AsyncFile;
//import io.vertx.core.file.FileSystem;
//import io.vertx.core.file.OpenOptions;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//import io.vertx.core.logging.Logger;
//import io.vertx.core.logging.LoggerFactory;
//import io.vertx.core.shareddata.Lock;
//import io.vertx.core.shareddata.SharedData;
//
// interface GenericHandler<T> {
// 	void get(T object);
// }
//
// public class SubbuFileMerger extends AbstractVerticle {
// 	Logger logger = LoggerFactory.getLogger(Merger.class);
// 	DatabaseConnection connection = null;
// 	S3Upload s3Upload = null;
// 	String path = null;
//
// 	@Override
// 	public void start() {
// 		try{
// 			path = "inputs";//config().getString("merge-path");
// 			connection = new Database(vertx, config());
// 			s3Upload = new UploadToS3(vertx, config().getJsonObject("awsconfig"));
// 			generateEventBusConsumerHandler();
// 			deleteMediaRecordHandler();
// 		}catch(Exception e){
// 			e.printStackTrace();
// 			logger.error(e);
// 		}
// 	}
//
// 	void generateEventBusConsumerHandler(){
// 		MessageConsumer<String> consumer = vertx.eventBus().consumer("media.process.fragments");
// 		consumer.handler(message -> {
// 			JsonObject fragment = new JsonObject(message.body());
// 			logger.info("fragment recieved: " + fragment.getString("fragmentName"));
// 			createFragment(fragment, res -> {
// 				SharedData sd = vertx.sharedData();
// 				sd.getLock(fragment.getString("fileName"), results -> {
// 					if (results.succeeded()) {
// 						Lock lock = results.result();
// 						selectOrInsert(fragment, lock, fragmentList -> {
// 							updateMediaRecord(fragmentList, fragment, lock, rs -> {
// 								recieveAllFragments(fragment, rs, fragmentsList -> {
// 									mergeFragments(fragment, fragmentsList, fileName -> {
// 										uploadAndUpdateMedia(fileName, fragment);
// 									});
// 								});
// 							});
// 						});
// 					}else{
// 						logger.error("failed to get lock " + results.cause());
// 					}
// 				});
// 			});
// 		});
// 	}
//
// 	void selectOrInsert(JsonObject fragment, Lock lock, GenericHandler<JsonArray> fragmentsListCallBack){
// 		connection.getResultSet(fragment, resultSet -> {
// 			if(resultSet.getNumRows() == 0){
// 						connection.insertMediaRecord(fragment, lock, fragmentsListCallBack);
// 			}else{
// 				JsonArray fragmentsList = new JsonArray(resultSet.getRows().get(0).getString("fragments"));
// 				fragmentsListCallBack.get(fragmentsList);
// 			}
// 		});
// 	}
//
// 	void updateMediaRecord(JsonArray fragmentsList, JsonObject fragment, Lock lock, GenericHandler<JsonArray> updateMedia){
// 		int fragmentNumber = fragment.getInteger("fragmentNumber");
// 		if(fragment.getInteger("fragmentsCount") == 1){
// 			lock.release();
// 			updateMedia.get(fragmentsList);
// 		} else {
// 			if(!fragmentsList.contains(fragmentNumber)){
// 				fragmentsList.add(fragmentNumber);
// 				connection.updateFragmentsList(fragment, fragmentsList, lock, updateMedia);
// 			} else {
// 				logger.info("current fragment has been already existed");
// 				lock.release();
// 			}
// 		}
// 	}
//
// 	void recieveAllFragments(JsonObject fragment, JsonArray fragmentsList, GenericHandler<JsonArray> callBack){
// 		if(fragment.getInteger("fragmentsCount") == fragmentsList.size()){
// 			callBack.get(fragmentsList);
// 		}else{
// 			logger.info(fragmentsList);
// 		}
// 	}
//
// 	void createFragment(JsonObject fragment, GenericHandler<String> createFile){
// 		FileSystem fs = vertx.fileSystem();
// 		Buffer content = Buffer.buffer(fragment.getBinary("fragmentContent"));
// 		String fragmentName = fragment.getString("fileName") + "-" + String.valueOf(fragment.getInteger("fragmentNumber"));
// 		fs.writeFile(path + "temp/" + fragmentName, content, res -> {
// 			if (res.succeeded()) {
// 				logger.info("fragment written " + fragmentName);
// 				createFile.get("");
// 			} else {
// 				logger.error("Failed to write fragment: " + res.cause());
// 			}
// 		});
// 	}
//
// 	void mergeFragments(JsonObject fragment, JsonArray fragmentsList, GenericHandler<String> fileHandler){
// 		FileSystem fs = vertx.fileSystem();
// 		String fileName = path + "Merged_" + fragment.getString("fileName");
// 		String fragmentsPath = path + "temp/" + fragment.getString("fileName");
// 		Future deleteFuture = Future.future();
// 		fs.exists(fileName, result -> {
// 			if (result.succeeded() && result.result()) {
// 				vertx.fileSystem().delete(fileName, r -> {
// 					logger.info("Existed file deleted.");
// 					deleteFuture.complete();
// 				});
// 			} else {
// 				logger.info("No existed file for delete: " + result.cause());
// 				deleteFuture.complete();
// 			}
// 		});
// 		deleteFuture.setHandler(ar -> {
// 			fs.open(fileName, new OpenOptions(), result -> {
// 				if (result.succeeded()) {
// 					AsyncFile file = result.result();
// 					int count = 0;
// 					int target = fragment.getInteger("fragmentsCount");
// 					if(!fragmentsList.contains(0)) {
// 						count = 1;
// 						target = target + 1;
// 					}
// 					while(count < target) {
// 						int index = count;
// 						int fragmentsCount = target;
// 						fs.readFile(fragmentsPath + "-" + index, res -> {
// 							if(res.succeeded()){
// 								file.write(res.result());
// 								logger.info("fragment merged");
// 								if(index == fragmentsCount - 1){
// 									fileHandler.get(fileName);
// 								}
// 							}else{
// 								logger.error("Failed to merge fragment: " + res.cause());
// 							}
// 						});
// 						count ++;
// 					}
// 				} else {
// 					logger.error("Cannot open file " + result.cause());
// 				}
// 			});
// 		});
// 	}
//
// 	void uploadAndUpdateMedia(String imagePath, JsonObject fragment){
// 		FileSystem fs = vertx.fileSystem();
// 		s3Upload.uploadMediaToAWS(imagePath, fragment, res -> {
// 			fs.props(imagePath, ar -> {
// 				int contentLength = (int) ar.result().size();
// 				String fileName = "Merged_" + fragment.getString("fileName");
// 				String s3url = config().getString("s3url") + fileName;
// 				String query = "UPDATE media set s3_url = '" + s3url + "',"
// 											+ "file_size = " + contentLength
// 											+ ", status = 'done' where file_name = '"
// 											+ fragment.getString("fileName") + "';";
// 				connection.updateMediaRecord(query, res1 -> {
// 					if(res1){ vertx.eventBus().publish("filemerger.aws.updated", "Media successfully uploaded into amazon");}
// 				});
// 			});
// 		});
// 	}
//
// 	void deleteMediaRecordHandler(){
// 		vertx.eventBus().consumer("filemerger.delete.media.file", message -> {
// 			connection.dropRecord(message.toString(), res -> {
// 				logger.info("record deleted successfully..");
// 			});
// 		});
// 	}
// }
