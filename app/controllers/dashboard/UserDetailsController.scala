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
import models.Registration.frontend.Address
import models.accounts.{DetailedIndividualAccount, GroupAccount, IndividualDetails}
import models.requests.CcaAuthenticatedRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.dashboard.UserDetailsView

import scala.concurrent.ExecutionContext

class UserDetailsController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: UserDetailsView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def show(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>


      implicit val authenticatedRequest: CcaAuthenticatedRequest[AnyContent] =
        CcaAuthenticatedRequest(
          organisationAccount = GroupAccount(
            id = 123L,
            groupId = "demo-group-id",
            companyName = "Demo Company Ltd",
            addressId = 999L,
            email = "test@gmail.com",
            phone = "07123456789",
            isAgent = false,
            agentCode = None
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
          agentCode = None,
          request = request.request,
          sessionId = "demo-session-id"
        )
      

      Ok(view(authenticatedRequest))
  }
}
