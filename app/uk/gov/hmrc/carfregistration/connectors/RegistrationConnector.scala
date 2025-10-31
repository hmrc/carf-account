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
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.carfregistration.config.AppConfig
import uk.gov.hmrc.carfregistration.models.requests.RegisterIndWithIdAPIRequest
import uk.gov.hmrc.carfregistration.models.responses.RegisterIndWithIdAPIResponse
import uk.gov.hmrc.carfregistration.models.{ApiError, InternalServerError, JsonValidationError, NotFoundError}
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

  def individualWithNino(
      request: RegisterIndWithIdAPIRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegisterIndWithIdAPIResponse] =
    registerIndividualWithId(request, url"$backendBaseUrl")

  private def registerIndividualWithId(
      request: RegisterIndWithIdAPIRequest,
      endpoint: URL
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegisterIndWithIdAPIResponse] =
    EitherT {
      http
        .post(endpoint)
        .withBody(Json.toJson(request))
        .execute[HttpResponse]
        .map {
          case response if response.status == OK        =>
            Try(response.json.as[RegisterIndWithIdAPIResponse]) match {
              case Success(data)      => Right(data)
              case Failure(exception) =>
                logger.warn(
                  s"Error parsing response as RegisterIndividualWithIdResponse with endpoint: ${endpoint.toURI}"
                )
                Left(JsonValidationError)
            }
          case response if response.status == NOT_FOUND =>
            logger.warn(
              s"No match could be found for this user: status code: ${response.status}, from endpoint: ${endpoint.toURI}"
            )
            Left(NotFoundError)
          case response                                 =>
            logger.warn(s"Unexpected response: status code: ${response.status}, from endpoint: ${endpoint.toURI}")
            Left(InternalServerError)
        }
    }

}
