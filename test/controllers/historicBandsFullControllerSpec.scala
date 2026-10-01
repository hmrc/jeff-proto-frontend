package controllers

import base.SpecBase
import models.PropertyDetails
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.historicBandsFullView

class historicBandsFullControllerSpec extends SpecBase {

  "historicBandsFull Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val property = PropertyDetails(
          address = "19, Somerby Court, Bramcote, Nottingham, NG9 3NB",
          band = "D",
          effectiveFrom = "9 October 2008",
          localAuthority = "Nottingham",
          localAuthorityReference = "1103 8000 8111 0001 00",
          improvementIndicator = true,
          mixedUseProperty = false,
          courtCode = "None",
          valuationList = "1993"
        )
        val request = FakeRequest(GET, routes.historicBandsFullController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[historicBandsFullView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(property)(request, messages(application)).toString
      }
    }
  }
}
