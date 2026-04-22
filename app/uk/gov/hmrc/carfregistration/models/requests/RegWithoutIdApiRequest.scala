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

import play.api.libs.json.{Json, OWrites, Writes}

case class RegWithoutIdApiRequest(
    registerWithoutIDRequest: RegWithoutIdApiRequestDetails
)

object RegWithoutIdApiRequest {
  implicit val writes: OWrites[RegWithoutIdApiRequest] = Json.writes[RegWithoutIdApiRequest]
}

case class RegWithoutIdApiRequestDetails(requestCommon: RequestCommon, requestDetail: RegWithoutIdRequestDetail)

object RegWithoutIdApiRequestDetails {
  implicit val writes: OWrites[RegWithoutIdApiRequestDetails] = Json.writes[RegWithoutIdApiRequestDetails]
}

sealed trait RegWithoutIdRequestDetail {
  val address: AddressDetailsApi
  val contactDetails: ContactDetailsFrontend
  val IsAnAgent: Boolean
  val IsAGroup: Boolean
}

object RegWithoutIdRequestDetail {
  implicit val writes: Writes[RegWithoutIdRequestDetail] = Writes {
    case i: RequestDetailIndividualWithoutId   => Json.toJson(i)
    case o: RequestDetailOrganisationWithoutId => Json.toJson(o)
  }
}

case class RequestDetailIndividualWithoutId(
    individual: IndividualDetailsWithoutId,
    address: AddressDetailsApi,
    contactDetails: ContactDetailsFrontend,
    IsAnAgent: Boolean = false,
    IsAGroup: Boolean = false
) extends RegWithoutIdRequestDetail

object RequestDetailIndividualWithoutId {
  implicit val writes: OWrites[RequestDetailIndividualWithoutId] = Json.writes[RequestDetailIndividualWithoutId]
}

case class RequestDetailOrganisationWithoutId(
    organisation: OrganisationDetailsWithoutId,
    address: AddressDetailsApi,
    contactDetails: ContactDetailsFrontend,
    IsAnAgent: Boolean = false,
    IsAGroup: Boolean = false
) extends RegWithoutIdRequestDetail

object RequestDetailOrganisationWithoutId {
  implicit val writes: OWrites[RequestDetailOrganisationWithoutId] = Json.writes[RequestDetailOrganisationWithoutId]
}
