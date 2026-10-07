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
import forms.mappings.EnterPostcodeFormProvider
import models.properties._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.FindAPropertyBridgeRepo
import services.SortingPostcodeAddressResultsService
import views.html.SearchResultsView

import scala.concurrent.Future

class SearchResultsControllerSpec extends SpecBase with MockitoSugar {

  val mockRepo = mock[FindAPropertyBridgeRepo]

  def createRecord(id: String, address: String) = Record(
    list = ValuationList(
      id = Id(Some(id)),
      classification = Classification(Some("code"), Some("meaning")),
      country = None,
      collection_authority = CollectionAuthority(Some("code"), Some("label")),
      inforcement_period = None,
      administration = None
    ),
    list_entry = ListEntry(
      id = Some(Id(Some("wrong-id"))),
      designated_person = None,
      relevant_property = None,
      use = None,
      valuation = Valuation(Some("100"), None, None),
      period = None,
      administration = None,
      workflow = None,
      property = Some(Property(
        id = Some(Id(Some(id))),
        collection_authority_ref = None,
        address = Some(Address(full = Some(address), first_line = None, postcode = None, known_as = None)),
        workflow = None
      ))
    )
  )

  val searchResult = PostcodeSearchResult(
    Results(
      current_page = Some(1),
      page_size = Some(2),
      total_results = Some(2),
      total_pages = Some(1),
      has_next = Some(false),
      has_previous = Some(false),
      self = None,
      next = None,
      prev = None,
      first = None,
      last = None,
      records = Seq(
        createRecord("1", "Address 1"),
        createRecord("2", "Address 2")
      )
    )
  )

  "SearchResults Controller" - {

    "must return OK and the correct view for a GET" in {

      val stored = NewStoredVMVProperties("id", searchResult)
      when(mockRepo.findByUserId(any())).thenReturn(Future.successful(Some(stored)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[FindAPropertyBridgeRepo].toInstance(mockRepo)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.SearchResultsController.onPageLoad(1, "AddressASC").url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SearchResultsView]
        val formProvider = application.injector.instanceOf[EnterPostcodeFormProvider]
        val sorting = application.injector.instanceOf[SortingPostcodeAddressResultsService]
        
        val sortedRecords = sorting.sort(searchResult.results.records.toList, "AddressASC")
        val pagedProperties = searchResult.copy(
          results = searchResult.results.copy(
            records = sortedRecords,
            current_page = Some(1),
            page_size = Some(10),
            total_results = Some(2),
            total_pages = Some(1),
            has_next = Some(false),
            has_previous = Some(false)
          )
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(pagedProperties, "AddressASC", formProvider())(request, messages(application)).toString
      }
    }

    "must redirect to SearchController.onPageLoad when no results are found in repo" in {

      when(mockRepo.findByUserId(any())).thenReturn(Future.successful(None))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[FindAPropertyBridgeRepo].toInstance(mockRepo)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.SearchResultsController.onPageLoad(1, "AddressASC").url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SearchController.onPageLoad().url
      }
    }
    
    }

    "must redirect back to SearchResultsController when selectProperty is called with invalid index" in {

      val stored = NewStoredVMVProperties("id", searchResult)
      when(mockRepo.findByUserId(any())).thenReturn(Future.successful(Some(stored)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[FindAPropertyBridgeRepo].toInstance(mockRepo)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.SearchResultsController.selectProperty(99, "AddressASC").url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SearchResultsController.onPageLoad(1, "AddressASC").url
      }
    }
  }
