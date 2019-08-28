package com.askia.HttpserviceDemo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

public class ServiceThreadMain {

	public static void main(String[] args) {
		startServer();
	}

	public static void startServer() {
		InetSocketAddress addr = new InetSocketAddress(12531);
		HttpServer server;
		try {
			server = HttpServer.create(addr, 0);
			server.createContext("/upload", new DeepChackHandler());
			server.setExecutor(Executors.newCachedThreadPool());
			server.start();
			System.out.println("Server is listening on port 12531");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
