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

package models.responses

import base.SpecBase
import uk.gov.hmrc.carfregistration.models.MissingFieldsError
import uk.gov.hmrc.carfregistration.models.responses.*

class RegWithIdIndFrontendResponseSpec extends SpecBase {

  "RegWithIdIndFrontendResponse apply method" - {
    "must return a RegWithIdIndFrontendResponse model when given a RegWithIdIndApiResponse" - {
      "when the data is populated" in {
        val testResponse: RegWithIdIndApiResponse = RegWithIdIndApiResponse(
          responseCommon = ResponseCommon(status = "OK"),
          responseDetail = ResponseDetail(
            SAFEID = "testSafe",
            address = testAddress,
            individual = Some(IndividualResponse("Jeremy", "Usbourne", None)),
            organisation = None
          )
        )

        val expectedResult: RegWithIdIndFrontendResponse = RegWithIdIndFrontendResponse(
          safeId = "testSafe",
          firstName = "Jeremy",
          lastName = "Usbourne",
          middleName = None,
          address = testAddress
        )

        val result = RegWithIdIndFrontendResponse.apply(testResponse)

        result mustEqual Right(expectedResult)
      }
    }
    "must return a MissingFieldsError model when given a RegWithIdIndApiResponse" - {
      "with IndividualResponse missing" in {
        val testResponse: RegWithIdIndApiResponse = RegWithIdIndApiResponse(
          responseCommon = ResponseCommon(status = "OK"),
          responseDetail = ResponseDetail(
            SAFEID = "testSafe",
            address = testAddress,
            individual = None,
            organisation = None
          )
        )

        val result = RegWithIdIndFrontendResponse.apply(testResponse)

        result mustEqual Left(MissingFieldsError)
      }
    }
  }
}
