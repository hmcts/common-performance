package xui

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Login {

  /*====================================================================================
  * Manage Case Login
  *===================================================================================*/

  def authenticate(email: String, password: String) = {

    exec { session =>
      val resolvedEmail =
        /* If the email value passed through is a gatling session reference e.g. "#{username}" then strip out the #{} and
        retrieve the value of the variable from the session */
        if (email.matches("""#\{.+}""")) session(email.substring(2, email.length - 1)).as[String]
        else email
      session.set("resolvedEmail", resolvedEmail)
    }

    .group("XUI_Login") {
      exec(http("XUI_Login_LoginRequest")
        .post(IdamUrl + "/login?client_id=xuiwebapp&redirect_uri=" + xuiUrl + "/oauth2/callback&state=#{state}&nonce=#{nonce}&response_type=code&scope=profile%20openid%20roles%20manage-user%20create-user%20search-user&prompt=")
        .formParam("username", "#{email}")
        .formParam("password", "#{password}")
        .formParam("azureLoginEnabled", "true")
        .formParam("mojLoginEnabled", "true")
        .formParam("selfRegistrationEnabled", "false")
        .formParam("_csrf", "#{csrf}")
        .headers(Headers.navigationHeader)
        .headers(Headers.postHeader)
        .check(regex("Manage cases")))

      //see xui-webapp cookie capture in the Homepage scenario for details of why this is being used
      .exec(addCookie(Cookie("xui-webapp", "#{xuiWebAppCookie}")
        .withMaxAge(28800)
        .withSecure(true)))

      .exec(http("XUI_Login_ConfigurationUI")
        .get(xuiUrl + "/external/configuration-ui/")
        .headers(Headers.commonHeader)
        .header("accept", "*/*")
        .check(substring("ccdGatewayUrl")))

      .exec(http("XUI_Login_ConfigJson")
        .get(xuiUrl + "/assets/config/config.json")
        .header("accept", "application/json, text/plain, */*")
        .check(substring("caseEditorConfig")))

      .exec(http("XUI_Login_TsAndCs")
        .get(xuiUrl + "/api/configuration?configurationKey=termsAndConditionsEnabled")
        .headers(Headers.commonHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("false")))

      .exec(http("XUI_Login_ConfigUI")
        .get(xuiUrl + "/external/config/ui")
        .headers(Headers.commonHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("ccdGatewayUrl")))

      .exec(http("XUI_Login_UserDetails")
        .get(xuiUrl + "/api/user/details?refreshRoleAssignments=undefined")
        .headers(Headers.commonHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(status.in(200, 304)))

      .exec(http("XUI_Login_IsAuthenticated")
        .get(xuiUrl + "/auth/isAuthenticated")
        .headers(Headers.commonHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(regex("true|false")))

      .exec(http("XUI_Login_MonitoringTools")
        .get(xuiUrl + "/api/monitoring-tools")
        .headers(Headers.commonHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(jsonPath("$.key").notNull))

      //if there is no in-flight case, set the case to 0 for the activity calls
      .doIf("#{caseId.isUndefined()}") {
        exec(_.set("caseId", "0"))
      }

      .exec(http("XUI_Login_Jurisdictions")
        .get(xuiUrl + "/aggregated/caseworkers/:uid/jurisdictions?access=read")
        .headers(Headers.commonHeader)
        .header("accept", "application/json")
        .check(substring("id")))

      .exec(getCookieValue(CookieKey("XSRF-TOKEN").withDomain(xuiUrl.replace("https://", "")).withSecure(true).saveAs("XSRFToken")))

      .exec(http("XUI_Login_OrgDetails")
        .get(xuiUrl + "/api/organisation")
        .headers(Headers.commonHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(regex("name|Organisation route error"))
        .check(status.in(200, 304, 401, 403)))

      .exec(http("XUI_Login_WorkBasketInputs")
        .get(xuiUrl + "/data/internal/case-types/#{caseType}/work-basket-inputs")
        .headers(Headers.commonHeader)
        .header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-workbasket-input-details.v2+json;charset=UTF-8")
        .check(regex("workbasketInputs|Not Found"))
        .check(status.in(200, 404)))

      .exec(http("XUI_Login_SearchCases")
        .post(xuiUrl + "/data/internal/searchCases?ctid=#{caseType}&use_case=WORKBASKET&view=WORKBASKET&page=1")
        .headers(Headers.commonHeader)
        .header("accept", "application/json")
        .formParam("x-xsrf-token", "#{XSRFToken}")
        .body(StringBody("""{"size":25}"""))
        .check(substring("columns")))
    }
}