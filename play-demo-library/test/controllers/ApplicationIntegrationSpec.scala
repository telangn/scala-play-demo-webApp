package controllers

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.ws._
import scala.concurrent.Future


class ApplicationIntegrationSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {

  val ws = app.injector.instanceOf[WSClient]

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(20, Seconds), interval = Span(1000, Millis))


  "Application" should {

    "be reachable" in  {

      val response: Future[WSResponse] = ws.url("http://localhost:" + 9000).get() //1

      whenReady(response) { response =>
        response.status mustBe StatusCodes.OK.intValue //2
        response.body must equal("Welcome to my Community Library!") //3
      }

    }
  }
}
