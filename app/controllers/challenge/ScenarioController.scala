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

package controllers.challenge

import controllers.actions._
import forms.mappings.ScenarioFormProvider
import pages.ScenarioPage
import play.api.i18n.I18nSupport
import play.api.mvc._
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.challenge.ScenarioView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ScenarioController @Inject()(
                                    val controllerComponents: MessagesControllerComponents,
                                    identify: IdentifierAction,
                                    getData: DataRetrievalAction,
                                    requireData: DataRequiredAction,
                                    sessionRepository: SessionRepository,
                                    formProvider: ScenarioFormProvider,
                                    view: ScenarioView
                                  )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>

      val preparedForm =
        request.userAnswers
          .get(ScenarioPage)
          .fold(formProvider())(formProvider().fill)

      Ok(view(preparedForm))
    }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors))
            ),
          answer =>
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(ScenarioPage, answer)
              )

              _ <- sessionRepository.set(updatedAnswers)

            } yield {
              answer match {
                case "lessThanSixMonths" =>
                  Redirect(routes.FormalReviewController.onPageLoad())

                case "moreThanSixMonths" =>
                  Redirect(routes.ScenarioController.onPageLoad())

                case "bandChanged" =>
                  Redirect(routes.ScenarioController.onPageLoad())

                case "propertyOrAreaChanged" =>
                  Redirect(routes.ScenarioController.onPageLoad())

                case "other" =>
                  Redirect(routes.ScenarioController.onPageLoad())

                case _ =>
                  Redirect(routes.ScenarioController.onPageLoad())
              }
            }
        )
    }
}