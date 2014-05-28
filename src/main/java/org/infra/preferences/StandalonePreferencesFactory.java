package org.infra.preferences;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * StandalonePreferencesFactory (one file per package)
 * <pre>
 * Usage:
 * 
 * -Djava.util.prefs.PreferencesFactory=org.infra.preferences.StandalonePreferencesFactory
 * </pre>
 * 
 * @see StandalonePreferences
 */
public class StandalonePreferencesFactory implements PreferencesFactory {
	private static Preferences SYSTEM_ROOT;
	private static EmptyPreferences USER_ROOT = new EmptyPreferences();

	@Override
	public synchronized Preferences systemRoot() {
		if (SYSTEM_ROOT == null) {
			SYSTEM_ROOT = new StandalonePreferences(null, "");
		}
		return SYSTEM_ROOT;
	}

	@Override
	public Preferences userRoot() {
		return USER_ROOT;
	}
}
