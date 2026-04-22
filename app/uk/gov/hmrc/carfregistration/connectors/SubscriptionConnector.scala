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

package uk.gov.hmrc.carfregistration.connectors

import cats.data.EitherT
import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.carfregistration.config.AppConfig
import uk.gov.hmrc.carfregistration.models
import uk.gov.hmrc.carfregistration.models.requests.SubscriptionRequest
import uk.gov.hmrc.carfregistration.models.responses.SubscriptionDisplayResponse
import uk.gov.hmrc.carfregistration.models.{JsonValidationError, *}
import uk.gov.hmrc.carfregistration.types.ResultT
import uk.gov.hmrc.carfregistration.utils.ErrorDetailsHandler
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.net.URL
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubscriptionConnector @Inject() (
    config: AppConfig,
    http: HttpClientV2
)(implicit ec: ExecutionContext)
    extends Logging {

  private val createSubscriptionBackendBaseUrl  = config.createSubscriptionBaseUrl
  private val displaySubscriptionBackendBaseUrl = config.displaySubscriptionBaseUrl

  def sendSubscriptionInformation(
      subscription: SubscriptionRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, HttpResponse] =
    createSubscription(subscription, url"$createSubscriptionBackendBaseUrl")

  def displaySubscriptionInformation(
      carfReference: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, SubscriptionDisplayResponse] =
    displaySubscription(url"$displaySubscriptionBackendBaseUrl/$carfReference")

  private def createSubscription(request: SubscriptionRequest, endpoint: URL)(implicit
      hc: HeaderCarrier
  ): ResultT[HttpResponse] = {
    logger.info(s"Calling endpoint: ${endpoint.toString}")
    EitherT {
      http
        .post(endpoint)
        .withBody(Json.toJson(request))
        .setHeader(additionalHeaders(config, "create-subscription"): _*)
        .execute[HttpResponse]
        .map { httpResponse =>
          httpResponse.status match {
            case OK                                                        =>
              Right(httpResponse)
            case UNPROCESSABLE_ENTITY                                      =>
              logDownStreamError(httpResponse.status, httpResponse.body)
              isAlreadyRegistered(httpResponse.body) match {
                case Some(json) =>
                  logger.warn(s"Already registered. ${httpResponse.status} response status")
                  Right(HttpResponse(UNPROCESSABLE_ENTITY, Json.stringify(json)))
                case None       =>
                  Right(httpResponse)
              }
            case BAD_REQUEST | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
              Left(ErrorDetailsHandler.errorParse(httpResponse, endpoint))
            case status                                                    =>
              logger.warn(s"Unexpected response: status code: $status, from endpoint: ${endpoint.toURI}")
              logDownStreamError(status, httpResponse.body)
              Left(InternalServerError)
          }
        }
    }
  }

  private def displaySubscription(endpoint: URL)(implicit
      hc: HeaderCarrier
  ): ResultT[SubscriptionDisplayResponse] = {
    logger.info(s"Calling endpoint: ${endpoint.toString}")
    EitherT {
      http
        .get(endpoint)
        .setHeader(additionalHeaders(config, "display-subscription"): _*)
        .execute[HttpResponse]
        .map { httpResponse =>
          httpResponse.status match {
            case OK                                                                               =>
              Try(httpResponse.json.as[SubscriptionDisplayResponse]) match {
                case Success(data)      =>
                  logger.info(s"Display subscription success! Response: ${Json.prettyPrint(Json.toJson(data))}")
                  Right(data)
                case Failure(exception) =>
                  logger.warn(
                    s"Error parsing response as SubscriptionDisplayResponse. Endpoint: <${endpoint.toURI}> Exception: <${exception.getMessage}>"
                  )
                  Left(JsonValidationError)
              }
            case BAD_REQUEST | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
              Left(ErrorDetailsHandler.errorParse(httpResponse, endpoint))
            case NOT_FOUND                                                                        =>
              logger.warn(
                s"No match could be found for this user: status code: ${httpResponse.status}, from endpoint: ${endpoint.toURI}"
              )
              Left(NotFoundError)
            case _                                                                                =>
              logger.warn(s"Unexpected response: status code: ${httpResponse.status}, from endpoint: ${endpoint.toURI}")
              Left(InternalServerError)
          }
        }
    }
  }

  private def logDownStreamError(status: Int, body: String): Unit = {
    val error = Try(Json.parse(body).validate[ErrorDetail])
    error match {
      case Success(JsSuccess(value, _)) =>
        logger.warn(
          s"Error with submission: ${value.errorDetail.sourceFaultDetail.map(_.detail.mkString)}"
        )
      case _                            =>
        logger.warn(s"Error with submission: $status: response is not valid JSON")
    }
  }

  private def isAlreadyRegistered(responseBody: String): Option[JsValue] =
    Try(Json.parse(responseBody)).toOption
      .flatMap(_.asOpt[JsObject])
      .flatMap { json =>
        (json \\ "errorCode").headOption
          .flatMap(_.asOpt[String])
          .filter(_ == "007")
          .map(_ => json + ("status" -> Json.toJson("already_registered")))
      }

}
