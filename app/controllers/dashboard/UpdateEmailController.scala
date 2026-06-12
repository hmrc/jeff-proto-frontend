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
import connectors.BridgeIntegrationConnector
import forms.mappings.UpdateEmail
import forms.mappings.UpdateEmail.form
import models.NormalMode
import models.Registration.frontend.AgentStatus.AUTONOMOUS
import models.Registration.frontend.{AgentStatus, RatepayerType, RegisterRatepayerRequest}
import models.accounts.{DetailedIndividualAccount, GroupAccount, IndividualDetails}
import models.requests.CcaAuthenticatedRequest
import pages.UpdateEmailPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.dashboard.UpdateEmailView

import scala.concurrent.{ExecutionContext, Future}

class UpdateEmailController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       sessionRepository: SessionRepository,
                                       navigator: Navigator,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: UpdateEmailView,
                                       bridgeIntegrationConnector: BridgeIntegrationConnector
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

      val email = "test@gmail.com"

  private def demoRequest(implicit request: Request[AnyContent]): CcaAuthenticatedRequest[AnyContent] =
    CcaAuthenticatedRequest(
      organisationAccount = GroupAccount(
        id = 123L,
        groupId = "demo-group-id",
        companyName = "Demo Company Ltd",
        addressId = 999L,
        email = email,
        phone = "07123456789",
        isAgent = false,
        agentCode = None//Some(10001L)
      ),
      individualAccount = DetailedIndividualAccount(
        externalId = "ext-123",
        trustId = "trust-123",
        organisationId = 123L,
        individualId = 456L,
        details = IndividualDetails(
          firstName = "Jake",
          lastName = "Reid",
          email = "jake.reid@mail.com",
          phone1 = "0794300957",
          phone2 = Some("0794300957"),
          addressId = 12345L
        )
      ),
      agentCode = None,
      request = request,
      sessionId = "demo-session-id"
    )


  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      implicit val dr: CcaAuthenticatedRequest[AnyContent] = demoRequest(request)

      val preparedForm = request.userAnswers.get(UpdateEmailPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm))
    }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      implicit val dr: CcaAuthenticatedRequest[AnyContent] = demoRequest(request)

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UpdateEmailPage, value))
            _ <- sessionRepository.set(updatedAnswers)
            _ <- bridgeIntegrationConnector.registerRatePayer(
              RegisterRatepayerRequest(
                userType = Some(RatepayerType.INDIVIDUAL),
                agentStatus = Some(AUTONOMOUS),
                name = s"${dr.individualAccount.details.firstName} ${dr.individualAccount.details.lastName}",
                tradingName = None,
                email = Some(value.email),
                nino = None,
                contactNumber = Some(dr.individualAccount.details.phone1),
                secondaryNumber = dr.individualAccount.details.phone2,
                address = Some(dr.individualAccount.details.addressId.toString),
                trnReferenceNumber = None,
                isRegistered = Some(false),
                recoveryId = None
              )
            )
          } yield Redirect(navigator.nextPage(UpdateEmailPage, NormalMode, updatedAnswers))
      )
    }
}
