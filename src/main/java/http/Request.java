package http;

import java.util.Map;

public record Request(
	Method method,
	String path,
	Map<String, String> headers
) {}