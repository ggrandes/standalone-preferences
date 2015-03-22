# Standalone-Preferences

Java Preferences API Implementation on Filesystem. Open Source Java project under Apache License v2.0

### Current Stable Version is [1.2.2](https://search.maven.org/#search|ga|1|g%3Aorg.javastack%20a%3Astandalone-preferences)

---

## Features

 - Small Footprint
 - Allow eval of get with System Properties and Back reference with ```${tagname}```
 - Allow disable eval on get (see configuration).
 - Config file per package.
 - System Preferences are supported, YES.
 - User Preferences are NOT supported (intentionally).
 - Allow ClassLoaderMap properties in ```org.javastack.preferences.sourcedir```

## DOC


#### Configuration: System Properties

 - Standard Java Preferences API for select the factory: ```java.util.prefs.PreferencesFactory```
   - Example: -Djava.util.prefs.PreferencesFactory=org.javastack.preferences.StandalonePreferencesFactory
 - Select source of System Preferences: ```org.javastack.preferences.sourcedir=directoryName``` (allow evaluation)
   - Example: -Dorg.javastack.preferences.sourcedir=${user.home}/myprefs/
   - Default value: ${user.home}/sysprefs/
 - For disable Eval of get (Global): ```org.javastack.preferences.evalget.disabled=true```
 - For autoexpire cache of preferences (Global): ```org.javastack.preferences.stale.millis=180000```
   - Default value: 0 (no expire)

#### Configuration: Local Properties

 - For disable Eval of get (Preferences Node): ```preferences.evalget.disabled=true```


#### Usage Example (basic)

```java
package org.javastack.preferences.example;

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
		System.out.println(config.get("urlbase", "my-default-value4"));
		System.out.println(config.get("urlsearch", "my-default-value5"));
	}
}
```

#### Usage Example (enum based)

```java
package org.javastack.preferences.example;

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
```

* More examples in [Example package](https://github.com/ggrandes/standalone-preferences/tree/master/src/main/java/org/javastack/preferences/example)
* More info: [Preferences API](http://docs.oracle.com/javase/7/docs/api/java/util/prefs/Preferences.html)



#### Example Config 

###### ```${user.home}/sysprefs/org.javastack.preferences.example.properties```

```properties
mykey=my-config-value
other.key=my-config-value-for-other
urlbase=https://www.acme.com
urlsearch=${urlbase}/search?user=${user.name}
# For disable Eval uncomment this line
#preferences.evalget.disabled=true
```


#### Running standalone

```
java -cp standalone-preferences-X.X.X.jar org.javastack.preferences.example.Example
```


#### Sample Output

```
my-config-value
my-default-value2
my-config-value-for-other
https://www.acme.com
https://www.acme.com/search?user=developer
```

#### Running in Tomcat Standalone

```
Copy standalone-preferences-X.X.X.jar to ${CATALINA_HOME}/lib/
You can set your CATALINA_OPTS="-Dorg.javastack.preferences.sourcedir=\${CATALINA_BASE}/appconf/ -Djava.util.prefs.PreferencesFactory=org.javastack.preferences.StandalonePreferencesFactory"
```

###### Note: Don't copy standalone-preferences-X.X.X.jar to your WEB-INF/lib/ (classloader problems)

#### Running in Tomcat in Eclipse

```
-Dorg.javastack.preferences.sourcedir=${workspace_loc:/Servers}/appconf/$"{servlet.ContextName:noname}"
-Djava.util.prefs.PreferencesFactory=org.javastack.preferences.StandalonePreferencesFactory
```

###### Note: If you want use servlet.ContextName you need [ClassLoaderMap](https://github.com/ggrandes/classloadermap)

#### Running Inside Spring

```xml
<!-- Config -->
<bean id="preferencePlaceHolder" 
        class="org.springframework.beans.factory.config.PreferencesPlaceholderConfigurer">
    <property name="systemTreePath" value="com.acme.foobar.package.name" />
</bean>
<!-- Example -->
<bean id="dataSource" 
        class="org.springframework.jdbc.datasource.DriverManagerDataSource">
    <property name="driverClassName" value="${jdbc.driverClassName}" />
    <property name="url" value="${jdbc.url}" />
    <property name="username" value="${jdbc.username}" />
    <property name="password" value="${jdbc.password}" />
</bean>
```


---

## MAVEN

Add the dependency to your pom.xml:

    <dependency>
        <groupId>org.javastack</groupId>
        <artifactId>standalone-preferences</artifactId>
        <version>1.2.2</version>
        <scope>provided</scope>
    </dependency>

---
Inspired in [Java Preferences API](http://docs.oracle.com/javase/7/docs/technotes/guides/preferences/index.html) and [Apache Commons Configuration](http://commons.apache.org/configuration/), this code is Java-minimalistic version.
