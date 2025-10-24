/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.carfregistration.models.responses

import play.api.libs.json.{Json, OFormat}

case class ResponseCommon(
    status: String
)

object ResponseCommon {
  implicit val format: OFormat[ResponseCommon] = Json.format[ResponseCommon]
}
