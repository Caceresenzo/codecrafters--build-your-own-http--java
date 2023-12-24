package http;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {

	public static final int PORT = 4221;

	public static void main(String[] args) throws IOException {
		System.out.println("codecrafters build-your-own-http");

		try (final var serverSocket = new ServerSocket(PORT)) {
			serverSocket.setReuseAddress(true);

			try (final var socket = serverSocket.accept()) {
				final var outputStream = socket.getOutputStream();

				outputStream.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
				outputStream.flush();
			}
		}
	}

}