package org.infra.preferences;

import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * For use in User Preferences
 */
class EmptyPreferences extends AbstractPreferences {
	protected EmptyPreferences() {
		super(null, "");
	}

	@Override
	protected void removeSpi(String key) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void removeNodeSpi() throws BackingStoreException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void putSpi(final String key, final String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException {
		return new String[0];
	}

	@Override
	protected String getSpi(final String key) {
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
	}
}
