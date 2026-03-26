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

case class RegWithoutIdApiRequest(
    registerWithoutIDRequest: RegWithoutIdApiRequestDetails
)

object RegWithoutIdApiRequest {
  implicit val format: OFormat[RegWithoutIdApiRequest] = Json.format[RegWithoutIdApiRequest]
}

case class RegWithoutIdApiRequestDetails(requestCommon: RequestCommon, requestDetail: RegWithoutIdRequestDetail)

object RegWithoutIdApiRequestDetails {
  implicit val format: OFormat[RegWithoutIdApiRequestDetails] = Json.format[RegWithoutIdApiRequestDetails]
}

sealed trait RegWithoutIdRequestDetail {
  val address: AddressDetailsFrontend
  val contactDetails: ContactDetailsFrontend
  val isAnAgent: Boolean
  val isAGroup: Boolean
}

object RegWithoutIdRequestDetail {
  implicit val format: OFormat[RegWithoutIdRequestDetail] = Json.format[RegWithoutIdRequestDetail]
}

case class RequestDetailIndividualWithoutId(
    individual: IndividualDetailsWithoutId,
    address: AddressDetailsFrontend,
    contactDetails: ContactDetailsFrontend,
    isAnAgent: Boolean = false,
    isAGroup: Boolean = false
) extends RegWithoutIdRequestDetail

object RequestDetailIndividualWithoutId {
  implicit val format: OFormat[RequestDetailIndividualWithoutId] = Json.format[RequestDetailIndividualWithoutId]
}

case class RequestDetailOrganisationWithoutId(
    organisation: OrganisationDetailsWithoutId,
    address: AddressDetailsFrontend,
    contactDetails: ContactDetailsFrontend,
    isAnAgent: Boolean = false,
    isAGroup: Boolean = false
) extends RegWithoutIdRequestDetail

object RequestDetailOrganisationWithoutId {
  implicit val format: OFormat[RequestDetailOrganisationWithoutId] = Json.format[RequestDetailOrganisationWithoutId]
}
