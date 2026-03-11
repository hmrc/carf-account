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
import uk.gov.hmrc.carfregistration.models.{JsonValidationError, MissingFieldsError, NotFoundError}
import uk.gov.hmrc.carfregistration.models.requests.{RegWithIdAutoMatchOrgFrontendRequest, RegWithIdUserEntryOrgFrontendRequest, RegWithNinoIndFrontendRequest, RegWithUtrIndFrontendRequest, RegWithoutIdIndFrontendRequest}
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
    withJsonBody[RegWithNinoIndFrontendRequest] { request =>
      logger.debug(s"registerIndividualWithNino \n-> $request")
      service.registerIndWithNino(request).flatMap {
        case Right(response)     => Future.successful(Ok(Json.toJson(response)))
        case Left(NotFoundError) =>
          Future.successful(NotFound("Could not find or create a business partner record for this user"))
        case Left(_)             => Future.successful(InternalServerError("Unexpected error"))
      }
    }
  }

  def registerIndividualWithUtr(): Action[JsValue] = authorise(parse.json).async { implicit request =>
    withJsonBody[RegWithUtrIndFrontendRequest] { request =>
      logger.debug(s"registerIndividualWithUtr request = \n-> $request")
      service.registerIndWithUtr(request).flatMap {
        case Right(response)     => Future.successful(Ok(Json.toJson(response)))
        case Left(NotFoundError) =>
          Future.successful(NotFound("Could not find or create a Sole Trader record for this user"))
        case Left(_)             => Future.successful(InternalServerError("Unexpected error"))
      }
    }
  }

  def registerUserEntryOrganisationWithId(): Action[JsValue] = authorise(parse.json).async { implicit request =>
    withJsonBody[RegWithIdUserEntryOrgFrontendRequest] { organisationRequest =>
      logger.debug(s"RegisterUserEnteredOrganisationWithIdFrontendRequest request = \n-> $organisationRequest")
      service.registerUserEntryOrgWithId(organisationRequest).flatMap {
        case Right(response)     => Future.successful(Ok(Json.toJson(response)))
        case Left(NotFoundError) =>
          Future.successful(NotFound("Could not find or create a business record for this organisation"))
        case Left(_)             =>
          Future.successful(InternalServerError("Unexpected error"))
      }
    }
  }

  def registerAutoMatchOrganisationWithId(): Action[JsValue] = authorise(parse.json).async { implicit request =>
    withJsonBody[RegWithIdAutoMatchOrgFrontendRequest] { organisationRequest =>
      logger.debug(s"RegisterAutoMatchOrganisationWithIdFrontendRequest request = \n-> $organisationRequest")
      service.registerAutoMatchOrgWithId(organisationRequest).flatMap {
        case Right(response)     => Future.successful(Ok(Json.toJson(response)))
        case Left(NotFoundError) =>
          Future.successful(NotFound("Could not find or create a business record for this organisation"))
        case Left(_)             =>
          Future.successful(InternalServerError("Unexpected error"))
      }
    }
  }

  def registerIndividualWithoutId(): Action[JsValue] = authorise(parse.json).async { implicit request =>

    lazy val unexpectedError = {
      logger.error("registerIndividualWithoutId - unexpected error")
      InternalServerError("Unexpected error")
    }

    withJsonBody[RegWithoutIdIndFrontendRequest] { req =>
      logger.debug(s"registerIndividualWithoutId request = \n-> $req")
      service.registerIndWithoutId(req).map {
        case Right(resp) => Ok(Json.toJson(resp))

        case Left(NotFoundError) =>
          logger.warn("registerIndividualWithoutId - NotFoundError")
          unexpectedError

        case Left(JsonValidationError) => unexpectedError
        case Left(MissingFieldsError)  => unexpectedError
        case Left(_)                   => unexpectedError
      }
    }
  }

}
