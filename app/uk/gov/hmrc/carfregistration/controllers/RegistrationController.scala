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
import uk.gov.hmrc.carfregistration.models.NotFoundError
import uk.gov.hmrc.carfregistration.models.requests.{RegisterIndWithIdFrontendRequest, RegisterIndWithNinoFrontendRequest, RegisterIndWithUtrFrontendRequest, RegisterOrganisationWithIdFrontendRequest}
import uk.gov.hmrc.carfregistration.services.RegistrationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class RegistrationController @Inject() (
    cc: ControllerComponents,
    authorise: AuthAction,
    service: RegistrationService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def registerIndividualWithNino(): Action[JsValue] = authorise(parse.json).async { implicit request =>
    withJsonBody[RegisterIndWithNinoFrontendRequest] { request =>
      logger.debug(s"registerIndividualWithNino \n-> $request")
      service.registerIndividualWithNino(request).flatMap {
        case Right(response)     => Future.successful(Ok(Json.toJson(response)))
        case Left(NotFoundError) =>
          Future.successful(NotFound("Could not find or create a business partner record for this user"))
        case Left(_)             => Future.successful(InternalServerError("Unexpected error"))
      }
    }
  }

  def registerIndividualWithUtr(): Action[JsValue] = authorise(parse.json).async { implicit request =>
    withJsonBody[RegisterIndWithUtrFrontendRequest] { request =>
      logger.debug(s"registerIndividualWithUtr request = \n-> $request")
      service.registerIndividualWithUtr(request).flatMap {
        case Right(response)     => Future.successful(Ok(Json.toJson(response)))
        case Left(NotFoundError) =>
          Future.successful(NotFound("Could not find or create a Sole Trader record for this user"))
        case Left(_)             => Future.successful(InternalServerError("Unexpected error"))
      }
    }
  }

  def registerOrganisationWithId(): Action[JsValue] = authorise(parse.json).async { implicit request =>
    withJsonBody[RegisterOrganisationWithIdFrontendRequest] { organisationRequest =>
      logger.debug(s" registerOrganisationWithId request = \n-> $organisationRequest")
      service.registerOrganisationWithId(organisationRequest).flatMap {
        case Right(response)     => Future.successful(Ok(Json.toJson(response)))
        case Left(NotFoundError) =>
          Future.successful(NotFound("Could not find or create a business record for this organisation"))
        case Left(_)             =>
          Future.successful(InternalServerError("Unexpected error"))
      }
    }
  }

}
