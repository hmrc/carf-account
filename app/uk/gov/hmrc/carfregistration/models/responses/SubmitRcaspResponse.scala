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

case class SubmitRcaspResponse(ResponseDetails: SubmitResponseDetails)

case class SubmitResponseDetails(ReturnParameters: SubmitReturnParameters)

case class SubmitReturnParameters(Key: String, Value: String)

object SubmitRcaspResponse {
  implicit val format: OFormat[SubmitRcaspResponse] = Json.format[SubmitRcaspResponse]
}

object SubmitResponseDetails {
  implicit val format: OFormat[SubmitResponseDetails] = Json.format[SubmitResponseDetails]
}

object SubmitReturnParameters {
  implicit val format: OFormat[SubmitReturnParameters] = Json.format[SubmitReturnParameters]
}
