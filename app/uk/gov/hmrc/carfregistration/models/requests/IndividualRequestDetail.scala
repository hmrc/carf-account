/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.carfregistration.models.requests

import play.api.libs.json.{Json, OFormat}

case class RequestDetail(
    requiresNameMatch: Boolean,
    IDNumber: String,
    IDType: String,
    individual: IndividualDetails,
    isAnAgent: Boolean
)

object RequestDetail {
  implicit val format: OFormat[RequestDetail] = Json.format[RequestDetail]
}
