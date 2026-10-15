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

package uk.gov.hmrc.carfaccount.utils

import uk.gov.hmrc.http.HttpResponse
import LoggerUtil.*
import uk.gov.hmrc.carfaccount.models.{ApiError, ErrorDetail, InternalServerError, JsonValidationError, NotFoundError}

import java.net.URL
import scala.util.{Failure, Success, Try}

object ErrorDetailsHandler {

  def errorParse(response: HttpResponse, endpoint: URL): ApiError =
    logError(s"[RcaspConnector][errorParse] Status code: ${response.status} from endpoint: ${endpoint.toURI}")
    Try(response.json.as[ErrorDetail]) match {
      case Success(error)     =>
        logError(
          s"[RcaspConnector][errorParse] Error code: ${error.errorDetail.errorCode}. Error message: ${error.errorDetail.errorMessage}. Source fault detail: ${error.errorDetail.sourceFaultDetail}"
        )
        InternalServerError
      case Failure(exception) =>
        logError(
          s"[RcaspConnector][errorParse] Error parsing response as ErrorDetails. Exception: <${exception.getMessage}>"
        )
        JsonValidationError
    }

  def errorParseForViewRcasp422(response: HttpResponse, endpoint: URL): ApiError =
    Try(response.json.as[ErrorDetail]) match {
      case Success(error)
          if error.errorDetail.sourceFaultDetail.flatMap(_.detail.headOption).forall(_.trim.startsWith("001")) =>
        logInfo(
          "[RcaspConnector][errorParseForViewRcasp422] UnprocessableEntity from View RCASP with detail 001. No RCASPs found."
        )
        NotFoundError
      case Success(error)     =>
        logError(
          s"[RcaspConnector][errorParseForViewRcasp422] Error code: ${error.errorDetail.errorCode}. Error message: ${error.errorDetail.errorMessage}. Source fault detail: ${error.errorDetail.sourceFaultDetail}"
        )
        InternalServerError
      case Failure(exception) =>
        logError(
          s"[RcaspConnector][errorParseForViewRcasp422] Error parsing response as ErrorDetails. Exception: <${exception.getMessage}>"
        )
        JsonValidationError
    }
}
