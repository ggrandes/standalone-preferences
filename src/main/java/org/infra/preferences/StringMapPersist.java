/* 
 * This class is derived for compatibility from original code of:
 * java.util.Properties from OpenJDK, and is Licensed as GPLv2
 * 
 * Copyright 1995-2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package org.infra.preferences;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;

/**
 * Generic Persistor of {@link java.util.Map}, adapted from {@link java.util.Properties Properties store/load}
 * 
 * @see java.util.Properties#load(InputStream)
 * @see java.util.Properties#store(OutputStream, String)
 * @see java.util.Properties#load(Reader)
 * @see java.util.Properties#store(Writer, String)
 */
class StringMapPersist {
	private static final Charset ISOLatin1 = Charset.forName("ISO-8859-1");
	private final Map<String, String> map;

	public StringMapPersist(final Map<String, String> map) {
		this.map = map;
	}

	/**
	 * Reads a property list (key and element pairs) from the input byte stream. The input stream is in a
	 * simple line-oriented format as specified in {@link java.util.Properties#load(java.io.Reader)} and is
	 * assumed to use the ISO 8859-1 character encoding; that is each byte is one Latin1 character. Characters
	 * not in Latin1, and certain special characters, are represented in keys and elements using <a
	 * href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.3">Unicode escapes</a>.
	 * <p>
	 * The specified stream remains open after this method returns.
	 * 
	 * @param inStream
	 *            the input stream.
	 * @exception IOException
	 *                if an error occurred when reading from the input stream.
	 * @throws IllegalArgumentException
	 *             if the input stream contains a malformed Unicode escape sequence.
	 */
	public void load(final InputStream inStream) throws IOException {
		load0(new LineReader(inStream));
	}

	/**
	 * Reads a property list (key and element pairs) from the input character stream in a simple line-oriented
	 * format.
	 * <p>
	 * Same semantic of {@link java.util.Properties#load(java.io.Reader)}
	 * <p>
	 * The specified stream remains open after this method returns.
	 * 
	 * @param reader
	 *            the input character stream.
	 * @throws IOException
	 *             if an error occurred when reading from the input stream.
	 * @throws IllegalArgumentException
	 *             if a malformed Unicode escape appears in the input.
	 */
	public void load(final Reader reader) throws IOException {
		load0(new LineReader(reader));
	}

	private void load0(final LineReader lr) throws IOException {
		char[] convtBuf = new char[1024];
		int limit;
		int keyLen;
		int valueStart;
		char c;
		boolean hasSep;
		boolean precedingBackslash;

		while ((limit = lr.readLine()) >= 0) {
			c = 0;
			keyLen = 0;
			valueStart = limit;
			hasSep = false;

			// System.out.println("line=<" + new String(lineBuf, 0, limit) + ">");
			precedingBackslash = false;
			while (keyLen < limit) {
				c = lr.lineBuf[keyLen];
				// need check if escaped.
				if ((c == '=' || c == ':') && !precedingBackslash) {
					valueStart = keyLen + 1;
					hasSep = true;
					break;
				} else if ((c == ' ' || c == '\t' || c == '\f') && !precedingBackslash) {
					valueStart = keyLen + 1;
					break;
				}
				if (c == '\\') {
					precedingBackslash = !precedingBackslash;
				} else {
					precedingBackslash = false;
				}
				keyLen++;
			}
			while (valueStart < limit) {
				c = lr.lineBuf[valueStart];
				if (c != ' ' && c != '\t' && c != '\f') {
					if (!hasSep && (c == '=' || c == ':')) {
						hasSep = true;
					} else {
						break;
					}
				}
				valueStart++;
			}
			final String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
			final String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
			map.put(key, value);
		}
	}

	/*
	 * Read in a "logical line" from an InputStream/Reader, skip all comment and blank lines and filter out
	 * those leading whitespace characters ( , and ) from the beginning of a "natural line". Method returns
	 * the char length of the "logical line" and stores the line in "lineBuf".
	 */
	class LineReader {
		char[] lineBuf = new char[1024];
		byte[] inByteBuf;
		char[] inCharBuf;
		int inLimit = 0;
		int inOff = 0;
		InputStream inStream;
		Reader reader;

		public LineReader(final InputStream inStream) {
			this.inStream = inStream;
			inByteBuf = new byte[8192];
		}

		public LineReader(final Reader reader) {
			this.reader = reader;
			inCharBuf = new char[8192];
		}

		int readLine() throws IOException {
			int len = 0;
			char c = 0;

			boolean skipWhiteSpace = true;
			boolean isCommentLine = false;
			boolean isNewLine = true;
			boolean appendedLineBegin = false;
			boolean precedingBackslash = false;
			boolean skipLF = false;

			while (true) {
				if (inOff >= inLimit) {
					inLimit = (inStream == null) ? reader.read(inCharBuf) : inStream.read(inByteBuf);
					inOff = 0;
					if (inLimit <= 0) {
						if ((len == 0) || isCommentLine) {
							return -1;
						}
						return len;
					}
				}
				if (inStream != null) {
					// The line below is equivalent to calling a
					// ISO8859-1 decoder.
					c = (char) (0xff & inByteBuf[inOff++]);
				} else {
					c = inCharBuf[inOff++];
				}
				if (skipLF) {
					skipLF = false;
					if (c == '\n') {
						continue;
					}
				}
				if (skipWhiteSpace) {
					if ((c == ' ') || (c == '\t') || (c == '\f')) {
						continue;
					}
					if (!appendedLineBegin && ((c == '\r') || (c == '\n'))) {
						continue;
					}
					skipWhiteSpace = false;
					appendedLineBegin = false;
				}
				if (isNewLine) {
					isNewLine = false;
					if ((c == '#') || (c == '!')) {
						isCommentLine = true;
						continue;
					}
				}

				if (c != '\n' && c != '\r') {
					lineBuf[len++] = c;
					if (len == lineBuf.length) {
						int newLength = lineBuf.length * 2;
						if (newLength < 0) {
							newLength = Integer.MAX_VALUE;
						}
						char[] buf = new char[newLength];
						System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
						lineBuf = buf;
					}
					// flip the preceding backslash flag
					if (c == '\\') {
						precedingBackslash = !precedingBackslash;
					} else {
						precedingBackslash = false;
					}
				} else {
					// reached EOL
					if (isCommentLine || (len == 0)) {
						isCommentLine = false;
						isNewLine = true;
						skipWhiteSpace = true;
						len = 0;
						continue;
					}
					if (inOff >= inLimit) {
						inLimit = (inStream == null) ? reader.read(inCharBuf) : inStream.read(inByteBuf);
						inOff = 0;
						if (inLimit <= 0) {
							return len;
						}
					}
					if (precedingBackslash) {
						len -= 1;
						// skip the leading whitespace characters in following line
						skipWhiteSpace = true;
						appendedLineBegin = true;
						precedingBackslash = false;
						if (c == '\r') {
							skipLF = true;
						}
					} else {
						return len;
					}
				}
			}
		}
	}

	/*
	 * Converts encoded &#92;uxxxx to unicode chars and changes special saved chars to their original forms
	 */
	private String loadConvert(final char[] in, int off, final int len, char[] convtBuf) {
		if (convtBuf.length < len) {
			int newLen = (len * 2);
			if (newLen < 0) {
				newLen = Integer.MAX_VALUE;
			}
			convtBuf = new char[newLen];
		}
		final char[] out = convtBuf;
		final int end = off + len;
		char aChar;
		int outLen = 0;
		while (off < end) {
			aChar = in[off++];
			if (aChar == '\\') {
				aChar = in[off++];
				if (aChar == 'u') {
					// Read the xxxx
					int value = 0;
					for (int i = 0; i < 4; i++) {
						aChar = in[off++];
						if ((aChar >= '0') && (aChar <= '9')) {
							value = (value << 4) + aChar - '0';
						} else if ((aChar >= 'a') && (aChar <= 'f')) {
							value = (value << 4) + 0xA + aChar - 'a';
						} else if ((aChar >= 'A') && (aChar <= 'F')) {
							value = (value << 4) + 0xA + aChar - 'A';
						} else {
							throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
						}
					}
					out[outLen++] = (char) value;
				} else {
					if (aChar == 't')
						aChar = '\t';
					else if (aChar == 'r')
						aChar = '\r';
					else if (aChar == 'n')
						aChar = '\n';
					else if (aChar == 'f')
						aChar = '\f';
					out[outLen++] = aChar;
				}
			} else {
				out[outLen++] = (char) aChar;
			}
		}
		return new String(out, 0, outLen);
	}

	/**
	 * This method outputs the comments, properties keys and values in the same format as specified in
	 * {@link java.util.Properties#store(OutputStream, String)}
	 * <p>
	 * After the entries have been written, the output stream is flushed. The output stream remains open after
	 * this method returns.
	 * <p>
	 * 
	 * @param out
	 *            an output stream.
	 * @param comments
	 *            a description of the property list.
	 * @exception IOException
	 *                if writing this property list to the specified output stream throws an
	 *                <tt>IOException</tt>.
	 * @exception ClassCastException
	 *                if this <code>Properties</code> object contains any keys or values that are not
	 *                <code>Strings</code>.
	 * @exception NullPointerException
	 *                if <code>out</code> is null.
	 */
	public void store(final OutputStream out, final String comments) throws IOException {
		store0(new BufferedWriter(new OutputStreamWriter(out, ISOLatin1)), comments, true);
	}

	/**
	 * This method outputs the comments, properties keys and values in the same format as specified in
	 * {@link java.util.Properties#store(Writer, String)}
	 * <p>
	 * After the entries have been written, the output stream is flushed. The output stream remains open after
	 * this method returns.
	 * <p>
	 * 
	 * @param writer
	 *            an output character stream writer.
	 * @param comments
	 *            a description of the property list.
	 * @exception IOException
	 *                if writing this property list to the specified output stream throws an
	 *                <tt>IOException</tt>.
	 * @exception ClassCastException
	 *                if this <code>Properties</code> object contains any keys or values that are not
	 *                <code>Strings</code>.
	 * @exception NullPointerException
	 *                if <code>writer</code> is null.
	 */
	public void store(final Writer writer, final String comments) throws IOException {
		store0((writer instanceof BufferedWriter) ? (BufferedWriter) writer : new BufferedWriter(writer),
				comments, false);
	}

	private void store0(final BufferedWriter bw, final String comments, final boolean escUnicode)
			throws IOException {
		if (comments != null) {
			writeComments(bw, comments);
		}
		bw.write("#" + new Date().toString());
		bw.newLine();
		for (String key : map.keySet()) {
			String val = map.get(key);
			key = saveConvert(key, true, escUnicode);
			/*
			 * No need to escape embedded and trailing spaces for value, hence pass false to flag.
			 */
			val = saveConvert(val, false, escUnicode);
			bw.write(key + "=" + val);
			bw.newLine();
		}
		bw.flush();
	}

	/*
	 * Converts unicodes to encoded &#92;uxxxx and escapes special characters with a preceding slash
	 */
	private String saveConvert(final String theString, final boolean escapeSpace, final boolean escapeUnicode) {
		final int len = theString.length();
		int bufLen = (len * 2);
		if (bufLen < 0) {
			bufLen = Integer.MAX_VALUE;
		}
		final StringBuilder outBuffer = new StringBuilder(bufLen);

		for (int x = 0; x < len; x++) {
			final char aChar = theString.charAt(x);
			// Handle common case first, selecting largest block that
			// avoids the specials below
			if ((aChar > 61) && (aChar < 127)) {
				if (aChar == '\\') {
					outBuffer.append('\\');
					outBuffer.append('\\');
					continue;
				}
				outBuffer.append(aChar);
				continue;
			}
			switch (aChar) {
			case ' ':
				if (x == 0 || escapeSpace)
					outBuffer.append('\\');
				outBuffer.append(' ');
				break;
			case '\t':
				outBuffer.append('\\');
				outBuffer.append('t');
				break;
			case '\n':
				outBuffer.append('\\');
				outBuffer.append('n');
				break;
			case '\r':
				outBuffer.append('\\');
				outBuffer.append('r');
				break;
			case '\f':
				outBuffer.append('\\');
				outBuffer.append('f');
				break;
			case '=': // Fall through
			case ':': // Fall through
			case '#': // Fall through
			case '!':
				outBuffer.append('\\');
				outBuffer.append(aChar);
				break;
			default:
				if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
					outBuffer.append('\\');
					outBuffer.append('u');
					outBuffer.append(toHex((aChar >> 12) & 0xF));
					outBuffer.append(toHex((aChar >> 8) & 0xF));
					outBuffer.append(toHex((aChar >> 4) & 0xF));
					outBuffer.append(toHex(aChar & 0xF));
				} else {
					outBuffer.append(aChar);
				}
			}
		}
		return outBuffer.toString();
	}

	private static void writeComments(final BufferedWriter bw, final String comments) throws IOException {
		bw.write("#");
		final int len = comments.length();
		int current = 0;
		int last = 0;
		final char[] uu = new char[6];
		uu[0] = '\\';
		uu[1] = 'u';
		while (current < len) {
			final char c = comments.charAt(current);
			if ((c > '\u00ff') || (c == '\n') || (c == '\r')) {
				if (last != current)
					bw.write(comments.substring(last, current));
				if (c > '\u00ff') {
					uu[2] = toHex((c >> 12) & 0xf);
					uu[3] = toHex((c >> 8) & 0xf);
					uu[4] = toHex((c >> 4) & 0xf);
					uu[5] = toHex(c & 0xf);
					bw.write(new String(uu));
				} else {
					bw.newLine();
					if ((c == '\r') && (current != (len - 1)) && (comments.charAt(current + 1) == '\n')) {
						current++;
					}
					if ((current == (len - 1))
							|| ((comments.charAt(current + 1) != '#') && (comments.charAt(current + 1) != '!'))) {
						bw.write("#");
					}
				}
				last = current + 1;
			}
			current++;
		}
		if (last != current)
			bw.write(comments.substring(last, current));
		bw.newLine();
	}

	/** A table of hex digits */
	private static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C',
			'D', 'E', 'F' };

	/**
	 * Convert a nibble to a hex character
	 * 
	 * @param nibble
	 *            the nibble to convert.
	 */
	private static char toHex(int nibble) {
		return hexDigit[(nibble & 0xF)];
	}
}
