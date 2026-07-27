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

package services

import config.FrontendAppConfig
import connectors.EmailVerificationConnector
import models.{ErrorResponse, UserAnswers}
import models.emailVerification.*
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import uk.gov.hmrc.http.HeaderCarrier

import java.net.URLEncoder
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationService @Inject()(config: FrontendAppConfig, emailVerificationConnector: EmailVerificationConnector)
                                        (implicit messagesApi: MessagesApi, executionContext: ExecutionContext) {

  def createRequest(credId: String, enteredEmail: String)(implicit messages: Messages): EmailVerificationRequest = {

    val language: String = messages.lang.code
    val enterEmailAddressPageUrl: String = config.startEmailVerificationBackUrl

    val email: EmailDetails = EmailDetails(
      address = enteredEmail,
      enterUrl = enterEmailAddressPageUrl
    )

    val english: Lang = Lang("en")
    val welsh: Lang = Lang("cy")

    def languageInfo(language: Lang): LanguageInfo = {
      LanguageInfo(
        pageTitle = messagesApi.preferred(Seq(language))("service.name"),
        userFacingServiceName = messagesApi.preferred(Seq(language))("emailVerificationJourney.signature")
      )
    }

    val labels: Labels = Labels(
      cy = languageInfo(welsh),
      en = languageInfo(english)
    )

    EmailVerificationRequest(
      credId = credId,
      continueUrl = config.startEmailVerificationContinueUrl,
      origin = URLEncoder.encode(messages("emailVerificationJourney.signature"), "utf-8"),
      deskproServiceName = "jeff-proto-frontend",
      accessibilityStatementUrl = "accessibility-statement",
      backUrl = enterEmailAddressPageUrl,
      email = email,
      labels = labels,
      lang = language,
      useNewGovUkServiceNavigation = true
    )
  }

  def retrieveAddressStatus(
                             verificationDetails: VerificationDetails,
                             emailAddress: String,
                             userAnswers: UserAnswers
                           )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, EmailVerificationDetails]] =
    for {
      response <- emailVerificationConnector.getEmailVerificationStatus(verificationDetails)
    } yield response.map(successResponse =>
      handleSuccess(emailAddress, successResponse)
    )

  private def handleSuccess(emailAddress: String, successResponse: GetVerificationStatusResponse): EmailVerificationDetails = {

    val isEmailVerified: Boolean =
      successResponse.emails.exists(email => email.emailAddress.equalsIgnoreCase(emailAddress) && email.verified)
    val isEmailLocked: Boolean =
      successResponse.emails.exists(email => email.emailAddress.equalsIgnoreCase(emailAddress) && email.locked)

    EmailVerificationDetails(emailAddress = emailAddress, isVerified = isEmailVerified, isLocked = isEmailLocked)

  }

  def redirectIfLocked(result: Future[Result], isLocked: Boolean): Future[Result] = {
    if (isLocked) {
      //TODO This needs to change to redirect to email locked controller
      Future.successful(Ok("Email is locked"))
    } else {
      result
    }
  }
 

}
