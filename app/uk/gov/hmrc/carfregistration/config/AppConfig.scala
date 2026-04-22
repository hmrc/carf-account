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

package uk.gov.hmrc.carfregistration.config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig):

  val appName: String = config.get[String]("appName")

  val bearerToken: String => String =
    (serviceName: String) => config.get[String](s"microservice.services.$serviceName.bearer-token")

  val environment: String => String =
    (serviceName: String) => config.get[String](s"microservice.services.$serviceName.environment")

  private val registerWithIdHost: String = servicesConfig.baseUrl("register-with-id")
  val registerWithIdBaseUrl: String      =
    s"$registerWithIdHost${config.get[String]("microservice.services.register-with-id.uri")}"

  private val registerWithoutIdHost: String = servicesConfig.baseUrl("register-without-id")

  val registerWithoutIdBaseUrl: String =
    s"$registerWithoutIdHost${config.get[String]("microservice.services.register-without-id.uri")}"

  private val createSubscriptionHost: String = servicesConfig.baseUrl("create-subscription")

  val createSubscriptionBaseUrl: String =
    s"$createSubscriptionHost${config.get[String]("microservice.services.create-subscription.uri")}"

  private val displaySubscriptionHost: String = servicesConfig.baseUrl("display-subscription")
  val displaySubscriptionBaseUrl: String      =
    s"$displaySubscriptionHost${config.get[String]("microservice.services.display-subscription.uri")}"
