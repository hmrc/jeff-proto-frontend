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
import models.SearchResult
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SearchResultsView

import javax.inject.Inject

class SearchResultsController @Inject()(
                                  val controllerComponents: MessagesControllerComponents,
                                  identify: IdentifierAction,
                                  formProvider: EnterPostcodeFormProvider,
                                  view: SearchResultsView
                                ) extends FrontendBaseController
  with I18nSupport {

  private val form: Form[String] = formProvider()

  def onPageLoad(postcode: String): Action[AnyContent] = Action { implicit request =>

    val results = Seq(
      SearchResult(
        "19 Somerby Court, Bramcote, Nottingham, NG9 3NB",
        "D",
        "Nottingham"
      ),
      SearchResult(
        "21 Somerby Court, Bramcote, Nottingham, NG9 3NB",
        "C",
        "Nottingham"
      )
    )

    Ok(
      view(
        postcode = postcode,
        results = results,
        searchForm = form
      )
    )
  }
}