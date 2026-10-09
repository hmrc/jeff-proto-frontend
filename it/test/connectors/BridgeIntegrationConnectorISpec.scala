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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import config.FrontendAppConfig
import forms.FindAPropertyBridgeForm
import models.properties._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.ExecutionContext.Implicits.global

class BridgeIntegrationConnectorISpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterAll {

  private val wireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  override def beforeAll(): Unit = {
    wireMockServer.start()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  private lazy val app = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.bridge-integration.port" -> wireMockServer.port(),
      "microservice.services.bridge-integration.host" -> "127.0.0.1",
      "internal-auth.token" -> "test-token"
    )
    .build()

  private lazy val connector = app.injector.instanceOf[BridgeIntegrationConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "BridgeIntegrationConnector" - {

    "postcodeSearch must return a result" in {
      val postcode = "AA1 1AA"
      val searchParams = FindAPropertyBridgeForm(Postcode(postcode), None)
      val expectedResult = PostcodeSearchResult(Results(None, None, None, None, None, None, None, None, None, None, None, Seq.empty))

      wireMockServer.stubFor(
        get(urlPathEqualTo("/bridge-integration/search"))
          .withQueryParam("postcode", equalTo("AA11AA"))
          .withQueryParam("listType", equalTo("CVW"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(expectedResult).toString())
          )
      )

      val result = connector.postcodeSearch("CVW", searchParams).futureValue

      result mustEqual Right(expectedResult)
    }

    "explore must return a result" in {
      val propertyReference = "ref123"
      val expectedResult = ExploreResult(ExploreResults(Seq.empty))

      wireMockServer.stubFor(
        get(urlEqualTo(s"/bridge-integration/explore/$propertyReference/CVW"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(expectedResult).toString())
          )
      )

      val result = connector.explore(propertyReference, "CVW").futureValue

      result mustEqual Right(expectedResult)
    }
  }
}
