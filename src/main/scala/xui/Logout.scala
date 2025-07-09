package xui

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Logout {

  /*====================================================================================
  * Manage Case Logout
  *===================================================================================*/

  val XUILogout =

    group("XUI_Logout") {
      exec(http("XUI_Logout_LogoutRequest")
        .get(xuiUrl + "/auth/logout")
        .headers(Headers.navigationHeader))
    }
}