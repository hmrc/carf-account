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
import uk.gov.hmrc.carfregistration.models.{ApiError, MissingFieldsError}

case class RegWithIdIndFrontendResponse(
    safeId: String,
    firstName: String,
    lastName: String,
    middleName: Option[String],
    address: AddressResponse
)

object RegWithIdIndFrontendResponse {
  implicit val format: OFormat[RegWithIdIndFrontendResponse] =
    Json.format[RegWithIdIndFrontendResponse]

  def apply(apiResponse: RegWithIdIndApiResponse): Either[ApiError, RegWithIdIndFrontendResponse] =
    apiResponse.responseDetail.individual match {
      case Some(individualResponse: IndividualResponse) =>
        Right(
          RegWithIdIndFrontendResponse(
            safeId = apiResponse.responseDetail.SAFEID,
            firstName = individualResponse.firstName,
            lastName = individualResponse.lastName,
            middleName = individualResponse.middleName,
            address = apiResponse.responseDetail.address
          )
        )
      case None                                         => Left(MissingFieldsError)
    }
}

case class IndividualResponse(
    firstName: String,
    lastName: String,
    middleName: Option[String]
)

object IndividualResponse {
  implicit val format: OFormat[IndividualResponse] = Json.format[IndividualResponse]
}
