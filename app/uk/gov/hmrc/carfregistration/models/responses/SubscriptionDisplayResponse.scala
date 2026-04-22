/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.carfregistration.models.responses

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.carfregistration.models.requests.Contact

case class CarfSubscriptionDetails(
    carfReference: String,
    tradingName: Option[String],
    gbUser: Boolean,
    primaryContact: Contact,
    secondaryContact: Option[Contact]
)

object CarfSubscriptionDetails {
  implicit val format: OFormat[CarfSubscriptionDetails] = Json.format[CarfSubscriptionDetails]
}

case class SubscriptionDisplaySuccess(processingDate: String, carfSubscriptionDetails: CarfSubscriptionDetails)

object SubscriptionDisplaySuccess {
  implicit val format: OFormat[SubscriptionDisplaySuccess] = Json.format[SubscriptionDisplaySuccess]
}

case class SubscriptionDisplayResponse(success: SubscriptionDisplaySuccess)

object SubscriptionDisplayResponse {
  implicit val format: OFormat[SubscriptionDisplayResponse] = Json.format[SubscriptionDisplayResponse]
}
