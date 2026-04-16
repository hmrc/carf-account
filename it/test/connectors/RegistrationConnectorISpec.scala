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

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import itutil.{ApplicationWithWiremock, ConnectorSpecHelper}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.carfregistration.connectors.RegistrationConnector
import uk.gov.hmrc.carfregistration.models.requests.*
import uk.gov.hmrc.carfregistration.models.responses.*
import uk.gov.hmrc.carfregistration.models.*
import uk.gov.hmrc.http.HeaderCarrier

class RegistrationConnectorISpec extends ApplicationWithWiremock with ScalaFutures with IntegrationPatience with ConnectorSpecHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: RegistrationConnector = app.injector.instanceOf[RegistrationConnector]

  val testApiResponseBody = RegWithIdIndApiResponse(
    responseCommon = ResponseCommon(status = "200"),
    responseDetail = ResponseDetail(
      SAFEID = "Test-SafeId",
      address = testAddressResponse,
      individual = Some(IndividualResponse(firstName = "Katie", lastName = "Long", middleName = Some("Bjorn"))),
      organisation = None
    )
  )

  val testApiResponseJson: String = """{
                              |  "responseCommon": {
                              |    "processingDate": "2025-11-03",
                              |    "returnParameters": [
                              |      {
                              |        "paramName": "Test-ParamName",
                              |        "paramValue": "Test-ParamValue"
                              |      }
                              |    ],
                              |    "status": "200",
                              |    "statusText": "Test-StatusText"
                              |  },
                              |  "responseDetail": {
                              |    "ARN": "Test-ARN",
                              |    "SAFEID": "Test-SafeId",
                              |    "address": {
                              |      "addressLine1": "64",
                              |      "addressLine2": "Zoo",
                              |      "addressLine3": "Lane",
                              |      "addressLine4": "Sixty Four",
                              |      "postalCode": "G66 2AZ",
                              |      "countryCode": "GB"
                              |    },
                              |    "contactDetails": {
                              |      "emailAddress": "test@example.com",
                              |      "faxNumber": "Test-FaxNo",
                              |      "mobileNumber": "Test-MobileNo",
                              |      "phoneNumber": "TestPhoneNo"
                              |    },
                              |    "individual": {
                              |      "dateOfBirth": "test-dob",
                              |      "firstName": "Katie",
                              |      "lastName": "Long",
                              |      "middleName": "Bjorn"
                              |    },
                              |    "isAnASAgent": false,
                              |    "isAnAgent": false,
                              |    "isAnIndividual": true,
                              |    "isEditable": false
                              |  }
                              |}""".stripMargin

  val testApiErrorDetailResponseJson: String = """{
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

  val testRegisterWithIdRequestInd = RegWithIdIndApiRequestDetails(
    requestCommon =
      RequestCommon(acknowledgementReference = "test-Ref", receiptDate = "test-Date", regime = "test-Regime"),
    requestDetail = RequestDetailIndividual(
      requiresNameMatch = true,
      IDNumber = "test-IDNumber",
      IDType = "test-IDType",
      individual = IndividualDetailsWithNino(dateOfBirth = "test-DOB", firstName = "Professor", lastName = "Rowan"),
      isAnAgent = false
    )
  )

  val testRequest = RegWithIdIndApiRequest(registerWithIDRequest = testRegisterWithIdRequestInd)

  private def testAddressResponse = AddressResponse(
    addressLine1 = "64",
    addressLine2 = Some("Zoo"),
    addressLine3 = Some("Lane"),
    addressLine4 = Some("Sixty Four"),
    postalCode = Some("G66 2AZ"),
    countryCode = "GB"
  )

  private val testAddressFrontend = AddressDetailsFrontend(
    addressLine1 = "123 Test Street",
    addressLine2 = Some("Flat 1"),
    addressLine3 = None,
    townOrCity = "France",
    postalCode = Some("75008"),
    countryCode = "FR"
  )

  private val testContactDetails = ContactDetailsFrontend(
    emailAddress = "john.doe@example.com",
    phoneNumber = Some("07123456789")
  )

  val testUserEnteredOrgWithUtrFrontendRequest = RegWithIdUserEntryOrgFrontendRequest(
    requiresNameMatch = false,
    IDType = "UTR",
    IDNumber = "1234567890",
    organisationName = "Test Limited",
    organisationType = "0001"
  )

  val testRegisterWithIdRequestOrg = RegWithIdOrgApiRequestDetails(
    requestCommon = RequestCommon(
      acknowledgementReference = "test-Ref",
      receiptDate = "test-Date",
      regime = "test-Regime"
    ),
    requestDetail = RequestDetailOrgUserEntry(testUserEnteredOrgWithUtrFrontendRequest)
  )

  val testOrganisationApiRequest = RegWithIdOrgApiRequest(registerWithIDRequest = testRegisterWithIdRequestOrg)

  val testOrganisationApiResponseJson: String =
    """{
    "responseCommon": {
      "status": "OK",
      "processingDate": "2025-11-03"
    },
    "responseDetail": {
      "SAFEID": "XE0000123456789",
      "organisation": {
        "organisationName": "Test Limited",
        "code": "0001"
      },
      "address": {
        "addressLine1": "123 Test Street",
        "addressLine2": "Islington",
        "countryCode": "GB"
      }
    }
  }"""

  val testOrganisationApiResponseBody = RegWithIdOrgApiResponse(
    responseCommon = ResponseCommon(status = "OK"),
    responseDetail = ResponseDetail(
      SAFEID = "XE0000123456789",
      organisation = Some(OrganisationResponse(organisationName = "Test Limited", code = Some("0001"))),
      individual = None,
      address = AddressResponse(
        addressLine1 = "123 Test Street",
        addressLine2 = Some("Islington"),
        addressLine3 = None,
        addressLine4 = None,
        countryCode = "GB",
        postalCode = None
      )
    )
  )

  val testIndWithoutIdRequest = RegWithoutIdApiRequest(registerWithoutIDRequest =
    RegWithoutIdApiRequestDetails(
      requestCommon = RequestCommon(
        acknowledgementReference = "test-Ref",
        receiptDate = "test-Date",
        regime = "CARF"
      ),
      requestDetail = RequestDetailIndividualWithoutId(
        individual = IndividualDetailsWithoutId(
          firstName = "John",
          lastName = "Doe",
          dateOfBirth = "1990-01-01"
        ),
        address = testAddressFrontend,
        contactDetails = testContactDetails
      )
    )
  )

  val testOrgWithoutIdRequest = RegWithoutIdApiRequest(registerWithoutIDRequest =
    RegWithoutIdApiRequestDetails(
      requestCommon = RequestCommon(
        acknowledgementReference = "test-Ref",
        receiptDate = "test-Date",
        regime = "CARF"
      ),
      requestDetail = RequestDetailOrganisationWithoutId(
        organisation = OrganisationDetailsWithoutId(
          organisationName = "ABC LTD"
        ),
        address = testAddressFrontend,
        contactDetails = testContactDetails
      )
    )
  )

  val testWithoutIdResponseJson: String =
    """
      |{
      |  "registerWithoutIDResponse": {
      |     "responseCommon": {
      |       "status": "OK"
      |     },
      |     "responseDetail": {
      |       "SAFEID": "SAFE123456"
      |     }
      |  }
      |}
      |""".stripMargin

  val testWithoutIdResponseBody =
    RegWithoutIdApiResponseDetails(
      responseCommon = ResponseCommon(status = "OK"),
      responseDetail = RegWithoutIdApiResponseDetail(
        SAFEID = "SAFE123456"
      )
    )

  "individualWithId" should {
    "successfully retrieve the api response and ensure required headers are present in request" in {

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching("/dac6/dprs0102/v1"))
      )

      stubFor(
        mappingBuilder
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(testApiResponseJson)
          )
      )
      val result = connector.individualWithId(testRequest).value.futureValue
      result mustBe Right(testApiResponseBody)
    }

    "return a Json validation error if unexpected response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalid response").toString)
          )
      )
      val result = connector.individualWithId(testRequest).value.futureValue
      result mustBe Left(JsonValidationError)
    }

    "return an error response if 400 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(testApiErrorDetailResponseJson)
          )
      )
      val result = connector.individualWithId(testRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return a not found error if 404 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(Json.toJson("test_body").toString)
          )
      )
      val result = connector.individualWithId(testRequest).value.futureValue
      result mustBe Left(NotFoundError)
    }

    "return an error response if 422 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(testApiErrorDetailResponseJson)
          )
      )
      val result = connector.individualWithId(testRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return an error response if 500 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(testApiErrorDetailResponseJson)
          )
      )
      val result = connector.individualWithId(testRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return an error response if 503 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody(testApiErrorDetailResponseJson)
          )
      )
      val result = connector.individualWithId(testRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }
  }

  "registerWithoutId" should {

    val expectedIndWithoutIdRequestJson: String =
      """
      {
        "registerWithoutIDRequest": {
          "requestCommon": {
            "acknowledgementReference": "test-Ref",
            "receiptDate": "test-Date",
            "regime": "CARF"
          },
          "requestDetail": {
            "individual": {
              "firstName": "John",
              "lastName": "Doe",
              "dateOfBirth": "1990-01-01"
            },
            "address": {
              "addressLine1": "123 Test Street",
              "addressLine2": "Flat 1",
              "townOrCity": "France",
              "postalCode": "75008",
              "countryCode": "FR"
            },
            "contactDetails": {
              "emailAddress": "john.doe@example.com",
              "phoneNumber": "07123456789"
            },
            "IsAnAgent": false,
            "IsAGroup": false
          }
        }
      }
      """.stripMargin

    val expectedOrgWithoutIdRequestJson: String =
      """
          {
            "registerWithoutIDRequest": {
              "requestCommon": {
                "acknowledgementReference": "test-Ref",
                "receiptDate": "test-Date",
                "regime": "CARF"
              },
              "requestDetail": {
                "organisation": {
                  "organisationName": "ABC LTD"
                },
                "address": {
                  "addressLine1": "123 Test Street",
                  "addressLine2": "Flat 1",
                  "townOrCity": "France",
                  "postalCode": "75008",
                  "countryCode": "FR"
                },
                "contactDetails": {
                  "emailAddress": "john.doe@example.com",
                  "phoneNumber": "07123456789"
                },
                "IsAnAgent": false,
                "IsAGroup": false
              }
            }
          }
          """.stripMargin

    "successfully retrieve the api response for ind without id request and ensure required headers are present" in {

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching("/dac6/dprs0101/v1"))
      )

      stubFor(
        mappingBuilder
          .withRequestBody(equalToJson(expectedIndWithoutIdRequestJson, true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(testWithoutIdResponseJson)
          )
      )

      val result = connector.registerWithoutId(testIndWithoutIdRequest).value.futureValue
      result mustBe Right(
        RegWithoutIdApiResponse(registerWithoutIDResponse =
          RegWithoutIdApiResponseDetails(ResponseCommon("OK"), RegWithoutIdApiResponseDetail("SAFE123456"))
        )
      )
    }

    "successfully retrieve the api response for org without id request and ensure required headers are present" in {

      val mappingBuilder = addMatchHeaders(
        post(urlPathMatching("/dac6/dprs0101/v1"))
      )

      stubFor(
        mappingBuilder
          .withRequestBody(equalToJson(expectedOrgWithoutIdRequestJson, true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(testWithoutIdResponseJson)
          )
      )

      val result = connector.registerWithoutId(testOrgWithoutIdRequest).value.futureValue
      result mustBe Right(
        RegWithoutIdApiResponse(registerWithoutIDResponse =
          RegWithoutIdApiResponseDetails(ResponseCommon("OK"), RegWithoutIdApiResponseDetail("SAFE123456"))
        )
      )
    }

    "return JsonValidationError if invalid JSON returned" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0101/v1"))
          .willReturn(aResponse().withStatus(OK).withBody("invalid-json"))
      )

      val result = connector.registerWithoutId(testIndWithoutIdRequest).value.futureValue
      result mustBe Left(JsonValidationError)
    }

    "return InternalServerError if 400 returned with error detail body" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0101/v1"))
          .willReturn(aResponse()
            .withStatus(BAD_REQUEST)
            .withBody(testApiErrorDetailResponseJson)
          )
      )

      val result = connector.registerWithoutId(testIndWithoutIdRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return InternalServerError if 422 returned with error detail body" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0101/v1"))
          .willReturn(aResponse()
            .withStatus(UNPROCESSABLE_ENTITY)
            .withBody(testApiErrorDetailResponseJson)
          )
      )

      val result = connector.registerWithoutId(testIndWithoutIdRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return InternalServerError for 500 response" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0101/v1"))
          .willReturn(aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(testApiErrorDetailResponseJson)
          )
      )

      val result = connector.registerWithoutId(testIndWithoutIdRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return InternalServerError for 503 response" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0101/v1"))
          .willReturn(aResponse()
            .withStatus(SERVICE_UNAVAILABLE)
            .withBody(testApiErrorDetailResponseJson)
          )
      )

      val result = connector.registerWithoutId(testIndWithoutIdRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return InternalServerError for 403 response" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0101/v1"))
          .willReturn(aResponse()
            .withStatus(FORBIDDEN)
          )
      )

      val result = connector.registerWithoutId(testIndWithoutIdRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return InternalServerError for 405 response" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0101/v1"))
          .willReturn(aResponse()
            .withStatus(METHOD_NOT_ALLOWED)
          )
      )

      val result = connector.registerWithoutId(testIndWithoutIdRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return JsonValidationError if ErrorDetail cannot be parsed in response" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0101/v1"))
          .willReturn(aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = connector.registerWithoutId(testIndWithoutIdRequest).value.futureValue
      result mustBe Left(JsonValidationError)
    }
  }

  "organisationWithID" should {
    "successfully retrieve the api response" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(testOrganisationApiResponseJson)
          )
      )

      val result = connector.organisationWithID(testOrganisationApiRequest).value.futureValue

      result mustBe Right(testOrganisationApiResponseBody)
    }

    "return a Json validation error if unexpected response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalid org response").toString)
          )
      )

      val result = connector.organisationWithID(testOrganisationApiRequest).value.futureValue

      result mustBe Left(JsonValidationError)
    }

    "return an error response if 400 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(testApiErrorDetailResponseJson)
          )
      )
      val result = connector.individualWithId(testRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return a not found error if 404 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(Json.toJson("test_body").toString)
          )
      )
      val result = connector.individualWithId(testRequest).value.futureValue
      result mustBe Left(NotFoundError)
    }

    "return an error response if 422 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(testApiErrorDetailResponseJson)
          )
      )
      val result = connector.individualWithId(testRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return an error response if 500 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(testApiErrorDetailResponseJson)
          )
      )
      val result = connector.individualWithId(testRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }

    "return an error response if 503 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/dac6/dprs0102/v1"))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody(testApiErrorDetailResponseJson)
          )
      )
      val result = connector.individualWithId(testRequest).value.futureValue
      result mustBe Left(InternalServerError)
    }
  }
}
