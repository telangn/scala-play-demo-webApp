package controllers


import org.scalatestplus.play.PlaySpec
import play.api.mvc._
import play.api.test.{FakeRequest}
import play.api.test.Helpers._

import scala.concurrent.Future



class ApplicationUnitSpec extends PlaySpec with Results {

  "Application Index Page" should {

    "Should be valid" in {

      val controller = new Application()
      val result: Future[Result] = controller.index.apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText mustBe "Welcome to my Community Library!"

    }
  }
}
