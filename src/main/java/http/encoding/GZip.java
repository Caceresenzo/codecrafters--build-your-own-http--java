package http.encoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class GZip implements Encoding {

	public static final String NAME = "gzip";

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public byte[] encode(byte[] input) throws IOException {
		final var byteOutputStream = new ByteArrayOutputStream();

		try (final var gzipOutputStream = new GZIPOutputStream(byteOutputStream)) {
			gzipOutputStream.write(input);
			gzipOutputStream.flush();
		}

		return byteOutputStream.toByteArray();
	}

}