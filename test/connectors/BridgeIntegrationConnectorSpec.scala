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

import base.SpecBase
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import config.FrontendAppConfig
import forms.FindAPropertyBridgeForm
import models.properties._
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import scala.concurrent.ExecutionContext.Implicits.global

class BridgeIntegrationConnectorSpec extends SpecBase
  with MockitoSugar
  with BeforeAndAfterAll {

  private val bridgeIntegrationPort = 11111
  private val server: WireMockServer = new WireMockServer(wireMockConfig().port(bridgeIntegrationPort))

  override def beforeAll(): Unit = {
    server.start()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    server.stop()
    super.afterAll()
  }

  private val mockAppConfig = mock[FrontendAppConfig]

  private lazy val connector = {
    val httpClient = new GuiceApplicationBuilder().build().injector.instanceOf[HttpClientV2]
    when(mockAppConfig.bridgeIntegration).thenReturn(s"http://localhost:$bridgeIntegrationPort")
    when(mockAppConfig.internalAuthToken).thenReturn("82fac546-6de3-47bb-a1d7-357d68ecd3ac")
    new BridgeIntegrationConnector(httpClient, mockAppConfig)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "postcodeSearch" - {

    val postcode = "AA1 1AA"
    val listType = "CVW"
    val searchParams = FindAPropertyBridgeForm(Postcode(postcode), None)

    val searchResult = PostcodeSearchResult(
      Results(
        current_page = Some(1),
        page_size = Some(1),
        total_results = Some(1),
        total_pages = Some(1),
        has_next = Some(false),
        has_previous = Some(false),
        self = None,
        next = None,
        prev = None,
        first = None,
        last = None,
        records = Seq(
          Record(
            list = ValuationList(
              id = Id(Some("1")),
              classification = Classification(Some("code"), Some("meaning")),
              country = None,
              collection_authority = CollectionAuthority(Some("code"), Some("label")),
              inforcement_period = None,
              administration = None
            ),
            list_entry = ListEntry(
              id = Some(Id(Some("1"))),
              designated_person = None,
              relevant_property = None,
              use = None,
              valuation = Valuation(Some("100"), None, None),
              period = None,
              administration = None,
              workflow = None,
              property = None
            )
          )
        )
      )
    )

    "must return Right(PostcodeSearchResult) when the server returns OK" in {

      server.stubFor(
        get(urlPathEqualTo("/bridge-integration/search"))
          .withQueryParam("postcode", equalTo("AA11AA"))
          .withQueryParam("listType", equalTo(listType))
          .withHeader("Authorization", equalTo("82fac546-6de3-47bb-a1d7-357d68ecd3ac"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(searchResult).toString())
          )
      )

      val result = connector.postcodeSearch(listType, searchParams).futureValue

      result mustEqual Right(searchResult)
    }

    "must return Right(PostcodeSearchResult) when the server returns NOT_FOUND with valid JSON" in {

      val emptyResult = PostcodeSearchResult(Results(None, None, None, None, None, None, None, None, None, None, None, Seq.empty))

      server.stubFor(
        get(urlPathEqualTo("/bridge-integration/search"))
          .withQueryParam("postcode", equalTo("AA11AA"))
          .withQueryParam("listType", equalTo(listType))
          .withHeader("Authorization", equalTo("82fac546-6de3-47bb-a1d7-357d68ecd3ac"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(Json.toJson(emptyResult).toString())
          )
      )

      val result = connector.postcodeSearch(listType, searchParams).futureValue

      result.isRight mustBe true
      result.toOption.get.results.records mustBe empty
    }

    "must return Left(ErrorResponse) when the server returns BAD_REQUEST" in {

      server.stubFor(
        get(urlPathEqualTo("/bridge-integration/search"))
          .withQueryParam("postcode", equalTo("AA11AA"))
          .withQueryParam("listType", equalTo(listType))
          .withHeader("Authorization", equalTo("82fac546-6de3-47bb-a1d7-357d68ecd3ac"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody("Bad Request")
          )
      )

      val result = connector.postcodeSearch(listType, searchParams).futureValue

      result mustEqual Left(ErrorResponse(BAD_REQUEST, "Bad Request"))
    }

    "must return Left(ErrorResponse) when the server returns INTERNAL_SERVER_ERROR" in {

      server.stubFor(
        get(urlPathEqualTo("/bridge-integration/search"))
          .withQueryParam("postcode", equalTo("AA11AA"))
          .withQueryParam("listType", equalTo(listType))
          .withHeader("Authorization", equalTo("82fac546-6de3-47bb-a1d7-357d68ecd3ac"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("Internal Server Error")
          )
      )

      val result = connector.postcodeSearch(listType, searchParams).futureValue

      result mustEqual Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Internal Server Error"))
    }
  }

  "explore" - {
    val propertyReference = "38DA4B95-5061-4423-8024-9CAC37956E42"
    val listType = "CVW"
    val url = s"/bridge-integration/explore/$propertyReference/$listType"

    val exploreResult = ExploreResult(
      results = ExploreResults(
        records = Seq.empty
      )
    )

    "must return Right(ExploreResult) when the server returns OK" in {

      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(exploreResult).toString())
          )
      )

      val result = connector.explore(propertyReference, listType).futureValue

      result mustEqual Right(exploreResult)
    }

    "must return Left(ErrorResponse) when the server returns an error" in {

      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("error")
          )
      )

      val result = connector.explore(propertyReference, listType).futureValue

      result mustEqual Left(ErrorResponse(INTERNAL_SERVER_ERROR, "error"))
    }
  }
}
