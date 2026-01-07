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

package uk.gov.hmrc.carfregistration.services

import uk.gov.hmrc.carfregistration.connectors.RegistrationConnector
import uk.gov.hmrc.carfregistration.models.requests.*
import uk.gov.hmrc.carfregistration.models.responses.{RegisterIndWithIdFrontendResponse, RegisterOrganisationWithIdFrontendResponse}
import uk.gov.hmrc.carfregistration.models.{ApiError, UuidGen}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationService @Inject() (connector: RegistrationConnector, clock: Clock, uuidGen: UuidGen)(implicit
    ec: ExecutionContext
) {

  def registerIndividualWithNino(
      frontendRequest: RegisterIndWithIdFrontendRequest
  )(implicit hc: HeaderCarrier): Future[Either[ApiError, RegisterIndWithIdFrontendResponse]] =
    connector
      .individualWithId(
        RegisterIndWithIdAPIRequest(
          requestCommon = RequestCommon("NINO", uuidGen, clock),
          requestDetail = RequestDetailIndividual(frontendRequest)
        )
      )
      .value
      .map {
        case Right(response) => Right(RegisterIndWithIdFrontendResponse(response))
        case Left(error)     => Left(error)
      }

  def registerIndividualWithUtr(
      frontendRequest: RegisterIndWithIdFrontendRequest
  )(implicit hc: HeaderCarrier): Future[Either[ApiError, RegisterIndWithIdFrontendResponse]] =
    connector
      .individualWithId(
        RegisterIndWithIdAPIRequest(
          requestCommon = RequestCommon("UTR", uuidGen, clock),
          requestDetail = RequestDetailIndividual(frontendRequest)
        )
      )
      .value
      .map {
        case Right(response) => Right(RegisterIndWithIdFrontendResponse(response))
        case Left(error)     => Left(error)
      }

  def registerOrganisationWithId(
      frontendOrganisationRequest: RegisterOrganisationWithIdFrontendRequest
  )(implicit hc: HeaderCarrier): Future[Either[ApiError, RegisterOrganisationWithIdFrontendResponse]] =
    connector
      .organisationWithID(
        RegisterOrganisationWithIdAPIRequest(
          requestCommon = RequestCommon("UTR", uuidGen, clock),
          requestDetail = RequestDetailOrganisation(frontendOrganisationRequest)
        )
      )
      .value
      .map {
        case Right(response) => Right(RegisterOrganisationWithIdFrontendResponse(response))
        case Left(error)     => Left(error)
      }
}
