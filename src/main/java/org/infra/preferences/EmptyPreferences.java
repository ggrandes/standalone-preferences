package org.infra.preferences;

import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * For use in User Preferences
 */
public class EmptyPreferences extends AbstractPreferences {
	public EmptyPreferences() {
		super(null, "");
	}

	protected EmptyPreferences(final AbstractPreferences parent, final String name) {
		super(parent, name);
	}

	@Override
	protected void removeSpi(String key) {
	}

	@Override
	protected void removeNodeSpi() throws BackingStoreException {
	}

	@Override
	protected void putSpi(final String key, final String value) {
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException {
		return new String[0];
	}

	@Override
	protected String getSpi(final String key) {
		throw null;
	}

	@Override
	protected void syncSpi() throws BackingStoreException {
	}

	@Override
	protected void flushSpi() throws BackingStoreException {
	}

	@Override
	protected String[] childrenNamesSpi() throws BackingStoreException {
		return new String[0];
	}

	@Override
	protected AbstractPreferences childSpi(final String name) {
		return new EmptyPreferences(this, name);
	}
}
