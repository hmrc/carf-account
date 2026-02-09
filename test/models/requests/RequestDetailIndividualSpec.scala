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

package models.requests

import base.SpecBase
import uk.gov.hmrc.carfregistration.models.requests.*

class RequestDetailIndividualSpec extends SpecBase {

  "RequestDetailIndividual apply method" - {
    "must return a RequestDetailIndividual model with a date of birth when given a RegWithNinoIndFrontendRequest" in {
      val testRequest: RegWithNinoIndFrontendRequest = RegWithNinoIndFrontendRequest(
        requiresNameMatch = true,
        IDNumber = "testUtr",
        IDType = "testType",
        dateOfBirth = "testDob",
        firstName = "Mark",
        lastName = "Corrigan"
      )

      val expectedResult: RequestDetailIndividual = RequestDetailIndividual(
        requiresNameMatch = true,
        IDNumber = "testUtr",
        IDType = "testType",
        individual = IndividualDetailsWithNino(dateOfBirth = "testDob", firstName = "Mark", lastName = "Corrigan"),
        isAnAgent = false
      )

      val result = RequestDetailIndividual.apply(testRequest)

      result mustEqual expectedResult
    }

    "must return a RequestDetailIndividual model with NO date of birth when given a RegWithUtrIndFrontendRequest" in {
      val testRequest: RegWithUtrIndFrontendRequest = RegWithUtrIndFrontendRequest(
        requiresNameMatch = true,
        IDNumber = "testUtr",
        IDType = "testType",
        firstName = "Mark",
        lastName = "Corrigan"
      )

      val expectedResult: RequestDetailIndividual = RequestDetailIndividual(
        requiresNameMatch = true,
        IDNumber = "testUtr",
        IDType = "testType",
        individual = IndividualDetailsWithUtr(firstName = "Mark", lastName = "Corrigan"),
        isAnAgent = false
      )

      val result = RequestDetailIndividual.apply(testRequest)

      result mustEqual expectedResult
    }
  }
}
