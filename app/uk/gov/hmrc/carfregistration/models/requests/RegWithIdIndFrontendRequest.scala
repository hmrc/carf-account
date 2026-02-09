/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.carfregistration.models.requests

import play.api.libs.json.{Json, OFormat, Writes}

sealed trait RegWithIdIndFrontendRequest {
  val requiresNameMatch: Boolean
  val IDNumber: String
  val IDType: String
  val firstName: String
  val lastName: String
}

object RegWithIdIndFrontendRequest {
  implicit val writes: Writes[RegWithIdIndFrontendRequest] = Writes {
    case r: RegWithNinoIndFrontendRequest =>
      Json.toJson(r)(RegWithNinoIndFrontendRequest.format)

    case r: RegWithUtrIndFrontendRequest =>
      Json.toJson(r)(RegWithUtrIndFrontendRequest.format)
  }
}

case class RegWithNinoIndFrontendRequest(
    requiresNameMatch: Boolean,
    IDNumber: String,
    IDType: String,
    dateOfBirth: String,
    firstName: String,
    lastName: String
) extends RegWithIdIndFrontendRequest

object RegWithNinoIndFrontendRequest {
  implicit val format: OFormat[RegWithNinoIndFrontendRequest] =
    Json.format[RegWithNinoIndFrontendRequest]
}

case class RegWithUtrIndFrontendRequest(
    requiresNameMatch: Boolean,
    IDNumber: String,
    IDType: String,
    firstName: String,
    lastName: String
) extends RegWithIdIndFrontendRequest

object RegWithUtrIndFrontendRequest {
  implicit val format: OFormat[RegWithUtrIndFrontendRequest] = Json.format[RegWithUtrIndFrontendRequest]
}
