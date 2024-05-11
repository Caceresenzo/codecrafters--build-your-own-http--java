package http;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {

	public static final int PORT = 4221;
	public static String WORKING_DIRECTORY = ".";

	public static void main(String[] args) throws IOException {
		System.out.println("codecrafters build-your-own-http");

		if (args.length == 2) {
			if (args[0].equals("--directory")) {
				final var workingDirectory = args[1];

				System.out.println("working directory: %s".formatted(workingDirectory));
				WORKING_DIRECTORY = workingDirectory;
			}
		}

		final var threadFactory = Thread.ofVirtual().factory();

		try (final var serverSocket = new ServerSocket(PORT)) {
			serverSocket.setReuseAddress(true);
			System.out.println("listen: %d".formatted(PORT));

			while (true) {
				final var socket = serverSocket.accept();
				final var client = new Client(socket);

				final var thread = threadFactory.newThread(client);
				thread.start();
			}
		}
	}

}