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

package uk.gov.hmrc.carfregistration.models

import play.api.libs.json.{Json, OFormat, Reads, Writes}

object viewAndUpdateRcasp {

  sealed trait RcaspDetails {
    val RCASPID: String
    val SubscriptionID: String
    val IsRCASPUser: Boolean
    val PartyType: String
    val TINDetails: Option[List[TinDetails]]
    val AddressDetails: RcaspAddress
    val PrimaryContactDetails: Option[RcaspContactDetails]

    def getName: String
  }

  case class IndividualRcaspDetails(
      RCASPID: String,
      SubscriptionID: String,
      IsRCASPUser: Boolean,
      PartyType: String,
      FirstName: String,
      LastName: String,
      TINDetails: Option[List[TinDetails]],
      AddressDetails: RcaspAddress,
      PrimaryContactDetails: Option[RcaspContactDetails]
  ) extends RcaspDetails {

    def getName: String = s"$FirstName $LastName"

  }

  case class OrganisationRcaspDetails(
      RCASPID: String,
      SubscriptionID: String,
      IsRCASPUser: Boolean,
      PartyType: String,
      RCASPName: String,
      TradingName: String,
      TINDetails: Option[List[TinDetails]],
      AddressDetails: RcaspAddress,
      PrimaryContactDetails: Option[RcaspContactDetails],
      SecondaryContactDetails: Option[RcaspContactDetails]
  ) extends RcaspDetails {

    def getName: String = RCASPName

  }

  object RcaspDetails {

    implicit val reads: Reads[RcaspDetails] = Reads { json =>
      (json \ "TradingName").validateOpt[String].flatMap {
        case Some(_) => json.validate[OrganisationRcaspDetails]
        case None    => json.validate[IndividualRcaspDetails]
      }
    }

    implicit val writes: Writes[RcaspDetails] = {
      case i: IndividualRcaspDetails   => IndividualRcaspDetails.format.writes(i)
      case o: OrganisationRcaspDetails => OrganisationRcaspDetails.format.writes(o)
    }
  }

  object IndividualRcaspDetails {
    implicit val format: OFormat[IndividualRcaspDetails] = Json.format[IndividualRcaspDetails]
  }

  object OrganisationRcaspDetails {
    implicit val format: OFormat[OrganisationRcaspDetails] = Json.format[OrganisationRcaspDetails]
  }

}
