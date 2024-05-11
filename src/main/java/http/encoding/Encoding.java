package http.encoding;

import java.io.IOException;
import java.util.Optional;

public interface Encoding {

	String name();

	byte[] encode(byte[] input) throws IOException;

	public static Optional<Encoding> of(String name) {
		return Optional.ofNullable(switch (name) {
			case GZip.NAME -> new GZip();
			default -> null;
		});
	}

}