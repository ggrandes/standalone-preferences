package org.infra.preferences.example;

import java.util.prefs.Preferences;

public class Example {
	/**
	 * Simple Test
	 */
	public static void main(final String[] args) throws Throwable {
		final Preferences config = Preferences.systemNodeForPackage(Example.class);
		System.out.println(config.get("mykey", "my-default-value1"));
		System.out.println(config.get("mykey2", "my-default-value2"));
		System.out.println(config.get("other.key", "my-default-value3"));
	}
}
