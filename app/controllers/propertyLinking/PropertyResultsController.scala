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

package controllers.propertyLinking

import controllers.actions.IdentifierAction
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.FindPropertyRepo
import services.SortingPostcodeAddressResultsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.PropertyResultsView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PropertyResultsController @Inject()(
                                           identify: IdentifierAction,
                                           repo: FindPropertyRepo,
                                           sorting: SortingPostcodeAddressResultsService,
                                           view: PropertyResultsView,
                                           mcc: MessagesControllerComponents
                                         )(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  private val pageSize = 10

  def onPageLoad(page: Int, sortBy: String): Action[AnyContent] =
    identify.async { implicit request =>
      repo.findByUserId(request.userId).map {
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
                current_page = safePage,
                page_size = pageSize,
                total_results = totalRecords,
                total_pages = totalPages,
                has_next = safePage < totalPages,
                has_previous = safePage > 1,
                records = pageRecords
              )
            )

          Ok(view(pagedProperties, sortBy))

        case None =>
          Redirect(routes.FindPropertyController.onPageLoad())
      }
    }

  def sort: Action[AnyContent] =
    identify { implicit request =>
      val sortBy =
        request.body.asFormUrlEncoded
          .flatMap(_.get("sortBy").flatMap(_.headOption))
          .getOrElse("AddressASC")

      Redirect(routes.PropertyResultsController.onPageLoad(1, sortBy))
    }

  def selectProperty(index: Int, sortBy: String): Action[AnyContent] =
    identify.async { implicit request =>
      repo.findByUserId(request.userId).map {
        case Some(stored) =>
          val sortedRecords =
            sorting.sort(stored.properties.results.records.toList, sortBy)

          sortedRecords.lift(index) match {
            case Some(selected) =>
              // selected.list_entry.addresses.property_full_address
              Redirect(routes.FindPropertyController.onPageLoad())

            case None =>
              Redirect(routes.PropertyResultsController.onPageLoad(1, sortBy))
          }

        case None =>
          Redirect(routes.FindPropertyController.onPageLoad())
      }
    }
}