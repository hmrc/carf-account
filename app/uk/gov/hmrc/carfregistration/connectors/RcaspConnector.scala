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

import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.carfregistration.config.AppConfig
import uk.gov.hmrc.carfregistration.connectors.additionalHeaders
import uk.gov.hmrc.carfregistration.models.requests.CreateRcaspRequest
import uk.gov.hmrc.carfregistration.models.responses.{SubmitRcaspResponse, ViewRcaspResponse}
import uk.gov.hmrc.carfregistration.models.{JsonValidationError, *}
import uk.gov.hmrc.carfregistration.types.ResultT
import uk.gov.hmrc.carfregistration.utils.ErrorDetailsHandler
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.net.URL
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class RcaspConnector @Inject() (
    config: AppConfig,
    http: HttpClientV2
)(implicit ec: ExecutionContext)
    extends Logging {

  private val viewRcaspBackendBaseUrl = config.viewRcaspBaseUrl

  def viewRcasps(
      carfId: String,
      rcaspId: String
  )(implicit hc: HeaderCarrier): ResultT[ViewRcaspResponse] = {
    val url = url"$viewRcaspBackendBaseUrl/$carfId/$rcaspId"
    logger.info(s"Calling endpoint: ${url.toString}")

    val requestBuilder = http
      .get(url)
      .setHeader(additionalHeaders(config, "view-rcasp"): _*)

    sendRequest(url, requestBuilder) { httpResponse =>
      Try(httpResponse.json.as[ViewRcaspResponse]) match {
        case Success(data)      =>
          logger.info(s"View RCASP success! Response: ${Json.prettyPrint(Json.toJson(data))}")
          Right(data)
        case Failure(exception) =>
          logger.warn(
            s"Error parsing response as ViewRcaspResponse. Endpoint: <${url.toURI}> Exception: <${exception.getMessage}>"
          )
          Left(JsonValidationError)
      }
    }
  }

  def submitRcasp(request: CreateRcaspRequest)(implicit hc: HeaderCarrier): ResultT[SubmitRcaspResponse] = {
    val url = url"${config.submitRcaspBaseUrl}"
    logger.info(s"Calling endpoint: ${url.toString}")

    val requestBuilder =
      http
        .post(url)
        .withBody(Json.toJson(request))
        .setHeader(additionalHeaders(config, "submit-rcasp"): _*)

    sendRequest(url, requestBuilder) { httpResponse =>
      Try(httpResponse.json.as[SubmitRcaspResponse]) match {
        case Success(data)      =>
          logger.debug(s"RCASP submission successful ! Response: ${Json.prettyPrint(Json.toJson(data))}")
          Right(data)
        case Failure(exception) =>
          logger.warn(
            s"Error parsing response as ViewRcaspResponse. Endpoint: <${url.toURI}> Exception: <${exception.getMessage}>"
          )
          Left(JsonValidationError)
      }
    }
  }

  private def sendRequest[T](url: URL, requestBuilder: RequestBuilder)(
      successfulResult: HttpResponse => Either[ApiError, T]
  ): ResultT[T] = {
    logger.info(s"Calling endpoint: ${url.toString}")

    ResultT.fromFuture(
      requestBuilder
        .execute[HttpResponse]
        .map { httpResponse =>
          httpResponse.status match {
            case OK => successfulResult(httpResponse)
            case BAD_REQUEST | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE |
                FORBIDDEN | METHOD_NOT_ALLOWED =>
              Left(ErrorDetailsHandler.errorParse(httpResponse, url))
            case _  =>
              logger.warn(s"Unexpected response: status code: ${httpResponse.status}, from endpoint: ${url.toURI}")
              Left(InternalServerError)
          }
        }
    )
  }
}
