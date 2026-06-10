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
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.carfregistration.models.responses.*
import uk.gov.hmrc.carfregistration.connectors.RcaspConnector
import uk.gov.hmrc.carfregistration.controllers.RcaspController
import uk.gov.hmrc.carfregistration.models.responses.{OrganisationRcaspDetails, RcaspAddress, RcaspContact, RcaspResponseCommon, RcaspResponseDetails, TinDetails, ViewRcasp, ViewRcaspResponse}
import uk.gov.hmrc.carfregistration.models.{ApiError, JsonValidationError}

import scala.concurrent.Future

class RcaspControllerSpec extends SpecBase {

  val mockConnector: RcaspConnector   = mock[RcaspConnector]
  val testController: RcaspController = new RcaspController(cc, fakeAuthAction, mockConnector)

  val exampleContact        =
    RcaspContact(ContactName = "Prof Sada", EmailAddress = "test@example.com", PhoneNumber = Some("07123412345"))
  val exampleCarfId         = "XCCAR0024000102"
  val exampleRcaspId        = "none"
  val exampleResponseCommon = RcaspResponseCommon(
    OriginatingSystem = "CADX",
    TransmittingSystem = "EIS",
    RequestType = "VIEW",
    Regime = "CARF",
    ResponseParameters = None
  )

  val testViewRcaspResponse = ViewRcaspResponse(
    ViewRCASP = ViewRcasp(
      ResponseCommon = exampleResponseCommon,
      ResponseDetails = RcaspResponseDetails(
        RCASPList = List(
          OrganisationRcaspDetails(
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

  val testViewRcaspResponseJson: String = Json.toJson(testViewRcaspResponse).toString

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  "RcaspController" - {
    "viewRcasp" - {
      "must return success response when the connector successfully sends GET request" in {
        when(mockConnector.viewRcaspInformation(any(), any())(any()))
          .thenReturn(
            EitherT.rightT[Future, ApiError](
              testViewRcaspResponse
            )
          )

        val result = testController.viewRcasp(exampleCarfId, exampleRcaspId)(fakeRequest)

        status(result)          mustBe OK
        contentAsString(result) mustBe testViewRcaspResponseJson
      }

      "must return internal server error when connector returns" - {

        "InternalServerError" in {
          when(mockConnector.viewRcaspInformation(any(), any())(any()))
            .thenReturn(EitherT.leftT[Future, ViewRcaspResponse](InternalServerError))

          val result = testController.viewRcasp(exampleCarfId, exampleRcaspId)(fakeRequest)

          status(result)        mustBe INTERNAL_SERVER_ERROR
          contentAsString(result) must include("Unexpected error")
        }

        "JsonValidationError" in {
          when(mockConnector.viewRcaspInformation(any(), any())(any()))
            .thenReturn(EitherT.leftT[Future, ViewRcaspResponse](JsonValidationError))

          val result = testController.viewRcasp(exampleCarfId, exampleRcaspId)(fakeRequest)

          status(result)        mustBe INTERNAL_SERVER_ERROR
          contentAsString(result) must include("Unexpected error")
        }

      }
    }
  }
}
