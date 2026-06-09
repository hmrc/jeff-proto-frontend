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

package controllers.dashboard

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import navigation.Navigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.HomeView
import views.html.dashboard.UpdateEmailView
import forms.mappings.UpdateEmail.form
import models.NormalMode
import models.accounts.{DetailedIndividualAccount, GroupAccount, IndividualDetails}
import models.requests.CcaAuthenticatedRequest
import pages.UpdateEmailPage

import scala.concurrent.{ExecutionContext, Future}

class UpdateEmailController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       sessionRepository: SessionRepository,
                                       navigator: Navigator,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: UpdateEmailView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val email = "test@gmail.com"

      implicit val demoRequest: CcaAuthenticatedRequest[AnyContent] =
        CcaAuthenticatedRequest(
          organisationAccount = GroupAccount(
            id = 123L,
            groupId = "demo-group-id",
            companyName = "Demo Company Ltd",
            addressId = 999L,
            email = email,
            phone = "07123456789",
            isAgent = false,
            agentCode = None //Some(10001L)
          ),
          individualAccount = DetailedIndividualAccount(
            externalId = "ext-123",
            trustId = "trust-123",
            organisationId = 123L,
            individualId = 456L,
            details = IndividualDetails(
              firstName = "Jake",
              lastName = "Reid",
              email= "jake.reid@mail.com",
              phone1 = "0794300957",
              phone2 = Some("0794300957"),
              addressId = 12345L
            )
          ),
          agentCode = None,//Some(10001L),
          request = request,
          sessionId = "demo-session-id"
        )

      Ok(view(form))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async  {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UpdateEmailPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(UpdateEmailPage, NormalMode, updatedAnswers))
      )
  }
}
