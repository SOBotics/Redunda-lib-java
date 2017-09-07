# Redunda-lib-java
A Java-library to communicate with [Redunda](https://redunda.sobotics.org).

# Implementation

### Add dependency

To implement this library in your Java bot, you need to add this to your `pom.xml`:

```xml
<dependency>
		<groupId>org.sobotics</groupId>
		<artifactId>redunda-lib</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Initialize

Right after launching your bot, you should initialize `PingService`. This should happen before your bot fetches anything from the Stack Exchange API.

```
PingService redunda = new PingService("your api-key", "bot version");
redunda.start();
```

<small>You can get your API-key in the instances overview of your bot.</small>

### Check the standby status

Before your scheduled executors fetch anything from the Stack Exchange API or respond to commands, you should check if the instance is on standby:

```
boolean standbyMode = PingService.standby.get();
```

If `standbyMode` is `true`, **DON'T** execute the code!

### Debug mode

If you don't want to use the standby mode while debugging, you can set this with the following line:

```
redunda.setDebugging(true);
```

This will prevent `PingService.standby` from becoming `true` and stops pinging the server.

# Documentation

The full documentation is available at [redunda-java.sobotics.org](http://redunda-java.sobotics.org)
