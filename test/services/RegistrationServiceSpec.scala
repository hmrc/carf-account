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

package services

import base.SpecBase
import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import uk.gov.hmrc.carfregistration.connectors.RegistrationConnector
import uk.gov.hmrc.carfregistration.models.requests.RegisterIndWithIdFrontendRequest
import uk.gov.hmrc.carfregistration.models.responses.*
import uk.gov.hmrc.carfregistration.models.{ApiError, InternalServerError, NotFoundError, UuidGen}
import uk.gov.hmrc.carfregistration.services.RegistrationService

import java.util.UUID
import scala.concurrent.Future

class RegistrationServiceSpec extends SpecBase {

  val mockConnector: RegistrationConnector = mock[RegistrationConnector]
  val mockUUIDGen: UuidGen                 = mock[UuidGen]

  val testService: RegistrationService = new RegistrationService(mockConnector, clock, mockUUIDGen)

  val testFrontendRequest =
    RegisterIndWithIdFrontendRequest(
      requiresNameMatch = true,
      IDNumber = "test-IDNumber",
      IDType = "test-Type",
      dateOfBirth = "test-DOB",
      firstName = "Colin",
      lastName = "Cranberry"
    )

  val testAPIResponse = RegisterIndWithIdAPIResponse(
    responseCommon = ResponseCommon(status = "200"),
    responseDetail = ResponseDetail(
      SAFEID = "test-SAFEID",
      address = testAddressResponse,
      individual = IndividualResponse(firstName = "Colin", lastName = "Cranberry", middleName = Some("Pikachu"))
    )
  )

  val testFrontendResponse = RegisterIndWithIdFrontendResponse(
    safeId = "test-SAFEID",
    firstName = "Colin",
    lastName = "Cranberry",
    middleName = Some("Pikachu"),
    address = testAddressResponse
  )

  private def testAddressResponse = AddressResponse(
    addressLine1 = "64",
    addressLine2 = Some("Zoo"),
    addressLine3 = Some("Lane"),
    addressLine4 = Some("Sixty Four"),
    postalCode = Some("G66 2AZ"),
    countryCode = "GB"
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
    when(mockUUIDGen.randomUUID()).thenReturn(UUID(1, 2))
  }

  "RegistrationController" - {
    "registerIndividualWithId" - {
      "must return success response when the service can retrieve a business partner record" in {
        when(mockConnector.individualWithNino(any())(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testAPIResponse))

        val result = testService.registerIndividualWithId(testFrontendRequest).futureValue

        result mustBe Right(testFrontendResponse)
      }
      "must return not found response when the service cannot retrieve a business partner record" in {
        when(mockConnector.individualWithNino(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndWithIdAPIResponse](NotFoundError))

        val result = testService.registerIndividualWithId(testFrontendRequest).futureValue

        result mustBe Left(NotFoundError)
      }
      "must return internal server error response when the service returns an unexpected error" in {
        when(mockConnector.individualWithNino(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndWithIdAPIResponse](InternalServerError))

        val result = testService.registerIndividualWithId(testFrontendRequest).futureValue

        result mustBe Left(InternalServerError)
      }
    }
  }
}
