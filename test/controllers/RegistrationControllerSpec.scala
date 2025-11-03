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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{BadRequest, Ok}
import uk.gov.hmrc.carfregistration.controllers.RegistrationController
import uk.gov.hmrc.carfregistration.models.Address
import uk.gov.hmrc.carfregistration.models.requests.{RegisterIndividualWithIdRequest, RegisterOrganisationWithIdRequest}
import uk.gov.hmrc.carfregistration.models.responses.{RegisterIndividualWithIdResponse, RegisterOrganisationWithIdResponse}
import uk.gov.hmrc.carfregistration.services.RegistrationService

import scala.concurrent.Future

class RegistrationControllerSpec extends SpecBase {

  val mockService: RegistrationService       = mock[RegistrationService]
  val testController: RegistrationController = new RegistrationController(cc, fakeAuthAction, mockService)

  val testServiceResponseBody: JsValue = Json.toJson(
    RegisterIndividualWithIdResponse(
      safeId = "test-safe-id",
      firstName = "testFName",
      lastName = "testLName",
      middleName = Some("testMName"),
      address = Address(
        addressLine1 = "TestLine1",
        addressLine2 = Some("TestLine2"),
        addressLine3 = Some("TestLine3"),
        addressLine4 = Some("TestLine4"),
        postalCode = Some("ABC 123"),
        countryCode = "GB"
      )
    )
  )

  val testRequest: JsValue = Json.toJson(
    RegisterIndividualWithIdRequest(
      requiresNameMatch = true,
      IDNumber = "123",
      IDType = "testType",
      dateOfBirth = "123",
      firstName = "Katie",
      lastName = "Long"
    )
  )

  // automatch
  val testOrganisationRequest: JsValue = Json.toJson(
    RegisterOrganisationWithIdRequest(
      requiresNameMatch = false,
      IDNumber = "1234567890",
      IDType = "UTR",
      organisationName = None,
      organisationType = None
    )
  )

  val testServiceOrganisationResponseBody: JsValue = Json.toJson(
    RegisterOrganisationWithIdResponse(
      safeId = "XW3249234924",
      code = Some("0001"),
      organisationName = "Monsters Inc",
      address = Address(
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
      "must return response from service" in {
        when(mockService.returnResponse(any())).thenReturn(Ok(testServiceResponseBody))

        val result = testController.registerIndividualWithId()(fakeRequestWithJsonBody(testRequest))

        result.toString mustBe Future.successful(Ok(testServiceResponseBody)).toString
      }
      "must return bad request when the request is not valid" in {
        val result = testController.registerIndividualWithId()(fakeRequestWithJsonBody(Json.toJson("invalid timmy")))

        result.toString mustBe Future.successful(BadRequest("")).toString
      }
    }
    "registerOrganisationWithId" - {
      "must return response from service" in {
        when(mockService.returnResponseOrganisation(any[RegisterOrganisationWithIdRequest]()))
          .thenReturn(Ok(testServiceOrganisationResponseBody))

        val result = testController.registerOrganisationWithId()(fakeRequestWithJsonBody(testOrganisationRequest))

        result.toString mustBe Future.successful(Ok(testServiceOrganisationResponseBody)).toString
      }
      "must return bad request when the request is not valid" in {
        val result = testController.registerOrganisationWithId()(fakeRequestWithJsonBody(Json.toJson("invalid johnny")))

        result.toString mustBe Future.successful(BadRequest("")).toString
      }
    }
  }
}
