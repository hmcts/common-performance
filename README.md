# common-performance

A shared library of Gatling utilities designed to make performance testing easier and more powerful across multiple Gatling projects.

This repository is intended to be imported into Gatling projects as a **Git submodule**, allowing common functionality to be reused and updated centrally.

---

# 📚 Contents

- [About Git Submodules](#-about-git-submodules)
- [Setup Instructions](#-setup-instructions)
- [ElasticSearch Feeder](#-elasticsearch-feeder)
    - [Usage in Simulation](#-usage-in-simulation)
    - [Config Overrides](#-config-overrides)
    - [ElasticSearch Tunnel Setup](#-elasticsearch-tunnel-setup)
- [Stats Generator](#-stats-generator)
    - [Setup in build.gradle](#-setup-in-buildgradle)
    - [Customising Transaction Names](#-customising-transaction-names)
- [Utilities](#-utilities)
    - [DateUtils](#-dateutils)
- [Running Tests](#-running-tests)
- [Updating common-performance](#-updating-common-performance)

---

# 🔗 About Git Submodules

A Git submodule is a repository embedded inside another repository.  
Submodules allow you to keep a Git repository as a subdirectory of another Git repository.

> ⚡ **Important:**
> - Submodules are _not_ automatically updated.
> - They point to a specific commit of the submodule repo.
> - Updates to the submodule must be pulled manually.
> - Changes to the submodule need to be committed separately in your project.

---

# ⚙️ Setup Instructions

To add `common-performance` as a submodule into an existing Gatling project, run:

```bash
git submodule add git@github.com:hmcts/common-performance.git common/common-performance
```

Then update your project files as follows:

### 1. `.gitmodules`

Ensure your `.gitmodules` file includes:

```text
[submodule "common/common-performance"]
    path = common/common-performance
    url = git@github.com:hmcts/common-performance.git
```

### 2. `settings.gradle`

Include the common-performance project:

```groovy
include ':common-performance'
```

### 3. `build.gradle`

Add `common-performance` as a dependency:

```groovy
implementation project(':common-performance')
```

---

After adding or updating a submodule, always run:

```bash
./gradlew clean build
```

to ensure the changes are picked up.

---

# 📊 ElasticSearch Feeder

A flexible feeder allowing Gatling scenarios to pull dynamic data from ElasticSearch.

## 🚀 Usage in Simulation

Add the following to your Gatling simulation:

```scala
ElasticSearchFeederConfig.set(UserElasticSearchFeederConfig) // required to override config defaults

val iterations = if (debugMode == "off") CalculateRecordsRequired.calculate(targetIterationsPerHour, rampUpDurationMins, testDurationMins, rampDownDurationMins) else 1

val caseIdFeeder = ElasticSearchCaseFeeder.feeder(esIndex, esQueryFilePath, feederType, iterations)
```
When calling `ElasticSearchFeeder.feeder()`, replace:
- esIndex value with the ElasticSearch index to perform the search against (listed in the ElasticSearchFeederConfig file)
  - example: `esIndex.ET-EnglandWales`
- esQueryFilePath with a location of your JSON file containing the ElasticSearch query string
  - example: `getClass.getClassLoader.getResource("elasticSearchQuery.json").getPath,`
  - note: the JSON file should reside within the `resources` folder (or a subfolder)
- FeederType with the FeederType you require (QUEUE, CIRCULAR, SHUFFLE, RANDOM)
  - example: `FeederType.QUEUE`

Example:
```scala
val caseIdFeeder = ElasticSearchCaseFeeder.feeder(
  esIndices.ET_EnglandWales,
  getClass.getClassLoader.getResource("elasticSearchQuery.json").getPath,
  FeederType.QUEUE,
  iterations)
```

For reference, `CalculateRecordsRequired.calculate` takes the following arguments, which are likely already defined in your simulation:

```scala
calculate(targetIterationsPerHour: Double, rampUpDurationMins: Int, testDurationMins: Int, rampDownDurationMins: Int)
```

Pass the feeder to your scenario function, for example:

```scala
CcdCacheWarm.getServiceToken(caseIdFeeder)
```

And update your scenario method to use the feeder:

```scala
def getServiceToken(caseIdFeeder: Iterator[Map[String, Any]]) =
  feed(caseIdFeeder)
  .exec(...)
```

Finally, you will need a JSON file containing a valid ElasticSearch 
query to execute against the `/_search` endpoint. This should return 
the `reference` field, as this contains the CCD case ID that will be extracted 
into the feeder (see example below).

<details>
  <summary>ElasticSearch Query Example (click to expand)</summary>

```JSON
{
  "_source": [
    "reference"
  ],
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "state": "Accepted"
          }
        },
        {
          "match": {
            "data.claimant": "Perf Test"
          }
        },
        {
          "match": {
            "data.preAcceptCase.dateAccepted": "2024-01-06"
          }
        }
      ]
    }
  }
}
  ```
</details>

---

## 🛠 Config Overrides

You can override default feeder settings by creating a custom config file `utils/UserElasticSearchFeederConfig.scala`:

```scala
package utils

import elasticSearchFeeder._

object UserElasticSearchFeederConfig extends ElasticSearchFeederConfigDefaultValues(
  RECORDS_REQUIRED_OVERRIDE = 20
  // add additional config overrides here (comma separated)
)
```

The default set of configurations can be found here, with descriptions of each configuration:
`/common/common-performance/src/main/scala/elasticSearchFeeder/ElasticSearchFeederConfig`

Ensure you also have:
- An appropriate JSON input file containing the ElasticSearch query
- A `caseIds.csv` file for overrides (can be used with the `OVERRIDE_ELASTICSEARCH_WITH_CSV_FILE` configuration override for debugging)

---

## 🌐 ElasticSearch Tunnel Setup

Before running tests against ElasticSearch, create a tunnel:

```bash
ssh -L 9200:ccd-elastic-search-perftest.service.core-compute-perftest.internal:9200 bastion-nonprod.platform.hmcts.net
```

This connects your local port `9200` to the ElasticSearch instance via the nonprod bastion.

To verify the connection, you may navigate to http://localhost:9200/_cat/indices?pretty&v&s=index, which also provides a
list of indices if the one you require is missing from those defined in the default configuration.

---

# 📈 Stats Generator

By default, Gatling aggregates all results into a single graph.  
The Stats Generator feature allows graphing individual transactions (KPIs) in Jenkins reports.

## 🔧 Setup in `build.gradle`

Add these configuration sections:

```groovy
configurations {
  gatlingImplementation.extendsFrom implementation
  gatlingRuntimeOnly.extendsFrom runtimeOnly
}

/* Generate stats per transaction for use in Jenkins */
ext {
  transactionNamesToGraph = ["CCDCacheWarm_000_Auth", "CCDCacheWarm_000_LoadJurisdictions"] // set the transactions to graph here
}

task generateStats(type: JavaExec) {
  dependsOn gatlingRun
  dependsOn compileGatlingScala
  mainClass.set('stats.GenerateStatsByTxn')
  args = transactionNamesToGraph
  classpath = sourceSets.gatling.runtimeClasspath + project(":common-performance").sourceSets.main.runtimeClasspath
}

gatlingRun.finalizedBy generateStats
```

---

## 🏷 Customising Transaction Names

You can configure which transactions you want to graph by editing:

```groovy
ext {
  transactionNamesToGraph = ["YourTransaction1", "YourTransaction2"]
}
```

These names must exactly match the Gatling `group()` or `http()` transaction names from your scenarios.

---

# 🧰 Utilities

## 📅 DateUtils

A simple Scala utility to generate dates for Gatling simulations or other automated tests.  
Supports generating:
- the current date
- past dates (fixed or randomised)
- future dates (fixed or randomised)

All dates are returned in a user-specified format using `java.time.format.DateTimeFormatter`.

---

## ✨ Features

- Get today's date in any format.
- Get a past date by subtracting fixed years, months, days.
- Get a random past date by specifying ranges for years, months, and days.
- Get a future date by adding fixed years, months, days.
- Get a random future date by specifying ranges for years, months, and days.
- Arguments in random functions are **optional** — you only need to provide the ones you want to randomise (years, months, or days).

---

## 💡 Usage Examples

```scala
// Get today's date
DateUtils.getDateNow("dd/MM/yyyy")

// Get a fixed past date: 2 years, 3 months, 5 days ago
DateUtils.getDatePast("dd/MM/yyyy", years = 2, months = 3, days = 5)

// Get a random past date: 10-50 years ago (no months or days)
DateUtils.getDatePastRandom("dd/MM/yyyy", minYears = 10, maxYears = 50)

// Get a random past date: 10-50 years, 0-11 months, 0-30 days ago
DateUtils.getDatePastRandom("dd/MM/yyyy", minYears = 10, maxYears = 50, minMonths = 0, maxMonths = 11, minDays = 0, maxDays = 30)

// Get a fixed future date: 1 year, 2 months, 15 days from now
DateUtils.getDateFuture("dd/MM/yyyy", years = 1, months = 2, days = 15)

// Get a random future date: 5-20 years in future
DateUtils.getDateFutureRandom("dd/MM/yyyy", minYears = 5, maxYears = 20)
```
In a Gatling scenario, you could use the feature as follows:
```scala
.exec(_.set("dob", DateUtils.getDatePastRandom("dd-MM-yyyy", minYears = 20, maxYears = 50)))
```
---

## 📋 Notes

- **Format strings** must follow Java's `DateTimeFormatter` syntax (e.g., `dd/MM/yyyy`, `yyyy-MM-dd`, etc.).
- **Random functions:**  
  If you only want to randomize years, you can simply provide `minYears` and `maxYears` — other parameters (`minMonths`, `maxMonths`, `minDays`, `maxDays`) are optional and default to `0`.
- **Using with Gatling:**  
  If you use a simple val assignment, such as `val dob = DateUtils.getDatePastRandom()`,
  the method is only called once at runtime, so every virtual user would have the same value for "dob".
  Using `.set()` or `.setAll()` will ensure each user will call the method to retrieve a value on-demand
  and save it into the Gatling session.

---

# 🧪 Running Tests

Tests are written using ScalaTest and ensure the functionality of `common-performance`.

To run tests:

```bash
./gradlew scalaTest
```

> 📢 **Note:** Always run tests if you modify the code!

---

# 🔄 Updating common-performance

If updates are needed:

1. **Update** in the source `common-performance` repository (not inside a Gatling repo).
2. **Add and run tests** to verify functionality:

   ```bash
   ./gradlew scalaTest
   ```

3. In each Gatling repo, **update the submodule** reference:

   ```bash
   git submodule update --recursive --remote
   ```

4. **Commit** the submodule changes like any normal change.
5. **Rebuild your project**:

   ```bash
   ./gradlew clean build
   ```

---

# 📝 Final Notes

> - Submodules must be **manually updated**.
> - **Always rebuild** your project after a submodule update.
> - Changes to `common-performance` benefit **all** projects using it.

---

✨ **Happy Testing!**
