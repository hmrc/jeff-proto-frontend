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
import models.properties._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.ExplorePropertyRepo
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import views.html.PropertyDetailsView

import scala.concurrent.Future

class PropertyDetailsControllerSpec extends SpecBase with MockitoSugar {

  val mockConnector = mock[BridgeIntegrationConnector]
  val mockRepo = mock[ExplorePropertyRepo]

  val exploreResult = ExploreResult(
    results = ExploreResults(
      records = Seq(
        ExploreRecord(
          data = ExploreData(
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
              valuation = Valuation(value = Some("D"), method = None, previous = None),
              period = Some(Period(effective_from_date = Some("2023-01-01"), effective_to_date = None)),
              administration = Some(Administration(
                alteration_date = None,
                alteration_seq_no = None,
                entry_seq_no = None,
                judicially_ordered_by = None,
                transitionally_certified = None,
                collection_authority_ref = Some("LA-REF")
              )),
              workflow = None,
              property = Some(Property(
                id = Some(Id(Some("123"))),
                collection_authority_ref = None,
                address = Some(Address(full = Some("123 Test St"), first_line = None, postcode = None, known_as = None)),
                workflow = Some(PropertyWorkflow(improvement_ind = Some("Y")))
              ))
            )
          )
        )
      )
    )
  )

  "PropertyDetails Controller" - {

    "must return OK and the correct view for a GET when propertyReference is in session" in {

      val propertyRef = "12345"
      when(mockConnector.explore(eqTo(propertyRef), eqTo("CVW"))(any()))
        .thenReturn(Future.successful(Right(exploreResult)))
      when(mockRepo.upsert(any(), any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[BridgeIntegrationConnector].toInstance(mockConnector),
            bind[ExplorePropertyRepo].toInstance(mockRepo)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.PropertyDetailsController.onPageLoad().url)
          .withSession("propertyReference" -> propertyRef)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PropertyDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(exploreResult)(request, messages(application)).toString
      }
    }

    "must redirect to Search page when propertyReference is missing from session" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[BridgeIntegrationConnector].toInstance(mockConnector),
            bind[ExplorePropertyRepo].toInstance(mockRepo)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.PropertyDetailsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SearchController.onPageLoad().url
      }
    }

    "must return the error status when connector fails" in {

      val propertyRef = "12345"
      val error = ErrorResponse(INTERNAL_SERVER_ERROR, "some error")
      when(mockConnector.explore(eqTo(propertyRef), eqTo("CVW"))(any()))
        .thenReturn(Future.successful(Left(error)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[BridgeIntegrationConnector].toInstance(mockConnector),
            bind[ExplorePropertyRepo].toInstance(mockRepo)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.PropertyDetailsController.onPageLoad().url)
          .withSession("propertyReference" -> propertyRef)

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentAsString(result) mustEqual "some error"
      }
    }

  }
}
