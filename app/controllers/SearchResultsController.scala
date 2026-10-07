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

import controllers.actions.IdentifierAction
import forms.mappings.EnterPostcodeFormProvider
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.FindAPropertyBridgeRepo
import services.SortingPostcodeAddressResultsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.SearchResultsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SearchResultsController @Inject()(
                                         val controllerComponents: MessagesControllerComponents,
                                         identify: IdentifierAction,
                                         repo: FindAPropertyBridgeRepo,
                                         sorting: SortingPostcodeAddressResultsService,
                                         formProvider: EnterPostcodeFormProvider,
                                         view: SearchResultsView
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController
  with I18nSupport with Logging {

  private val form: Form[String] = formProvider()
  private val pageSize = 10

  def onPageLoad(page: Int, sortBy: String): Action[AnyContent] =
    identify.async { implicit request =>
      val userId = request.userId

      repo.findByUserId(userId).map {
        case Some(stored) =>
          val sortedRecords =
            sorting.sort(stored.properties.results.records.toList, sortBy)

          val totalRecords =
            sortedRecords.size

          val totalPages =
            Math.ceil(totalRecords.toDouble / pageSize).toInt.max(1)

          val safePage =
            page.max(1).min(totalPages)

          val from =
            (safePage - 1) * pageSize

          val until =
            from + pageSize

          val pageRecords =
            sortedRecords.slice(from, until)

          val pagedProperties =
            stored.properties.copy(
              results = stored.properties.results.copy(
                current_page = Some(safePage),
                page_size = Some(pageSize),
                total_results = Some(totalRecords),
                total_pages = Some(totalPages),
                has_next = Some(safePage < totalPages),
                has_previous = Some(safePage > 1),
                records = pageRecords
              )
            )

          Ok(view(pagedProperties, sortBy, form))

        case None =>
          Redirect(routes.SearchController.onPageLoad())
      }
    }

  def sort: Action[AnyContent] =
    identify { implicit request =>
      val sortBy =
        request.body.asFormUrlEncoded
          .flatMap(_.get("sortBy").flatMap(_.headOption))
          .getOrElse("AddressASC")

      Redirect(routes.SearchResultsController.onPageLoad(1, sortBy))
    }

  def selectProperty(
                      index: Int,
                      sortBy: String
                    ): Action[AnyContent] =
    identify.async {
      implicit request =>
        val userId = request.userId

        repo.findByUserId(userId).map {
          case Some(stored) =>
            val sortedRecords =
              sorting.sort(stored.properties.results.records.toList, sortBy)

            val record = sortedRecords.lift(index)

            logger.info(s"Selected record at index $index: $record")

            val propertyRef =
              record.flatMap(_.list_entry.property).flatMap(_.id).flatMap(_.value)
                .orElse(record.flatMap(_.list_entry.id).flatMap(_.value))
                .getOrElse {
                  logger.warn(s"Property reference not found for index=$index, userId=$userId")
                  ""
                }

            logger.info(s"Extracted propertyRef: '$propertyRef'")

            if (propertyRef.nonEmpty) {
              val result = Redirect(
                controllers.routes.PropertyDetailsController.onPageLoad()
              ).addingToSession("propertyReference" -> propertyRef)
              logger.info(s"Redirecting to property details with session: ${result.newSession}")
              result
            } else {
              Redirect(routes.SearchResultsController.onPageLoad(1, sortBy))
            }

          case None =>
            Redirect(routes.SearchController.onPageLoad())
        }
    }
}