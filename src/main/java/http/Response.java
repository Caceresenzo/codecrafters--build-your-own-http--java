package http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public record Response(
	Status status,
	Headers headers,
	byte[] body
) {

	public static Response status(Status status) {
		return new Response(
			status,
			new Headers(),
			new byte[0]
		);
	}

	public static Response plainText(String content) {
		final var bytes = content.getBytes();

		return new Response(
			Status.OK,
			new Headers()
				.put(Headers.CONTENT_TYPE, "text/plain"),
			bytes
		);
	}

	public static Response file(File file) throws IOException {
		try (final var inputStream = new FileInputStream(file)) {
			final var bytes = inputStream.readAllBytes();

			return new Response(
				Status.OK,
				new Headers()
					.put(Headers.CONTENT_TYPE, "application/octet-stream"),
				bytes
			);
		} catch (FileNotFoundException exception) {
			return status(Status.NOT_FOUND);
		}
	}

}