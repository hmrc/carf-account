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

import com.github.tomakehurst.wiremock.client.WireMock.*
import itutil.{ApplicationWithWiremock, ConnectorSpecHelper}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.*
import uk.gov.hmrc.carfregistration.connectors.RcaspConnector
import uk.gov.hmrc.carfregistration.models.requests.createRcasp.RcaspRequest as CreateRcaspRequest
import uk.gov.hmrc.carfregistration.models.requests.updateRcasp.RcaspRequest as UpdateRcaspRequest
import uk.gov.hmrc.carfregistration.models.requests.deleteRcasp.RcaspRequest as DeleteRcaspRequest
import uk.gov.hmrc.carfregistration.models.requests.{createRcasp, deleteRcasp, updateRcasp, RcaspRequestCommon}
import uk.gov.hmrc.carfregistration.models.responses.*
import uk.gov.hmrc.carfregistration.models.*
import uk.gov.hmrc.http.HeaderCarrier

class RcaspConnectorISpec
    extends ApplicationWithWiremock
    with ScalaFutures
    with IntegrationPatience
    with Matchers
    with ConnectorSpecHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: RcaspConnector = app.injector.instanceOf[RcaspConnector]

  val exampleContact        =
    RcaspContactDetails(ContactName = "Prof Sada", EmailAddress = "test@example.com", PhoneNumber = Some("07123412345"))
  val exampleCarfId         = "XCCAR0024000102"
  val exampleRcaspId        = "none"
  val exampleResponseCommon = RcaspResponseCommon(
    OriginatingSystem = "MDTP",
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

  "viewRcasp" should {

    val testUrl = s"/dac6/viewrcaspdata/v1/$exampleCarfId/$exampleRcaspId"

    val testViewRcaspResponseJson: String =
      """{
        |  "ViewRCASP": {
        |    "ResponseCommon": {
        |      "OriginatingSystem": "MDTP",
        |      "TransmittingSystem": "EIS",
        |      "RequestType": "VIEW",
        |      "Regime": "CARF"
        |    },
        |    "ResponseDetails": {
        |      "RCASPList": [
        |        {
        |          "SubscriptionID": "XCCAR0024000102",
        |          "RCASPID": "none",
        |          "IsRCASPUser": true,
        |          "PartyType": "Organisation",
        |          "RCASPName": "Mesagoza",
        |          "TradingName": "Uva Academy",
        |          "TINDetails": [
        |            {
        |              "TINType": "UTR",
        |              "TIN": "68936493",
        |              "IssuedBy": "GB"
        |            }
        |          ],
        |          "AddressDetails": {
        |            "AddressLine1": "64",
        |            "AddressLine2": "Zoo",
        |            "AddressLine3": "Lane",
        |            "AddressLine4": "Sixty Four",
        |            "PostalCode": "G66 2AZ",
        |            "CountryCode": "GB"
        |          },
        |          "PrimaryContactDetails": {
        |            "ContactName": "Prof Sada",
        |            "EmailAddress": "test@example.com",
        |            "PhoneNumber": "07123412345"
        |          },
        |          "SecondaryContactDetails": {
        |            "ContactName": "Prof Turo",
        |            "EmailAddress": "test@example.com",
        |            "PhoneNumber": "07123412345"
        |          }
        |        }
        |      ]
        |    }
        |  }
        |}""".stripMargin

    "successfully retrieve the API response for a 200 OK" in {
      val mappingBuilder = addMatchHeaders(
        get(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder
          .willReturn(aResponse().withStatus(OK).withBody(testViewRcaspResponseJson))
      )

      val result = connector.viewRcasps(exampleCarfId, exampleRcaspId).value.futureValue
      result mustBe Right(testViewRcaspResponse)
    }

    "return Left JsonValidationError if json is incorrectly formatter" in {
      val mappingBuilder = addMatchHeaders(
        get(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder
          .willReturn(aResponse().withStatus(OK).withBody(""))
      )

      val result = connector.viewRcasps(exampleCarfId, exampleRcaspId).value.futureValue
      result mustBe Left(JsonValidationError)
    }

    "return Left NotFoundError if UNPROCESSABLE_ENTITY status response with detail 001 is returned from backend" in {
      val errorDetailResponseJson: String =
        """{
          |  "errorDetail": {
          |    "errorCode": "422",
          |    "errorMessage": "No matching records found for the request",
          |    "source": "Backend",
          |    "sourceFaultDetail": {
          |      "detail": [
          |        "001 - No matching records found for the request"
          |      ]
          |    },
          |    "timestamp": "2020-09-25T21:54:12.015Z",
          |    "correlationId": "1ae81b45-41b4-4642-ae1c-db1126900001"
          |  }
          |}""".stripMargin

      stubFor(
        get(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(UNPROCESSABLE_ENTITY).withBody(errorDetailResponseJson))
      )

      val result = connector.viewRcasps(exampleCarfId, exampleRcaspId).value.futureValue
      result mustBe Left(NotFoundError)
    }

    "return Left InternalServerError if some other UNPROCESSABLE_ENTITY status response is returned from backend" in {
      stubFor(
        get(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(UNPROCESSABLE_ENTITY).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.viewRcasps(exampleCarfId, exampleRcaspId).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if BAD_REQUEST status response is returned from backend" in {
      stubFor(
        get(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.viewRcasps(exampleCarfId, exampleRcaspId).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if SERVICE_UNAVAILABLE status response is returned from backend" in {
      stubFor(
        get(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.viewRcasps(exampleCarfId, exampleRcaspId).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if FORBIDDEN status response is returned from backend" in {
      stubFor(
        get(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(FORBIDDEN).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.viewRcasps(exampleCarfId, exampleRcaspId).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if 500 status response is returned from backend" in {
      stubFor(
        get(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.viewRcasps(exampleCarfId, exampleRcaspId).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if unexpected status code is returned from backend" in {
      stubFor(
        get(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(502))
      )

      val result = connector.viewRcasps(exampleCarfId, exampleRcaspId).value.futureValue
      result mustBe Left(InternalServerError)
    }
  }

  "createRcasp" should {
    val testUrl = "/dac6/SubmitRCASPData/v1"

    val createRcaspRequest: CreateRcaspRequest =
      CreateRcaspRequest(
        createRcasp.RcaspManagementRequest(
          RcaspRequestCommon(
            OriginatingSystem = "MDTP",
            TransmittingSystem = "EIS",
            RequestType = "CREATE",
            Regime = "CARF",
            RequestParameters = None
          ),
          createRcasp.IndividualRcaspDetails(
            SubscriptionID = "XCARF000000001",
            IsRCASPUser = true,
            PartyType = "Individual",
            FirstName = "Penny",
            LastName = "Cassiopeia",
            TINDetails = Some(
              List(
                TinDetails(
                  TINType = "UTR",
                  TIN = "6893649",
                  IssuedBy = "GB"
                )
              )
            ),
            AddressDetails = RcaspAddress(
              AddressLine1 = "2 High Street",
              AddressLine2 = Some("Birmingham"),
              AddressLine3 = Some("Nowhereshire"),
              AddressLine4 = Some("Down the road"),
              PostalCode = "B23 2AZ",
              CountryCode = "GB"
            ),
            PrimaryContactDetails = Some(
              RcaspContactDetails(
                ContactName = "Penny Cassiopeia",
                EmailAddress = "penny.cassiopeia@uva.edu.org",
                PhoneNumber = Some("07123412345")
              )
            )
          )
        )
      )

    val submitStubResponse =
      """
        |{
        |  "ResponseDetails": {
        |    "ReturnParameters": {
        |      "Key": "RCASPID",
        |      "Value": "RCASP12345"
        |    }
        |  }
        |}
        |""".stripMargin

    "successfully retrieve the API response for a 200 OK" in {

      val expectedResponse = SubmitRcaspResponse(
        SubmitResponseDetails(
          SubmitReturnParameters(
            "RCASPID",
            "RCASP12345"
          )
        )
      )

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(submitStubResponse)
          )
      )

      val result = connector.createRcasp(createRcaspRequest).value.futureValue
      result mustBe Right(expectedResponse)
    }

    "return Left JsonValidationError if json is incorrectly formatter" in {
      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder.willReturn(aResponse().withStatus(OK).withBody(""))
      )

      val result = connector.createRcasp(createRcaspRequest).value.futureValue
      result mustBe Left(JsonValidationError)
    }

    "return Left InternalServerError if BAD_REQUEST status response is returned from backend" in {

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder.willReturn(aResponse().withStatus(BAD_REQUEST).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.createRcasp(createRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if SERVICE_UNAVAILABLE status response is returned from backend" in {

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder.willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.createRcasp(createRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if FORBIDDEN status response is returned from backend" in {

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder
          .willReturn(
            aResponse()
              .withStatus(FORBIDDEN)
              .withBody(testApiErrorDetailResponseJson)
          )
      )

      val result = connector.createRcasp(createRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if UNPROCESSABLE_ENTITY status response is returned from backend" in {
      stubFor(
        post(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(UNPROCESSABLE_ENTITY).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.createRcasp(createRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if 500 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.createRcasp(createRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if unexpected status code is returned from backend" in {
      stubFor(
        get(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(502))
      )

      val result = connector.createRcasp(createRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }
  }

  "updateRcasp" should {
    val testUrl = "/dac6/SubmitRCASPData/v1"

    val updateRcaspRequest: UpdateRcaspRequest =
      UpdateRcaspRequest(
        updateRcasp.RcaspManagementRequest(
          RcaspRequestCommon(
            OriginatingSystem = "MDTP",
            TransmittingSystem = "EIS",
            RequestType = "UPDATE",
            Regime = "CARF",
            RequestParameters = None
          ),
          viewAndUpdateRcasp.IndividualRcaspDetails(
            RCASPID = "RCASP12345",
            SubscriptionID = "XCARF000000001",
            IsRCASPUser = true,
            PartyType = "Individual",
            FirstName = "Penny",
            LastName = "Cassiopeia",
            TINDetails = Some(
              List(
                TinDetails(
                  TINType = "UTR",
                  TIN = "6893649",
                  IssuedBy = "GB"
                )
              )
            ),
            AddressDetails = RcaspAddress(
              AddressLine1 = "2 High Street",
              AddressLine2 = Some("Birmingham"),
              AddressLine3 = Some("Nowhereshire"),
              AddressLine4 = Some("Down the road"),
              PostalCode = "B23 2AZ",
              CountryCode = "GB"
            ),
            PrimaryContactDetails = Some(
              RcaspContactDetails(
                ContactName = "Penny Cassiopeia",
                EmailAddress = "penny.cassiopeia@uva.edu.org",
                PhoneNumber = Some("07123412345")
              )
            )
          )
        )
      )

    val submitStubResponse =
      """
        |{
        |  "ResponseDetails": {
        |    "ReturnParameters": {
        |      "Key": "RCASPID",
        |      "Value": "RCASP12345"
        |    }
        |  }
        |}
        |""".stripMargin

    "successfully retrieve the API response for a 200 OK" in {

      val expectedResponse = SubmitRcaspResponse(
        SubmitResponseDetails(
          SubmitReturnParameters(
            "RCASPID",
            "RCASP12345"
          )
        )
      )

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(submitStubResponse)
          )
      )

      val result = connector.updateRcasp(updateRcaspRequest).value.futureValue
      result mustBe Right(expectedResponse)
    }

    "return Left JsonValidationError if json is incorrectly formatter" in {
      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder.willReturn(aResponse().withStatus(OK).withBody(""))
      )

      val result = connector.updateRcasp(updateRcaspRequest).value.futureValue
      result mustBe Left(JsonValidationError)
    }

    "return Left InternalServerError if BAD_REQUEST status response is returned from backend" in {

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder.willReturn(aResponse().withStatus(BAD_REQUEST).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.updateRcasp(updateRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if SERVICE_UNAVAILABLE status response is returned from backend" in {

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder.willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.updateRcasp(updateRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if FORBIDDEN status response is returned from backend" in {

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder
          .willReturn(
            aResponse()
              .withStatus(FORBIDDEN)
              .withBody(testApiErrorDetailResponseJson)
          )
      )

      val result = connector.updateRcasp(updateRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if UNPROCESSABLE_ENTITY status response is returned from backend" in {
      stubFor(
        post(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(UNPROCESSABLE_ENTITY).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.updateRcasp(updateRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if 500 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.updateRcasp(updateRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if unexpected status code is returned from backend" in {
      stubFor(
        get(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(502))
      )

      val result = connector.updateRcasp(updateRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }
  }

  "deleteRcasp" should {
    val testUrl = "/dac6/SubmitRCASPData/v1"

    val deleteRcaspRequest: DeleteRcaspRequest =
      DeleteRcaspRequest(
        deleteRcasp.RcaspManagementRequest(
          RcaspRequestCommon(
            OriginatingSystem = "MDTP",
            TransmittingSystem = "EIS",
            RequestType = "DELETE",
            Regime = "CARF",
            RequestParameters = None
          ),
          deleteRcasp.RcaspDetails(
            RCASPID = "RCASP12345",
            SubscriptionID = "XCARF000000001"
          )
        )
      )

    val submitStubResponse =
      """
        |{
        |  "ResponseDetails": {
        |    "ReturnParameters": {
        |      "Key": "RCASPID",
        |      "Value": "RCASP12345"
        |    }
        |  }
        |}
        |""".stripMargin

    "successfully retrieve the API response for a 200 OK" in {

      val expectedResponse = SubmitRcaspResponse(
        SubmitResponseDetails(
          SubmitReturnParameters(
            "RCASPID",
            "RCASP12345"
          )
        )
      )

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(submitStubResponse)
          )
      )

      val result = connector.deleteRcasp(deleteRcaspRequest).value.futureValue
      result mustBe Right(expectedResponse)
    }

    "return Left JsonValidationError if json is incorrectly formatter" in {
      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder.willReturn(aResponse().withStatus(OK).withBody(""))
      )

      val result = connector.deleteRcasp(deleteRcaspRequest).value.futureValue
      result mustBe Left(JsonValidationError)
    }

    "return Left InternalServerError if BAD_REQUEST status response is returned from backend" in {

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder.willReturn(aResponse().withStatus(BAD_REQUEST).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.deleteRcasp(deleteRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if SERVICE_UNAVAILABLE status response is returned from backend" in {

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder.willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.deleteRcasp(deleteRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if FORBIDDEN status response is returned from backend" in {

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching(testUrl))
      )

      stubFor(
        mappingBuilder
          .willReturn(
            aResponse()
              .withStatus(FORBIDDEN)
              .withBody(testApiErrorDetailResponseJson)
          )
      )

      val result = connector.deleteRcasp(deleteRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if UNPROCESSABLE_ENTITY status response is returned from backend" in {
      stubFor(
        post(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(UNPROCESSABLE_ENTITY).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.deleteRcasp(deleteRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if 500 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody(testApiErrorDetailResponseJson))
      )

      val result = connector.deleteRcasp(deleteRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return Left InternalServerError if unexpected status code is returned from backend" in {
      stubFor(
        get(urlPathMatching(testUrl))
          .willReturn(aResponse().withStatus(502))
      )

      val result = connector.deleteRcasp(deleteRcaspRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }
  }
}
