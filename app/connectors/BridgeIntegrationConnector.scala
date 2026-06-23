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

package connectors

import config.FrontendAppConfig
import models.Registration.frontend.RegisterRatepayerRequest
import models.dashboard.Persons
import play.api.http.Status.*
import play.api.i18n.Lang.logger
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.net.URI
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.writeableOf_JsValue
import play.api.i18n.Lang.logger
import play.api.http.Status.*


class BridgeIntegrationConnector @Inject()(
                                            http: HttpClientV2,
                                            appConfig: FrontendAppConfig
                                          )(implicit ec: ExecutionContext) {

  private def uri(path: String) = new URI(s"${appConfig.bridgeIntegration}/bridge-integration/$path")

  def registerRatePayer(ratepayerRegistration: RegisterRatepayerRequest)
                       (implicit hc: HeaderCarrier): Future[Boolean] = {

    http.post(uri(s"register-ratepayer/123456789567").toURL)
      .withBody(Json.toJson(ratepayerRegistration))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => true
          case NOT_FOUND =>
            logger.warn("Ratepayer not found")
            false
          case BAD_REQUEST =>
            logger.warn("Invalid register ratepayer request")
            false
          case BAD_GATEWAY =>
            logger.error("Upstream service unavailable")
            false
          case INTERNAL_SERVER_ERROR =>
            logger.error(s"Server error: ${response.body}")
            false
          case other =>
            logger.error(s"Unexpected response status: $other")
            false
        }
      }
      .recover {
        case ex: Exception =>
          logger.error(s"Call to bridge-integration register-ratepayer failed: ${ex.getMessage}", ex)
          false
      }
  }

  def exploreRatePayer(credId: String = "123456789567")
                      (implicit hc: HeaderCarrier): Future[Option[Persons]] = {
    val url = uri(s"explore-ratepayer/$credId").toURL
    http.get(url)
      .execute[Option[Persons]]
      .recover {
        case ex =>
          logger.warn(s"Failed to retrieve explore ratepayer for credId=$credId: ${ex.getMessage}")
          None
      }
  }


}
