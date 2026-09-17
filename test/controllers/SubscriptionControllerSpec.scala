/*
 * Copyright 2026 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{contentAsJson, contentAsString, status}
import uk.gov.hmrc.carfregistration.connectors.SubscriptionConnector
import uk.gov.hmrc.carfregistration.controllers.SubscriptionController
import uk.gov.hmrc.carfregistration.models.*
import uk.gov.hmrc.carfregistration.models.requests.{Contact, SubscriptionRequest}
import uk.gov.hmrc.carfregistration.models.responses.{CarfSubscriptionDetails, SubscriptionDisplayResponse, SubscriptionDisplaySuccess}
import uk.gov.hmrc.carfregistration.types.ResultT
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class SubscriptionControllerSpec extends SpecBase {

  private val mockConnector: SubscriptionConnector   = mock[SubscriptionConnector]
  private val testController: SubscriptionController =
    new SubscriptionController(cc, fakeAuthAction, mockConnector)

  private val testContact: Contact = Contact(
    email = "test@example.com",
    individual = Some(Individual("John", "Doe")),
    organisation = None,
    phone = Some("1234567890"),
    mobile = None
  )

  private val testSubscriptionRequest: SubscriptionRequest = SubscriptionRequest(
    gbUser = true,
    idNumber = "XM000123456789",
    idType = "SAFE",
    primaryContact = testContact,
    secondaryContact = None,
    tradingName = Some("Test Trading Ltd")
  )

  private val testSubscriptionRequestJson: JsValue = Json.toJson(testSubscriptionRequest)

  private val testSuccessResponseBody: String =
    """{
      |  "success": {
      |    "CARFReference": "XCARF1234567890",
      |    "processingDate": "2001-12-17T09:30:47Z"
      |  }
      |}""".stripMargin

  private val testCarfId: String = "XCARF1234567890"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  "SubscriptionController" - {
    "createSubscription" - {
      "must return success response when the connector successfully sends subscription information" in {
        when(mockConnector.sendSubscriptionInformation(any())(any()))
          .thenReturn(
            EitherT.rightT[Future, uk.gov.hmrc.carfregistration.models.ApiError](
              HttpResponse(OK, testSuccessResponseBody)
            )
          )

        val result = testController.createSubscription()(fakeRequestWithJsonBody(testSubscriptionRequestJson))

        status(result)          mustBe OK
        contentAsString(result) mustBe testSuccessResponseBody

        verify(mockConnector, times(1)).sendSubscriptionInformation(eqTo(testSubscriptionRequest))(any())
      }

      "must return internal server error when the connector returns SERVICE_UNAVAILABLE (connector converts to Left)" in {
        when(mockConnector.sendSubscriptionInformation(any())(any()))
          .thenReturn(EitherT.leftT[Future, HttpResponse](InternalServerError))

        val result = testController.createSubscription()(fakeRequestWithJsonBody(testSubscriptionRequestJson))

        status(result)        mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Error sending subscription information")

        verify(mockConnector, times(1)).sendSubscriptionInformation(eqTo(testSubscriptionRequest))(any())
      }

      "must return unprocessable entity with already_registered status when error code returned is 007" in {
        val enrichedBody =
          """{"errorDetail":{"errorCode":"007","errorMessage":"Already registered"},"status":"already_registered"}"""

        when(mockConnector.sendSubscriptionInformation(any())(any()))
          .thenReturn(
            EitherT.rightT[Future, uk.gov.hmrc.carfregistration.models.ApiError](
              HttpResponse(UNPROCESSABLE_ENTITY, enrichedBody)
            )
          )

        val result = testController.createSubscription()(fakeRequestWithJsonBody(testSubscriptionRequestJson))

        status(result)        mustBe UNPROCESSABLE_ENTITY
        contentAsString(result) must include("already_registered")

        verify(mockConnector, times(1)).sendSubscriptionInformation(eqTo(testSubscriptionRequest))(any())
      }

      "must return unprocessable entity 422 with 'Invalid ID type' message when error code returned is 015" in {
        val duplicateSubmissionResponseBody =
          """{"errorDetail":{"errorCode":"015","errorMessage":"Invalid ID type"}}"""

        when(mockConnector.sendSubscriptionInformation(any())(any()))
          .thenReturn(
            EitherT.rightT[Future, uk.gov.hmrc.carfregistration.models.ApiError](
              HttpResponse(UNPROCESSABLE_ENTITY, duplicateSubmissionResponseBody)
            )
          )

        val result = testController.createSubscription()(fakeRequestWithJsonBody(testSubscriptionRequestJson))

        status(result)        mustBe UNPROCESSABLE_ENTITY
        contentAsString(result) must include("Invalid ID type")
      }

      "must return bad request when the request body is not valid JSON" in {
        val result = testController.createSubscription()(fakeRequestWithJsonBody(Json.toJson("invalid request")))

        status(result)        mustBe BAD_REQUEST
        contentAsString(result) must include("SubscriptionRequest is invalid")

        verify(mockConnector, times(0)).sendSubscriptionInformation(any())(any())
      }
    }

    "updateSubscription" - {
      "must return success response when the connector successfully sends subscription information" in {
        when(mockConnector.updateSubscription(any())(any()))
          .thenReturn(
            EitherT.rightT[Future, (ApiError, Option[ErrorDetail])](
              HttpResponse(OK, testSuccessResponseBody)
            )
          )

        val result = testController.updateSubscription()(fakeRequestWithJsonBody(testSubscriptionRequestJson))

        status(result)          mustBe OK
        contentAsString(result) mustBe testSuccessResponseBody

        verify(mockConnector, times(1)).updateSubscription(eqTo(testSubscriptionRequest))(any())
      }

      "must return internal server error when the connector returns InternalServerError with ErrorDetail" in {
        val errorDetail = ErrorDetail(
          errorDetail = ErrorDetails(
            "2026-05-11T21:54:12.015Z",
            "1ae81b45-41b4-4642-ae1c-db1126900001",
            Some("503"),
            Some("Service Unavailable"),
            None,
            None
          )
        )

        when(mockConnector.updateSubscription(any())(any()))
          .thenReturn(EitherT.leftT[Future, HttpResponse]((InternalServerError, Some(errorDetail))))

        val result = testController.updateSubscription()(fakeRequestWithJsonBody(testSubscriptionRequestJson))

        status(result) mustBe INTERNAL_SERVER_ERROR

        contentAsString(result) must include("Service Unavailable")
        contentAsString(result) must include("correlationId")

        verify(mockConnector, times(1)).updateSubscription(eqTo(testSubscriptionRequest))(any())
      }

      "must return internal server error when the connector returns InternalServerError without ErrorDetail" in {
        when(mockConnector.updateSubscription(any())(any()))
          .thenReturn(EitherT.leftT[Future, HttpResponse]((InternalServerError, None)))

        val result = testController.updateSubscription()(fakeRequestWithJsonBody(testSubscriptionRequestJson))

        status(result)        mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Error sending updated subscription information")

        verify(mockConnector, times(1)).updateSubscription(eqTo(testSubscriptionRequest))(any())
      }

      "must return bad request when the request body is not valid JSON" in {
        val result = testController.updateSubscription()(fakeRequestWithJsonBody(Json.toJson("invalid request")))

        status(result)        mustBe BAD_REQUEST
        contentAsString(result) must include("Update Subscription Request's Json is invalid")

        verify(mockConnector, times(0)).updateSubscription(any())(any())
      }
    }

    "displaySubscription" - {
      "must return OK with SubscriptionDisplayResponse in json when the connector successfully returns subscription information" in {
        val testSubscriptionDisplayResponse = SubscriptionDisplayResponse(
          success = SubscriptionDisplaySuccess(
            processingDate = "2024-01-25T09:26:17Z",
            carfSubscriptionDetails = CarfSubscriptionDetails(
              carfReference = testCarfId,
              tradingName = Some("CARF LTD"),
              gbUser = true,
              primaryContact = testContact,
              secondaryContact = None
            )
          )
        )

        when(mockConnector.displaySubscriptionInformation(any())(any()))
          .thenReturn(ResultT.fromValue(testSubscriptionDisplayResponse))

        val result = testController.displaySubscription(testCarfId)(fakeRequest)

        status(result)        mustBe OK
        contentAsJson(result) mustBe Json.toJson(testSubscriptionDisplayResponse)

        verify(mockConnector, times(1)).displaySubscriptionInformation(eqTo(testCarfId))(any())
      }

      "must return NotFound when the connector returns NotFoundError" in {
        when(mockConnector.displaySubscriptionInformation(any())(any()))
          .thenReturn(ResultT.fromError(NotFoundError))

        val result = testController.displaySubscription(testCarfId)(fakeRequest)

        status(result)        mustBe NOT_FOUND
        contentAsString(result) must include("Could not find a subscription record for this user")

        verify(mockConnector, times(1)).displaySubscriptionInformation(eqTo(testCarfId))(any())
      }

      "must return InternalServerError when the connector returns InternalServerError" in {
        when(mockConnector.displaySubscriptionInformation(any())(any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val result = testController.displaySubscription(testCarfId)(fakeRequest)

        status(result)        mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Unexpected error")

        verify(mockConnector, times(1)).displaySubscriptionInformation(eqTo(testCarfId))(any())
      }

      "must return InternalServerError when the connector returns JsonValidationError" in {
        when(mockConnector.displaySubscriptionInformation(any())(any()))
          .thenReturn(ResultT.fromError(JsonValidationError))

        val result = testController.displaySubscription(testCarfId)(fakeRequest)

        status(result)        mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Unexpected error")

        verify(mockConnector, times(1)).displaySubscriptionInformation(eqTo(testCarfId))(any())
      }
    }
  }
}
