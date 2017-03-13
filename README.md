# Redunda-lib-java
A Java-library to communicate with [Redunda](https://redunda.erwaysoftware.com).

# Implementation

## Add dependency

To implement this library in your Java bot, you need to add the Maven-dependency `org.sobotics.redunda-lib`.

## Initialize

Right after launching your bot, you should initialize `PingService`. This should happen before your bot fetches anything from the Stack Exchange API.

```
PingService redunda = new PingService("your api-key");
redunda.start();
```

<small>You can get your API-key in the instances overview of your bot.</small>

## Check the standby status

Before your scheduled executors fetch anything from the Stack Exchange API or respond to commands, you should check if the instance is on standby:

```
boolean standbyMode = PingService.standby.get();
```

If `standbyMode` is `true`, **DON'T** execute the code!
