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

import uk.gov.hmrc.carfregistration.models.Address
import uk.gov.hmrc.carfregistration.models.responses.RegisterIndividualWithIdResponse

import javax.inject.Inject

class RegistrationService @Inject() () {

  def createRegisterIndividualResponse(): RegisterIndividualWithIdResponse =
    RegisterIndividualWithIdResponse(
      safeId = "test-safe-id",
      firstName = "timmy",
      lastName = "timmy",
      middleName = Some("hi"),
      address = Address(
        addressLine1 = "2 High Street",
        addressLine2 = Some("Birmingham"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("B23 2AZ"),
        countryCode = "GB"
      )
    )
}
