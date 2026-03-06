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

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import uk.gov.hmrc.carfregistration.models.{Individual, Organisation}

case class Contact(
    email: String,
    individual: Option[Individual],
    organisation: Option[Organisation],
    phone: Option[String],
    mobile: Option[String]
)

case class SubscriptionRequest(
    gbUser: Boolean,
    idNumber: String,
    idType: String,
    primaryContact: Contact,
    secondaryContact: Option[Contact],
    tradingName: Option[String]
)

object SubscriptionRequest {

  implicit lazy val reads: Reads[SubscriptionRequest] =
    (
      (__ \ "gbUser").read[Boolean] and
        (__ \ "idNumber").read[String] and
        (__ \ "idType").read[String] and
        (__ \ "primaryContact").read[Contact] and
        (__ \ "secondaryContact").readNullable[Contact] and
        (__ \ "tradingName").readNullable[String]
    )(SubscriptionRequest.apply _)

  implicit val writes: OWrites[SubscriptionRequest] = Json.writes[SubscriptionRequest]

}

object Contact {

  implicit val reads: Reads[Contact] = (
    (__ \ "email").read[String] and
      (__ \ "individual").readNullable[Individual] and
      (__ \ "organisation").readNullable[Organisation] and
      (__ \ "phone").readNullable[String] and
      (__ \ "mobile").readNullable[String]
  )(Contact.apply _).flatMap { contact =>
    (contact.individual, contact.organisation) match {
      case (Some(_), None) | (None, Some(_)) => Reads.pure(contact)
      case _                                 => Reads.failed("Contact must have exactly one of 'individual' or 'organisation'")
    }
  }

  implicit val writes: OWrites[Contact] = Json.writes[Contact]
}
