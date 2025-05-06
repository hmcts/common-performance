package ccd

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utilities.AzureKeyVault

object CcdHelper {

  val idamAPIURL = "https://idam-api.#{env}.platform.hmcts.net"
  val rpeAPIURL = "http://rpe-service-auth-provider-#{env}.service.core-compute-#{env}.internal"
  val ccdAPIURL = "http://ccd-data-store-api-#{env}.service.core-compute-#{env}.internal"

  val clientSecret = AzureKeyVault.loadClientSecret("ccd-perftest", "ccd-api-gateway-oauth2-client-secret")

  def authenticate(email: String, password: String, microservice: String, clientId: String = "ccd_gateway") =

    exec(http("CCD_AuthLease")
      .post(rpeAPIURL + "/testing-support/lease")
      .body(StringBody(s"""{"microservice":"$microservice"}""")).asJson
      .check(regex("(.+)").saveAs("authToken"))
    )

    .pause(1)

    .exec(http("CCD_GetBearerToken")
      .post(idamAPIURL + "/o/token")
      .formParam("grant_type", "password")
      .formParam("username", s"${email}")
      .formParam("password", s"${password}")
      .formParam("client_id", clientId)
      .formParam("client_secret", clientSecret)
      .formParam("scope", "openid profile roles")
      .header("Content-Type", "application/x-www-form-urlencoded")
      .check(jsonPath("$.access_token").saveAs("bearerToken"))
    )

    .pause(1)

    .exec(http("CCD_GetIdamID")
      .get(idamAPIURL + "/details")
      .header("Authorization", "Bearer #{bearerToken}")
      .check(jsonPath("$.id").saveAs("idamId"))
    )

    .pause(1)

  def addCaseEvent(userEmail: String, userPassword: String, caseType: CcdCaseType, eventName: String, payloadPath: String) =

    exec(authenticate(userEmail, userPassword, caseType.microservice, caseType.clientId))

    .exec(http("CCD_GetEventToken")
      .get(ccdAPIURL + s"/caseworkers/#{idamId}/jurisdictions/${caseType.jurisdictionId}/case-types/${caseType.caseTypeId}/cases/#{caseId}/event-triggers/${eventName}/token")
      .header("Authorization", "Bearer #{bearerToken}")
      .header("ServiceAuthorization", "#{authToken}")
      .header("Content-Type", "application/json")
      .check(jsonPath("$.token").saveAs("eventToken"))
    )

    .pause(1)

    .exec(http(s"CCD_SubmitEvent_${eventName}")
      .post(ccdAPIURL + s"/caseworkers/#{idamId}/jurisdictions/${caseType.jurisdictionId}/case-types/${caseType.caseTypeId}/cases/#{caseId}/events")
      .header("Authorization", "Bearer #{bearerToken}")
      .header("ServiceAuthorization", "#{authToken}")
      .header("Content-Type", "application/json")
      .body(ElFileBody(payloadPath))
      .check(jsonPath("$.id"))
    )

    .pause(1)
}