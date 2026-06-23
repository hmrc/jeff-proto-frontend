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
import connectors.BridgeIntegrationConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.mappings.ContactDetails.form
import models.UserCounts
import models.accounts.*
import models.dashboard.Persons
import models.propertyLinks.PropertyLinkStatus
import models.propertyLinks.owner.OwnerAuthorisation
import models.requests.CcaAuthenticatedRequest
import navigation.Navigator
import pages.CompleteContactDetailsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.HomeView

import scala.concurrent.ExecutionContext

class HomeController @Inject()(
                                override val messagesApi: MessagesApi,
                                identify: IdentifierAction,
                                getData: DataRetrievalAction,
                                requireData: DataRequiredAction,
                                sessionRepository: SessionRepository,
                                navigator: Navigator,
                                val controllerComponents: MessagesControllerComponents,
                                view: HomeView,
                                connector: BridgeIntegrationConnector
                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val userCounts: UserCounts = UserCounts(
    draftCount = 1,
    hasMessages = true,
    unreadMessageCount = 2,
    agentsCount = 1,
    ownerPropertyLinkCount = 3,
    approvedAgentPropertyLinkCount = None
  )

  private val ownerAuthorisation = OwnerAuthorisation(
    authorisationId = 12,
    status = PropertyLinkStatus.APPROVED,
    submissionId = "test Submission id",
    uarn = 12,
    address = "Test address",
    localAuthorityRef = "Local Authority Ref",
    agents = None
  )


  def buildDemoRequest(request: Request[AnyContent], persons: Persons): CcaAuthenticatedRequest[AnyContent] = {
    val email = "test@gmail.com"
    val item = persons.persons.head.items.head

    CcaAuthenticatedRequest(
      organisationAccount = GroupAccount(
        id = 123L,
        groupId = "demo-group-id",
        companyName = item.data.names.corporate_name.getOrElse(""),
        addressId = 999L,
        email = email,
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
          firstName = item.data.names.forenames.getOrElse(""),
          lastName = item.data.names.surname.getOrElse(""),
          email = item.data.communications.email.getOrElse(""),
          phone1 = item.data.communications.telephone_number.getOrElse(""),
          phone2 = Some("0794300957"),
          addressId = 12345L
        )
      ),
      agentCode = None,
      request = request,
      sessionId = "demo-session-id"
    )
  }

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      connector.exploreRatePayer().map {
        case Some(persons) =>
          implicit val demoRequest: CcaAuthenticatedRequest[AnyContent] =
            buildDemoRequest(request, persons)

          Ok(
            view(
              userCounts = userCounts,
              manageAgentUrl = Some(""),
              ownerAuthorisationOpt = Some(ownerAuthorisation)
            )
          )

        case None =>
          InternalServerError("Failed to get ratepayer info")
      }
    }

}

