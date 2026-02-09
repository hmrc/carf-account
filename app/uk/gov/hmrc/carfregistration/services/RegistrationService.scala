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
import uk.gov.hmrc.carfregistration.models.responses.{RegWithIdIndFrontendResponse, RegWithIdOrgFrontendResponse}
import uk.gov.hmrc.carfregistration.models.{ApiError, UuidGen}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationService @Inject() (connector: RegistrationConnector, clock: Clock, uuidGen: UuidGen)(implicit
    ec: ExecutionContext
) {

  def registerIndWithNino(
      frontendRequest: RegWithIdIndFrontendRequest
  )(implicit hc: HeaderCarrier): Future[Either[ApiError, RegWithIdIndFrontendResponse]] =
    connector
      .individualWithId(
        RegWithIdIndApiRequest(
          requestCommon = RequestCommon("NINO", uuidGen, clock),
          requestDetail = RequestDetailIndividual(frontendRequest)
        )
      )
      .value
      .map {
        case Right(response) => RegWithIdIndFrontendResponse.apply(response)
        case Left(error)     => Left(error)
      }

  def registerIndWithUtr(
      frontendRequest: RegWithUtrIndFrontendRequest
  )(implicit hc: HeaderCarrier): Future[Either[ApiError, RegWithIdIndFrontendResponse]] =
    connector
      .individualWithId(
        RegWithIdIndApiRequest(
          requestCommon = RequestCommon("UTR", uuidGen, clock),
          requestDetail = RequestDetailIndividual(frontendRequest)
        )
      )
      .value
      .map {
        case Right(response) => RegWithIdIndFrontendResponse.apply(response)
        case Left(error)     => Left(error)
      }

  def registerUserEntryOrgWithId(
      frontendRequest: RegWithIdUserEntryOrgFrontendRequest
  )(implicit hc: HeaderCarrier): Future[Either[ApiError, RegWithIdOrgFrontendResponse]] =
    connector
      .organisationWithID(
        RegWithIdOrgApiRequest(
          requestCommon = RequestCommon("UTR", uuidGen, clock),
          requestDetail = RequestDetailOrgUserEntry(frontendRequest)
        )
      )
      .value
      .map {
        case Right(response) => RegWithIdOrgFrontendResponse.apply(response)
        case Left(error)     => Left(error)
      }

  def registerAutoMatchOrgWithId(
      frontendRequest: RegWithIdAutoMatchOrgFrontendRequest
  )(implicit hc: HeaderCarrier): Future[Either[ApiError, RegWithIdOrgFrontendResponse]] =
    connector
      .organisationWithID(
        RegWithIdOrgApiRequest(
          requestCommon = RequestCommon("UTR", uuidGen, clock),
          requestDetail = RequestDetailOrgCtAutoMatch(frontendRequest)
        )
      )
      .value
      .map {
        case Right(response) => RegWithIdOrgFrontendResponse.apply(response)
        case Left(error)     => Left(error)
      }
}
