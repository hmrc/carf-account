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

package uk.gov.hmrc.carfregistration.utils

import play.api.Logging
import uk.gov.hmrc.carfregistration.models.{ApiError, ErrorDetail, InternalServerError, JsonValidationError}
import uk.gov.hmrc.http.HttpResponse

import java.net.URL
import scala.util.{Failure, Success, Try}

object ErrorDetailsHandler extends Logging {

  def errorParse(response: HttpResponse, endpoint: URL): ApiError =
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
