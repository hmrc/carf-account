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

package uk.gov.hmrc.carfregistration.controllers

import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.carfregistration.controllers.actions.AuthAction
import uk.gov.hmrc.carfregistration.models.requests.RegisterIndividualWithIdRequest
import uk.gov.hmrc.carfregistration.services.RegistrationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class RegisterController @Inject() (
    cc: ControllerComponents,
    authorise: AuthAction,
    service: RegistrationService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def registerIndividualWithId(): Action[JsValue] = authorise(parse.json).async { implicit request =>
    request.body
      .validate[RegisterIndividualWithIdRequest]
      .fold(
        invalid = failure =>
          println("AAAAAAAA")
          println(failure)
          Future.successful(BadRequest(""))
        ,
        valid = sub =>
          println("BBBBBBBB")
          println(sub)
          Future.successful(Ok(Json.toJson(service.createRegisterIndividualResponse())))
      )
  }

}
