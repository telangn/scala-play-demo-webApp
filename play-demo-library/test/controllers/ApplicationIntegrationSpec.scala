package controllers

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.ws._
import play.api.test._

import scala.concurrent.Future

import scala.concurrent._
import ExecutionContext.Implicits.global

class ApplicationIntegrationSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures with Matchers {

  val ws = app.injector.instanceOf[WSClient]

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(20, Seconds), interval = Span(1000, Millis))


  "Application" should {

    "be reachable" in new WithServer {

      val response: Future[WSResponse] = ws.url("http://localhost:" + port).get() //1

      whenReady(response.map(response =>
        response.status shouldBe StatusCodes.OK
      )) //2

      whenReady(response.map(response =>
        response.body should contain("Welcome to my Community Library!")
      )) //3
    }
  }
}
