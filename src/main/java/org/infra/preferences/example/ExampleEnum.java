package org.infra.preferences.example;

import java.util.prefs.Preferences;

public enum ExampleEnum {
	/**
	 * My Test Key
	 */
	MYKEY("my-default-value1"),
	/**
	 * Other Test Key
	 */
	OTHER_KEY("my-default-value3"),
	//
	;
	//
	private static final Preferences conf;
	private final String keyName;
	private final String defaultValue;

	static {
		conf = Preferences.systemNodeForPackage(ExampleEnum.class);
	}

	ExampleEnum(final String defaultValue) {
		this.keyName = name().toLowerCase().replace('_', '.');
		this.defaultValue = defaultValue;
	}

	public String get() {
		return conf.get(keyName, defaultValue);
	}

	public int getInt() {
		return Integer.parseInt(get());
	}

	public long getLong() {
		return Long.parseLong(get());
	}

	public boolean getBoolean() {
		return Boolean.parseBoolean(get());
	}

	/**
	 * Simple Test
	 */
	public static void main(final String[] args) throws Throwable {
		System.out.println(ExampleEnum.MYKEY.get());
		System.out.println(ExampleEnum.OTHER_KEY.get());
	}
}
