package org.javastack.preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

import org.javastack.mapexpression.InvalidExpression;
import org.javastack.mapexpression.MapExpression;
import org.javastack.stringproperties.StringProperties;

/**
 * StandalonePreferencesFactory (one file per package)
 * 
 * <pre>
 * Usage:
 * 
 * -Djava.util.prefs.PreferencesFactory=org.javastack.preferences.StandalonePreferencesFactory
 * -Dorg.javastack.preferences.sourcedir=directoryName
 * Default dir: ${user.home}/sysprefs/
 * </pre>
 */
public class StandalonePreferences extends AbstractPreferences {
	private static final Logger log = Logger.getLogger(StandalonePreferences.class.getName());
	private static final String packageName = StandalonePreferences.class.getPackage().getName();
	private static final String PROP_SOURCE_DIR = packageName + ".sourcedir";
	private static final String PROP_GLOBAL_EVAL_DISABLED_NAME = packageName + ".evalget.disabled";
	private static final String PROP_LOCAL_EVAL_DISABLED_NAME = "preferences.evalget.disabled";
	private static final File PROP_SOURCE_DIR_DEF_VALUE;
	private static MapExpression SOURCE_EXPR = null;
	private static final boolean globalEvalDisabled;
	private static final String ROOT_NAME = "ROOT";
	private static final String FILE_EXTENSION = ".properties";
	private final File SOURCE_DIR;
	private final String fileName;
	private final File file;
	private final StringProperties data;
	private boolean nodeEvalDisabled = false;
	private boolean isDirty = false;

	static {
		PROP_SOURCE_DIR_DEF_VALUE = new File(System.getProperty("user.home"), "sysprefs");
		globalEvalDisabled = Boolean.getBoolean(PROP_GLOBAL_EVAL_DISABLED_NAME);
		final String exp = System.getProperty(PROP_SOURCE_DIR);
		if (exp != null) {
			try {
				SOURCE_EXPR = new MapExpression(exp, null, CustomMapper.getInstance(), true);
			} catch (InvalidExpression e) {
				log.log(Level.WARNING, "Error in eval of " + exp + ": " + e.toString());
			}
		}
	}

	protected StandalonePreferences(final StandalonePreferences parent, final String name) {
		super(parent, name);
		SOURCE_DIR = getSourceDir();
		fileName = getFileName();
		file = new File(SOURCE_DIR, fileName + FILE_EXTENSION);
		data = new StringProperties().getRootView();
		load();
		nodeEvalDisabled = Boolean.parseBoolean(data.getProperty(PROP_LOCAL_EVAL_DISABLED_NAME, "false"));
	}

	private final File getSourceDir() {
		if (SOURCE_EXPR != null) {
			try {
				final StringBuilder sb = new StringBuilder();
				SOURCE_EXPR.eval(sb); // Thread-Safe
				return new File(sb.toString());
			} catch (InvalidExpression e) {
				final String exp = SOURCE_EXPR.getExpression();
				log.log(Level.WARNING, "Error in eval of " + exp + ": " + e.toString());
			}
		}
		return PROP_SOURCE_DIR_DEF_VALUE;
	}

	private final String getFileName() {
		String name = absolutePath();
		if (name.charAt(0) == '/')
			name = name.substring(1);
		name = name.replace('/', '.');
		if (name.isEmpty())
			name = ROOT_NAME;
		return name;
	}

	private final void load() {
		if (!file.exists()) {
			log.log(Level.WARNING, "File for StandalonePreferences not exists " + file);
			return;
		}
		log.log(Level.INFO, "Loading StandalonePreferences from file " + file);
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			data.getRootView().load(is);
		} catch (IOException e) {
			log.log(Level.WARNING, "Error loading StandalonePreferences from file " + //
					file + ": " + e.toString());
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (Exception e) {
			}
		}
	}

	private final void save() throws IOException {
		log.log(Level.INFO, "Saving StandalonePreferences to file " + file);
		OutputStream os = null;
		try {
			os = new FileOutputStream(file);
			data.getRootView().store(os, fileName);
			isDirty = false;
		} finally {
			try {
				if (os != null)
					os.close();
			} catch (Exception e) {
			}
		}
	}

	@Override
	protected String getSpi(final String key) {
		if (globalEvalDisabled || nodeEvalDisabled)
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
		if (PROP_LOCAL_EVAL_DISABLED_NAME.equals(key)) {
			nodeEvalDisabled = Boolean.parseBoolean(value);
		}
		isDirty = true;
		data.setProperty(key, value);
	}

	@Override
	protected void removeSpi(final String key) {
		isDirty = true;
		data.removeProperty(key);
	}

	@Override
	protected void removeNodeSpi() throws BackingStoreException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException {
		final Set<String> names = data.stringPropertyNames();
		return names.toArray(new String[names.size()]);
	}

	@Override
	protected String[] childrenNamesSpi() throws BackingStoreException {
		final String baseName = (ROOT_NAME.equals(fileName) ? "" : fileName + ".");
		final FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				final File file = new File(dir, name);
				if (!file.isFile())
					return false;
				if (!name.endsWith(FILE_EXTENSION))
					return false;
				if (!name.startsWith(baseName))
					return false;
				return true;
			}
		};
		final LinkedHashSet<String> subs = new LinkedHashSet<String>();
		for (String candidate : SOURCE_DIR.list(filter)) {
			if ((baseName.length() + FILE_EXTENSION.length()) > candidate.length())
				continue;
			candidate = candidate.substring(baseName.length(), candidate.length() - FILE_EXTENSION.length());
			final int offset = candidate.indexOf('.');
			if (offset >= 0) {
				candidate = candidate.substring(0, offset);
			}
			if (!candidate.isEmpty())
				subs.add(candidate);
		}
		return subs.toArray(new String[subs.size()]);
	}

	@Override
	protected AbstractPreferences childSpi(final String name) {
		return new StandalonePreferences(this, name);
	}

	@Override
	protected void syncSpi() throws BackingStoreException {
		flushSpi();
	}

	@Override
	protected void flushSpi() throws BackingStoreException {
		try {
			if (isDirty)
				save();
		} catch (IOException e) {
			throw new BackingStoreException(e);
		}
	}
}
