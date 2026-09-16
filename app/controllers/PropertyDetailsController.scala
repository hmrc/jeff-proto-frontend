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
import models.{PropertyDetails, SearchResult}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PropertyDetailsView

import javax.inject.Inject

class PropertyDetailsController @Inject()(
                                  val controllerComponents: MessagesControllerComponents,
                                  identify: IdentifierAction,
                                  view: PropertyDetailsView
                                ) extends FrontendBaseController
  with I18nSupport {

  def onPageLoad(reference: String): Action[AnyContent] = Action { implicit request =>

    val property = PropertyDetails(
      address = "19, Somerby Court, Bramcote, Nottingham, NG9 3NB",
      band = "D",
      effectiveFrom = "9 October 2008",
      localAuthority = "Nottingham",
      localAuthorityReference = "1103 8000 8111 0001 00",
      improvementIndicator = true,
      mixedUseProperty = false,
      courtCode = "None"
    )

    Ok(view(property))
  }
}