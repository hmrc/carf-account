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
import uk.gov.hmrc.carfregistration.models.*
import uk.gov.hmrc.carfregistration.models.requests.{RegWithIdAutoMatchOrgFrontendRequest, RegWithIdUserEntryOrgFrontendRequest, RegWithNinoIndFrontendRequest, RegWithUtrIndFrontendRequest}
import uk.gov.hmrc.carfregistration.models.responses.*
import uk.gov.hmrc.carfregistration.services.RegistrationService

import java.util.UUID
import scala.concurrent.Future

class RegistrationServiceSpec extends SpecBase {

  val mockConnector: RegistrationConnector = mock[RegistrationConnector]
  val mockUUIDGen: UuidGen                 = mock[UuidGen]

  val testService: RegistrationService = new RegistrationService(mockConnector, clock, mockUUIDGen)

  val testFrontendRequestIndWithNino =
    RegWithNinoIndFrontendRequest(
      requiresNameMatch = true,
      IDNumber = "test-IDNumber",
      IDType = "test-Type",
      dateOfBirth = "test-DOB",
      firstName = "Colin",
      lastName = "Cranberry"
    )

  val testFrontendRequestIndWithUtr =
    RegWithUtrIndFrontendRequest(
      requiresNameMatch = true,
      IDNumber = "test-IDNumber",
      IDType = "test-Type",
      firstName = "Colin",
      lastName = "Cranberry"
    )

  val testAPIResponseIndividual = RegWithIdIndApiResponse(
    responseCommon = ResponseCommon(status = "200"),
    responseDetail = ResponseDetail(
      SAFEID = "test-SAFEID",
      address = testAddressResponse,
      individual = Some(IndividualResponse(firstName = "Colin", lastName = "Cranberry", middleName = Some("Pikachu"))),
      organisation = None
    )
  )

  val testFrontendResponse = RegWithIdIndFrontendResponse(
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

  val testUserEnteredOrgWithUtrFrontendRequest = RegWithIdUserEntryOrgFrontendRequest(
    requiresNameMatch = true,
    IDNumber = "1234567890",
    IDType = "UTR",
    organisationName = "Testing Ltd",
    organisationType = "0001"
  )

  val testApiResponseUserEntryOrg = RegWithIdOrgApiResponse(
    responseCommon = ResponseCommon(status = "OK"),
    responseDetail = ResponseDetail(
      SAFEID = "test-SAFEID-org",
      address = testAddressResponse,
      individual = None,
      organisation = Some(OrganisationResponse(organisationName = "Testing Ltd", code = Some("0001")))
    )
  )

  val testOrganisationFrontendResponse = RegWithIdOrgFrontendResponse(
    safeId = "test-SAFEID-org",
    organisationName = "Testing Ltd",
    code = Some("0001"),
    address = testAddressResponse
  )

  val testAutoMatchOrgWithUtrFrontendRequest = RegWithIdAutoMatchOrgFrontendRequest(
    requiresNameMatch = false,
    IDNumber = "9876543210",
    IDType = "UTR"
  )

  val testApiResponseAutoMatchOrg = RegWithIdOrgApiResponse(
    responseCommon = ResponseCommon(status = "OK"),
    responseDetail = ResponseDetail(
      SAFEID = "test-SAFEID-automatch",
      address = testAddressResponse,
      individual = None,
      organisation = Some(OrganisationResponse(organisationName = "AutoMatch Ltd", code = Some("0002")))
    )
  )

  val testAutoMatchOrganisationFrontendResponse = RegWithIdOrgFrontendResponse(
    safeId = "test-SAFEID-automatch",
    organisationName = "AutoMatch Ltd",
    code = Some("0002"),
    address = testAddressResponse
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
    when(mockUUIDGen.randomUUID()).thenReturn(UUID(1, 2))
  }

  "RegistrationService" - {
    "registerIndividualWithNino [Nino]" - {
      "must return success frontend response when connector returns successful response to a REQ with a Dob" in {
        when(mockConnector.individualWithId(any())(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testAPIResponseIndividual))
        val result = testService.registerIndWithNino(testFrontendRequestIndWithNino).futureValue
        result mustBe Right(testFrontendResponse)
      }
      "must return success frontend response when connector returns successful response to a REQ without a Dob" in {
        when(mockConnector.individualWithId(any())(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testAPIResponseIndividual))
        val result = testService.registerIndWithNino(testFrontendRequestIndWithUtr).futureValue
        result mustBe Right(testFrontendResponse)
      }
      "must return not found when the connector returns a not found" in {
        when(mockConnector.individualWithId(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithIdIndApiResponse](NotFoundError))
        val result = testService.registerIndWithNino(testFrontendRequestIndWithNino).futureValue
        result mustBe Left(NotFoundError)
      }
      "must return an internal server error when the connector encounters an unexpected error" in {
        when(mockConnector.individualWithId(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithIdIndApiResponse](InternalServerError))
        val result = testService.registerIndWithNino(testFrontendRequestIndWithNino).futureValue
        result mustBe Left(InternalServerError)
      }
      "must return an json validation error when the connector cannot parse the response" in {
        when(mockConnector.individualWithId(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithIdIndApiResponse](JsonValidationError))
        val result = testService.registerIndWithNino(testFrontendRequestIndWithNino).futureValue
        result mustBe Left(JsonValidationError)
      }
    }

    "registerIndividualWithUtr" - {
      "must return success frontend response model when the connector returns a successful response" in {
        when(mockConnector.individualWithId(any())(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testAPIResponseIndividual))
        val result = testService.registerIndWithUtr(testFrontendRequestIndWithUtr).futureValue
        result mustBe Right(testFrontendResponse)
      }
      "must return not found when the connector returns a not found" in {
        when(mockConnector.individualWithId(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithIdIndApiResponse](NotFoundError))
        val result = testService.registerIndWithUtr(testFrontendRequestIndWithUtr).futureValue
        result mustBe Left(NotFoundError)
      }
      "must return an internal server error when the connector encounters an unexpected error" in {
        when(mockConnector.individualWithId(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithIdIndApiResponse](InternalServerError))
        val result = testService.registerIndWithUtr(testFrontendRequestIndWithUtr).futureValue
        result mustBe Left(InternalServerError)
      }
      "must return an json validation error when the connector cannot parse the response" in {
        when(mockConnector.individualWithId(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithIdIndApiResponse](JsonValidationError))
        val result = testService.registerIndWithUtr(testFrontendRequestIndWithUtr).futureValue
        result mustBe Left(JsonValidationError)
      }
    }

    "registerUserEnteredOrganisationWithId" - {
      "must return success frontend response when the connector returns a successful response" in {
        when(mockConnector.organisationWithID(any())(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testApiResponseUserEntryOrg))

        val result =
          testService.registerUserEntryOrgWithId(testUserEnteredOrgWithUtrFrontendRequest).futureValue

        result mustBe Right(testOrganisationFrontendResponse)
      }

      "must return not found when the connector returns a not found error" in {
        when(mockConnector.organisationWithID(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithIdOrgApiResponse](NotFoundError))

        val result =
          testService.registerUserEntryOrgWithId(testUserEnteredOrgWithUtrFrontendRequest).futureValue

        result mustBe Left(NotFoundError)
      }

      "must return an internal server error when the connector returns an unexpected error" in {
        when(mockConnector.organisationWithID(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithIdOrgApiResponse](InternalServerError))

        val result =
          testService.registerUserEntryOrgWithId(testUserEnteredOrgWithUtrFrontendRequest).futureValue

        result mustBe Left(InternalServerError)
      }

      "must return a json validation error when the connector cannot parse the response" in {
        when(mockConnector.organisationWithID(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithIdOrgApiResponse](JsonValidationError))

        val result =
          testService.registerUserEntryOrgWithId(testUserEnteredOrgWithUtrFrontendRequest).futureValue

        result mustBe Left(JsonValidationError)
      }
    }

    "registerAutoMatchOrganisationWithId" - {
      "must return success frontend response when the connector returns a successful response" in {
        when(mockConnector.organisationWithID(any())(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testApiResponseAutoMatchOrg))

        val result =
          testService.registerAutoMatchOrgWithId(testAutoMatchOrgWithUtrFrontendRequest).futureValue

        result mustBe Right(testAutoMatchOrganisationFrontendResponse)
      }

      "must return not found when the connector returns a not found error" in {
        when(mockConnector.organisationWithID(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithIdOrgApiResponse](NotFoundError))

        val result =
          testService.registerAutoMatchOrgWithId(testAutoMatchOrgWithUtrFrontendRequest).futureValue

        result mustBe Left(NotFoundError)
      }

      "must return an internal server error when the connector returns an unexpected error" in {
        when(mockConnector.organisationWithID(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithIdOrgApiResponse](InternalServerError))

        val result =
          testService.registerAutoMatchOrgWithId(testAutoMatchOrgWithUtrFrontendRequest).futureValue

        result mustBe Left(InternalServerError)
      }

      "must return a json validation error when the connector cannot parse the response" in {
        when(mockConnector.organisationWithID(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithIdOrgApiResponse](JsonValidationError))

        val result =
          testService.registerAutoMatchOrgWithId(testAutoMatchOrgWithUtrFrontendRequest).futureValue

        result mustBe Left(JsonValidationError)
      }
    }
  }
}
