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

package viewmodels

import pages.*
import viewmodels.CheckAnswersSummaryListRow.summarise
import play.api.i18n.Messages
import play.api.mvc.Call
import models.{CheckMode, NormalMode, UserAnswers}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import controllers.registration.routes as registrationRoutes

object RegistrationCheckAnswers {

  // -----------------------------------------------------
  // Base row builder
  // -----------------------------------------------------

  def buildRow(
                labelKey: String,
                value: String,
                linkId: String,
                href: Option[Call],
                hiddenKey: String
              )(implicit messages: Messages): CheckAnswersSummaryListRow =
    CheckAnswersSummaryListRow(
      titleMessageKey = labelKey,
      captionKey = None,
      value = Seq(value),
      changeLink = href.map { call =>
        Link(
          href = call,
          linkId = linkId,
          messageKey = "site.change",
          visuallyHiddenMessageKey = Some(hiddenKey)
        )
      }
    )



  // -----------------------------------------------------
  // Conditional helpers
  // -----------------------------------------------------

  private def requiredRow(
                           labelKey: String,
                           value: String,
                           href: Option[Call] = None
                         )(implicit messages: Messages): CheckAnswersSummaryListRow =
    buildRow(labelKey = labelKey, value = value, linkId = labelKey, href = href, hiddenKey = labelKey)

  private def optionalRow(
                           labelKey: String,
                           value: Option[String],
                           href: Option[Call] = None
                         )(implicit messages: Messages): Option[CheckAnswersSummaryListRow] =
    value.filter(_.nonEmpty).map { v =>
      buildRow(labelKey = labelKey, value = v, linkId = labelKey, href = href, hiddenKey = labelKey)
    }

  private def listRow(
                       labelKey: String,
                       values: List[String]
                     )(implicit messages: Messages): Option[CheckAnswersSummaryListRow] =
    Option.when(values.nonEmpty) {
      buildRow(labelKey, values.mkString(", "), labelKey, None, labelKey)
    }


// -----------------------------------------------------
  // Registration summary
  // -----------------------------------------------------

  def createRegistrationSummaryRows(
                                     answers: UserAnswers
                                   )(implicit messages: Messages): SummaryList = {

    val rows = Seq(
      answers.get(AddressPage).map { address =>
        buildRow(
          labelKey = "checkAnswers.address",
          value = address.toString,
          linkId = "contact-address",
          href = Some(registrationRoutes.AddressController.onPageLoad(CheckMode)),
          hiddenKey = "contact-email"
        )
      },
      answers.get(EmailPage).map { email =>
        buildRow(
          labelKey = "checkAnswers.email",
          value = email.email,
          linkId = "contact-email",
          href = Some(registrationRoutes.EmailController.onPageLoad(CheckMode)),
          hiddenKey = "contact-email"
        )
      },
      answers.get(ContactNumberPage).map { contactNumber =>
        buildRow(
          labelKey = "checkAnswers.contactNumber",
          value = contactNumber.value,
          linkId = "contact-number",
          href = Some(registrationRoutes.ContactNumberController.onPageLoad(CheckMode)),
          hiddenKey = "contact-number"
        )
      },

      answers.get(MobilePhonePage).map { secondNumber =>
        buildRow(
          labelKey = "checkAnswers.secondNumber",
          value = secondNumber.mobilePhone,
          linkId = "second-contact-number",
          href = Some(registrationRoutes.DoYouHaveASecondaryContactNumberController.onPageLoad(CheckMode)),
          hiddenKey = "second-contact-number"
        )
      },

      answers.get(TradingNamePage).map { tradingName =>
        buildRow(
          labelKey = "checkAnswers.tradingName",
          value = tradingName.value,
          linkId = "trading-name",
          href = Some(registrationRoutes.DoYouHaveATradingNameController.onPageLoad(CheckMode)),
          hiddenKey = "trading-name"
        )
      }

    )

    SummaryList(
      rows.flatten.map(summarise),
      classes = "govuk-!-margin-bottom-9"
    )
  }
}