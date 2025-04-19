package http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;
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
	private static final Pattern FILES_PATTERN = Pattern.compile("\\/files\\/(.+)");

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
			while (true) {
				final var inputStream = new BufferedInputStream(socket.getInputStream());
				final var outputStream = new BufferedOutputStream(socket.getOutputStream());

				final var request = parse(inputStream);
				if (request == null) {
					break;
				}

				final var response = handle(request);

				final var shouldClose = shouldConnectionBeClosed(request);
				if (shouldClose) {
					response.headers().put(Headers.CONNECTION, "close");
				}

				final var modified = middleware(request, response);

				System.out.println("%d: %s %s -> %s".formatted(id, request.method(), request.path(), response.status()));

				send(modified, outputStream);

				if (shouldClose) {
					break;
				}
			}
		} catch (IOException exception) {
			System.err.println("%d: returned an error: %s".formatted(id, exception.getMessage()));
			exception.printStackTrace();
		}

		System.out.println("%d: disconnected".formatted(id));
	}

	public Request parse(InputStream inputStream) throws IOException {
		var line = nextLine(inputStream);
		if (line.isEmpty()) {
			return null;
		}

		@SuppressWarnings("resource")
		var scanner = new Scanner(line);

		final var method = Method.valueOf(scanner.next());

		final var path = scanner.next();
		if (!path.startsWith("/")) {
			throw new IllegalStateException("path does not start with a slash: " + path);
		}

		final var version = scanner.next();
		if (!HTTP_1_1.equals(version)) {
			throw new IllegalStateException("unsupported version: " + version);
		}

		if (scanner.hasNext()) {
			throw new IllegalStateException("content after version: " + scanner.next());
		}

		final var headers = new Headers();

		while (!(line = nextLine(inputStream)).isEmpty()) {
			final var parts = line.split(":", 2);

			if (parts.length != 2) {
				throw new IllegalStateException("missing header value: " + line);
			}

			final var key = parts[0];
			final var value = parts[1].stripLeading();

			headers.put(key, value);
		}

		if (Method.POST.equals(method)) {
			final var contentLength = headers.contentLength();
			final var body = inputStream.readNBytes(contentLength);

			return new Request(method, path, headers, body);
		}

		return new Request(method, path, headers, null);
	}

	public Response handle(Request request) throws IOException {
		return switch (request.method()) {
			case GET -> handleGet(request);
			case POST -> handlePost(request);
		};
	}

	public Response handleGet(Request request) throws IOException {
		if (request.path().equals("/")) {
			return Response.status(Status.OK);
		}

		if (request.path().equals("/user-agent")) {
			final var userAgent = request.headers().userAgent();
			return Response.plainText(userAgent);
		}

		{
			final var match = ECHO_PATTERN.matcher(request.path());
			if (match.find()) {
				final var message = match.group(1);

				return Response.plainText(message);
			}
		}

		{
			final var match = FILES_PATTERN.matcher(request.path());
			if (match.find()) {
				// TODO Protect against `../` */
				final var path = match.group(1);

				return Response.file(new File(Main.WORKING_DIRECTORY, path));
			}
		}

		return Response.status(Status.NOT_FOUND);
	}

	public Response handlePost(Request request) throws IOException {
		{
			final var match = FILES_PATTERN.matcher(request.path());
			if (match.find()) {
				// TODO Protect against `../` */
				final var path = match.group(1);

				try (final var outputStream = new FileOutputStream(new File(Main.WORKING_DIRECTORY, path))) {
					outputStream.write(request.body());
				}

				return Response.status(Status.CREATED);
			}
		}

		return Response.status(Status.NOT_FOUND);
	}

	public Response middleware(Request request, Response response) throws IOException {
		final var encodings = request.headers().acceptEncoding();
		if (!encodings.isEmpty()) {
			final var encoding = encodings.getFirst();

			final var encodedBody = encoding.encode(response.body());

			final var headers = response.headers().clone()
				.put(Headers.CONTENT_ENCODING, encoding.name())
				.put(Headers.CONTENT_LENGTH, String.valueOf(encodedBody.length));

			response = new Response(response.status(), headers, encodedBody);
		}

		return response;
	}

	public void send(Response response, OutputStream outputStream) throws IOException {
		outputStream.write(HTTP_1_1_BYTES);
		outputStream.write(SPACE_BYTE);

		outputStream.write(response.status().line().getBytes());
		outputStream.write(CRLF_BYTES);

		for (final var entry : response.headers().entrySet()) {
			final var key = entry.getKey();
			if (Headers.CONTENT_LENGTH.equalsIgnoreCase(key)) {
				continue;
			}

			final var value = entry.getValue();

			outputStream.write(key.getBytes());
			outputStream.write(COLON_SPACE_BYTE);
			outputStream.write(value.getBytes());
			outputStream.write(CRLF_BYTES);
		}

		final var body = response.body();
		if (body != null) {
			outputStream.write(Headers.CONTENT_LENGTH.getBytes());
			outputStream.write(COLON_SPACE_BYTE);
			outputStream.write(String.valueOf(body.length).getBytes());
			outputStream.write(CRLF_BYTES);
		}

		outputStream.write(CRLF_BYTES);

		if (body != null) {
			outputStream.write(body);
		}

		outputStream.flush();
	}

	public boolean shouldConnectionBeClosed(Request request) {
		final var connection = request.headers().connection();

		return connection != null && connection.equalsIgnoreCase("close");
	}

	private String nextLine(InputStream inputStream) throws IOException {
		final var builder = new StringBuilder();

		var cariageReturn = false;

		int value;
		while ((value = inputStream.read()) != -1) {
			if ('\n' == value && cariageReturn) {
				break;
			} else if ('\r' == value) {
				cariageReturn = true;
			} else {
				if (cariageReturn) {
					builder.append('\r');
				}

				builder.append((char) value);
				cariageReturn = false;
			}
		}

		return builder.toString();
	}

}