# Standalone-Preferences

Java Preferences API Implementation on Filesystem. Open Source Java project under Apache License v2.0

### Current Version is [1.0.0](https://maven-release.s3.amazonaws.com/release/org/infra/standalone-preferences/1.0.0/standalone-preferences-1.0.0.jar)

---

## Features

 - No external dependencies
 - Allow eval of get with System Properties and Back reference with ```${tagname}```
 - Allow disable eval on get (see configuration)

## DOC


#### Configuration: System Properties


 - Standard Java Preferences API for select the factory: ```java.util.prefs.PreferencesFactory```
   - Example: -Djava.util.prefs.PreferencesFactory=org.infra.preferences.StandalonePreferencesFactory
 - Select source of System Preferences: ```org.infra.preferences.source=filename``` (allow evaluation)
   - Example: -Dorg.infra.preferences.source=${user.home}/mypreferences.properties
   - Default value: ${user.home}/sysprefs.properties   
 - For disable Eval of get: ```org.infra.preferences.evalget.disabled=true```


#### Usage Example

```java
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
		System.out.println(config.get("urlbase", "my-default-value4"));
		System.out.println(config.get("urlsearch", "my-default-value5"));
	}
}
```

* More info: [Preferences API](http://docs.oracle.com/javase/7/docs/api/java/util/prefs/Preferences.html)


#### Example Config ```${user.home}/sysprefs.properties```

```properties
org.infra.preferences.example.mykey=my-config-value
org.infra.preferences.example.other.key=my-config-value-for-other
org.infra.preferences.example.urlbase=https://www.acme.com
org.infra.preferences.example.urlsearch=${urlbase}/search?user=${user.name}
```


#### Running

```
java -cp standalone-preferences-X.X.X.jar org.infra.preferences.example.Example
```


#### Sample Output

```
my-config-value
my-default-value2
my-config-value-for-other
https://www.acme.com
https://www.acme.com/search?user=developer
```

---

## MAVEN

Add the maven repository location to your pom.xml: 

    <repositories>
        <repository>
            <id>ggrandes-maven-s3-repo</id>
            <url>https://maven-release.s3.amazonaws.com/release/</url>
        </repository>
    </repositories>

Add the dependency to your pom.xml:

    <dependency>
        <groupId>org.infra</groupId>
        <artifactId>standalone-preferences</artifactId>
        <version>1.0.0</version>
    </dependency>

---
Inspired in [Java Preferences API](http://docs.oracle.com/javase/7/docs/technotes/guides/preferences/index.html) and [Apache Commons Configuration](http://commons.apache.org/configuration/), this code is Java-minimalistic version.
