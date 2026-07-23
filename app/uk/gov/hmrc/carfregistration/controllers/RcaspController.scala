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

package uk.gov.hmrc.carfregistration.controllers

import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.*
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.carfregistration.connectors.RcaspConnector
import uk.gov.hmrc.carfregistration.controllers.actions.AuthAction
import uk.gov.hmrc.carfregistration.models.NotFoundError
import uk.gov.hmrc.carfregistration.models.requests.createRcasp.RcaspRequest as CreateRcaspRequest
import uk.gov.hmrc.carfregistration.models.requests.deleteRcasp.RcaspRequest as DeleteRcaspRequest
import uk.gov.hmrc.carfregistration.models.requests.updateRcasp.RcaspRequest as UpdateRcaspRequest
import uk.gov.hmrc.carfregistration.models.responses.SubmitRcaspResponse
import uk.gov.hmrc.carfregistration.types.ResultT
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class RcaspController @Inject() (
    cc: ControllerComponents,
    authorise: AuthAction,
    rcaspConnector: RcaspConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def viewRcasp(carfId: String, rcaspId: String): Action[AnyContent] = authorise.async { implicit request =>
    rcaspConnector.viewRcasps(carfId, rcaspId).value.map {
      case Right(response)     => Ok(Json.toJson(response))
      case Left(NotFoundError) => NotFound("No RCASPs found")
      case Left(_)             => InternalServerError("Unexpected error")
    }
  }

  def createRcasp: Action[JsValue] = authorise(parse.json).async { implicit request =>
    processRcaspRequest("Create", request.body.validate[CreateRcaspRequest]) { request =>
      rcaspConnector.createRcasp(request)
    }
  }

  def updateRcasp: Action[JsValue] = authorise(parse.json).async { implicit request =>
    processRcaspRequest("Update", request.body.validate[UpdateRcaspRequest]) { request =>
      rcaspConnector.updateRcasp(request)
    }
  }

  def deleteRcasp: Action[JsValue] = authorise(parse.json).async { implicit request =>
    processRcaspRequest("Delete", request.body.validate[DeleteRcaspRequest]) { request =>
      rcaspConnector.deleteRcasp(request)
    }
  }

  private def processRcaspRequest[A](
      action: String,
      jsResult: JsResult[A]
  )(futureResult: A => ResultT[SubmitRcaspResponse]): Future[Result] =
    jsResult
      .fold(
        invalid = _ => Future.successful(BadRequest(s"${action}RcaspRequest is invalid")),
        valid = validRequest =>
          futureResult(validRequest).value.map {
            case Right(submitResponse) =>
              Ok(Json.toJson(submitResponse))
            case Left(apiError)        =>
              logger.warn(s"Error sending rcasp ${action.toLowerCase} information: $apiError")
              InternalServerError(s"Error sending rcasp ${action.toLowerCase} information")
          }
      )
}
