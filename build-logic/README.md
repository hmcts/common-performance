# Shared Gradle build logic

This directory contains the shared Gradle configuration used by the Gatling performance test repositories.

The main configuration is in `src/main/groovy/performance.gradle`

It configures:

- Gradle plugins
- Java 21
- Maven repositories
- OWASP Dependency Check
- Netty version alignment
- Gatling source directories
- The runSimulation task
- The generateStats task

Plugin and dependency versions are defined in `gradle.properties`

### Using the plugin

The parent repository must include this build in its `settings.gradle`:

```groovy
pluginManagement {
    includeBuild('common/common-performance/build-logic')
}
```

The existing parent `build.gradle` can be replaced, by simply apply the shared configuration 
and defining the necessary customisations:

```groovy
plugins {
    id 'performance'
}

performance {
    simulationClass = 'simulations.ExampleSimulation'
    transactionNamesToGraph = ['ExampleTransaction']
}
```

Use an empty list when no transactions need to be added to the Jenkins Gatling graphs:

```groovy
transactionNamesToGraph = []
```

Repository-specific Gradle configuration can still be added to the parent `build.gradle` when required.