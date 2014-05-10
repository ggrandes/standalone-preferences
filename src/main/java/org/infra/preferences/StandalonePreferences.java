package org.infra.preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

import org.infra.preferences.MapExpression.InvalidExpression;

/**
 * Usage:
 * 
 * <pre>
 * -Djava.util.prefs.PreferencesFactory=org.infra.preferences.StandalonePreferencesFactory
 * -Dorg.infra.preferences.source=filename
 * Default file: ${user.home}/sysprefs.properties
 * </pre>
 */
public class StandalonePreferences extends AbstractPreferences {
	private static final Logger log = Logger.getLogger(StandalonePreferences.class.getName());
	private static final String packageName = StandalonePreferences.class.getPackage().getName();
	private static final String PROP_SOURCE_NAME = packageName + ".source";
	private static final String PROP_EVAL_DISABLED_NAME = packageName + ".evalget.disabled";
	private static final File PROP_SOURCE_DEF_VALUE;
	private static final File SOURCE_FILE;
	private static final boolean PROP_EVAL_DISABLED;
	private final StringProperties data;

	static {
		PROP_SOURCE_DEF_VALUE = new File(System.getProperty("user.home"), "sysprefs.properties");
		PROP_EVAL_DISABLED = Boolean.getBoolean(PROP_EVAL_DISABLED_NAME);
		String source = System.getProperty(PROP_SOURCE_NAME);
		if (source != null) {
			try {
				source = new MapExpression(source).eval().get();
			} catch (InvalidExpression e) {
				log.log(Level.WARNING, "Error in eval of " + source + ": " + e.toString());
				source = null;
			}
		}
		SOURCE_FILE = (source != null ? new File(source) : PROP_SOURCE_DEF_VALUE);
	}

	protected StandalonePreferences(final StandalonePreferences parent, final String name) {
		super(parent, name);
		if (parent == null) {
			data = new StringProperties().getRootView();
			load();
		} else {
			data = parent.getSubView(name);
		}
	}

	private final void load() {
		InputStream is = null;
		try {
			is = new FileInputStream(SOURCE_FILE);
			data.getRootView().load(is);
		} catch (IOException e) {
			log.log(Level.WARNING, "Error loading StandalonePreferences from file " + //
					SOURCE_FILE + ": " + e.toString());
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (Exception e) {
			}
		}
	}

	private final void save() throws IOException {
		OutputStream os = null;
		try {
			os = new FileOutputStream(SOURCE_FILE);
			data.getRootView().store(os, this.getClass().getName());
		} finally {
			try {
				if (os != null)
					os.close();
			} catch (Exception e) {
			}
		}
	}

	private StringProperties getSubView(final String name) {
		return data.getSubView(name);
	}

	@Override
	protected String getSpi(final String key) {
		if (PROP_EVAL_DISABLED)
			return data.getProperty(key);
		try {
			return data.getPropertyEval(key);
		} catch (InvalidExpression e) {
			log.log(Level.WARNING, "Error in eval of " + absolutePath() + "/" + key + ": " + e.toString());
			return data.getProperty(key);
		}
	}

	@Override
	protected void putSpi(final String key, final String value) {
		data.setProperty(key, value);
	}

	@Override
	protected void removeSpi(final String key) {
		data.removeProperty(key);
	}

	@Override
	protected void removeNodeSpi() throws BackingStoreException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException {
		final Set<String> names = data.stringFirstLevelPropertyNames();
		final ArrayList<String> validNames = new ArrayList<String>();
		for (final String name : names) {
			if (data.getProperty(name) != null)
				validNames.add(name);
		}
		return validNames.toArray(new String[validNames.size()]);
	}

	@Override
	protected String[] childrenNamesSpi() throws BackingStoreException {
		final Set<String> names = data.stringFirstLevelPropertyNames();
		final ArrayList<String> validNames = new ArrayList<String>();
		for (final String name : names) {
			if (data.getProperty(name) == null)
				validNames.add(name);
		}
		return validNames.toArray(new String[validNames.size()]);
	}

	@Override
	protected AbstractPreferences childSpi(final String name) {
		return new StandalonePreferences(this, name);
	}

	@Override
	protected void syncSpi() throws BackingStoreException {
	}

	@Override
	protected void flushSpi() throws BackingStoreException {
	}

	@Override
	public void sync() throws BackingStoreException {
		flush();
	}

	@Override
	public void flush() throws BackingStoreException {
		try {
			save();
		} catch (IOException e) {
			throw new BackingStoreException(e);
		}
	}
}
