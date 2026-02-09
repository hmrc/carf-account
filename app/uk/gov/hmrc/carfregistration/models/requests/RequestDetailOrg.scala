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

package uk.gov.hmrc.carfregistration.models.requests

import play.api.libs.json.*

sealed trait RequestDetailOrg {
  val requiresNameMatch: Boolean
  val IDNumber: String
  val IDType: String
  val isAnAgent: Boolean
}

object RequestDetailOrg {
  implicit val format: OFormat[RequestDetailOrg] = Json.format[RequestDetailOrg]
}

case class RequestDetailOrgUserEntry(
    requiresNameMatch: Boolean,
    IDNumber: String,
    IDType: String,
    organisation: OrganisationDetails,
    isAnAgent: Boolean
) extends RequestDetailOrg

object RequestDetailOrgUserEntry {
  implicit val format: OFormat[RequestDetailOrgUserEntry] = Json.format[RequestDetailOrgUserEntry]

  def apply(
      frontendRequest: RegWithIdUserEntryOrgFrontendRequest
  ): RequestDetailOrgUserEntry =
    RequestDetailOrgUserEntry(
      requiresNameMatch = frontendRequest.requiresNameMatch,
      IDNumber = frontendRequest.IDNumber,
      IDType = frontendRequest.IDType,
      organisation = OrganisationDetails(
        organisationName = frontendRequest.organisationName,
        organisationType = frontendRequest.organisationType
      ),
      isAnAgent = false
    )
}

case class RequestDetailOrgCtAutoMatch(
    requiresNameMatch: Boolean,
    IDNumber: String,
    IDType: String,
    isAnAgent: Boolean
) extends RequestDetailOrg

object RequestDetailOrgCtAutoMatch {
  implicit val format: OFormat[RequestDetailOrgCtAutoMatch] = Json.format[RequestDetailOrgCtAutoMatch]

  def apply(frontendRequest: RegWithIdAutoMatchOrgFrontendRequest): RequestDetailOrgCtAutoMatch =
    RequestDetailOrgCtAutoMatch(
      requiresNameMatch = frontendRequest.requiresNameMatch,
      IDNumber = frontendRequest.IDNumber,
      IDType = frontendRequest.IDType,
      isAnAgent = false
    )
}
