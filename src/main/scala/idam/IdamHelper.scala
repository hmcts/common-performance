package idam

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utilities.{DateUtils, StringUtils}

object IdamHelper {

  val idamAPIURL = "https://idam-api.#{env}.platform.hmcts.net"

  def CreateCitizen(
     email: String = null,
     password: String = null,
     emailFieldName: String = "emailAddress",
     passwordFieldName: String = "password"
   ) = {

    val newUserFeeder = Iterator.continually(Map(
      emailFieldName -> Option(email).getOrElse(("perftest" + DateUtils.getDateNow("yyyyMMdd") + "@perftest-" + StringUtils.randomString(5) + ".com")),
      passwordFieldName -> Option(password).getOrElse("Pa55word11"),
      "role" -> "citizen"
    ))

    feed(newUserFeeder)
      // Set temporary gatling session variable to use in the JSON payload
      .exec { session =>
        session
          .set("idamCreateEmail", session(emailFieldName).as[String])
          .set("idamCreatePassword", session(passwordFieldName).as[String])
      }
      .group("IDAM_000_CreateCitizen") {
        exec(
          http("CreateCitizen")
            .post(idamAPIURL + "/testing-support/accounts")
            .body(ElFileBody("CreateUserTemplate.json")).asJson
            .check(status.is(201))
        )
      }
      // Remove temporary gatling session variables
      .exec { session =>
        session
          .remove("idamCreateEmail")
          .remove("idamCreatePassword")
      }
  }

  def DeleteCitizen(emailFieldName: String = "emailAddress") =
    doIf(s"#{$emailFieldName.exists()}") {
      group("IDAM_000_DeleteCitizen") {
        exec(http("DeleteCitizen")
          .delete(idamAPIURL + s"/testing-support/accounts/#{$emailFieldName}")
          .check(status.is(204)))
      }
    }

}