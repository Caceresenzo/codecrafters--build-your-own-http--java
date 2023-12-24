package http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public record Response(
	Status status,
	Map<String, String> headers,
	byte[] body
) {

	public static Response ok() {
		return new Response(
			Status.OK,
			Collections.emptyMap(),
			new byte[0]
		);
	}

	public static Response notFound() {
		return new Response(
			Status.NOT_FOUND,
			Collections.emptyMap(),
			new byte[0]
		);
	}

	public static Response plainText(String content) {
		final var bytes = content.getBytes();

		return new Response(
			Status.OK,
			Map.of(
				"Content-Type", "text/plain",
				"Content-Length", String.valueOf(bytes.length)
			),
			bytes
		);
	}

	public static Response file(File file) throws IOException {
		try (final var inputStream = new FileInputStream(file)) {
			final var bytes = inputStream.readAllBytes();
			
			return new Response(
				Status.OK,
				Map.of(
					"Content-Type", "application/octet-stream",
					"Content-Length", String.valueOf(bytes.length)
				),
				bytes
			);
		} catch (FileNotFoundException exception) {
			return notFound();
		}
	}

}