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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathMatching}
import itutil.ApplicationWithWiremock
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.carfregistration.connectors.RegistrationConnector
import uk.gov.hmrc.carfregistration.models.requests.{IndividualDetails, RegisterIndWithIdAPIRequest, RequestCommon, RequestDetailIndividual}
import uk.gov.hmrc.carfregistration.models.responses.*
import uk.gov.hmrc.carfregistration.models.{InternalServerError, JsonValidationError, NotFoundError}
import uk.gov.hmrc.http.HeaderCarrier

class RegistrationConnectorISpec
    extends ApplicationWithWiremock
    with ScalaFutures
    with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: RegistrationConnector = app.injector.instanceOf[RegistrationConnector]

  val testApiResponseBody = RegisterIndWithIdAPIResponse(
      responseCommon = ResponseCommon(status = "200"),
      responseDetail = ResponseDetail(
        SAFEID = "Test-SafeId",
        address = testAddressResponse,
        individual = IndividualResponse(firstName = "Katie", lastName = "Long", middleName = None)
      )
    )


  val testRequest = RegisterIndWithIdAPIRequest(
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


  private def testAddressResponse = AddressResponse(
    addressLine1 = "64",
    addressLine2 = Some("Zoo"),
    addressLine3 = Some("Lane"),
    addressLine4 = Some("Sixty Four"),
    postalCode = Some("G66 2AZ"),
    countryCode = "GB"
  )

  "individualWithNino" should {
    "successfully retrieve the api response" in {
      stubFor(
        post(urlPathMatching("/dac6/dct102b/v1"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(testApiResponseBody).toString)
          )
      )

      val result = connector.individualWithNino(testRequest).value.futureValue

      result mustBe Right(testApiResponseBody)
    }

    "return a Json validation error if unexpected response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dct102b/v1"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalid response").toString)
          )
      )

      val result = connector.individualWithNino(testRequest).value.futureValue

      result mustBe Left(JsonValidationError)
    }

    "return a not found error if 404 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dct102b/v1"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(Json.toJson("test_body").toString)
          )
      )

      val result = connector.individualWithNino(testRequest).value.futureValue

      result mustBe Left(NotFoundError)
    }

    "return an internal server error if 500 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dct102b/v1"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(Json.toJson("test_body").toString)
          )
      )

      val result = connector.individualWithNino(testRequest).value.futureValue

      result mustBe Left(InternalServerError)
    }
  }
}
