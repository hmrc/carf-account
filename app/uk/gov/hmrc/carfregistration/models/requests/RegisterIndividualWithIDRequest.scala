/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.carfregistration.models.requests

import play.api.libs.json.{Json, OFormat}

case class RegisterIndividualWithIDRequest(requestCommon: RequestCommon, requestDetail: RequestDetail)

object RegisterIndividualWithIDRequest {
  implicit val format: OFormat[RegisterIndividualWithIDRequest] = Json.format[RegisterIndividualWithIDRequest]
}
