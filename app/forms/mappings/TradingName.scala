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

import forms.mappings.EmailAddressValidation.isValidEmail
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}
import play.api.libs.json.{Json, OFormat}

final case class TradingName(
                                 value: String,
                               )

object TradingName extends Mappings {

  implicit val format: OFormat[TradingName] = Json.format[TradingName]

  def unapply(tradingName: TradingName): Option[(String)] = Some(tradingName.value)
  
  def form: Form[TradingName] =
    Form(
      mapping(
        "tradingName" ->
          text("tradingName.error.required")
            .transform(_.trim, identity)
            .verifying("tradingName.error.required", _.nonEmpty)
            .verifying("tradingName.error.length", _.length <= 250)
      )(TradingName.apply)(TradingName.unapply)
    )
}