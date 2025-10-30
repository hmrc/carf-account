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

import play.api.libs.json.{Json, OFormat}

case class RequestDetailIndividual(
    requiresNameMatch: Boolean,
    IDNumber: String,
    IDType: String,
    individual: IndividualDetails,
    isAnAgent: Boolean
)

object RequestDetailIndividual {
  implicit val format: OFormat[RequestDetailIndividual] = Json.format[RequestDetailIndividual]

  def apply(frontendRequest: RegisterIndWithIdFrontendRequest): RequestDetailIndividual =
    RequestDetailIndividual(
      requiresNameMatch = frontendRequest.requiresNameMatch,
      IDNumber = frontendRequest.IDNumber,
      IDType = frontendRequest.IDType,
      individual = IndividualDetails(
        dateOfBirth = frontendRequest.dateOfBirth,
        firstName = frontendRequest.firstName,
        lastName = frontendRequest.lastName
      ),
      isAnAgent = false
    )
}

case class IndividualDetails(dateOfBirth: String, firstName: String, lastName: String)

object IndividualDetails {
  implicit val format: OFormat[IndividualDetails] = Json.format[IndividualDetails]
}
