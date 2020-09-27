package com.mahaswami.trainee.filemerger.verticles;

import java.util.Random;
import java.util.ArrayList;
import java.util.Base64;
import java.io.*;
import io.vertx.core.*;
import io.vertx.core.json.*;
import io.vertx.core.shareddata.LocalMap;

public class Test extends AbstractVerticle
{
	LocalMap<String, String> status_map;
	public void start()throws Exception
	{
		status_map = vertx.sharedData().getLocalMap("mymap1");
		File[] files = new File("images/").listFiles();
		Random random = new Random();
		vertx.setPeriodic(1000, id -> {
			ArrayList<File> fileList = new ArrayList<File>();
			File file = files[random.nextInt(files.length)];
			try {
				splitFile(file);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		});
		// File file = new File("src/main/java/images/lion.png");
		// splitFile(file);
		vertx.eventBus().consumer("test_delete_status", message ->{
			JsonObject obj = new JsonObject(message.body().toString());
			String fileName = obj.getString("fileName");
			int fragmentCount = Integer.parseInt(obj.getString("fragmentCount"));
			for(int index = 1; index <= fragmentCount; index++){
				status_map.remove(fileName + "_" + index);
			}
			vertx.eventBus().send("filemerger_delete_media",fileName);
		});
	}

	void splitFile(File file)throws Exception
	{
		int partNumber = 1;
		int fragmentSize = 256000;
		int fragmentCount = 0;
		byte[] buffer = new byte[fragmentSize];
		String fileName = file.getName();
		FileInputStream fileInput = new FileInputStream(file);
		BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);
		int bytesAmount = 0;
		ArrayList<Integer> filesList = new ArrayList<Integer>();
		while((bytesAmount = bufferedInput.read(buffer)) > 0)
		{
			String fragmentName = String.format("%s_%02d.txt", fileName, partNumber++);
			File newFile = new File("images/fragments",fragmentName);
			FileOutputStream out = new FileOutputStream(newFile);
			out.write(buffer, 0, bytesAmount);
			filesList.add(partNumber - 1);
			fragmentCount++;
		}
		Random random = new Random();
		int randomFragment = filesList.get(random.nextInt(filesList.size()));

		if(status_map.get(fileName + "_" +randomFragment) == null) {
			status_map.put(fileName + "_" +randomFragment,fileName + "_" + randomFragment);
			System.out.println("No of Fragments created for " + fileName
			+ ": " + fragmentCount);
			System.out.println("Fragments sent ===>"+ fileName + "_" +randomFragment);
			JsonObject object = new JsonObject();
			object.put("fileName", fileName);
			object.put("fragmentCount", String.valueOf(fragmentCount));
			object.put("fragmentNumber", String.valueOf(randomFragment));
			File fragmentFile = new File("images/fragments",
			String.format("%s_%02d.txt", fileName, randomFragment));
			object.put("fragmentSize", fragmentFile.length() / 1024 + "");
			object.put("fragmentContent", encoder(fragmentFile));
			vertx.eventBus().send("filemerger.send_fragments", object.encode(), ar -> {
				if (ar.succeeded()) {
					System.out.println("Sent a random fragment...!");
				}
			});
		}
	}

	String encoder(File file) throws FileNotFoundException, Exception
	{
		String image = "";
		FileInputStream fragmentFile = new FileInputStream(file);
		byte fileData[] = new byte[(int)file.length()];
		fragmentFile.read(fileData);
		image = Base64.getEncoder().encodeToString(fileData);
		return image;
	}
}
