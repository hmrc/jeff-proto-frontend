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

package forms.mappings

import models.Enumerable
import play.api.data.{FieldMapping, FormError, Forms, Mapping}
import play.api.data.Forms.{of, text}
import play.api.i18n.Messages
import play.api.data.validation.{Constraint, Constraints}

import java.time.LocalDate

trait Mappings extends Formatters with Constraints {

  protected def text(errorKey: String = "error.required", args: Seq[String] = Seq.empty): FieldMapping[String] =
    of(stringFormatter(errorKey, args))

  protected def textWithLimit(minLength: Int = 0, maxLength: Int = Int.MaxValue): Mapping[String] = (minLength, maxLength) match {
    case (min, Int.MaxValue) => Forms.text.verifying(Constraints.minLength(min))
    case (0, max) => Forms.text.verifying(Constraints.maxLength(max))
    case (min, max) => Forms.text.verifying(Constraints.minLength(min), Constraints.maxLength(max))
  }

  protected def int(requiredKey: String = "error.required",
                    wholeNumberKey: String = "error.wholeNumber",
                    nonNumericKey: String = "error.nonNumeric",
                    args: Seq[String] = Seq.empty): FieldMapping[Int] =
    of(intFormatter(requiredKey, wholeNumberKey, nonNumericKey, args))

  protected def boolean(requiredKey: String = "error.required",
                        invalidKey: String = "error.boolean",
                        args: Seq[String] = Seq.empty): FieldMapping[Boolean] =
    of(booleanFormatter(requiredKey, invalidKey, args))


  protected def enumerable[A](requiredKey: String = "error.required",
                              invalidKey: String = "error.invalid",
                              args: Seq[String] = Seq.empty)(implicit ev: Enumerable[A]): FieldMapping[A] =
    of(enumerableFormatter[A](requiredKey, invalidKey, args))

  protected def localDate(
                           invalidKey: String,
                           allRequiredKey: String,
                           twoRequiredKey: String,
                           requiredKey: String,
                           args: Seq[String] = Seq.empty)(implicit messages: Messages): FieldMapping[LocalDate] =
    of(new LocalDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args))

  protected def currency(requiredKey: String = "error.required",
                         invalidNumeric: String = "error.invalidNumeric",
                         nonNumericKey: String = "error.nonNumeric",
                         args: Seq[String] = Seq.empty): FieldMapping[BigDecimal] =
    of(currencyFormatter(requiredKey, invalidNumeric, nonNumericKey, args))

  protected def validatedText(
                               requiredKey: String,
                               invalidKey: String,
                               lengthKey: String,
                               regex: String,
                               maxLength: Int,
                               minLength: Int = 11,
                               msgArg: String = ""
                             ): FieldMapping[String] =
    of(validatedTextFormatter(requiredKey, invalidKey, lengthKey, regex, maxLength, minLength, msgArg))

      // Valid formats: 1234567890 | +44 1234567890 | +44 0123 456 789 | +441234567890 | 0123 456 7890 | 0123-456-7890 | +44 123-456-7890

  val phoneNumberRegex =
        "((\\+44\\s?\\(0\\)\\s?\\d{2,4})|(\\+44\\s?(01|02|03|07|08)\\d{2,3})|(\\+44\\s?(1|2|3|7|8)\\d{2,3})|(\\(\\+44\\)\\s?\\d{3,4})|(\\(\\d{5}\\))|((01|02|03|07|08)\\d{2,3})|(\\d{5}))(\\s|-|.)(((\\d{3,4})(\\s|-)(\\d{3,4}))|((\\d{6,7})))"

  protected def validatePhoneNumber = {

    def validPhoneNumberLength(num: String) = num.length >= 11 && num.length <= 20
      Forms.text
        .verifying("error.phoneNumber.required", num => num.nonEmpty)
        .verifying("error.phoneNumber.invalidLength", num => if (num.nonEmpty) validPhoneNumberLength(num) else true)
        .verifying(
          "error.phoneNumber.invalidFormat",
          num => if (num.nonEmpty && validPhoneNumberLength(num)) num.matches(phoneNumberRegex) else true
        )
      }

  case class TextMatching(other: String, errorKey: String, key: String = "", constraints: Seq[Constraint[String]] = Nil)
    extends Mapping[String] {
    override val mappings = Nil

    override def bind(data: Map[String, String]) =
      (Forms.text.withPrefix(key).bind(data), Forms.text.withPrefix(other).bind(data)) match {
        case (l@Left(_), _) => l
        case (r@Right(_), Left(_)) => r
        case (r@Right(a), Right(b)) if a == b => r
        case (Right(_), Right(_)) => Left(Seq(FormError(key, errorKey)))
      }

    override def unbind(value: String) = Forms.text.unbind(value)

    override def unbindAndValidate(value: String) = Forms.text.unbindAndValidate(value)

    override def withPrefix(prefix: String) = copy(key = prefix + key)

    override def verifying(c: Constraint[String]*) = copy(constraints = constraints ++ c.toSeq)
  }
}
