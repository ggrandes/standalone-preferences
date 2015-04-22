package org.javastack.preferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SourceFile {
	public static final int DEFAULT_CONNECT_TIMEOUT = 180000; 	// Default: 3min
	public static final int DEFAULT_READ_TIMEOUT = 120000; 		// Default: 2min
	private static final Logger log = Logger.getLogger(SourceFile.class.getName());
	private static final Charset LATIN1 = Charset.forName("ISO-8859-1");
	protected final String dir;
	protected final String file;

	private SourceFile(final String dir, final String file) {
		this.dir = dir;
		this.file = file;
	}

	public abstract String[] directoryList(final NameFilter filter) throws IOException;

	public abstract boolean fileExists();

	public abstract InputStream getInputStream() throws IOException;

	public abstract OutputStream getOutputStream() throws IOException;

	@Override
	public String toString() {
		return dir + "/" + file;
	}

	public static SourceFile getSource(final String dir, final String file) {
		// Check for URL
		final String proto = getProto(dir);
		if (proto != null) {
			if (proto.equalsIgnoreCase("http") || proto.equalsIgnoreCase("https")) {
				return new SourceFileHTTP(dir, file);
			} else if (proto.equalsIgnoreCase("file")) {
				return new SourceFileLocal(dir.substring("file:".length()), file);
			}
		}
		// Check for Local File
		switch (dir.charAt(0)) {
			case '\\': // Windows
			case '/':
			case '.':
				return new SourceFileLocal(dir, file);
		}
		if ((dir.length() > 1) && (dir.charAt(1) == ':')) { // Windows
			return new SourceFileLocal(dir, file);
		}
		// Oops!
		throw new IllegalArgumentException("Invalid path or URL: dir=" + dir + " file=" + file);
	}

	private static String getProto(final String url) {
		final int offset = url.indexOf(':');
		if (offset != -1) {
			return url.substring(0, offset);
		}
		return null;
	}

	static interface NameFilter {
		boolean accept(final String name);
	}

	static class SourceFileLocal extends SourceFile {
		protected final File f;

		SourceFileLocal(final String dir, final String file) {
			super(dir, file);
			this.f = new File(dir, file);
		}

		@Override
		public String[] directoryList(final NameFilter filter) {
			return new File(dir).list(new FilenameFilter() {
				@Override
				public boolean accept(final File dir, final String name) {
					final File file = new File(dir, name);
					if (!file.isFile()) {
						return false;
					}
					return filter.accept(name);
				}
			});
		}

		@Override
		public boolean fileExists() {
			return f.exists();
		}

		@Override
		public InputStream getInputStream() throws FileNotFoundException {
			return new FileInputStream(f);
		}

		@Override
		public OutputStream getOutputStream() throws FileNotFoundException {
			return new FileOutputStream(f);
		}
	}

	static class SourceFileHTTP extends SourceFile {
		SourceFileHTTP(final String dir, final String file) {
			super(dir, file);
		}

		private URL getURL() throws MalformedURLException {
			return new URL(dir + "/" + file);
		}

		@Override
		public String[] directoryList(final NameFilter filter) throws IOException {
			final HttpURLConnection conn = (HttpURLConnection) new URL(dir + "/").openConnection();
			conn.setInstanceFollowRedirects(true);
			conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
			conn.setReadTimeout(DEFAULT_READ_TIMEOUT);
			conn.setRequestMethod("GET");
			conn.setDoOutput(false);
			conn.setDoInput(true);
			conn.connect();
			InputStream is = null;
			BufferedReader in = null;
			try {
				is = conn.getInputStream();
				final int code = conn.getResponseCode();
				if (code != HttpURLConnection.HTTP_OK) {
					if (is != null) {
						is.close();
					}
					throw new IOException("Error getting InputStream from: " + conn.getURL()
							+ " ResponseCode: " + code);
				}
				in = new BufferedReader(new InputStreamReader(is, LATIN1));
				final ArrayList<String> files = new ArrayList<String>();
				String line = null;
				while ((line = in.readLine()) != null) {
					final String name = line.trim();
					if (name.isEmpty()) {
						continue;
					}
					if (filter.accept(name)) {
						files.add(name);
					}
				}
				return files.toArray(new String[files.size()]);
			} finally {
				if (in != null) {
					in.close();
				}
				if (is != null) {
					is.close();
				}
			}
		}

		@Override
		public boolean fileExists() {
			InputStream is = null;
			HttpURLConnection conn = null;
			try {
				conn = (HttpURLConnection) getURL().openConnection();
				conn.setInstanceFollowRedirects(true);
				conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
				conn.setReadTimeout(DEFAULT_READ_TIMEOUT);
				conn.setRequestMethod("HEAD");
				conn.setDoOutput(false);
				conn.setDoInput(true);
				conn.connect();
				try {
					is = conn.getInputStream();
				} catch (Exception e) {
					is = conn.getErrorStream();
				} finally {
					consume(is);
				}
				final int code = conn.getResponseCode();
				if (code == HttpURLConnection.HTTP_OK) {
					return true;
				}
				log.log(Level.WARNING, "Error getting InputStream from: " + conn.getURL() + " ResponseCode: "
						+ code);
			} catch (IOException e) {
				log.log(Level.SEVERE, "Error getting InputStream from: "
						+ ((conn != null) ? conn.getURL() : this) + ": " + e.toString(), e);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (Exception ign) {
					}
				}
			}
			return false;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			final HttpURLConnection conn = (HttpURLConnection) getURL().openConnection();
			conn.setInstanceFollowRedirects(true);
			conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
			conn.setReadTimeout(DEFAULT_READ_TIMEOUT);
			conn.setRequestMethod("GET");
			conn.setDoOutput(false);
			conn.setDoInput(true);
			conn.connect();
			InputStream is = null;
			try {
				is = conn.getInputStream();
			} catch (Exception e) {
				is = conn.getErrorStream();
			}
			final int code = conn.getResponseCode();
			if (code != HttpURLConnection.HTTP_OK) {
				if (is != null) {
					consume(is);
					is.close();
				}
				throw new IOException("Error getting InputStream from: " + conn.getURL() + " ResponseCode: "
						+ code);
			}
			return is;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			final HttpURLConnection conn = (HttpURLConnection) getURL().openConnection();
			conn.setInstanceFollowRedirects(true);
			conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
			conn.setReadTimeout(DEFAULT_READ_TIMEOUT);
			conn.setRequestMethod("PUT");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.connect();
			return new FilterOutputStream(conn.getOutputStream()) {
				@Override
				public void close() throws IOException {
					super.close();
					InputStream is = null;
					try {
						is = conn.getInputStream();
					} catch (Exception e) {
						is = conn.getInputStream();
					} finally {
						if (is != null) {
							consume(is);
							is.close();
						}
					}
				}
			};
		}

		private static final void consume(final InputStream is) {
			if (is != null) {
				try {
					final byte[] b = new byte[512];
					while (is.read(b) != -1) {
						continue;
					}
				} catch (IOException e) {
				}
			}
		}
	}
}
