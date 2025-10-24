/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.carfregistration.models.responses

import play.api.libs.json.{Json, OFormat}

case class ResponseDetail(
    SAFEID: String,
    address: AddressResponse,
    individual: Option[IndividualResponse],
)

object ResponseDetail {
  implicit val format: OFormat[ResponseDetail] = Json.format[ResponseDetail]
}
