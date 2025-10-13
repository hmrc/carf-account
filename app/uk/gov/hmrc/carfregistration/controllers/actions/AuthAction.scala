/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.carfregistration.controllers.actions

import com.google.inject.{ImplementedBy, Inject}
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.Results.Status
import play.api.mvc.*
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, credentialRole}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject() (
    override val authConnector: AuthConnector,
    val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends AuthAction
    with AuthorisedFunctions {

  override def invokeBlock[A](
      request: Request[A],
      block: Request[A] => Future[Result]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised(AuthProviders(GovernmentGateway) and ConfidenceLevel.L50)
      .retrieve(
        affinityGroup and credentialRole
      ) { case userAffinityGroup ~ userCredentialRole =>
        if (isPermittedUserType(userAffinityGroup, userCredentialRole)) {
          block(request)
        } else Future.successful(Status(UNAUTHORIZED))
      }
      .recover {
        case _: NoActiveSession        => Status(UNAUTHORIZED)
        case _: AuthorisationException => Status(UNAUTHORIZED)
      }
  }

  def isPermittedUserType(
      affinityGroup: Option[AffinityGroup],
      credentialRole: Option[CredentialRole]
  ): Boolean =
    affinityGroup match {
      case Some(Organisation) | Some(Individual) =>
        credentialRole.fold(false)(cr => cr == User)
      case _                                     => false
    }

}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[Request, AnyContent] with ActionFunction[Request, Request]
