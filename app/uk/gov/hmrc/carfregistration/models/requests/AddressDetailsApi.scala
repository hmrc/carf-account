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

package uk.gov.hmrc.carfregistration.models.requests

import play.api.libs.json.{Json, OFormat}

import scala.collection.immutable.Seq

case class AddressDetailsApi(
    addressLine1: String,
    addressLine2: Option[String],
    addressLine3: Option[String],
    addressLine4: Option[String],
    postalCode: Option[String],
    countryCode: String
)

object AddressDetailsApi {
  implicit val format: OFormat[AddressDetailsApi] = Json.format[AddressDetailsApi]

  def apply(
      addressDetailsFrontend: AddressDetailsFrontend
  ): AddressDetailsApi =
    val addressOptionalLines = Seq(
      addressDetailsFrontend.addressLine2,
      addressDetailsFrontend.addressLine3,
      Some(addressDetailsFrontend.townOrCity)
    ).flatten

    AddressDetailsApi(
      addressLine1 = addressDetailsFrontend.addressLine1,
      addressLine2 = addressOptionalLines.headOption,
      addressLine3 = addressOptionalLines.lift(1),
      addressLine4 = addressOptionalLines.lift(2),
      postalCode = addressDetailsFrontend.postalCode,
      countryCode = addressDetailsFrontend.countryCode
    )

}
