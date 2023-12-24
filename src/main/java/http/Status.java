package http;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum Status {

	OK(200, "OK"),
	NOT_FOUND(404, "Not Found");

	private final int code;
	private final String phrase;

	public String line() {
		return "%d %s".formatted(code, phrase);
	}

}