package ccd

case class CcdCaseType(
  name: String,
  jurisdictionId: String,
  caseTypeId: String,
  microservice: String,
  clientId: String = "ccd_gateway"
)

object CcdCaseTypes {

  val CCD = CcdCaseType(
    name = "CCD",
    jurisdictionId = "N/A",
    caseTypeId = "N/A",
    microservice = "ccd_data"
  )

  val FPL = CcdCaseType(
    name = "FPL",
    jurisdictionId = "PUBLICLAW",
    caseTypeId = "CARE_SUPERVISION_EPO",
    microservice = "ccd_data"
  )

  val NFD = CcdCaseType(
    name = "NFD",
    jurisdictionId = "DIVORCE",
    caseTypeId = "NFD",
    microservice = "nfdiv_case_api"
  )

}