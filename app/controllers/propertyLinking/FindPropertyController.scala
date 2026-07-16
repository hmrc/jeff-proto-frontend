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

import connectors.BridgeIntegrationConnector
import controllers.actions.IdentifierAction
import controllers.propertyLinking.routes as propertyLinkingRoutes
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.FindPropertyRepo
import views.html.FindPropertyView
import forms.mappings.FindPropertyForm.form
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FindPropertyController @Inject()(findPropertyView: FindPropertyView,
                                       identify: IdentifierAction,
                                       connector: BridgeIntegrationConnector,
                                       repo: FindPropertyRepo,
                                       mcc: MessagesControllerComponents
                                      )(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    identify.async { implicit request =>
      Future.successful(Ok(findPropertyView(form)))
    }

  def onSubmit: Action[AnyContent] =
    identify.async { implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(findPropertyView(formWithErrors))),

        findAProperty => {
          connector.findPropertyPostcodeSearch(findAProperty).flatMap {
            case Left(error) =>
              Future.successful(Status(error.statusCode)(Json.toJson(error)))

            case Right(properties) if properties.results.records.isEmpty =>
              repo.upsert(request.userId, properties).map { _ =>
                Redirect(propertyLinkingRoutes.NoResultsFoundController.onPageLoad())
              }

            case Right(properties) =>
              repo.upsert(request.userId, properties).map { _ =>
                Redirect(propertyLinkingRoutes.PropertyResultsController.onPageLoad())
              }
          }
        }
      )
    }
}
