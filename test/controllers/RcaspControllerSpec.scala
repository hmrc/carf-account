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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.InternalServerError
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.carfaccount.connectors.RcaspConnector
import uk.gov.hmrc.carfaccount.controllers.management.{routes, RcaspController}
import uk.gov.hmrc.carfaccount.models
import uk.gov.hmrc.carfaccount.models.*
import uk.gov.hmrc.carfaccount.models.responses.*

import scala.concurrent.Future

class RcaspControllerSpec extends SpecBase {

  inline private val exampleCarfId = "XCCAR0024000102"

  val mockConnector: RcaspConnector   = mock[RcaspConnector]
  val testController: RcaspController = new RcaspController(
    cc,
    fakeAuthAction,
    fakeCheckCarfIdAction,
    mockConnector
  )

  private val exampleContact =
    RcaspContactDetails(ContactName = "Prof Sada", EmailAddress = "test@example.com", PhoneNumber = Some("07123412345"))

  inline private val exampleRcaspId = "none"
  private val exampleResponseCommon = RcaspResponseCommon(
    OriginatingSystem = "MDTP",
    TransmittingSystem = "EIS",
    RequestType = "VIEW",
    Regime = "CARF",
    ResponseParameters = None
  )

  private val testViewRcaspResponse = ViewRcaspResponse(
    ViewRCASP = ViewRcasp(
      ResponseCommon = exampleResponseCommon,
      ResponseDetails = RcaspResponseDetails(
        RCASPList = List(
          viewAndUpdateRcasp.OrganisationRcaspDetails(
            SubscriptionID = exampleCarfId,
            RCASPID = exampleRcaspId,
            IsRCASPUser = true,
            PartyType = "Organisation",
            RCASPName = "Mesagoza",
            TradingName = "Uva Academy",
            TINDetails = Some(List(TinDetails(TINType = "UTR", TIN = "68936493", IssuedBy = "GB"))),
            AddressDetails = testAddressResponse,
            PrimaryContactDetails = Some(exampleContact),
            SecondaryContactDetails = Some(exampleContact.copy(ContactName = "Prof Turo"))
          )
        )
      )
    )
  )

  private def testAddressResponse = RcaspAddress(
    AddressLine1 = "64",
    AddressLine2 = Some("Zoo"),
    AddressLine3 = Some("Lane"),
    AddressLine4 = Some("Sixty Four"),
    PostalCode = "G66 2AZ",
    CountryCode = "GB"
  )

  private val testViewRcaspResponseJson: String = Json.toJson(testViewRcaspResponse).toString

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  "RcaspController" - {
    val testCreateResponseBody = SubmitRcaspResponse(
      ResponseDetails = Some(
        SubmitResponseDetails(
          SubmitReturnParameters(
            "RCASPID",
            "RCASP12345"
          )
        )
      )
    )

    val testUpdateDeleteResponseBody = SubmitRcaspResponse(ResponseDetails = None)

    val expectedCreateResponse =
      """{"ResponseDetails":{"ReturnParameters":{"Key":"RCASPID","Value":"RCASP12345"}}}"""

    "viewRcasp" - {
      "must return success response when the connector successfully sends GET request" in {
        when(mockConnector.viewRcasps(any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testViewRcaspResponse))

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.RcaspController.viewRcasp(exampleCarfId, exampleRcaspId).url)
          val result  = route(application, request).value

          status(result)          mustBe OK
          contentAsString(result) mustBe testViewRcaspResponseJson
        }
      }

      "must return a NotFound when the connector returns NotFoundError" in {
        when(mockConnector.viewRcasps(any(), any())(any()))
          .thenReturn(EitherT.leftT[Future, ViewRcaspResponse](NotFoundError))

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.RcaspController.viewRcasp(exampleCarfId, exampleRcaspId).url)
          val result  = route(application, request).value

          status(result)        mustBe NOT_FOUND
          contentAsString(result) must include("No RCASPs found")
        }
      }

      "must return internal server error when connector returns" - {
        "InternalServerError" in {
          when(mockConnector.viewRcasps(any(), any())(any()))
            .thenReturn(EitherT.leftT[Future, ViewRcaspResponse](InternalServerError))

          val application = applicationBuilder()
            .overrides(
              bind[RcaspConnector].toInstance(mockConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.RcaspController.viewRcasp(exampleCarfId, exampleRcaspId).url)
            val result  = route(application, request).value

            status(result)        mustBe INTERNAL_SERVER_ERROR
            contentAsString(result) must include("Unexpected error")
          }
        }

        "JsonValidationError" in {
          when(mockConnector.viewRcasps(any(), any())(any()))
            .thenReturn(EitherT.leftT[Future, ViewRcaspResponse](JsonValidationError))

          val application = applicationBuilder()
            .overrides(
              bind[RcaspConnector].toInstance(mockConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.RcaspController.viewRcasp(exampleCarfId, exampleRcaspId).url)
            val result  = route(application, request).value

            status(result)        mustBe INTERNAL_SERVER_ERROR
            contentAsString(result) must include("Unexpected error")
          }
        }
      }
    }

    "createRcasp" - {
      val buildCreateOrgRcaspJson: JsValue =
        Json.parse(
          s"""
             |{
             |  "RCASPManagement": {
             |    "RequestCommon": {
             |      "OriginatingSystem": "MDTP",
             |      "TransmittingSystem": "EIS",
             |      "RequestType": "CREATE",
             |      "Regime": "CARF",
             |      "RequestParameters": [
             |        {
             |          "ParamName": "Test",
             |          "ParamValue": "Test"
             |        }
             |      ]
             |    },
             |    "RequestDetails": {
             |      "RCASPName": "Amazon UK",
             |      "IsRCASPUser": false,
             |      "SubscriptionID": "345567808",
             |      "PartyType": "Organisation",
             |      "TradingName": "Tools for Traders Limited",
             |      "TINDetails": [
             |        {
             |          "TINType": "UTR",
             |          "TIN": "68936493",
             |          "IssuedBy": "GB"
             |        }
             |      ],
             |      "AddressDetails": {
             |        "AddressLine1": "22",
             |        "AddressLine2": "High Street",
             |        "AddressLine3": "Dawley",
             |        "AddressLine4": "Dawley",
             |        "CountryCode": "GB",
             |        "PostalCode": "TF22 2RE"
             |      },
             |      "PrimaryContactDetails": {
             |        "ContactName": "John Smith",
             |        "EmailAddress": "test@gmail.com",
             |        "PhoneNumber": "0789876568"
             |      },
             |      "SecondaryContactDetails": {
             |        "ContactName": "John Smith",
             |        "EmailAddress": "jdoe@example.com",
             |        "PhoneNumber": "0789876568"
             |      }
             |    }
             |  }
             |}
             |
             |""".stripMargin
        )

      "must return success when the connector returns a submit rcasp response" in {
        when(mockConnector.createRcasp(any())(any()))
          .thenReturn(
            EitherT.rightT[Future, ApiError](testCreateResponseBody)
          )

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RcaspController.createRcasp(exampleCarfId).url)
            .withJsonBody(buildCreateOrgRcaspJson)
          val result  = route(application, request).value

          status(result)          mustBe OK
          contentAsString(result) mustBe expectedCreateResponse
        }
      }

      "must return Internal Server Error when the connector returns Internal server error" in {
        when(mockConnector.createRcasp(any())(any()))
          .thenReturn(
            EitherT.leftT[Future, ApiError](models.InternalServerError)
          )

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RcaspController.createRcasp(exampleCarfId).url)
            .withJsonBody(buildCreateOrgRcaspJson)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "must return Internal Server Error when the connector returns Json validation error" in {
        when(mockConnector.createRcasp(any())(any()))
          .thenReturn(
            EitherT.leftT[Future, ApiError](JsonValidationError)
          )

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RcaspController.createRcasp(exampleCarfId).url)
            .withJsonBody(buildCreateOrgRcaspJson)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "must return Bad Request when the request body is not valid JSON" in {
        when(mockConnector.createRcasp(any())(any()))
          .thenReturn(
            EitherT.leftT[Future, ApiError](JsonValidationError)
          )

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RcaspController.createRcasp(exampleCarfId).url)
            .withJsonBody(Json.toJson("invalid request"))

          val result = route(application, request).value

          status(result)          mustBe BAD_REQUEST
          contentAsString(result) mustBe "CreateRcaspRequest is invalid"
        }
      }
    }

    "updateRcasp" - {

      val updateRequest: JsValue = Json.parse(
        s"""
           |{
           |  "RCASPManagement": {
           |    "RequestCommon": {
           |      "OriginatingSystem": "MDTP",
           |      "TransmittingSystem": "EIS",
           |      "RequestType": "UPDATE",
           |      "Regime": "CARF",
           |      "RequestParameters": [
           |        {
           |          "ParamName": "Test",
           |          "ParamValue": "Test"
           |        }
           |      ]
           |    },
           |    "RequestDetails": {
           |      "RCASPName": "Amazon UK",
           |      "IsRCASPUser": false,
           |      "RCASPID": "ZMCAR0123456780",
           |      "SubscriptionID": "XCARF000000001",
           |      "PartyType": "Organisation",
           |      "TradingName": "Tools for Traders Limited",
           |      "TINDetails": [
           |        {
           |          "TINType": "UTR",
           |          "TIN": "68936493",
           |          "IssuedBy": "GB"
           |        }
           |      ],
           |      "AddressDetails": {
           |        "AddressLine1": "22",
           |        "AddressLine2": "High Street",
           |        "AddressLine3": "Dawley",
           |        "AddressLine4": "Dawley",
           |        "CountryCode": "GB",
           |        "PostalCode": "TF22 2RE"
           |      },
           |      "PrimaryContactDetails": {
           |        "ContactName": "John Smith",
           |        "EmailAddress": "jdoe@example.com",
           |        "PhoneNumber": "0789876568"
           |      },
           |      "SecondaryContactDetails": {
           |        "ContactName": "John Smith",
           |        "EmailAddress": "jdoe@example.com",
           |        "PhoneNumber": "0789876568"
           |      }
           |    }
           |  }
           |}
           |""".stripMargin
      )

      "must return success when the connector returns a submit rcasp response" in {
        when(mockConnector.updateRcasp(any())(any()))
          .thenReturn(
            EitherT.rightT[Future, ApiError](testUpdateDeleteResponseBody)
          )

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RcaspController.updateRcasp(exampleCarfId).url)
            .withJsonBody(updateRequest)

          val result = route(application, request).value

          status(result)          mustBe OK
          contentAsString(result) mustBe "{}"
        }
      }

      "must return Internal Server Error when the connector returns Internal server error" in {
        when(mockConnector.updateRcasp(any())(any()))
          .thenReturn(
            EitherT.leftT[Future, ApiError](models.InternalServerError)
          )

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RcaspController.updateRcasp(exampleCarfId).url)
            .withJsonBody(updateRequest)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "must return Internal Server Error when the connector returns Json validation error" in {
        when(mockConnector.updateRcasp(any())(any()))
          .thenReturn(
            EitherT.leftT[Future, ApiError](JsonValidationError)
          )

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RcaspController.updateRcasp(exampleCarfId).url)
            .withJsonBody(updateRequest)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "must return Bad Request when the request body is not valid JSON" in {
        when(mockConnector.updateRcasp(any())(any()))
          .thenReturn(
            EitherT.leftT[Future, ApiError](JsonValidationError)
          )

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RcaspController.updateRcasp(exampleCarfId).url)
            .withJsonBody(Json.toJson("invalid request"))

          val result = route(application, request).value

          status(result)          mustBe BAD_REQUEST
          contentAsString(result) mustBe "UpdateRcaspRequest is invalid"
        }
      }
    }

    "deleteRcasp" - {
      val deleteRequest: JsValue = Json.parse(
        s"""
           |{
           |  "RCASPManagement": {
           |    "RequestCommon": {
           |      "OriginatingSystem": "MDTP",
           |      "TransmittingSystem": "EIS",
           |      "RequestType": "DELETE",
           |      "Regime": "CARF",
           |      "RequestParameters": [
           |        {
           |          "ParamName": "TEST",
           |          "ParamValue": "TEST"
           |        }
           |      ]
           |    },
           |    "RequestDetails": {
           |      "RCASPID": "683373339",
           |      "SubscriptionID": "XCARF000000001"
           |    }
           |  }
           |}
           |""".stripMargin
      )

      "must return success when the connector returns a submit rcasp response" in {
        when(mockConnector.deleteRcasp(any())(any()))
          .thenReturn(
            EitherT.rightT[Future, ApiError](testUpdateDeleteResponseBody)
          )

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(DELETE, routes.RcaspController.deleteRcasp(exampleCarfId).url)
            .withJsonBody(deleteRequest)

          val result = route(application, request).value

          status(result)          mustBe OK
          contentAsString(result) mustBe "{}"
        }
      }

      "must return Internal Server Error when the connector returns Internal server error" in {
        when(mockConnector.deleteRcasp(any())(any()))
          .thenReturn(
            EitherT.leftT[Future, ApiError](models.InternalServerError)
          )

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(DELETE, routes.RcaspController.deleteRcasp(exampleCarfId).url)
            .withJsonBody(deleteRequest)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "must return Internal Server Error when the connector returns Json validation error" in {
        when(mockConnector.deleteRcasp(any())(any()))
          .thenReturn(
            EitherT.leftT[Future, ApiError](JsonValidationError)
          )

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(DELETE, routes.RcaspController.deleteRcasp(exampleCarfId).url)
            .withJsonBody(deleteRequest)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "must return Bad Request when the request body is not valid JSON" in {
        when(mockConnector.deleteRcasp(any())(any()))
          .thenReturn(
            EitherT.leftT[Future, ApiError](JsonValidationError)
          )

        val application = applicationBuilder()
          .overrides(
            bind[RcaspConnector].toInstance(mockConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(DELETE, routes.RcaspController.deleteRcasp(exampleCarfId).url)
            .withJsonBody(Json.toJson("invalid request"))

          val result = route(application, request).value

          status(result)          mustBe BAD_REQUEST
          contentAsString(result) mustBe "DeleteRcaspRequest is invalid"
        }
      }
    }
  }
}
