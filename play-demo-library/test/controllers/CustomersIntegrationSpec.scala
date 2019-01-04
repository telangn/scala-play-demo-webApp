package controllers

import akka.http.scaladsl.model.StatusCodes
import dataAccessLayer.LibraryRepository
import model.Customer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import scala.concurrent.Future


class CustomersIntegrationSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {

  val ws = app.injector.instanceOf[WSClient]

  implicit val customerReader: Reads[Customer] = (
    (JsPath \ "id").read[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "address").read[String]
    ) (Customer.apply _)

  val Localhost = "http://localhost:"

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(20, Seconds), interval = Span(1000, Millis))

  "The Customers controller" should {

    "Provide access to a single customer" in {

      val response: Future[WSResponse] = ws.url(Localhost + 9000 + "/customer/1").get()

      whenReady(response) { response =>
        response.status mustBe StatusCodes.OK.intValue
        (response.json \ "customer").as[Customer].name mustBe "Bruce Wayne"
        (response.json \ "customer").as[Customer].address mustBe "1 Wayne Enterprise Ave, Gotham"
      }

    }

    "Return an error when a " +
      "non-existent customer is requested" in {

      val response: Future[WSResponse] = ws.url(Localhost + 9000 + "/customer/99999").get()

      whenReady(response) { response =>
        response.status mustBe StatusCodes.BadRequest.intValue
      }
    }

    "Create new users" in {

      val newCustomerName = "Joe Reader"
      val newCustomerAddress = "123 Elm St."

      //      val currentCustomerCount = LibraryRepository.customers.size //1

      val response = ws.url(Localhost + 9000 + "/customer/new")
        .post(Map("name" -> Seq(newCustomerName),
          "address" -> Seq(newCustomerAddress)))

      whenReady(response) { response =>
        response.status mustBe StatusCodes.Created.intValue
      }

      //      LibraryRepository.customers.size must equal(currentCustomerCount + 1) //2

      //      val newCustomer = LibraryRepository.customers.maxBy(c => c.id)
      //
      //      newCustomer.name must equal(newCustomerName) //3
      //      newCustomer.address must equal(newCustomerAddress) //3
    }

    "Return the newly created user" in {

      val newCustomerName = "Joe Listener"
      val newCustomerAddress = "123 Nightmare St."

      val response: Future[WSResponse] = ws.url(Localhost + 9000 + "/customer/new")
        .post(Map("name" -> Seq(newCustomerName),
          "address" -> Seq(newCustomerAddress)))

      whenReady(response) { response =>
        response.status mustBe StatusCodes.Created.intValue
        (response.json \ "customer").as[Customer].name mustBe newCustomerName
        (response.json \ "customer").as[Customer].address mustBe newCustomerAddress
        (response.json \ "customer").as[Customer].id mustBe 5
      }

    }
  }
}
