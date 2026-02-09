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
import uk.gov.hmrc.carfregistration.models.requests.{OrganisationDetails, RegWithIdAutoMatchOrgFrontendRequest, RegWithIdUserEntryOrgFrontendRequest, RequestDetailOrgCtAutoMatch, RequestDetailOrgUserEntry}

class RequestDetailOrgSpec extends SpecBase {

  "RequestDetailOrgUserEntry apply method" - {
    "must return a RequestDetailOrgUserEntry model when given a RegWithIdUserEntryOrgFrontendRequest" in {
      val testRequest: RegWithIdUserEntryOrgFrontendRequest = RegWithIdUserEntryOrgFrontendRequest(
        requiresNameMatch = true,
        IDNumber = "testUtr",
        IDType = "testType",
        organisationName = "testName",
        organisationType = "testType"
      )

      val expectedResult: RequestDetailOrgUserEntry = RequestDetailOrgUserEntry(
        requiresNameMatch = true,
        IDNumber = "testUtr",
        IDType = "testType",
        organisation = OrganisationDetails(organisationName = "testName", organisationType = "testType"),
        isAnAgent = false
      )

      val result = RequestDetailOrgUserEntry.apply(testRequest)

      result mustEqual expectedResult
    }
  }

  "RequestDetailOrgCtAutoMatch apply method" - {
    "must return a RequestDetailOrgCtAutoMatch model when given a RegWithIdAutoMatchOrgFrontendRequest" in {
      val testRequest: RegWithIdAutoMatchOrgFrontendRequest = RegWithIdAutoMatchOrgFrontendRequest(
        requiresNameMatch = true,
        IDNumber = "testUtr",
        IDType = "testType"
      )

      val expectedResult: RequestDetailOrgCtAutoMatch = RequestDetailOrgCtAutoMatch(
        requiresNameMatch = true,
        IDNumber = "testUtr",
        IDType = "testType",
        isAnAgent = false
      )

      val result = RequestDetailOrgCtAutoMatch.apply(testRequest)

      result mustEqual expectedResult
    }
  }
}
