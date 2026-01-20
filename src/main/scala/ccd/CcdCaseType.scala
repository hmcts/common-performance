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

  val ADOPTION_A58 = CcdCaseType(
    name = "ADOPTION_A58",
    jurisdictionId = "ADOPTION",
    caseTypeId = "A58",
    microservice = "ccd_data"
  )

  val CIVIL_CIVIL = CcdCaseType(
    name = "CIVIL_CIVIL",
    jurisdictionId = "CIVIL",
    caseTypeId = "CIVIL",
    microservice = "ccd_data"
  )

  val CMC_MoneyClaimCase = CcdCaseType(
    name = "CMC_MoneyClaimCase",
    jurisdictionId = "CMC",
    caseTypeId = "MoneyClaimCase",
    microservice = "ccd_data"
  )

  val DIVORCE_NFD = CcdCaseType(
    name = "DIVORCE_NFD",
    jurisdictionId = "DIVORCE",
    caseTypeId = "NFD",
    microservice = "nfdiv_case_api"
  )

  val EMPLOYMENT_EnglandWales = CcdCaseType(
    name = "EMPLOYMENT_EnglandWales",
    jurisdictionId = "EMPLOYMENT",
    caseTypeId = "ET_EnglandWales",
    microservice = "ccd_data"
  )

  val EMPLOYMENT_Scotland = CcdCaseType(
    name = "EMPLOYMENT_Scotland",
    jurisdictionId = "EMPLOYMENT",
    caseTypeId = "ET_Scotland",
    microservice = "ccd_data"
  )

  val HRS_HearingRecordings = CcdCaseType(
    name = "HRS_HearingRecordings",
    jurisdictionId = "HRS",
    caseTypeId = "HearingRecordings",
    microservice = "ccd_data"
  )

  val IA_Asylum = CcdCaseType(
    name = "IA_Asylum",
    jurisdictionId = "IA",
    caseTypeId = "Asylum",
    microservice = "ccd_data"
  )

  val IA_Bail = CcdCaseType(
    name = "IA_Bail",
    jurisdictionId = "IA",
    caseTypeId = "Bail",
    microservice = "iac"
  )

  val PRIVATELAW_PRLAPPS = CcdCaseType(
    name = "PRIVATELAW_PRLAPPS",
    jurisdictionId = "PRIVATELAW",
    caseTypeId = "PRLAPPS",
    microservice = "ccd_data"
  )

  val PROBATE_GrantOfRepresentation = CcdCaseType(
    name = "PROBATE_GrantOfRepresentation",
    jurisdictionId = "PROBATE",
    caseTypeId = "GrantOfRepresentation",
    microservice = "probate_backend"
  )

  val PUBLICLAW_CARE_SUPERVISION_EPO = CcdCaseType(
    name = "PUBLICLAW_CARE_SUPERVISION_EPO",
    jurisdictionId = "PUBLICLAW",
    caseTypeId = "CARE_SUPERVISION_EPO",
    microservice = "ccd_data"
  )

  val SSCS_Benefit = CcdCaseType(
    name = "SSCS_Benefit",
    jurisdictionId = "SSCS",
    caseTypeId = "Benefit",
    microservice = "ccd_data"
  )

  val ST_CIC_CriminalInjuriesCompensation = CcdCaseType(
    name = "ST_CIC_CriminalInjuriesCompensation",
    jurisdictionId = "ST_CIC",
    caseTypeId = "CriminalInjuriesCompensation",
    microservice = "ccd_data"
  )

  val WA_WaCaseType = CcdCaseType(
    name = "WA_WaCaseType",
    jurisdictionId = "WA",
    caseTypeId = "WaCaseType",
    microservice = "ccd_data"
  )

}
