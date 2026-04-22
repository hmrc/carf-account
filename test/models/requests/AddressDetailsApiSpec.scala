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
import uk.gov.hmrc.carfregistration.models.requests.{AddressDetailsApi, AddressDetailsFrontend}

class AddressDetailsApiSpec extends SpecBase {

  "AddressDetailsApi apply method" - {
    "must return an AddressDetails model when given an AddressDetailsFrontendRequest" - {
      "when addressLine2 is empty, addressLine2 is populated by townOrCity" in {
        val testAddressDetailsFrontend: AddressDetailsFrontend = AddressDetailsFrontend(
          addressLine1 = "Route 103",
          addressLine2 = None,
          addressLine3 = None,
          townOrCity = "Oldale Town",
          postalCode = None,
          countryCode = "GB"
        )

        val expectedResult: AddressDetailsApi = AddressDetailsApi(
          addressLine1 = "Route 103",
          addressLine2 = Some("Oldale Town"),
          addressLine3 = None,
          addressLine4 = None,
          postalCode = None,
          countryCode = "GB"
        )

        val result = AddressDetailsApi.apply(testAddressDetailsFrontend)

        result mustEqual expectedResult
      }
      "when addressLine2 is filled, addressLine3 is populated by townOrCity" in {
        val testAddressDetailsFrontend: AddressDetailsFrontend = AddressDetailsFrontend(
          addressLine1 = "New Mauville",
          addressLine2 = Some("Route 110"),
          addressLine3 = None,
          townOrCity = "Mauville",
          postalCode = None,
          countryCode = "GB"
        )

        val expectedResult: AddressDetailsApi = AddressDetailsApi(
          addressLine1 = "New Mauville",
          addressLine2 = Some("Route 110"),
          addressLine3 = Some("Mauville"),
          addressLine4 = None,
          postalCode = None,
          countryCode = "GB"
        )

        val result = AddressDetailsApi.apply(testAddressDetailsFrontend)

        result mustEqual expectedResult
      }
      "when addressLine2 and 3 are filled, addressLine4 is populated by townOrCity" in {
        val testAddressDetailsFrontend: AddressDetailsFrontend = AddressDetailsFrontend(
          addressLine1 = "Champions Room",
          addressLine2 = Some("Hoenn League"),
          addressLine3 = Some("Victory Road"),
          townOrCity = "Ever Grande City",
          postalCode = None,
          countryCode = "GB"
        )

        val expectedResult: AddressDetailsApi = AddressDetailsApi(
          addressLine1 = "Champions Room",
          addressLine2 = Some("Hoenn League"),
          addressLine3 = Some("Victory Road"),
          addressLine4 = Some("Ever Grande City"),
          postalCode = None,
          countryCode = "GB"
        )

        val result = AddressDetailsApi.apply(testAddressDetailsFrontend)

        result mustEqual expectedResult
      }

      "when addressLine2 is empty and addressLine3 is filled, addressLine2 is populated by addressLine3 and addressLine3 is populated by townOrCity" in {
        val testAddressDetailsFrontend: AddressDetailsFrontend = AddressDetailsFrontend(
          addressLine1 = "Mossdeep Space Centre",
          addressLine2 = None,
          addressLine3 = Some("Route 125"),
          townOrCity = "Mossdeep City",
          postalCode = None,
          countryCode = "GB"
        )

        val expectedResult: AddressDetailsApi = AddressDetailsApi(
          addressLine1 = "Mossdeep Space Centre",
          addressLine2 = Some("Route 125"),
          addressLine3 = Some("Mossdeep City"),
          addressLine4 = None,
          postalCode = None,
          countryCode = "GB"
        )

        val result = AddressDetailsApi.apply(testAddressDetailsFrontend)

        result mustEqual expectedResult
      }
    }
  }
}
