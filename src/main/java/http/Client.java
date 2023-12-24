package http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class Client implements Runnable {

	public static final String HTTP_1_1 = "HTTP/1.1";
	public static final String CRLF = "\r\n";

	private static final byte[] HTTP_1_1_BYTES = HTTP_1_1.getBytes();
	private static final byte[] CRLF_BYTES = CRLF.getBytes();
	private static final byte SPACE_BYTE = ' ';
	private static final byte[] COLON_SPACE_BYTE = { ':', ' ' };

	private static final Pattern ECHO_PATTERN = Pattern.compile("\\/echo\\/(.*)");

	private static final AtomicInteger ID_INCREMENT = new AtomicInteger();

	private final int id;
	private final Socket socket;

	public Client(Socket socket) throws IOException {
		this.id = ID_INCREMENT.incrementAndGet();
		this.socket = socket;
	}

	@Override
	public void run() {
		System.out.println("%d: connected".formatted(id));

		try (socket) {
			final var inputStream = new BufferedInputStream(socket.getInputStream());
			final var outputStream = new BufferedOutputStream(socket.getOutputStream());

			final var request = parse(inputStream);
			//			System.out.println(request);
			final var response = handle(request);
			//			System.out.println(response);

			send(response, outputStream);
		} catch (IOException exception) {
			System.err.println("%d: returned an error: %s".formatted(id, exception.getMessage()));
			exception.printStackTrace();
		}

		System.out.println("%d: disconnected".formatted(id));
	}

	public Request parse(InputStream inputStream) throws IOException {
		@SuppressWarnings("resource")
		final var scanner = new Scanner(inputStream);

		final var method = Method.valueOf(scanner.next());

		final var path = scanner.next();
		if (!path.startsWith("/")) {
			throw new IllegalStateException("path does not start with a slash: " + path);
		}

		final var version = scanner.next();
		if (!HTTP_1_1.equals(version)) {
			throw new IllegalStateException("unsupported version: " + version);
		}

		final var remaining = scanner.nextLine();
		if (!remaining.isEmpty()) {
			throw new IllegalStateException("content after version: " + remaining);
		}

		final var headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

		String line;
		while (!(line = scanner.nextLine()).isEmpty()) {
			final var parts = line.split(":", 2);

			if (parts.length != 2) {
				throw new IllegalStateException("missing header value: " + line);
			}

			final var key = parts[0];
			final var value = parts[1];

			headers.put(key, value);
		}

		return new Request(method, path, headers);
	}

	public Response handle(Request request) {
		if (request.path().equals("/")) {
			return Response.ok();
		}

		{
			final var match = ECHO_PATTERN.matcher(request.path());
			if (match.find()) {
				final var message = match.group(1);

				return Response.plainText(message);
			}
		}

		return Response.notFound();
	}

	public void send(Response response, OutputStream outputStream) throws IOException {
		outputStream.write(HTTP_1_1_BYTES);
		outputStream.write(SPACE_BYTE);

		outputStream.write(response.status().line().getBytes());
		outputStream.write(SPACE_BYTE);

		outputStream.write(CRLF_BYTES);

		for (final var entry : response.headers().entrySet()) {
			final var key = entry.getKey();
			final var value = entry.getValue();

			outputStream.write(key.getBytes());
			outputStream.write(COLON_SPACE_BYTE);
			outputStream.write(value.getBytes());
			outputStream.write(CRLF_BYTES);
		}

		outputStream.write(CRLF_BYTES);

		outputStream.write(response.body());

		outputStream.flush();
	}

}