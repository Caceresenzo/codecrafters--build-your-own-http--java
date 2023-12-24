package http;

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

}