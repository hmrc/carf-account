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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.BadRequest
import play.api.test.Helpers.{contentAsJson, contentAsString, status}
import uk.gov.hmrc.carfregistration.controllers.RegistrationController
import uk.gov.hmrc.carfregistration.models.requests.{RegisterIndWithIdFrontendRequest, RegisterOrganisationWithIdFrontendRequest}
import uk.gov.hmrc.carfregistration.models.responses.{AddressResponse, RegisterIndWithIdFrontendResponse, RegisterOrganisationWithIdFrontendResponse}
import uk.gov.hmrc.carfregistration.models.{InternalServerError, NotFoundError}
import uk.gov.hmrc.carfregistration.services.RegistrationService

import scala.concurrent.Future

class RegistrationControllerSpec extends SpecBase {

  val mockService: RegistrationService       = mock[RegistrationService]
  val testController: RegistrationController = new RegistrationController(cc, fakeAuthAction, mockService)

  val testServiceResponseSuccess = RegisterIndWithIdFrontendResponse(
    safeId = "test-SafeId",
    firstName = "Alex",
    lastName = "Hamilton",
    middleName = Some("Mathilda"),
    address = testAddressResponse
  )

  val testFrontendRequestJson: JsValue = Json.toJson(
    RegisterIndWithIdFrontendRequest(
      requiresNameMatch = true,
      IDNumber = "test-Nino",
      IDType = "NINO",
      dateOfBirth = "test-DOB",
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

  // automatch
  val testOrganisationRequest: JsValue = Json.toJson(
    RegisterOrganisationWithIdFrontendRequest(
      requiresNameMatch = false,
      IDNumber = "1234567890",
      IDType = "UTR",
      organisationName = None,
      organisationType = None
    )
  )

  val testServiceOrganisationResponseBody: JsValue = Json.toJson(
    RegisterOrganisationWithIdFrontendResponse(
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
    "registerIndividualWithId" - {
      "must return success response when the service can retrieve a business partner record" in {
        when(mockService.registerIndividualWithId(any())(any())).thenReturn(Future(Right(testServiceResponseSuccess)))

        val result = testController.registerIndividualWithId()(fakeRequestWithJsonBody(testFrontendRequestJson))

        status(result)        mustBe OK
        contentAsJson(result) mustBe Json.toJson(testServiceResponseSuccess)
      }
      "must return not found response when the service cannot retrieve a business partner record" in {
        when(mockService.registerIndividualWithId(any())(any())).thenReturn(Future(Left(NotFoundError)))

        val result = testController.registerIndividualWithId()(fakeRequestWithJsonBody(testFrontendRequestJson))

        status(result)        mustBe NOT_FOUND
        contentAsString(result) must include("Could not find or create a business partner record for this user")
      }
      "must return internal server error response when the service returns an unexpected error" in {
        when(mockService.registerIndividualWithId(any())(any())).thenReturn(Future(Left(InternalServerError)))

        val result = testController.registerIndividualWithId()(fakeRequestWithJsonBody(testFrontendRequestJson))

        status(result)        mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Unexpected error")
      }
      "must return bad request when the request is not valid" in {
        val result = testController.registerIndividualWithId()(fakeRequestWithJsonBody(Json.toJson("invalid timmy")))

        result.toString mustBe Future.successful(BadRequest("")).toString
      }
    }

    "registerOrganisationWithId" - {
      "must return success response when the service can retrieve a business record" in {

        val expectedOrgResponse = testServiceOrganisationResponseBody.as[RegisterOrganisationWithIdFrontendResponse]

        when(mockService.registerOrganisationWithId(any())(any()))
          .thenReturn(Future.successful(Right(expectedOrgResponse)))

        val result = testController.registerOrganisationWithId()(fakeRequestWithJsonBody(testOrganisationRequest))

        status(result) mustBe OK

        contentAsJson(result) mustBe testServiceOrganisationResponseBody

      }

      "must return not found response when the service cannot retrieve a business record" in {
        when(mockService.registerOrganisationWithId(any())(any()))
          .thenReturn(Future.successful(Left(NotFoundError)))

        val result = testController.registerOrganisationWithId()(fakeRequestWithJsonBody(testOrganisationRequest))

        status(result)        mustBe NOT_FOUND
        contentAsString(result) must include("Could not find or create a business record for this organisation")
      }

      "must return internal server error response when the service returns an unexpected error" in {
        when(mockService.registerOrganisationWithId(any())(any()))
          .thenReturn(Future.successful(Left(InternalServerError)))

        val result = testController.registerOrganisationWithId()(fakeRequestWithJsonBody(testOrganisationRequest))

        status(result)        mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Unexpected error")
      }

      "must return bad request when the request is not valid" in {
        val result = testController.registerOrganisationWithId()(fakeRequestWithJsonBody(Json.toJson("invalid johnny")))

        result.toString mustBe Future.successful(BadRequest("")).toString
      }
    }
  }
}
