/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.carfregistration.models.responses

import play.api.libs.json.{Json, OFormat}

case class RegisterIndividualWithIDResponse(responseCommon: ResponseCommon, responseDetail: Option[ResponseDetail])

object RegisterIndividualWithIDResponse {
  implicit val format: OFormat[RegisterIndividualWithIDResponse] = Json.format[RegisterIndividualWithIDResponse]
}
