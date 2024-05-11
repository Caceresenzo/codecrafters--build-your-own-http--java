package http;

public record Request(
	Method method,
	String path,
	Headers headers,
	byte[] body
) {}