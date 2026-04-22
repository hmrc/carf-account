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

package uk.gov.hmrc.carfregistration.connectors

import cats.data.EitherT
import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.carfregistration.config.AppConfig
import uk.gov.hmrc.carfregistration.models.requests.{RegWithIdIndApiRequest, RegWithIdOrgApiRequest, RegWithoutIdApiRequest}
import uk.gov.hmrc.carfregistration.models.responses.{RegWithIdApiResponse, RegWithoutIdApiResponse}
import uk.gov.hmrc.carfregistration.models.*
import uk.gov.hmrc.carfregistration.utils.ErrorDetailsHandler
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class RegistrationConnector @Inject() (val config: AppConfig, val http: HttpClientV2)(implicit
    ec: ExecutionContext
) extends Logging {

  private val backendBaseUrl = config.registerWithIdBaseUrl

  def individualWithId(
      request: RegWithIdIndApiRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegWithIdApiResponse] =
    registerIndividualWithId(request, url"$backendBaseUrl")

  def organisationWithID(request: RegWithIdOrgApiRequest)(implicit
      hc: HeaderCarrier
  ): EitherT[Future, ApiError, RegWithIdApiResponse] =
    registerOrganisationWithID(request, url"$backendBaseUrl")

  private def registerOrganisationWithID(request: RegWithIdOrgApiRequest, endpoint: URL)(implicit
      hc: HeaderCarrier
  ): EitherT[Future, ApiError, RegWithIdApiResponse] =
    EitherT {
      http
        .post(endpoint)
        .withBody(Json.toJson(request))
        .setHeader(additionalHeaders(config, "register-with-id"): _*)
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case OK                                                                               =>
              Try(response.json.as[RegWithIdApiResponse]) match {
                case Success(data)      =>
                  logger.debug(s"Register organisation with ID Success! Response: $data")
                  Right(data)
                case Failure(exception) =>
                  logger.warn(
                    s"Error parsing response as RegWithIdOrgApiResponse. Endpoint: <${endpoint.toURI}> Exception: <${exception.getMessage}>"
                  )
                  Left(JsonValidationError)
              }
            case BAD_REQUEST | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
              Left(ErrorDetailsHandler.errorParse(response, endpoint))
            case NOT_FOUND                                                                        =>
              logger.warn(
                s"No match could be found for this organisation: status code: ${response.status}, from endpoint: ${endpoint.toURI}"
              )
              Left(NotFoundError)
            case _                                                                                =>
              logger.warn(s"Unexpected response: status code: ${response.status}, from endpoint: ${endpoint.toURI}")
              Left(InternalServerError)
          }
        }
    }

  private def registerIndividualWithId(
      request: RegWithIdIndApiRequest,
      endpoint: URL
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegWithIdApiResponse] =
    EitherT {
      http
        .post(endpoint)
        .withBody(Json.toJson(request))
        .setHeader(additionalHeaders(config, "register-with-id"): _*)
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case OK                                                                               =>
              Try(response.json.as[RegWithIdApiResponse]) match {
                case Success(data)      =>
                  logger.debug(s"Register individual with ID Success! Response: $data")
                  Right(data)
                case Failure(exception) =>
                  logger.warn(
                    s"Error parsing response as RegWithIdApiResponse. Endpoint: <${endpoint.toURI}> Exception: <${exception.getMessage}>"
                  )
                  Left(JsonValidationError)
              }
            case BAD_REQUEST | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
              Left(ErrorDetailsHandler.errorParse(response, endpoint))
            case NOT_FOUND                                                                        =>
              logger.warn(
                s"No match could be found for this user: status code: ${response.status}, from endpoint: ${endpoint.toURI}"
              )
              Left(NotFoundError)
            case _                                                                                =>
              logger.warn(s"Unexpected response: status code: ${response.status}, from endpoint: ${endpoint.toURI}")
              Left(InternalServerError)
          }
        }
    }

  def registerWithoutId(
      request: RegWithoutIdApiRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegWithoutIdApiResponse] =
    EitherT {
      val endpoint: URL = url"${config.registerWithoutIdBaseUrl}"
      logger.info(s"[RegistrationConnector] Calling registerWithoutId API with endpoint: ${endpoint.toURI}")
      http
        .post(endpoint)
        .withBody(Json.toJson(request))
        .setHeader(additionalHeaders(config, "register-without-id"): _*)
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case OK                                                                               =>
              Try(response.json.as[RegWithoutIdApiResponse]) match {
                case Success(response)  =>
                  logger.debug(s"Register without ID Success! Response: $response")
                  Right(response)
                case Failure(exception) =>
                  logger.warn(
                    s"Error parsing response as RegWithoutIdApiResponse. Endpoint: <${endpoint.toURI}> Exception: <${exception.getMessage}>"
                  )
                  Left(JsonValidationError)
              }
            case BAD_REQUEST | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
              Left(ErrorDetailsHandler.errorParse(response, endpoint))
            case _                                                                                =>
              logger.warn(s"Unexpected response: status code: ${response.status}, from endpoint: ${endpoint.toURI}")
              Left(InternalServerError)
          }
        }
    }

}
