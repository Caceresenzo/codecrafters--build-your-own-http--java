package http;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import http.encoding.Encoding;

public class Headers implements Cloneable {

	private final Map<String, String> storage;

	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	public static final String CONTENT_ENCODING = "Content-Encoding";
	public static final String CONTENT_LENGTH = "Content-Length";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String USER_AGENT = "User-Agent";
	public static final String CONNECTION = "Connection";

	public Headers() {
		this.storage = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	}

	public Headers(Headers headers) {
		this();

		this.storage.putAll(headers.storage);
	}

	public int contentLength() {
		return getAsInteger(CONTENT_LENGTH, 0);
	}

	public List<Encoding> acceptEncoding() {
		final var header = storage.get(Headers.ACCEPT_ENCODING);
		if (header == null) {
			return Collections.emptyList();
		}

		return Arrays.stream(header.split(",\s+"))
			.map(Encoding::of)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.toList();
	}

	public String userAgent() {
		return storage.get(USER_AGENT);
	}

	public String connection() {
		return storage.get(CONNECTION);
	}

	public Headers put(String key, String value) {
		storage.put(key, value);
		return this;
	}

	public int getAsInteger(String key, int defaultValue) {
		final var raw = storage.get(key);
		return raw != null ? Integer.parseInt(raw) : defaultValue;
	}

	public Set<Map.Entry<String, String>> entrySet() {
		return storage.entrySet();
	}

	@Override
	public Headers clone() {
		return new Headers(this);
	}

}