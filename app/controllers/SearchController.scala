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

import connectors.BridgeIntegrationConnector
import controllers.actions.IdentifierAction
import forms.FindAPropertyBridgeForm
import models.properties.PostcodeSearchResult
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.FindAPropertyBridgeRepo
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.SearchView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SearchController @Inject()(
                                  val controllerComponents: MessagesControllerComponents,
                                  identify: IdentifierAction,
                                  connector: BridgeIntegrationConnector,
                                  repo: FindAPropertyBridgeRepo,
                                  view: SearchView
                                )(implicit ec: ExecutionContext) extends FrontendBaseController
  with I18nSupport {

  private val form: Form[FindAPropertyBridgeForm] = FindAPropertyBridgeForm.form

  def onPageLoad(): Action[AnyContent] =
    identify { implicit request =>
      Ok(view(form))
    }

  def onSubmit(): Action[AnyContent] =
    identify.async { implicit request =>
      val hc = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      val userId = request.userId
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors))),

        findAPropertyBridge => {
          connector.postcodeSearch("CVW", findAPropertyBridge)(hc).flatMap {

            case Right(searchResult) if searchResult.results.records.isEmpty =>
              Future.successful(Redirect(routes.NoLiableController.onPageLoad()))

            case Right(searchResult) =>
              repo.upsert(userId, searchResult).map { _ =>
                Redirect(routes.SearchResultsController.onPageLoad(1, "AddressASC"))
              }
            case Left(error) =>
              Future.successful(Status(error.statusCode)(Json.toJson(error)))

          }
        }
      )
    }
}