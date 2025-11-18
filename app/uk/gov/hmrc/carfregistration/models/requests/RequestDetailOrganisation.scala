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

import play.api.libs.json.*

case class RequestDetailOrganisation(
    requiresNameMatch: Boolean,
    IDNumber: String,
    IDType: String,
    individual: Option[IndividualDetails],
    isAnAgent: Boolean,
    organisation: Option[OrganisationDetails]
)

object RequestDetailOrganisation {
  implicit val format: OFormat[RequestDetailOrganisation] = Json.format[RequestDetailOrganisation]

  def apply(frontendOrganisationRequest: RegisterOrganisationWithIdFrontendRequest): RequestDetailOrganisation =
    RequestDetailOrganisation(
      requiresNameMatch = frontendOrganisationRequest.requiresNameMatch,
      IDNumber = frontendOrganisationRequest.IDNumber,
      IDType = frontendOrganisationRequest.IDType,
      individual = None,
      isAnAgent = false,
      organisation = Some(
        OrganisationDetails(
          organisationName = frontendOrganisationRequest.organisationName.getOrElse("OrganisationName"),
          organisationType = frontendOrganisationRequest.organisationType.getOrElse("OrganisationType")
        )
      )
    )
}

case class OrganisationDetails(
    organisationName: String,
    organisationType: String
)

object OrganisationDetails {
  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
}
