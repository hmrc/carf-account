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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.carfregistration.connectors.SubscriptionConnector
import uk.gov.hmrc.carfregistration.controllers.actions.AuthAction
import uk.gov.hmrc.carfregistration.models.NotFoundError
import uk.gov.hmrc.carfregistration.models.requests.SubscriptionRequest
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionController @Inject() (
    cc: ControllerComponents,
    authorise: AuthAction,
    subscriptionConnector: SubscriptionConnector
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def createSubscription: Action[JsValue] = authorise(parse.json).async { implicit request =>
    request.body
      .validate[SubscriptionRequest]
      .fold(
        invalid = _ =>
          Future.successful(
            BadRequest("SubscriptionRequest is invalid")
          ),
        valid = subscription =>
          subscriptionConnector.sendSubscriptionInformation(subscription).value.map {
            case Right(httpResponse) =>
              Status(httpResponse.status)(httpResponse.body)
            case Left(apiError)      =>
              logger.warn(s"Error sending subscription information: $apiError")
              InternalServerError("Error sending subscription information")
          }
      )
  }

  def displaySubscription(carfId: String): Action[AnyContent] = authorise.async { implicit request =>
    subscriptionConnector.displaySubscriptionInformation(carfId).value.flatMap {
      case Right(response)     => Future.successful(Ok(Json.toJson(response)))
      case Left(NotFoundError) =>
        Future.successful(NotFound("Could not find a subscription record for this user"))
      case Left(_)             =>
        Future.successful(InternalServerError("Unexpected error"))
    }
  }

}
