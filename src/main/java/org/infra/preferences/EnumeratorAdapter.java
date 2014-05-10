package org.infra.preferences;

import java.util.Enumeration;
import java.util.Iterator;

class EnumeratorAdapter<T> implements Enumeration<T> {
	private Iterator<T> i;

	public EnumeratorAdapter(final Iterator<T> i) {
		this.i = i;
	}

	@Override
	public boolean hasMoreElements() {
		return i.hasNext();
	}

	@Override
	public T nextElement() {
		return i.next();
	}
}
