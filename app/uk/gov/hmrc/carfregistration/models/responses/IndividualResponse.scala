/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.carfregistration.models.responses

import play.api.libs.json.{Json, OFormat}

case class IndividualResponse(
    firstName: String,
    lastName: String,
    middleName: Option[String]
)

object IndividualResponse {
  implicit val format: OFormat[IndividualResponse] = Json.format[IndividualResponse]
}
