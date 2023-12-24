package http;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {

	public static final int PORT = 4221;

	public static void main(String[] args) throws IOException {
		System.out.println("codecrafters build-your-own-http");

		final var threadFactory = Thread.ofVirtual().factory();

		try (final var serverSocket = new ServerSocket(PORT)) {
			serverSocket.setReuseAddress(true);

			while (true) {
				final var socket = serverSocket.accept();
				final var client = new Client(socket);

				final var thread = threadFactory.newThread(client);
				thread.start();
			}
		}
	}

}