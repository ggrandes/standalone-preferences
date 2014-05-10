package org.infra.preferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.infra.preferences.MapExpression.InvalidExpression;

/**
 * The <code>StringProperties</code> class represents a persistent set of string properties. The
 * <code>StringProperties</code> can be saved or loaded from a stream. Each key and its corresponding value in
 * the property list is a string.
 * 
 * <p>
 * Why this? standard java.util.Properties extends Hashtable <lu>
 * <li>no order</li>
 * <li>you can put no-string keys/values</li> </lu>
 * 
 * <p>
 * <code>StringProperties</code> NOT inherits from <code>Hashtable</code> or <code>Map</code>, to avoid
 * problems in legacy <code>Properties</code> object that can contains a non-<code>String</code> key or value.
 * 
 * <p>
 * The {@link #load(java.io.StringReader) load(StringReader)} <tt>/</tt>
 * {@link #store(java.io.Writer, java.lang.String) store(Writer, String)} methods load and store properties
 * from and to a character based stream in a simple line-oriented format specified below.
 * 
 * The {@link #load(java.io.InputStream) load(InputStream)} <tt>/</tt>
 * {@link #store(java.io.OutputStream, java.lang.String) store(OutputStream, String)} methods work the same
 * way as the load(Reader)/store(Writer, String) pair, except the input/output stream is encoded in ISO-8859-1
 * character encoding. Characters that cannot be directly represented in this encoding can be written using <a
 * href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.3">Unicode escapes</a> ; only a
 * single 'u' character is allowed in an escape sequence. The native2ascii tool can be used to convert
 * property files to and from other character encodings.
 * 
 * <p>
 * This class is thread-safe: multiple threads can share a single <tt>StringProperties</tt> object without the
 * need for external synchronization.
 * 
 * @see java.util.Properties
 * @see StringPropertiesRoot
 */
class StringProperties {
	public static final String SEPARATOR = ".";

	final LinkedHashMap<String, String> map;
	final StringMapPersist persist;
	final String rootPrefix;

	public StringProperties() {
		this(new LinkedHashMap<String, String>(), null);
	}

	StringProperties(final StringProperties source, final String rootPrefix) {
		this(source.map, appendPrefix(source.rootPrefix, rootPrefix));
	}

	StringProperties(final LinkedHashMap<String, String> map, final String rootPrefix) {
		this.map = map;
		this.persist = new StringMapPersist(map);
		this.rootPrefix = ((rootPrefix != null) && rootPrefix.isEmpty() ? null : rootPrefix);
	}

	private static final String appendPrefix(final String rootPrefix, final String prefix) {
		if (prefix == null)
			return null;
		if (rootPrefix != null)
			return rootPrefix + prefix + SEPARATOR;
		return prefix + SEPARATOR;
	}

	// VIEWS

	public StringPropertiesRoot getRootView() {
		return new StringPropertiesRoot(this, null);
	}

	public StringProperties getSubView(final String prefix) {
		return new StringProperties(this, prefix);
	}

	// SET

	public String setProperty(String key, final String value) {
		synchronized (map) {
			if (rootPrefix != null)
				key = rootPrefix + key;
			return map.put(key, value);
		}
	}

	public StringProperties setMultiIndex(final String... params) {
		return setMultiIndex(0, params);
	}

	public StringProperties setMultiIndex(final int beginIndex, final String... params) {
		return setMultiIndex(beginIndex, Arrays.asList(params));
	}

	public StringProperties setMultiIndex(final List<String> params) {
		return setMultiIndex(0, params);
	}

	public StringProperties setMultiIndex(final int beginIndex, final List<String> params) {
		int i = beginIndex;
		for (final String p : params) {
			setProperty(Integer.toString(i), p);
			i++;
		}
		return this;
	}

	// REMOVE

	public String removeProperty(String key) {
		synchronized (map) {
			if (rootPrefix != null)
				key = rootPrefix + key;
			return map.remove(key);
		}
	}

	public StringProperties removeMultiIndex() {
		return removeMultiIndex(0);
	}

	public StringProperties removeMultiIndex(final int beginIndex) {
		int i = beginIndex;
		while (removeProperty(Integer.toString(i++)) != null)
			continue;
		return this;
	}

	// GET

	public String getProperty(String key) {
		synchronized (map) {
			if (rootPrefix != null)
				key = rootPrefix + key;
			return map.get(key);
		}
	}

	public String getProperty(String key, final String defaultValue) {
		synchronized (map) {
			if (rootPrefix != null)
				key = rootPrefix + key;
			final String value = map.get(key);
			return ((value != null) ? value : defaultValue);
		}
	}

	public StringPropertiesRoot getPropertyAsProperties(final String key, final char separator) {
		final String value = getProperty(key);
		final StringPropertiesRoot p = new StringPropertiesRoot();
		p.load(new StringReader(value.replace(separator, '\n')));
		return p;
	}

	public List<String> getMultiIndex() {
		return getMultiIndex(0);
	}

	public List<String> getMultiIndex(final int beginIndex) {
		final ArrayList<String> list = new ArrayList<String>();
		int i = beginIndex;
		String p = null;
		while ((p = getProperty(Integer.toString(i++))) != null) {
			list.add(p);
		}
		return list;
	}

	// GET with MapExpression

	private static final String eval(final String str) throws InvalidExpression {
		if (str == null)
			return null;
		return new MapExpression(str).eval().get();
	}

	public String getPropertyEval(final String key) throws InvalidExpression {
		return eval(getProperty(key));
	}

	public String getPropertyEval(String key, final String defaultValue) throws InvalidExpression {
		return eval(getProperty(key, defaultValue));
	}

	public StringPropertiesRoot getPropertyAsPropertiesEval(final String key, final char separator)
			throws InvalidExpression {
		final String value = getPropertyEval(key);
		final StringPropertiesRoot p = new StringPropertiesRoot();
		p.load(new StringReader(value.replace(separator, '\n')));
		return p;
	}

	public List<String> getMultiIndexEval() throws InvalidExpression {
		return getMultiIndexEval(0);
	}

	public List<String> getMultiIndexEval(final int beginIndex) throws InvalidExpression {
		final ArrayList<String> list = new ArrayList<String>();
		int i = beginIndex;
		String p = null;
		while ((p = getPropertyEval(Integer.toString(i++))) != null) {
			list.add(p);
		}
		return list;
	}

	// LIST

	public void list(final PrintStream out) {
		list(out, false);
	}

	public void list(final PrintStream out, final boolean fullLine) {
		final StringBuilder sb = new StringBuilder();
		final int maxLength = (fullLine ? Integer.MAX_VALUE : 70);
		out.println("### BEGIN: listing properties ###");
		synchronized (map) {
			for (final String key : stringPropertyNames()) {
				final String value = getProperty(key);
				sb.setLength(0);
				sb.append(key, 0, Math.min(key.length(), maxLength)).append('=')
						.append(value, 0, Math.min(value.length(), maxLength));
				if (sb.length() > maxLength) {
					sb.setLength(maxLength);
					sb.append("...");
				}
				out.println(sb.toString());
			}
		}
		out.println("### END ###");
	}

	public Set<String> stringFirstLevelPropertyNames() {
		return stringPropertyNames(true);
	}

	public Set<String> stringPropertyNames() {
		return stringPropertyNames(false);
	}

	private Set<String> stringPropertyNames(final boolean firstLevel) {
		synchronized (map) {
			final LinkedHashSet<String> set = new LinkedHashSet<String>();
			if ((rootPrefix == null) && !firstLevel) {
				set.addAll(map.keySet());
				return Collections.unmodifiableSet(set);
			}
			final int prefixLength = (rootPrefix == null ? 0 : rootPrefix.length());
			for (String key : map.keySet()) {
				if (rootPrefix == null) {
					set.add(firstLevel ? getFirstLevel(key) : key);
				} else {
					if (!key.startsWith(rootPrefix))
						continue;
					key = key.substring(prefixLength);
					set.add(firstLevel ? getFirstLevel(key) : key);
				}
			}
			return Collections.unmodifiableSet(set);
		}
	}

	private static final String getFirstLevel(final String key) {
		final int end = key.indexOf(SEPARATOR);
		if (end < 0)
			return key;
		return key.substring(0, end);
	}

	public Enumeration<String> propertyNames() {
		return new EnumeratorAdapter<String>(stringPropertyNames().iterator());
	}

	public static class StringPropertiesRoot extends StringProperties {
		public StringPropertiesRoot() {
			super();
		}

		StringPropertiesRoot(final StringProperties source, final String rootPrefix) {
			super(source, rootPrefix);
		}

		public RootViewMap getRootViewAsMap() {
			return new RootViewMap(this);
		}

		// Import / Export Legacy Properties

		public void putAll(final Properties p) {
			synchronized (map) {
				for (final String key : p.stringPropertyNames()) {
					map.put(key, p.getProperty(key));
				}
			}
		}

		public Properties toProperties() {
			synchronized (map) {
				final Properties p = new Properties();
				for (final Map.Entry<String, String> e : map.entrySet()) {
					p.setProperty(e.getKey(), e.getValue());
				}
				return p;
			}
		}

		// LOAD

		public void load(final InputStream inStream) throws IOException {
			synchronized (map) {
				persist.load(inStream);
			}
		}

		public void load(final StringReader reader) {
			synchronized (map) {
				try {
					persist.load(reader);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		// STORE

		public void store(final OutputStream out, final String comments) throws IOException {
			// Persist over OutputStream is preferred over Writer (this escape unicode characters)
			synchronized (map) {
				persist.store(out, comments);
			}
		}

		// From interface Map<String, String>

		public void clear() {
			synchronized (map) {
				map.clear();
			}
		}

		public void putAll(final Map<? extends String, ? extends String> m) {
			synchronized (map) {
				map.putAll(m);
			}
		}

		public int size() {
			synchronized (map) {
				return map.size();
			}
		}

		public boolean isEmpty() {
			return (size() == 0);
		}
	}

	/**
	 * StringPropertiesRoot as Map
	 */
	public static class RootViewMap implements Map<String, String> {
		private final StringPropertiesRoot properties;

		public RootViewMap(final StringProperties properties) {
			this.properties = properties.getRootView();
		}

		@Override
		public int size() {
			return properties.size();
		}

		@Override
		public boolean isEmpty() {
			return properties.isEmpty();
		}

		@Override
		public boolean containsKey(final Object key) {
			if (key instanceof String)
				return (properties.getProperty((String) key) != null);
			throw new IllegalArgumentException("key must be String");
		}

		@Override
		public String get(final Object key) {
			if (key instanceof String)
				return properties.getProperty((String) key);
			throw new IllegalArgumentException("key must be String");
		}

		@Override
		public String put(final String key, final String value) {
			return properties.setProperty(key, value);
		}

		@Override
		public String remove(final Object key) {
			if (key instanceof String)
				return properties.removeProperty((String) key);
			throw new IllegalArgumentException("key must be String");
		}

		@Override
		public void putAll(final Map<? extends String, ? extends String> m) {
			properties.putAll(m);
		}

		@Override
		public void clear() {
			properties.clear();
		}

		@Override
		public Set<String> keySet() {
			return properties.stringPropertyNames();
		}

		@Override
		public boolean containsValue(final Object value) {
			if (value instanceof String) {
				synchronized (properties.map) {
					return properties.map.containsValue(value);
				}
			}
			throw new IllegalArgumentException("value must be String");
		}

		@Override
		public Collection<String> values() {
			synchronized (properties.map) {
				return Collections.unmodifiableCollection(properties.map.values());
			}
		}

		@Override
		public Set<Map.Entry<String, String>> entrySet() {
			synchronized (properties.map) {
				return Collections.unmodifiableSet(properties.map.entrySet());
			}
		}

		@Override
		public String toString() {
			synchronized (properties.map) {
				return properties.map.toString();
			}
		}
	}

	/**
	 * Simple Test
	 * 
	 * @param args
	 * @throws Throwable
	 */
	public static void main(final String[] args) throws Throwable {
		StringPropertiesRoot p = new StringPropertiesRoot();
		p.setProperty("root1.key1", "test-root1-value-1");
		p.setProperty("root1.key2", "test-root1-value-2");
		p.setProperty("root2.key3", "test-root2-value-3");
		p.setProperty("root2.key4", "test-root2-value-4");
		p.list(System.out);
		StringProperties p1 = p.getSubView("root1");
		p1.list(System.out);
		p1.setProperty("key5", "test-root1-value-5");
		p1.list(System.out);
		StringProperties pr = p.getRootView();
		pr.list(System.out);
		StringProperties p2 = p.getSubView("root2");
		p2.list(System.out);
		System.out.println(p.getRootViewAsMap());
		p.store(System.out, null);
	}
}
