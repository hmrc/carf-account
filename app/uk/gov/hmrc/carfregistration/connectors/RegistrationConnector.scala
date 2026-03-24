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
import uk.gov.hmrc.carfregistration.models.requests.{RegWithIdIndApiRequest, RegWithIdOrgApiRequest, RegWithoutIdIndApiRequest, RegWithoutIdIndApiRequestWrapper}
import uk.gov.hmrc.carfregistration.models.responses.{RegWithIdIndApiResponse, RegWithIdOrgApiResponse, RegWithoutIdIndApiResponse, RegWithoutIdIndApiResponseWrapper}
import uk.gov.hmrc.carfregistration.models.{ApiError, ErrorDetail, ErrorDetails, InternalServerError, JsonValidationError, NotFoundError}
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
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegWithIdIndApiResponse] =
    registerIndividualWithId(request, url"$backendBaseUrl")

  def organisationWithID(request: RegWithIdOrgApiRequest)(implicit
      hc: HeaderCarrier
  ): EitherT[Future, ApiError, RegWithIdOrgApiResponse] =
    registerOrganisationWithID(request, url"$backendBaseUrl")

  def individualWithoutId(
      request: RegWithoutIdIndApiRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegWithoutIdIndApiResponse] =
    registerIndividualWithoutId(request, url"${config.registerWithoutIdBaseUrl}")

  private def registerOrganisationWithID(request: RegWithIdOrgApiRequest, endpoint: URL)(implicit
      hc: HeaderCarrier
  ): EitherT[Future, ApiError, RegWithIdOrgApiResponse] =
    EitherT {
      http
        .post(endpoint)
        .withBody(Json.toJson(request))
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case OK                                                                               =>
              Try(response.json.as[RegWithIdOrgApiResponse]) match {
                case Success(data)      => Right(data)
                case Failure(exception) =>
                  logger.warn(
                    s"Error parsing response as RegWithIdOrgApiResponse. Endpoint: <${endpoint.toURI}> Exception: <${exception.getMessage}>"
                  )
                  Left(JsonValidationError)
              }
            case BAD_REQUEST | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
              Left(errorParse(response, endpoint))
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
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegWithIdIndApiResponse] =
    EitherT {
      http
        .post(endpoint)
        .withBody(Json.toJson(request))
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case OK                                                                               =>
              Try(response.json.as[RegWithIdIndApiResponse]) match {
                case Success(data)      => Right(data)
                case Failure(exception) =>
                  logger.warn(
                    s"Error parsing response as RegWithIdIndApiResponse. Endpoint: <${endpoint.toURI}> Exception: <${exception.getMessage}>"
                  )
                  Left(JsonValidationError)
              }
            case BAD_REQUEST | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
              Left(errorParse(response, endpoint))
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

  private def registerIndividualWithoutId(
      request: RegWithoutIdIndApiRequest,
      endpoint: URL
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegWithoutIdIndApiResponse] =
    EitherT {

      val wrappedRequest = RegWithoutIdIndApiRequestWrapper(registerWithoutIDRequest = request)

      logger.debug(s"[RegistrationConnector] Calling individualWithoutId endpoint: ${endpoint.toURI}")

      http
        .post(endpoint)
        .withBody(Json.toJson(wrappedRequest))
        .execute[HttpResponse]
        .map { response =>

          def extractSourceFaultDetail(resp: HttpResponse): Option[String] =
            scala.util
              .Try(resp.json)
              .toOption
              .flatMap(js => (js \ "errorDetail" \ "sourceFaultDetail" \ "detail").asOpt[Seq[String]])
              .map(_.mkString(" | "))

          response.status match {

            case OK =>
              Try(response.json.as[RegWithoutIdIndApiResponseWrapper]) match {

                case Success(wrapper) =>
                  Right(wrapper.regWithoutIdIndApiResponse)

                case Failure(_) =>
                  Try(response.json.as[RegWithoutIdIndApiResponse]) match {

                    case Success(plainResponse) =>
                      Right(plainResponse)

                    case Failure(exception) =>
                      logger.error(
                        s"[RegistrationConnector] Failed to parse response for individualWithoutId from ${endpoint.toURI}. " +
                          s"Exception: ${exception.getMessage}"
                      )
                      Left(JsonValidationError)
                  }
              }

            case BAD_REQUEST | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
              val detail = extractSourceFaultDetail(response)

              detail match {
                case Some(d) =>
                  logger.warn(
                    s"[RegistrationConnector] Upstream error ${response.status} from ${endpoint.toURI}. " +
                      s"sourceFaultDetail.detail=[$d]"
                  )
                case None    =>
                  logger.warn(
                    s"[RegistrationConnector] Upstream error ${response.status} from ${endpoint.toURI}"
                  )
              }

              Left(InternalServerError)

            case FORBIDDEN | METHOD_NOT_ALLOWED =>
              logger.warn(
                s"[RegistrationConnector] HTTP ${response.status} returned from ${endpoint.toURI}"
              )
              Left(InternalServerError)

            case other =>
              logger.error(
                s"[RegistrationConnector] Unexpected response $other from ${endpoint.toURI}"
              )
              Left(InternalServerError)
          }
        }
    }

  private def errorParse(response: HttpResponse, endpoint: URL): ApiError =
    logger.warn(s"Status code: ${response.status} from endpoint: ${endpoint.toURI}")
    Try(response.json.as[ErrorDetail]) match {
      case Success(error)     =>
        logger.warn(
          s"Error code: ${error.errorDetail.errorCode}. Error message: ${error.errorDetail.errorMessage}. Source fault detail: ${error.errorDetail.sourceFaultDetail}"
        )
        InternalServerError
      case Failure(exception) =>
        logger.warn(
          s"Error parsing response as ErrorDetails. Exception: <${exception.getMessage}>"
        )
        JsonValidationError
    }
}
