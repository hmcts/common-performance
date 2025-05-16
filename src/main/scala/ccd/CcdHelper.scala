package ccd

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utilities.AzureKeyVault

object CcdHelper {

  val idamAPIURL = "https://idam-api.#{env}.platform.hmcts.net"
  val rpeAPIURL = "http://rpe-service-auth-provider-#{env}.service.core-compute-#{env}.internal"
  val ccdAPIURL = "http://ccd-data-store-api-#{env}.service.core-compute-#{env}.internal"
  val cdamAPIURL = "http://ccd-case-document-am-api-#{env}.service.core-compute-#{env}.internal"

  val clientSecret = AzureKeyVault.loadClientSecret("ccd-perftest", "ccd-api-gateway-oauth2-client-secret")

  def authenticate(email: String, password: String, microservice: String, clientId: String = "ccd_gateway") = {

    exec { session =>
      val resolvedEmail =
      /* If the email value passed through is a gatling session reference e.g. "#{username}" then strip out the #{} and
      retrieve the value of the variable from the session */
        if (email.matches("""#\{.+}""")) session(email.substring(2, email.length - 1)).as[String]
        else email
      session.set("resolvedEmail", resolvedEmail)
    }

    /* If the user is already authenticated, re-use the existing tokens stored in the Gatling session */
    .doIfOrElse(session => !session.contains("authenticatedCcdUser") || session("authenticatedCcdUser").as[String] != session("resolvedEmail").as[String]) {

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

      //set the email address of the authenticated user in the session so the tokens can be re-used for subsequent calls
      .exec {
        session => session.set("authenticatedCcdUser", session("resolvedEmail").as[String])
      }

      .pause(1)
    }{
      exec {
        session =>
        println("User is already authenticated - reusing tokens...")
        session
      }
    }
  }

  def createCase(userEmail: String, userPassword: String, caseType: CcdCaseType, eventName: String, payloadPath: String) =

    exec(authenticate(userEmail, userPassword, caseType.microservice, caseType.clientId))

    .exec(http("CCD_GetEventToken")
      .get(ccdAPIURL + s"/caseworkers/#{idamId}/jurisdictions/${caseType.jurisdictionId}/case-types/${caseType.caseTypeId}/event-triggers/${eventName}/token")
      .header("Authorization", "Bearer #{bearerToken}")
      .header("ServiceAuthorization", "#{authToken}")
      .header("Content-Type", "application/json")
      .check(jsonPath("$.token").saveAs("eventToken"))
    )

    .pause(1)

    .exec(http(s"CCD_CreateCase_${caseType.caseTypeId}")
      .post(ccdAPIURL + s"/caseworkers/#{idamId}/jurisdictions/${caseType.jurisdictionId}/case-types/${caseType.caseTypeId}/cases")
      .header("Authorization", "Bearer #{bearerToken}")
      .header("ServiceAuthorization", "#{authToken}")
      .header("Content-Type", "application/json")
      .body(ElFileBody(payloadPath))
      .check(jsonPath("$.case_type_id").is(caseType.caseTypeId))
      .check(jsonPath("$.id").saveAs("caseId"))
    )

    .pause(1)

  def addCaseEvent(userEmail: String, userPassword: String, caseType: CcdCaseType, caseId: String, eventName: String, payloadPath: String) =

    exec(authenticate(userEmail, userPassword, caseType.microservice, caseType.clientId))

    .exec(http("CCD_GetEventToken")
      .get(ccdAPIURL + s"/caseworkers/#{idamId}/jurisdictions/${caseType.jurisdictionId}/case-types/${caseType.caseTypeId}/cases/${caseId}/event-triggers/${eventName}/token")
      .header("Authorization", "Bearer #{bearerToken}")
      .header("ServiceAuthorization", "#{authToken}")
      .header("Content-Type", "application/json")
      .check(jsonPath("$.token").saveAs("eventToken"))
    )

    .pause(1)

    .exec(http(s"CCD_SubmitEvent_${eventName}")
      .post(ccdAPIURL + s"/caseworkers/#{idamId}/jurisdictions/${caseType.jurisdictionId}/case-types/${caseType.caseTypeId}/cases/${caseId}/events")
      .header("Authorization", "Bearer #{bearerToken}")
      .header("ServiceAuthorization", "#{authToken}")
      .header("Content-Type", "application/json")
      .body(ElFileBody(payloadPath))
      .check(jsonPath("$.id"))
    )

    .pause(1)

  def uploadDocumentToCdam(userEmail: String, userPassword: String, caseType: CcdCaseType, filepath: String) =

    exec(authenticate(userEmail, userPassword, caseType.microservice, caseType.clientId))

    .exec(_.set("filename", filepath.split("/").last))

    .exec(http("CCD_CDAM_DocumentUpload")
      .post(cdamAPIURL + "/cases/documents")
      .header("Authorization", "Bearer #{bearerToken}")
      .header("ServiceAuthorization", "#{authToken}")
      .header("Content-Type", "multipart/form-data")
      .formParam("classification", "PUBLIC")
      .formParam("caseTypeId", s"${caseType.caseTypeId}")
      .formParam("jurisdictionId", s"${caseType.jurisdictionId}")
      .bodyPart(RawFileBodyPart("files", s"${filepath}")
        .fileName("#{filename}")
        .transferEncoding("binary"))
      .check(regex("""documents/([0-9a-z-]+?)/binary""").saveAs("docId"))
      .check(jsonPath("$.documents[0].hashToken").saveAs("hashToken")))

    .pause(1)

}