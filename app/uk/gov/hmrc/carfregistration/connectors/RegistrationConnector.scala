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
import uk.gov.hmrc.carfregistration.models.ErrorError
import uk.gov.hmrc.carfregistration.models.requests.{RegisterIndividualWithIDRequest, RegisterIndividualWithIdFrontendRequest}
import uk.gov.hmrc.carfregistration.models.responses.RegisterIndividualWithIDResponse
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class RegistrationConnector @Inject() (val config: AppConfig, val http: HttpClientV2)(implicit
    ec: ExecutionContext
) extends Logging {

  private val backendBaseUrl = config.registerWithIdBaseUrl

  def individualWithNino(
      request: RegisterIndividualWithIDRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorError, RegisterIndividualWithIDResponse] =
    registerIndividualWithId(request, url"$backendBaseUrl")

  private def registerIndividualWithId(
      request: RegisterIndividualWithIDRequest,
      endpoint: URL
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorError, RegisterIndividualWithIDResponse] =
    EitherT {
      http
        .post(endpoint)
        .withBody(Json.toJson(request))
        .execute[HttpResponse]
        .map {
          case response if response.status == OK        =>
            Try(response.json.as[RegisterIndividualWithIDResponse]) match {
              case Success(data)      => Right(data)
              case Failure(exception) =>
                logger.warn(
                  s"Error parsing response as RegisterIndividualWithIdResponse with endpoint: ${endpoint.toURI}"
                )
                Left(ErrorError(500, "Could not parse response as a RegisterIndividualWithIDResponse"))
            }
          case response if response.status == NOT_FOUND =>
            logger.warn(
              s"No match could be found for this user: status code: ${response.status}, from endpoint: ${endpoint.toURI}"
            )
            Left(ErrorError(404, "Could not match user to a business partner record"))
          case response                                 =>
            logger.warn(s"Unexpected response: status code: ${response.status}, from endpoint: ${endpoint.toURI}")
            Left(ErrorError(500, "Unexpected errorerror"))
        }
    }

}
