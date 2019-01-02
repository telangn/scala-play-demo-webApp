package controllers

import dataAccessLayer.LibraryRepository
import model.Customer
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.test.{PlaySpecification, WithServer}


class CustomersIntegrationSpec extends PlaySpecification {

  implicit val customerReader: Reads[Customer] = (
    (JsPath \ "id").read[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "address").read[String]
    ) (Customer.apply _)

  val Localhost = "http://localhost:"

  "The Customers controller" should {

    "Provide access to a single customer" in new WithServer {
      val response = await(WS.url(Localhost + port + "/customer/1").get())
      response.status must equalTo(OK)

      val customer: Customer = (response.json \ "customer").as[Customer]

      customer.name must equalTo("Bruce Wayne")
      customer.address must equalTo("1 Wayne Enterprise Ave, Gotham")
    }

    "Return an error when a" +
      "non-existent customer is requested" in new WithServer {
      val response = await(WS.url(Localhost + port + "/customer/99999").get())

      response.status must equalTo(BAD_REQUEST)
    }

    "Create new users" in new WithServer {
      val newCustomerName = "Joe Reeder"
      val newCustomerAddress = "123 Elm St."

      val currentCustomerCount = LibraryRepository.customers.size //1

      val response = await(WS.url(Localhost + port + "/customer/new")
        .post(Map("name" -> Seq(newCustomerName),
          "address" -> Seq(newCustomerAddress))))

      LibraryRepository.customers.size must equalTo(currentCustomerCount + 1) //2

      val newCustomer = LibraryRepository.customers.maxBy(c => c.id)

      newCustomer.name must equalTo(newCustomerName) //3
      newCustomer.address must equalTo(newCustomerAddress) //3
    }

    "Return the newly created user" in new WithServer {
      val newCustomerName = "Joe Reeder"
      val newCustomerAddress = "123 Elm St."

      val response = await(WS.url(Localhost + port + "/customer/new")
        .post(Map("name" -> Seq(newCustomerName),
          "address" -> Seq(newCustomerAddress))))

      response.status must equalTo(CREATED)
      val newCustomer = (response.json \ "customer").as[Customer]

      newCustomer.name must equalTo(newCustomerName)
      newCustomer.address must equalTo(newCustomerAddress)
      newCustomer.id must greaterThan(3)
    }

  }

}
