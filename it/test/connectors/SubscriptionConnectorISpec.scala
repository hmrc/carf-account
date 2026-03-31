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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathMatching}
import itutil.ApplicationWithWiremock
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.carfregistration.connectors.SubscriptionConnector
import uk.gov.hmrc.carfregistration.models.requests.{Contact, SubscriptionRequest}
import uk.gov.hmrc.carfregistration.models.{ApiError, Individual, InternalServerError}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

class SubscriptionConnectorISpec
    extends ApplicationWithWiremock
    with ScalaFutures
    with IntegrationPatience
    with Matchers {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: SubscriptionConnector = app.injector.instanceOf[SubscriptionConnector]

  val exampleIndividual = Individual("John", "Doe")
  val exampleContact    = Contact("test@example.com", Some(exampleIndividual), None, Some("1234567890"), None)

  val testSubscriptionRequest = SubscriptionRequest(
    gbUser = true,
    idNumber = "XM000123456799",
    idType = "SAFE",
    primaryContact = exampleContact,
    secondaryContact = None,
    tradingName = None
  )

  val testSubscriptionResponseJson: String =
    """{
      |  "success": {
      |    "crfaReference": "XMFA1234567890",
      |    "processingDate": "2001-12-17T09:30:47Z"
      |  }
    |}""".stripMargin

  val testApiErrorDetailResponseJson: String =
    """{
      |  "errorDetail": {
      |    "errorCode": "400",
      |    "errorMessage": "Test Error Message",
      |    "source": "Test",
      |    "sourceFaultDetail": {
      |      "detail": [
      |        "Test Error Detail"
      |      ]
      |    },
      |    "timestamp": "2020-09-25T21:54:12.015Z",
      |    "correlationId": "1ae81b45-41b4-4642-ae1c-db1126900001"
      |  }
      |}""".stripMargin

  "sendSubscriptionInformation" should {

    "successfully retrieve the API response for a 200 OK" in {
      stubFor(
        post(urlPathMatching("/dac6/createsubscriptiondata/carf/v1"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(testSubscriptionResponseJson)
          )
      )

      val result: Either[ApiError, HttpResponse] =
        connector.sendSubscriptionInformation(testSubscriptionRequest).value.futureValue
      result.map(_.body) mustBe Right(testSubscriptionResponseJson)
    }

    "return Right with UNPROCESSABLE_ENTITY and the json body with already_registered when error code is 007" in {
      val body = """{"errorDetail":{"errorCode":"007","errorMessage":"Already registered"}}"""
      stubFor(
        post(urlPathMatching("/dac6/createsubscriptiondata/carf/v1"))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(body)
          )
      )

      val result: Either[ApiError, HttpResponse] =
        connector.sendSubscriptionInformation(testSubscriptionRequest).value.futureValue

      result.isRight                                              mustBe true
      result.map(_.status)                                        mustBe Right(UNPROCESSABLE_ENTITY)
      result.map(r => (Json.parse(r.body) \ "status").as[String]) mustBe Right("already_registered")
    }

    "return Right with UNPROCESSABLE_ENTITY and original body when error code is not 007" in {
      val body = """{"errorDetail":{"errorCode":"015","errorMessage":"Invalid ID type"}}"""
      stubFor(
        post(urlPathMatching("/dac6/createsubscriptiondata/carf/v1"))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(body)
          )
      )

      val result: Either[ApiError, HttpResponse] =
        connector.sendSubscriptionInformation(testSubscriptionRequest).value.futureValue

      result.isRight       mustBe true
      result.map(_.status) mustBe Right(UNPROCESSABLE_ENTITY)
      result.map(_.body)   mustBe Right(body)
    }

    "return Left InternalServerError if NOT_FOUND status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/createsubscriptiondata/carf/v1"))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      val result = connector.sendSubscriptionInformation(testSubscriptionRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if BAD_REQUEST status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/createsubscriptiondata/carf/v1"))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.sendSubscriptionInformation(testSubscriptionRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if SERVICE_UNAVAILABLE status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/createsubscriptiondata/carf/v1"))
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.sendSubscriptionInformation(testSubscriptionRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if FORBIDDEN status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/createsubscriptiondata/carf/v1"))
          .willReturn(aResponse().withStatus(FORBIDDEN))
      )

      val result = connector.sendSubscriptionInformation(testSubscriptionRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if 500 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/createsubscriptiondata/carf/v1"))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.sendSubscriptionInformation(testSubscriptionRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if unexpected status code is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/createsubscriptiondata/carf/v1"))
          .willReturn(aResponse().withStatus(502))
      )

      val result = connector.sendSubscriptionInformation(testSubscriptionRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }
  }
}
