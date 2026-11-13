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

package controllers

import base.SpecBase
import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.carfaccount.controllers.routes
import uk.gov.hmrc.carfaccount.models.requests.{RegWithIdAutoMatchOrgFrontendRequest, RegWithIdUserEntryOrgFrontendRequest, RegWithNinoIndFrontendRequest, RegWithUtrIndFrontendRequest}
import uk.gov.hmrc.carfaccount.models.responses.{AddressResponse, RegWithIdIndFrontendResponse, RegWithIdOrgFrontendResponse, RegWithoutIdFrontendResponse}
import uk.gov.hmrc.carfaccount.models.{ApiError, InternalServerError, JsonValidationError, NotFoundError}
import uk.gov.hmrc.carfaccount.services.RegistrationService

import scala.concurrent.Future

class RegistrationControllerSpec extends SpecBase {

  val mockService: RegistrationService = mock[RegistrationService]

  val testServiceResponseSuccess = RegWithIdIndFrontendResponse(
    safeId = "test-SafeId",
    firstName = "Alex",
    lastName = "Hamilton",
    middleName = Some("Mathilda"),
    address = testAddressResponse
  )

  val testFrontendRequestWithNinoJson: JsValue = Json.toJson(
    RegWithNinoIndFrontendRequest(
      requiresNameMatch = true,
      IDNumber = "test-Nino",
      IDType = "NINO",
      dateOfBirth = "test-DOB",
      firstName = "Alex",
      lastName = "Hamilton"
    )
  )

  val testFrontendRequestWithUtrJson: JsValue = Json.toJson(
    RegWithUtrIndFrontendRequest(
      requiresNameMatch = true,
      IDNumber = "test-Utr",
      IDType = "UTR",
      firstName = "Alex",
      lastName = "Hamilton"
    )
  )

  private def testAddressResponse = AddressResponse(
    addressLine1 = "64",
    addressLine2 = Some("Zoo"),
    addressLine3 = Some("Lane"),
    addressLine4 = Some("Sixty Four"),
    postalCode = Some("G66 2AZ"),
    countryCode = "GB"
  )

  // user entry organisation
  val testOrganisationWithUtrUserEntryRequest: JsValue = Json.toJson(
    RegWithIdUserEntryOrgFrontendRequest(
      requiresNameMatch = true,
      IDNumber = "1234567890",
      IDType = "UTR",
      organisationName = "Testing Ltd",
      organisationType = "0001"
    )
  )

  // automatch
  val testOrganisationWithUtrAutoMatchRequest: JsValue = Json.toJson(
    RegWithIdAutoMatchOrgFrontendRequest(
      requiresNameMatch = false,
      IDNumber = "1234567890",
      IDType = "UTR"
    )
  )

  val testServiceOrganisationResponseBody: JsValue = Json.toJson(
    RegWithIdOrgFrontendResponse(
      safeId = "XW3249234924",
      code = Some("0001"),
      organisationName = "Monsters Inc",
      address = AddressResponse(
        addressLine1 = "TestLine1",
        addressLine2 = Some("TestLine2"),
        addressLine3 = Some("TestLine3"),
        addressLine4 = Some("TestLine4"),
        postalCode = Some("ABC 123"),
        countryCode = "GB"
      )
    )
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockService)
  }

  "RegistrationController" - {
    "registerIndividualWithNino" - {
      "must return success response when the service can retrieve a business partner record" in {
        when(mockService.registerIndWithNino(any())(any())).thenReturn(Future(Right(testServiceResponseSuccess)))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerIndividualWithNino().url)
            .withJsonBody(testFrontendRequestWithNinoJson)

          val result = route(application, request).value

          status(result)        mustBe OK
          contentAsJson(result) mustBe Json.toJson(testServiceResponseSuccess)
        }
      }

      "must return not found response when the service cannot retrieve a business partner record" in {
        when(mockService.registerIndWithNino(any())(any())).thenReturn(Future(Left(NotFoundError)))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerIndividualWithNino().url)
            .withJsonBody(testFrontendRequestWithNinoJson)

          val result = route(application, request).value

          status(result)        mustBe NOT_FOUND
          contentAsString(result) must include("Could not find or create a business partner record for this user")

        }
      }

      "must return internal server error response when the service returns an unexpected error" in {
        when(mockService.registerIndWithNino(any())(any())).thenReturn(Future(Left(InternalServerError)))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerIndividualWithNino().url)
            .withJsonBody(testFrontendRequestWithNinoJson)

          val result = route(application, request).value

          status(result)        mustBe INTERNAL_SERVER_ERROR
          contentAsString(result) must include("Unexpected error")
        }
      }

      "must return bad request when the request is not valid" in {

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerIndividualWithNino().url)
            .withJsonBody(Json.toJson("invalid timmy"))

          val result = route(application, request).value

          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "registerIndividualWithUtr" - {
      "must return success response when the service can retrieve a business partner record" in {
        when(mockService.registerIndWithUtr(any())(any())).thenReturn(Future(Right(testServiceResponseSuccess)))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerIndividualWithUtr().url)
            .withJsonBody(testFrontendRequestWithUtrJson)

          val result = route(application, request).value

          status(result)        mustBe OK
          contentAsJson(result) mustBe Json.toJson(testServiceResponseSuccess)

        }
      }

      "must return not found response when the service cannot retrieve a business partner record" in {
        when(mockService.registerIndWithUtr(any())(any())).thenReturn(Future(Left(NotFoundError)))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerIndividualWithUtr().url)
            .withJsonBody(testFrontendRequestWithUtrJson)

          val result = route(application, request).value

          status(result)        mustBe NOT_FOUND
          contentAsString(result) must include("Could not find or create a Sole Trader record for this user")

        }
      }

      "must return internal server error response when the service returns an unexpected error" in {
        when(mockService.registerIndWithUtr(any())(any())).thenReturn(Future(Left(InternalServerError)))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerIndividualWithUtr().url)
            .withJsonBody(testFrontendRequestWithUtrJson)

          val result = route(application, request).value

          status(result)        mustBe INTERNAL_SERVER_ERROR
          contentAsString(result) must include("Unexpected error")
        }
      }
      "must return bad request when the request is not valid" in {

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerIndividualWithUtr().url)
            .withJsonBody(Json.toJson("invalid timmy"))

          val result = route(application, request).value

          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "registerUserEntryOrganisationWithId" - {
      "must return success response when the service can retrieve a business record" in {
        val expectedOrgResponse = testServiceOrganisationResponseBody.as[RegWithIdOrgFrontendResponse]

        when(mockService.registerUserEntryOrgWithId(any())(any()))
          .thenReturn(Future.successful(Right(expectedOrgResponse)))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerUserEntryOrganisationWithId().url)
            .withJsonBody(testOrganisationWithUtrUserEntryRequest)

          val result = route(application, request).value

          status(result)        mustBe OK
          contentAsJson(result) mustBe testServiceOrganisationResponseBody

        }
      }

      "must return not found response when the service cannot retrieve a business record" in {
        when(mockService.registerUserEntryOrgWithId(any())(any()))
          .thenReturn(Future.successful(Left(NotFoundError)))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerUserEntryOrganisationWithId().url)
            .withJsonBody(testOrganisationWithUtrUserEntryRequest)

          val result = route(application, request).value

          status(result)        mustBe NOT_FOUND
          contentAsString(result) must include("Could not find or create a business record for this organisation")

        }
      }

      "must return internal server error response when the service returns an unexpected error" in {
        when(mockService.registerUserEntryOrgWithId(any())(any()))
          .thenReturn(Future.successful(Left(InternalServerError)))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerUserEntryOrganisationWithId().url)
            .withJsonBody(testOrganisationWithUtrUserEntryRequest)

          val result = route(application, request).value

          status(result)        mustBe INTERNAL_SERVER_ERROR
          contentAsString(result) must include("Unexpected error")
        }
      }

      "must return bad request when the request is not valid" in {

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerUserEntryOrganisationWithId().url)
            .withJsonBody(Json.toJson("invalid johnny"))

          val result = route(application, request).value

          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "registerAutoMatchOrganisationWithId" - {
      "must return success response when the service can retrieve a business record" in {
        val expectedOrgResponse = testServiceOrganisationResponseBody.as[RegWithIdOrgFrontendResponse]

        when(mockService.registerAutoMatchOrgWithId(any())(any()))
          .thenReturn(Future.successful(Right(expectedOrgResponse)))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerAutoMatchOrganisationWithId().url)
            .withJsonBody(testOrganisationWithUtrAutoMatchRequest)

          val result = route(application, request).value

          status(result)        mustBe OK
          contentAsJson(result) mustBe testServiceOrganisationResponseBody

        }
      }

      "must return not found response when the service cannot retrieve a business record" in {
        when(mockService.registerAutoMatchOrgWithId(any())(any()))
          .thenReturn(Future.successful(Left(NotFoundError)))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerAutoMatchOrganisationWithId().url)
            .withJsonBody(testOrganisationWithUtrAutoMatchRequest)

          val result = route(application, request).value

          status(result)        mustBe NOT_FOUND
          contentAsString(result) must include("Could not find or create a business record for this organisation")

        }
      }

      "must return internal server error response when the service returns an unexpected error" in {
        when(mockService.registerAutoMatchOrgWithId(any())(any()))
          .thenReturn(Future.successful(Left(InternalServerError)))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerAutoMatchOrganisationWithId().url)
            .withJsonBody(testOrganisationWithUtrAutoMatchRequest)

          val result = route(application, request).value

          status(result)        mustBe INTERNAL_SERVER_ERROR
          contentAsString(result) must include("Unexpected error")
        }
      }

      "must return bad request when the request is not valid" in {

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerAutoMatchOrganisationWithId().url)
            .withJsonBody(Json.toJson("invalid johnny"))

          val result = route(application, request).value

          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "registerIndividualWithoutId" - {

      val frontendRequestJsonStr: String =
        """
          |{
          |  "firstName": "John",
          |  "lastName": "Doe",
          |  "dateOfBirth": "1990-01-01",
          |  "address": {
          |    "addressLine1": "123 Test Street",
          |    "addressLine2": "Flat 1",
          |    "addressLine3": null,
          |    "townOrCity": "France",
          |    "postalCode": "SW1A 1AA",
          |    "countryCode": "FR"
          |  },
          |  "contactDetails": {
          |    "emailAddress": "john.doe@example.com",
          |    "phoneNumber": "07123456789"
          |  }
          |}
          |""".stripMargin

      val frontendRequestJson: JsValue = Json.parse(frontendRequestJsonStr)

      "must return OK when the service returns a successful response" in {
        val apiResponse =
          RegWithoutIdFrontendResponse(
            safeId = "SAFE123456"
          )

        when(mockService.registerIndWithoutId(any())(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](apiResponse))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerIndividualWithoutId().url)
            .withJsonBody(frontendRequestJson)

          val result = route(application, request).value

          status(result)        mustBe OK
          contentAsJson(result) mustBe Json.toJson(apiResponse)
        }
      }

      "must return internal server error when the service returns NotFoundError" in {
        when(mockService.registerIndWithoutId(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithoutIdFrontendResponse](NotFoundError))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerIndividualWithoutId().url)
            .withJsonBody(frontendRequestJson)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "must return internal server error when the service returns InternalServerError" in {
        when(mockService.registerIndWithoutId(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithoutIdFrontendResponse](InternalServerError))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerIndividualWithoutId().url)
            .withJsonBody(frontendRequestJson)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "must return internal server error when the service returns JsonValidationError" in {
        when(mockService.registerIndWithoutId(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithoutIdFrontendResponse](JsonValidationError))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerIndividualWithoutId().url)
            .withJsonBody(frontendRequestJson)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "registerOrganisationWithoutId" - {

      val frontendRequestJsonStr: String =
        """
          |{
          |  "organisationName": "ABC Limited",
          |  "address": {
          |    "addressLine1": "123 Test Street",
          |    "addressLine2": "Flat 1",
          |    "addressLine3": null,
          |    "townOrCity": "France",
          |    "postalCode": "SW1A 1AA",
          |    "countryCode": "FR"
          |  },
          |  "contactDetails": {
          |    "emailAddress": "john.doe@example.com",
          |    "phoneNumber": "07123456789"
          |  }
          |}
          |""".stripMargin

      val frontendRequestJson: JsValue = Json.parse(frontendRequestJsonStr)

      "must return OK when the service returns a successful response" in {
        val apiResponse =
          RegWithoutIdFrontendResponse(
            safeId = "SAFE123456"
          )

        when(mockService.registerOrgWithoutId(any())(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](apiResponse))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerOrganisationWithoutId().url)
            .withJsonBody(frontendRequestJson)

          val result = route(application, request).value

          status(result)        mustBe OK
          contentAsJson(result) mustBe Json.toJson(apiResponse)
        }
      }

      "must return internal server error when the service returns NotFoundError" in {
        when(mockService.registerOrgWithoutId(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithoutIdFrontendResponse](NotFoundError))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerOrganisationWithoutId().url)
            .withJsonBody(frontendRequestJson)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "must return internal server error when the service returns InternalServerError" in {
        when(mockService.registerOrgWithoutId(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithoutIdFrontendResponse](InternalServerError))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerOrganisationWithoutId().url)
            .withJsonBody(frontendRequestJson)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "must return internal server error when the service returns JsonValidationError" in {
        when(mockService.registerOrgWithoutId(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegWithoutIdFrontendResponse](JsonValidationError))

        val application = applicationBuilder()
          .overrides(
            bind[RegistrationService].toInstance(mockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RegistrationController.registerOrganisationWithoutId().url)
            .withJsonBody(frontendRequestJson)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}
