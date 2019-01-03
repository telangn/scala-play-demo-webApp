package controllers

import akka.http.scaladsl.model.StatusCodes
import dataAccessLayer.LibraryRepository
import model.Customer
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.WithServer

import scala.concurrent.Future

import scala.concurrent._
import ExecutionContext.Implicits.global


class CustomersIntegrationSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures with Matchers {

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

    "Provide access to a single customer" in new WithServer {
      val response: Future[WSResponse] = ws.url(Localhost + port + "/customer/1").get()

      whenReady(response.map(response =>
        response.status shouldBe StatusCodes.OK
      ))

      whenReady(response.map(response =>
        (response.json \ "customer").as[Customer].name shouldBe "Bruce Wayne"
      ))

      whenReady(response.map(response =>
        (response.json \ "customer").as[Customer].address shouldBe "1 Wayne Enterprise Ave, Gotham"
      ))
    }

    "Return an error when a" +
      "non-existent customer is requested" in new WithServer {
      val response: Future[WSResponse] = ws.url(Localhost + port + "/customer/99999").get()

      whenReady(response.map(response =>
        response.status shouldBe StatusCodes.BadRequest))
    }

    "Create new users" in new WithServer {

      val newCustomerName = "Joe Reeder"
      val newCustomerAddress = "123 Elm St."

      val currentCustomerCount = LibraryRepository.customers.size //1

      val response = ws.url(Localhost + port + "/customer/new")
        .post(Map("name" -> Seq(newCustomerName),
          "address" -> Seq(newCustomerAddress)))

      LibraryRepository.customers.size should equal(currentCustomerCount + 1) //2

      val newCustomer = LibraryRepository.customers.maxBy(c => c.id)

      newCustomer.name should equal(newCustomerName) //3
      newCustomer.address should equal(newCustomerAddress) //3
    }

    "Return the newly created user" in new WithServer {
      val newCustomerName = "Joe Reeder"
      val newCustomerAddress = "123 Elm St."

      val response = ws.url(Localhost + port + "/customer/new")
        .post(Map("name" -> Seq(newCustomerName),
          "address" -> Seq(newCustomerAddress)))

      whenReady(response.map(response =>
        response.status shouldBe StatusCodes.Created))

      whenReady(response.map(response =>
        (response.json \ "customer").as[Customer].name shouldBe newCustomerName
      ))

      whenReady(response.map(response =>
        (response.json \ "customer").as[Customer].address shouldBe newCustomerAddress
      ))

      whenReady(response.map(response =>
        (response.json \ "customer").as[Customer].id shouldBe 3
      ))

    }
  }
}
