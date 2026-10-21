package models

import models.deletion.DeletionOptions
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class DeletionOptionsSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "DeletionOptions" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(DeletionOptions.values.toSeq)

      forAll(gen) {
        deletionOptions =>

          JsString(deletionOptions.toString).validate[DeletionOptions].asOpt.value mustEqual deletionOptions
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!DeletionOptions.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[DeletionOptions] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(DeletionOptions.values.toSeq)

      forAll(gen) {
        deletionOptions =>

          Json.toJson(deletionOptions) mustEqual JsString(deletionOptions.toString)
      }
    }
  }
}
