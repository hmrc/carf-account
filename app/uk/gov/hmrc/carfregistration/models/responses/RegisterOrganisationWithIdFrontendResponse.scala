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

package uk.gov.hmrc.carfregistration.models.responses

import play.api.libs.json.{Json, OFormat}

case class RegisterOrganisationWithIdFrontendResponse(
    safeId: String,
    code: Option[String],
    organisationName: String,
    address: AddressResponse
)

object RegisterOrganisationWithIdFrontendResponse {
  implicit val format: OFormat[RegisterOrganisationWithIdFrontendResponse] =
    Json.format[RegisterOrganisationWithIdFrontendResponse]

  def apply(apiResponse: RegisterOrganisationWithIdAPIResponse): RegisterOrganisationWithIdFrontendResponse =
    RegisterOrganisationWithIdFrontendResponse(
      safeId = apiResponse.responseDetail.SAFEID,
      code = apiResponse.responseDetail.organisation.map(_.code),
      organisationName = apiResponse.responseDetail.organisation.map(_.organisationName).get,
      address = AddressResponse(
        addressLine1 = apiResponse.responseDetail.address.addressLine1,
        addressLine2 = apiResponse.responseDetail.address.addressLine2,
        addressLine3 = apiResponse.responseDetail.address.addressLine3,
        addressLine4 = apiResponse.responseDetail.address.addressLine4,
        postalCode = apiResponse.responseDetail.address.postalCode,
        countryCode = apiResponse.responseDetail.address.countryCode
      )
    )
}

case class OrganisationResponse(
    organisationName: String,
    code: String
)

object OrganisationResponse {
  implicit val format: OFormat[OrganisationResponse] = Json.format[OrganisationResponse]
}
