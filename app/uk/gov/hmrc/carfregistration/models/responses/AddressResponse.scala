/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.carfregistration.models.responses

import play.api.libs.json.{Json, OFormat}

case class AddressResponse(
    addressLine1: String,
    addressLine2: Option[String],
    addressLine3: Option[String],
    addressLine4: Option[String],
    postalCode: Option[String],
    countryCode: String
)

object AddressResponse {
  implicit val format: OFormat[AddressResponse] = Json.format[AddressResponse]
}
