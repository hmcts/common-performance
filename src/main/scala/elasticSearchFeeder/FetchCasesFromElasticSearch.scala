package elasticSearchFeeder

import com.google.gson.{Gson, JsonElement, JsonObject, JsonParser}

import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.{InetAddress, URI}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters._
import java.net.ConnectException
import java.nio.channels.ClosedChannelException
import scala.util.{Try, Failure, Success}

object FetchCasesFromElasticSearch {

  def fetchCaseIds(esIndex: String, esQueryFilePath: String, recordsRequired: Int): List[String] = {

    val httpClient: HttpClient = HttpClient.newHttpClient()

    val hostname: String = InetAddress.getLocalHost.getHostName

    println("Hostname: " + hostname)

    // Set ElasticSearch server based on the hostname
    val ELASTICSEARCH_SERVER: String = if (ElasticSearchFeederConfig.config.hostnameStringsWithDirectElasticSearchConnectivity.exists(hostname.contains)) {
      val esServer = ElasticSearchFeederConfig.config.ELASTICSEARCH_SERVER_DIRECT + ":" + ElasticSearchFeederConfig.config.ELASTICSEARCH_SERVER_PORT
      println("Using ElasticSearch direct URL (you must be on an Azure VM or Jenkins Slave): " + esServer)
      esServer
    } else {
      val esServer = ElasticSearchFeederConfig.config.ELASTICSEARCH_SERVER_LOCAL + ":" + ElasticSearchFeederConfig.config.ELASTICSEARCH_SERVER_PORT
      println("Using ElasticSearch local URL (tunneling through the bastion): " + esServer)
      esServer
    }

    // Elasticsearch endpoint for the query
    val endpoint = s"http://${ELASTICSEARCH_SERVER}/${esIndex}/_search"

    // Read the JSON file as a string
    val jsonString = new String(Files.readAllBytes(Paths.get(esQueryFilePath)), StandardCharsets.UTF_8)

    // Parse the JSON string into a JsonObject using Gson
    val gson = new Gson()
    val jsonElement: JsonElement = JsonParser.parseString(jsonString)
    val jsonObject: JsonObject = jsonElement.getAsJsonObject

    // Update the JSON object with the new value
    jsonObject.addProperty("size", recordsRequired.toString)

    // Add a total hits object to return the total number of records available
    jsonObject.addProperty("track_total_hits", true)

    // Convert the updated JsonObject back to a JSON string
    val updatedJsonString = gson.toJson(jsonObject)

    // Create the HTTP request
    val request = HttpRequest.newBuilder()
      .uri(new URI(endpoint))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(updatedJsonString))
      .build()

    // Send the request and get the response, with connection error handling
    val responseTry = Try {
      httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    responseTry match {
      case Success(response) =>
        // Error if ElasticSearch returns no results
        if (response.body() == "{}") {
          println("ERROR: ElasticSearch returned no results. Exiting.")
          sys.exit(1)
        }

        // Check if the response is successful
        if (response.statusCode() == 200) {
          // Parse the JSON response using Gson
          val jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject

          // Navigate to the hits array and extract the total number of Case IDs available
          val hitsCount = jsonResponse.getAsJsonObject("hits").getAsJsonObject("total").get("value").getAsInt
          println("INFO: ElasticSearch has " + hitsCount.toString + " cases that match your search query")

          // Extract the Case IDs
          val hits = jsonResponse.getAsJsonObject("hits").getAsJsonArray("hits")
          hits.iterator().asScala.toList.flatMap { hitElement =>
            val hit = hitElement.getAsJsonObject
            val source = hit.getAsJsonObject("_source")
            if (source.has("reference")) Some(source.get("reference").getAsString) else None
          }
        } else {
          throw new RuntimeException(s"Failed to fetch data from Elasticsearch: ${response.statusCode()} ${response.body()}")
        }

      case Failure(e: ConnectException) =>
        println(s"\n🚫 ERROR: Could not connect to ElasticSearch at $ELASTICSEARCH_SERVER")
        println("💡 If running locally, the ElasticSearch tunnel may not be active.")
        println("👉 Please run the following command before retrying:\n")
        println("   ssh -L 9200:ccd-elastic-search-perftest.service.core-compute-perftest.internal:9200 bastion-nonprod.platform.hmcts.net\n")
        sys.exit(1)

      case Failure(e: ClosedChannelException) =>
        println("\n🚫 ERROR: Connection to ElasticSearch was unexpectedly closed.")
        println("💡 This may indicate that the ElasticSearch tunnel is not set up.")
        sys.exit(1)

      case Failure(e) =>
        println(s"\n❌ Unexpected error while contacting ElasticSearch: ${e.getMessage}")
        sys.exit(1)
    }

  }

}