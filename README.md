# common-performance

A shared library of utilities designed to make performance testing easier and more powerful across multiple Gatling projects.

This repository is intended to be imported into Gatling projects as a **Git submodule**, allowing common functionality to be reused and updated centrally.

**Current Features:**
- 📂 CCD Helper
- 📡 ElasticSearch Feeder
- 📈 Jenkins Stats Generator
- 🔐 Azure Key Vault Integration
- 📅 Date Utils

---

# 📚 Contents

- [About Git Submodules](#-about-git-submodules)
- [Setup Instructions](#-setup-instructions)
- [CCD Helper](#-ccd-helper)
  - [Create a Case](#-create-a-case)
  - [Add a Case Event](#-add-a-case-event)
  - [CDAM Document Upload](#-cdam-document-upload)
  - [Authentication](#-authentication)
  - [Case Type Definitions](#-case-type-definitions)
- [ElasticSearch Feeder](#-elasticsearch-feeder)
    - [Usage in Simulation](#-usage-in-simulation)
    - [Config Overrides](#-config-overrides)
    - [ElasticSearch Tunnel Setup](#-elasticsearch-tunnel-setup)
- [Stats Generator](#-stats-generator)
    - [Setup in build.gradle](#-setup-in-buildgradle)
    - [Customising Transaction Names](#-customising-transaction-names)
- [Utilities](#-utilities)
    - [Azure Key Vault Integration](#-azure-key-vault-integration)
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
> - Once updates are pulled, they will need to be committed to your project's repo.

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
rootProject.name = '<insert-the-name-of-your-Gatling-repo>' //e.g. 'probate-performance'

include ':common-performance'

project(':common-performance').projectDir = file('common/common-performance')
```

### 3. `build.gradle`

Add `common-performance` as a dependency:

```groovy
dependencies {
  implementation project(':common-performance')
}
```

Reference the common CVE suppression file:

```groovy
dependencyCheck {
    suppressionFile = "common/common-performance/owasp/owasp-suppressions.xml"
}
```

Update the Scala version in to at least `2.13.11` to support all the features in this repo:

```groovy
gatling {
    scalaVersion '2.13.11'
}
```

### 4. `Jenkins_nightly`

Initialise the submodule when cloned by Jenkins by adding the following before the 
call to `enablePerformanceTest()`:
```groovy
afterAlways('checkout') {
    sh """ git submodule update --init --recursive"""
}
```

---

To pull the latest submodule code at any time, run:
```bash
git submodule update --recursive --remote
```

After adding or updating a submodule, always run:

```bash
./gradlew clean build
```

to ensure the changes are picked up.

Commit any submodule updates to your project:

```bash
git add common/common-performance
git commit -m "Updated common-performance submodule"
git push
```
> 📢 **Note:** if you plan to change the submodule code, do so in the common-performance repo directly, 
not in your project's repo. The changes will then be available to all Gatling projects. See the 
section [Updating common-performance](#-updating-common-performance).

**Cloning a repository that contains a submodule**

Once your project contains the submodule, if you clone your project's repo (for example on a VM), 
you will also need to run this command to initialise the submodule after cloning.

```bash
git submodule update --init --recursive
```

---

# 📂 CCD Helper

## 🧭 Overview

A helper utility to simplify authentication and API interactions with 
CCD Data Store during performance tests.

---

## ✨ Features
- Authentication via IDAM and Service-to-Service (s2s)
- Case Creation using prebuilt JSON payloads 
- Event Submission to add data to existing cases
- Token reuse to improve efficiency

This utility supports performance test scenarios across jurisdictions and the functions can be particularly
helpful for progressing cases or creating/updating cases for data prep.

> 📢 **Note:** If the CCD Helper functions are used multiple times within a Gatling scenario (for example to create a case then update it), 
the user will authenticate for the first request 
and subsequent requests will reuse the authorisation tokens.

---

## 🚀 Usage

Import the package into your Gatling scenario:

```scala
import ccd._
```

### 🏛 Create a Case

Authenticates and creates a case using the specified event and request body.

```scala
CcdHelper.createCase(userEmail, userPassword, caseType, eventName, payloadPath)
```
**Parameters:**
- `userEmail` – user to authenticate as
- `userPassword` – password for the user
- `caseType` – predefined CcdCaseType (e.g. CcdCaseTypes.PROBATE_GrantOfRepresentation)
- `eventName` – event to trigger (e.g. createCase)
- `payloadPath` – path to the JSON file body used for case creation

**Example:**
```scala
.exec(CcdHelper.createCase(
  "#{user}", //you could use this in conjunction with a file feeder
  "#{password}",
  CcdCaseTypes.PROBATE_GrantOfRepresentation, //a collection of case types are defined in CcdCaseType.scala
  "createCase",
  "create-case-payload.json"
))
```

### 🧾 Add a Case Event

Authenticates and adds an event to an existing case.

```scala
CcdHelper.addCaseEvent(userEmail, userPassword, caseType, caseId, eventName, payloadPath)
```

**Parameters:**
- Same as createCase, plus:
- `caseId` – the ID of the existing CCD case (could be taken from the Gatling session)

**Example:**
```scala
.exec(CcdHelper.addCaseEvent(
  userEmail = "#{user}", //you could use this in conjunction with a file feeder
  userPassword = "#{password}",
  caseType = CcdCaseTypes.PROBATE_GrantOfRepresentation, //a collection of case types are defined in CcdCaseType.scala
  caseId = "#{caseId}",
  eventName = "submitEvent",
  payloadPath = "submit-event-payload.json"
))
```

Ensure the payload JSON is placed in the `resources` directory or a subfolder and follows the structure expected by CCD APIs.

### 📤 CDAM Document Upload

Authenticates and uploads a document to dm-store via CDAM.

```scala
uploadDocumentToCdam(userEmail, userPassword, caseType, filepath)
```

**Parameters:**
- Same as createCase, plus:
- `filepath` - the path to the file to upload (within the resources folder)

**Example:**
```scala
.exec(CcdHelper.addCaseEvent(
  userEmail = "#{user}", //you could use this in conjunction with a file feeder
  userPassword = "#{password}",
  caseType = CcdCaseTypes.PROBATE_GrantOfRepresentation, //a collection of case types are defined in CcdCaseType.scala
  filepath = "documents/TestDocument.pdf"
))
```

Ensure the document is placed in the `resources` directory or a subfolder.

> 📢 **Note:** The document's name will be set to the filename e.g. TestDocument.pdf

### 🔐 Authentication

Authenticates a user via IDAM and retrieves the necessary bearerToken and authToken,
reusing tokens across multiple requests where possible.

> 📢 **Note:** the functions listed above automatically authenticate as part of the call,
so there is no need to call the authenticate method separately.

To authenticate only (e.g. for use with a different CCD API):

```scala
CcdHelper.authenticate(email, password, microservice)
```

**Parameters:**
- `userEmail` – user to authenticate as
- `userPassword` – password for the user
- `microservice` – predefined CCD microservice (ccd_data)

**Example:**
```scala
.exec(CcdHelper.authenticate(
  "#{user}", //you could use this in conjunction with a file feeder
  "#{password}",
  CcdCaseTypes.CCD.microservice
))
```

### 📦 Case Type Definitions

All supported case types are declared in CcdCaseTypes, e.g.:

```scala
val caseType = CcdCaseTypes.DIVORCE_NFD
```

Each case type includes:
- jurisdictionId
- caseTypeId
- microservice
- optional clientId (defaults to ccd_gateway)

---

# 📡 ElasticSearch Feeder

A flexible feeder allowing Gatling scenarios to pull dynamic data from ElasticSearch.

## 🚀 Usage in Simulation

Add the following to your Gatling simulation:

```scala
ElasticSearchFeederConfig.set(UserElasticSearchFeederConfig) // required to override config defaults

val iterations = if (debugMode == "off") CalculateRecordsRequired.calculate(targetIterationsPerHour, rampUpDurationMins, testDurationMins, rampDownDurationMins) else 1

val caseIdFeeder = ElasticSearchCaseFeeder.feeder(esIndex, esQueryFilePath, feederType, iterations)
```
When calling `ElasticSearchFeeder.feeder()`, replace:
- **esIndex** with the ElasticSearch index to perform the search against (listed in the ElasticSearchFeederConfig file)
  - example: `esIndex.ET-EnglandWales`
- **esQueryFilePath** with a location of your JSON file containing the ElasticSearch query string
  - example: `getClass.getClassLoader.getResource("elasticSearchQuery.json").getPath,`
  - note: the JSON file should reside within the `resources` folder (or a subfolder)
- **FeederType** with the FeederType you require (QUEUE, CIRCULAR, SHUFFLE, RANDOM)
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
.exec(MyScenario.getCase(caseIdFeeder))
```

And update your scenario method to use the feeder:

```scala
def getCase(caseIdFeeder: Iterator[Map[String, Any]]) =
  feed(caseIdFeeder)
  .exec(...) //your usual Gatling code here
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

Before running tests against ElasticSearch from your local machine, connect to the VPN and create a tunnel:

```bash
ssh -L 9200:ccd-elastic-search-perftest.service.core-compute-perftest.internal:9200 bastion-nonprod.platform.hmcts.net
```

This connects your local port `9200` to the ElasticSearch instance via the nonprod bastion.

To verify the connection, you may navigate to http://localhost:9200/_cat/indices?pretty&v&s=index, which also provides a
list of indices if the one you require is missing from those defined in the default configuration.

If running from a VM or Jenkins, a tunnel is not required.

---

# 📈 Stats Generator

By default, the Gatling Jenkins plugin aggregates all transaction timings into a single data point on the graph.  
The Stats Generator feature allows graphing individual transactions (KPIs) in Jenkins pipelines.

## 🔧 Setup in `build.gradle`

Add these configuration sections:

```groovy
configurations {
  gatlingImplementation.extendsFrom implementation
  gatlingRuntimeOnly.extendsFrom runtimeOnly
}

/* Generate stats per transaction for use in Jenkins */
ext {
  transactionNamesToGraph = ["Probate_090_StartApplication", "Probate_250_SubmitApplication"] // set the transactions to graph here
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

## 🔐 **Azure Key Vault Integration**

A utility to securely retrieve secrets (such as client secrets) directly from **Azure Key Vault** using either environment variables or Azure authentication methods.

---

### 🧬 How It Works

- If `CLIENT_SECRET` is set as an environment variable (often set in the Jenkins_nightly file), it will be used.
- Otherwise, the secret is retrieved from Azure Key Vault using:
  - **Azure CLI authentication** (`az login`) when running locally or on a VM.
  - **Managed Identity / DefaultAzureCredential** when running in Jenkins or cloud environments.

---

### 🪪 Local / VM Setup Instructions

Authenticate locally or on a VM and retrieve the required secrets in Gatling:

1. Before running the simulation, always ensure you're logged into Azure via CLI:  
   ```bash  
   az login  
   ```

2. Add the following lines to your Gatling scenario, to fetch the secret from Key Vault 
based on the vault and secret name passed to the shared function:  
   ```scala
   import utilities.AzureKeyVault
   
   val clientSecret = AzureKeyVault.loadClientSecret("ccd-perftest", "ccd-api-gateway-oauth2-client-secret")  
   ```

3. Pass the client secret as a form parameter in any request as usual:
   ```scala
   .formParam("client_secret", clientSecret)
   ```
 
4. Delete `/arc/gatling/resources/application.conf` as this is no longer required.

5. Remove the following code from your Gatling scenario as it is no longer required:
   ```scala
   import com.typesafe.config.ConfigFactory

   val clientSecret = ConfigFactory.load.getString("auth.clientSecret")
   ```

---

### 🚫 Error Handling

- If the secret is missing or cannot be retrieved, the test will exit with an error.
- If you're not logged in to Azure CLI, you'll see:  
  ```text
  Please run `az login` and try again.  
  ```

This ensures secure and consistent secret management for both local and CI/CD environments.

---

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
6. **Run the dependency check** to identify CVEs that might need suppressing:

   ```bash
   ./gradlew dependencyCheckAggregate
   ```
7. **Add CVE suppressions** to the following file in the common-performance repo:

   ```text
   owasp/owasp-suppressions.xml
   ```
   > 📢 **Note:** if CVE suppressions are added, you'll need to repeat steps 1-6 to apply and test the changes.

---

# 📝 Final Notes

> - Submodules must be **manually updated**.
> - **Always rebuild** your project after a submodule update.
> - Changes to `common-performance` benefit **all** projects using it.

---

✨ **Happy Testing!**
