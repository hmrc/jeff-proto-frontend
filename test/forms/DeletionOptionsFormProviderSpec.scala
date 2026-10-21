package forms

import forms.behaviours.OptionFieldBehaviours
import models.deletion.DeletionOptions
import play.api.data.FormError

class DeletionOptionsFormProviderSpec extends OptionFieldBehaviours {

  val form = new DeletionOptionsFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "deletionOptions.error.required"

    behave like optionsField[DeletionOptions](
      form,
      fieldName,
      validValues  = DeletionOptions.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
