package http;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import http.encoding.Encoding;

public record Request(
	Method method,
	String path,
	Map<String, String> headers,
	byte[] body
) {

	public List<Encoding> acceptEncoding() {
		final var header = headers.get(Headers.ACCEPT_ENCODING);
		if (header == null) {
			return Collections.emptyList();
		}
		
		final var encoding = Encoding.of(header);
		if (encoding.isEmpty()) {
			return Collections.emptyList();
		}
		
		return List.of(encoding.get());
	}
	
}