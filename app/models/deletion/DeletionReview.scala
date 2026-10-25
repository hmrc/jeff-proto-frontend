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

package models.deletion

import models.{Enumerable, WithName}

sealed trait DeletionReview

object DeletionReview extends Enumerable.Implicits {

  case object PartDemolished
    extends WithName("demolished/part-demolished")
      with DeletionReview

  case object FullDemolition
    extends WithName("demolished/full-demolition")
      with DeletionReview

  case object Uninhabitable
    extends WithName("unhabitable")
      with DeletionReview

  case object MajorRenovations
    extends WithName("major-renovations")
      with DeletionReview

  val values: Seq[DeletionReview] =
    Seq(
      PartDemolished,
      FullDemolition,
      Uninhabitable,
      MajorRenovations
    )

  implicit val enumerable: Enumerable[DeletionReview] =
    Enumerable(values.map(v => v.toString -> v): _*)
}