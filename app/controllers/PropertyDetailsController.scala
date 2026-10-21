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

import controllers.Execution.trampoline
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.mappings.EnterPostcodeFormProvider
import models.{NormalMode, PropertyDetails, SearchResult}
import navigation.Navigator
import pages.{ContactNumberPage, PropertyDetailsPage}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PropertyDetailsView

import javax.inject.Inject
import scala.concurrent.Future

class PropertyDetailsController @Inject()(
                                  val controllerComponents: MessagesControllerComponents,
                                  identify: IdentifierAction,
                                  getData: DataRetrievalAction,
                                  requireData: DataRequiredAction,
                                  sessionRepository: SessionRepository,
                                  navigator: Navigator,
                                  view: PropertyDetailsView
                                ) extends FrontendBaseController
  with I18nSupport {

  def onPageLoad(reference: String): Action[AnyContent] = Action { implicit request =>

    val property = PropertyDetails(
      reference,
      address = "19, Somerby Court, Bramcote, Nottingham, NG9 3NB",
      band = "D",
      effectiveFrom = "9 October 2008",
      localAuthority = "Nottingham",
      localAuthorityReference = "1103 8000 8111 0001 00",
      improvementIndicator = true,
      mixedUseProperty = false,
      courtCode = "None",
      valuationList = "None"
    )

    Ok(view(property))
  }

  def onSubmit(reference: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      val property = PropertyDetails(
        reference,
        address = "19, Somerby Court, Bramcote, Nottingham, NG9 3NB",
        band = "D",
        effectiveFrom = "9 October 2008",
        localAuthority = "Nottingham",
        localAuthorityReference = "1103 8000 8111 0001 00",
        improvementIndicator = true,
        mixedUseProperty = false,
        courtCode = "None",
        valuationList = "None"
      )

      for {
        updatedAnswers <- Future.fromTry(
          request.userAnswers.set(PropertyDetailsPage, property)
        )
        _ <- sessionRepository.set(updatedAnswers)
      } yield Redirect(
        controllers.routes.SelfCertController.onPageLoad(NormalMode)
      )
    }
}