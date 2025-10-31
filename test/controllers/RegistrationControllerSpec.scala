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
import uk.gov.hmrc.carfregistration.models.requests.{IndividualDetails, RegisterIndWithIdAPIRequest, RegisterIndWithIdFrontendRequest, RequestCommon, RequestDetailIndividual}
import uk.gov.hmrc.carfregistration.models.responses.{AddressResponse, IndividualResponse, RegisterIndWithIdAPIResponse, ResponseCommon, ResponseDetail}
import uk.gov.hmrc.carfregistration.services.RegistrationService

import scala.concurrent.Future

class RegistrationControllerSpec extends SpecBase {

  val mockService: RegistrationService       = mock[RegistrationService]
  val testController: RegistrationController = new RegistrationController(cc, fakeAuthAction, mockService)

  val testApiResponseBody: JsValue = Json.toJson(
    RegisterIndWithIdAPIResponse(
      responseCommon = ResponseCommon(status = "200"),
      responseDetail = ResponseDetail(
        SAFEID = "Test-SafeId",
        address = testAddressResponse,
        individual = IndividualResponse(firstName = "Katie", lastName = "Long", middleName = None)
      )
    )
  )

  val testRequest: JsValue = Json.toJson(
    RegisterIndWithIdAPIRequest(
      requestCommon =
        RequestCommon(acknowledgementReference = "test-Ref", receiptDate = "test-Date", regime = "test-Regime"),
      requestDetail = RequestDetailIndividual(
        requiresNameMatch = true,
        IDNumber = "test-IDNumber",
        IDType = "test-IDType",
        individual = IndividualDetails(dateOfBirth = "test-DOB", firstName = "Professor", lastName = "Rowan"),
        isAnAgent = false
      )
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

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockService)
  }

  "RegistrationController" - {
    "registerIndividualWithId" - {
      "must return response from service" in {
        when(mockService.registerIndividualWithId(any())).thenReturn(???)

        val result = testController.registerIndividualWithId()(fakeRequestWithJsonBody(testRequest))

        result.toString mustBe Future.successful(???).toString
      }
      "must return bad request when the request is not valid" in {
        val result = testController.registerIndividualWithId()(fakeRequestWithJsonBody(Json.toJson("invalid timmy")))

        result.toString mustBe Future.successful(BadRequest("")).toString
      }
    }
  }
}
