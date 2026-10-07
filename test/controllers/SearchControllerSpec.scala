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

package controllers

import base.SpecBase
import connectors.BridgeIntegrationConnector
import forms.FindAPropertyBridgeForm
import models.properties._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.FindAPropertyBridgeRepo
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import views.html.SearchView
import scala.concurrent.Future

class SearchControllerSpec extends SpecBase with MockitoSugar {

  val mockConnector = mock[BridgeIntegrationConnector]
  val mockRepo = mock[FindAPropertyBridgeRepo]

  "Search Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SearchController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SearchView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(FindAPropertyBridgeForm.form)(request, messages(application)).toString
      }
    }

    "must redirect to SearchResultsController when valid data is submitted and results are found" in {

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

      when(mockConnector.postcodeSearch(any(), any())(any()))
        .thenReturn(Future.successful(Right(searchResult)))
      when(mockRepo.upsert(any(), any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[BridgeIntegrationConnector].toInstance(mockConnector),
            bind[FindAPropertyBridgeRepo].toInstance(mockRepo)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.SearchController.onSubmit().url)
            .withFormUrlEncodedBody(("postcode-value", "AA1 1AA"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SearchResultsController.onPageLoad(1, "AddressASC").url
      }
    }

    "must redirect to NoLiableController when valid data is submitted and no results are found" in {

      val emptySearchResult = PostcodeSearchResult(
        Results(
          current_page = Some(1),
          page_size = Some(0),
          total_results = Some(0),
          total_pages = Some(0),
          has_next = Some(false),
          has_previous = Some(false),
          self = None,
          next = None,
          prev = None,
          first = None,
          last = None,
          records = Seq.empty
        )
      )

      when(mockConnector.postcodeSearch(any(), any())(any()))
        .thenReturn(Future.successful(Right(emptySearchResult)))
      when(mockRepo.upsert(any(), any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[BridgeIntegrationConnector].toInstance(mockConnector),
            bind[FindAPropertyBridgeRepo].toInstance(mockRepo)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.SearchController.onSubmit().url)
            .withFormUrlEncodedBody(("postcode-value", "AA1 1AA"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NoLiableController.onPageLoad().url
      }
    }

    "must return BadRequest when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.SearchController.onSubmit().url)
            .withFormUrlEncodedBody(("postcode-value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must return Status(error.statusCode) when connector returns an error" in {

      val errorResponse = ErrorResponse(INTERNAL_SERVER_ERROR, "Error message")

      when(mockConnector.postcodeSearch(any(), any())(any()))
        .thenReturn(Future.successful(Left(errorResponse)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[BridgeIntegrationConnector].toInstance(mockConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.SearchController.onSubmit().url)
            .withFormUrlEncodedBody(("postcode-value", "CF143AA"))

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }
}
