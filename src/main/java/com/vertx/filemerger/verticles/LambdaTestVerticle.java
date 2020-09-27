package com.vertx.filemerger.verticles;
import io.vertx.core.*;
import io.vertx.core.buffer.*;


interface GenericCallback1<T> {
	void perform(T result);
}

public class LambdaTestVerticle extends AbstractVerticle {
  public void start() {
    System.out.println(" LambdaVerticle Started...!");
    readTextContent("myfile.txt", content -> { 
      System.out.println("Received: " + content);
    });
  }


  void readTextContent(String fileName, GenericCallback1<String> callbackFunction) {
      //1. use vertx file sytem to read the content
      //2. call the callback function
      vertx.fileSystem().readFile(fileName, handler -> {
          if (handler.succeeded()) {
              Buffer content = handler.result();
              callbackFunction.perform(fileName + content);
          }
      });

  }




}
