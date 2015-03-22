package org.javastack.preferences;

import java.util.WeakHashMap;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * StandalonePreferencesFactory (one file per package)
 * 
 * <pre>
 * Usage:
 * 
 * -Djava.util.prefs.PreferencesFactory=org.javastack.preferences.StandalonePreferencesFactory
 * </pre>
 * 
 * @see StandalonePreferences
 */
public class StandalonePreferencesFactory implements PreferencesFactory {
	private WeakHashMap<ClassLoader, StandalonePreferences> SYSTEM_ROOT = new WeakHashMap<ClassLoader, StandalonePreferences>();
	private EmptyPreferences USER_ROOT = new EmptyPreferences();

	@Override
	public synchronized Preferences systemRoot() {
		final ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
		StandalonePreferences p = SYSTEM_ROOT.get(ctxLoader);
		if ((p == null) || p.isStaled()) {
			p = new StandalonePreferences(null, "");
			SYSTEM_ROOT.put(ctxLoader, p);
		}
		return p;
	}

	@Override
	public synchronized Preferences userRoot() {
		return USER_ROOT;
	}

	/**
	 * Clear loaded preferences (cache)
	 */
	protected synchronized void clear() {
		SYSTEM_ROOT.clear();
		USER_ROOT = new EmptyPreferences();
	}
}
