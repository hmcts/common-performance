package ccd

case class CcdCaseType(
  name: String,
  jurisdictionId: String,
  caseTypeId: String,
  microservice: String,
  clientId: String = "ccd_gateway"
)

object CcdCaseTypes {
  val NFD = CcdCaseType(
    name = "NFD",
    jurisdictionId = "DIVORCE",
    caseTypeId = "NFD",
    microservice = "nfdiv_case_api"
  )

}