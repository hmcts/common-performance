package xui

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Logout {

  val xuiUrl = "https://manage-case.#{env}.platform.hmcts.net"

  /*====================================================================================
  * Manage Case Logout
  *===================================================================================*/

  val XUILogout =

    group("XUI_999_Logout") {
      exec(http("XUI_999_005_Logout")
        .get(xuiUrl + "/auth/logout")
        .headers(Headers.navigationHeader))
    }

}