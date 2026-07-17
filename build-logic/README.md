# Shared Gradle build logic

This directory contains the shared Gradle build logic used by the Gatling performance test repositories.

The main configuration is in `src/main/groovy/performance.gradle`

It configures:

- Gradle plugins
- Java 21
- Maven repositories
- OWASP Dependency Check
- Netty version alignment
- Gatling source directories
- The `generateStats` task

Plugin and dependency versions are defined in `gradle.properties`

### Using the plugin

The parent repository must include this build in its `settings.gradle`:

```groovy
pluginManagement {
    includeBuild('common/common-performance/build-logic')
}
```

The existing parent `build.gradle` can then be simplified to applying the shared plugin and providing the repository-specific configuration:

```groovy
plugins {
    id 'performance'
}

performance {
    simulationClass = 'simulations.ExampleSimulation'
    transactionNamesToGraph = ['ExampleTransaction']
}
```
- `simulationClass` is mandatory and should point to the location of the Gatling simulation to be run.
- `transactionNamesToGraph` is optional. When omitted, no individual transactions are added to the Jenkins Gatling graphs.

### Repository-specific configuration

Repositories can still define additional Gradle or Gatling configuration in their own `build.gradle` when required.

As an example, Gatling uses the JVM’s standard heap sizing by default. 
Additional JVM arguments can be appended if a repository requires custom memory settings or other JVM options:

```groovy
gatling {
    jvmArgs += ['-Xms2048m', '-Xmx4096m']
}
```

This appends the additional JVM arguments while preserving the default JVM arguments configured by the Gatling Gradle plugin.