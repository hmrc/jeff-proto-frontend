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

import viewmodels.Link
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.{Key, SummaryListRow, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions}

final case class CheckAnswersSummaryListRow(
                                    titleMessageKey: String,
                                    captionKey: Option[String],
                                    value: Seq[String],
                                    changeLink: Option[Link],
                                    titleLink: Option[Link] = None,
                                    valueClasses: Option[String] = None,
                                    classes: String = "")

object CheckAnswersSummaryListRow {
  def summarise(checkYourAnswerRow: CheckAnswersSummaryListRow)(implicit messages: Messages): SummaryListRow = {

    val caption = checkYourAnswerRow.captionKey
    val titleLink = checkYourAnswerRow.titleLink
    val keyContent = if (caption.nonEmpty) {
      HtmlContent(
        Html(
          Messages(checkYourAnswerRow.titleMessageKey) + "<br>" +
            s"""<div id="account-number-hint" class="govuk-hint">${Messages(caption.getOrElse(""))}</div>"""
        )
      )
    } else if (titleLink.nonEmpty) {
      HtmlContent(
        Html(
          s"""<a href="${titleLink.get.href.url}" class="govuk-link govuk-summary-list__key_width">${Messages(checkYourAnswerRow.titleMessageKey)}</a>"""
        )
      )
    } else {
      Text(Messages(checkYourAnswerRow.titleMessageKey))
    }

    val key = Key(content = keyContent, classes = checkYourAnswerRow.classes)

    val idMaker = checkYourAnswerRow.titleMessageKey.toLowerCase().replace(" ", "-")

    val valueClasses = checkYourAnswerRow.valueClasses

    def valueContent(seqOfString: Seq[String]): HtmlContent =
      if (valueClasses.nonEmpty) {
        HtmlContent(
          s"""<span id="$idMaker-id" class="${valueClasses.getOrElse("")}">${seqOfString.map(Messages(_)).mkString("</br>")}</span>"""
        )
      } else {
        HtmlContent(s"""<span id="$idMaker-id">${seqOfString.map(Messages(_)).mkString("</br>")}</span>""")
      }

    checkYourAnswerRow.value match {
      case seqOfString if seqOfString.nonEmpty => SummaryListRow(
        key = key,
        value = Value(content = valueContent(seqOfString)),
        actions = checkYourAnswerRow.changeLink match {
          case Some(changeLink) => Some(
            Actions(items = Seq(ActionItem(
              href = changeLink.href.url,
              content = Text(Messages(changeLink.messageKey)),
              visuallyHiddenText = changeLink.visuallyHiddenMessageKey,
              attributes = Map(
                "id" -> changeLink.linkId
              )
            )))
          )
          case None => None
        }
      )
      case _ => SummaryListRow(
        key = Key(content = Text(Messages(checkYourAnswerRow.titleMessageKey))),
        value = checkYourAnswerRow.changeLink match {
          case Some(link) => Value(HtmlContent(s"""<a id="${link.linkId}" href="${link.href.url}" class="govuk-link">${messages(link.messageKey)}</a>"""))
          case None => Value()
        }
      )
    }
  }
}
