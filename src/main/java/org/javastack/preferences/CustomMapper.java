package org.javastack.preferences;

import org.javastack.classloadermap.ClassLoaderMap;
import org.javastack.mapexpression.mapper.Mapper;

class CustomMapper implements Mapper {
	private static final CustomMapper singleton = new CustomMapper();

	private CustomMapper() {
	}

	/**
	 * Map PropertyName in format "keyName:default-value" to
	 * {@link ClassLoaderMap#get(ClassLoader, String, String)} or {@link System#getProperty(String)} or
	 * {@code default-value}
	 * 
	 * @param propName string in format keyName[:default-value]
	 * @return maped value
	 */
	@Override
	public String map(final String propName) {
		if (propName == null)
			return null;
		String keyName = propName, defValue = null;
		final int offsetDefault = propName.indexOf(':');
		if (offsetDefault != -1) {
			keyName = propName.substring(0, offsetDefault);
			defValue = propName.substring(offsetDefault + 1);
		}
		final ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
		String value = ClassLoaderMap.get(ctxLoader, keyName, null);
		if (value == null) {
			value = System.getProperty(propName, defValue);
		}
		return value;
	}

	public static CustomMapper getInstance() {
		return singleton;
	}
}
