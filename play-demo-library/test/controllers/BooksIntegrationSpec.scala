package controllers

import akka.http.scaladsl.model.StatusCodes
import dataAccessLayer.LibraryRepository
import model.Book
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.test.WithServer

import scala.concurrent.Future

import scala.concurrent._
import ExecutionContext.Implicits.global


class BooksIntegrationSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures with Matchers {

  val Localhost = "http://localhost:"
  val ws = app.injector.instanceOf[WSClient]

  implicit val bookReader: Reads[Book] = (
    (JsPath \ "id").read[Int] and
      (JsPath \ "author").read[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "available").read[Boolean]
    ) (Book.apply _)

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(20, Seconds), interval = Span(1000, Millis))

  "Books Controller" should {
    "Provide access to all books" in new WithServer {

      val response: Future[WSResponse] = ws.url(Localhost + port + "/books").get()

      whenReady(response.map(response =>
        response.status shouldBe StatusCodes.OK
      ))

      whenReady(response.map(response =>
        (response.json \ "books").as[Seq[Book]].size shouldBe 5))
    }

    def searchHelper(port: Int, params: (String, String)*) = {

      val response: Future[WSResponse] = ws.url(Localhost + port + "/books/search").
        withQueryString(params: _*).get()

      whenReady(response.map(response =>
        response.status shouldBe StatusCodes.OK))

      response
    }

    "Return search results when given an author" in new WithServer {
      val response: Future[WSResponse] = searchHelper(port, ("author", "dickens"))

      whenReady(response.map(response =>
        (response.json \ "books").as[Seq[Book]].size shouldBe 2
      ))
    }

    "Return search results when given a title" in new WithServer {
      val response: Future[WSResponse] = searchHelper(port, ("title", "galaxy"))

      whenReady(response.map(response =>
        (response.json \ "books").as[Seq[Book]].size shouldBe 1
      ))
    }

    "Return search results when given an author and title" in new WithServer {
      val response: Future[WSResponse] = searchHelper(port, ("title", "miserables"), ("author", "hugo"))
      whenReady(response.map(response =>
        (response.json \ "books").as[Seq[Book]].size shouldBe 1
      ))
    }

    "Return an empty list when no search results found" in new WithServer {
      val response = searchHelper(port, ("author", "wolfe"))

      whenReady(response.map(response =>
        (response.json \ "books").as[Seq[Book]].size shouldBe List.empty
      ))
    }

    "Check out books" in new WithServer {
      LibraryRepository.getBook(1).get.available = true //1

      val response = ws.url(Localhost + port + "/book/1/checkout")
        .post("") //2

      whenReady(response.map(response =>
        response.status shouldBe StatusCodes.OK))

      whenReady(response.map(response =>
        (response.json \ "book").as[Book].available shouldBe false //3
      ))

      LibraryRepository.getBook(1).get.available shouldBe false //4
    }

    "Checkin books" in new WithServer {
      LibraryRepository.getBook(1).get.available = false

      val response = ws.url(Localhost + port + "/book/1/checkin")
        .post("")

      whenReady(response.map(response =>
        response.status shouldBe StatusCodes.OK))

      whenReady(response.map(response =>
        (response.json \ "book").as[Book].available shouldBe true //3
      ))

      LibraryRepository.getBook(1).get.available shouldBe true
    }

    "Returns an error when checking in/out a nonexistent book" +
      "a book that does not exist" in new WithServer {
      val responseCheckIn = ws.url(Localhost + port + "/book/99999/checkin")
        .post("")

      whenReady(responseCheckIn.map(response =>
        response.status shouldBe StatusCodes.BadRequest))

      val responseCheckOut = ws.url(Localhost + port + "/book/99999/checkout")
        .post("")

      whenReady(responseCheckOut.map(response =>
        response.status shouldBe StatusCodes.BadRequest))
    }

    "Returns an error when repeating a checkin/out" in new WithServer {
      LibraryRepository.getBook(1).get.available = true
      val responseCheckOut = ws.url(Localhost + port + "/book/1/checkout")
        .post("")

      whenReady(responseCheckOut.map(response =>
        response.status shouldBe StatusCodes.OK))

      //Repeat the checkout
      val responseCheckOutAgain = ws.url(Localhost + port + "/book/1/checkout")
        .post("")
      whenReady(responseCheckOutAgain.map(response =>
        response.status shouldBe StatusCodes.BadRequest))

      //Now check it back in
      val responseCheckIn = ws.url(Localhost + port + "/book/1/checkin")
        .post("")

      whenReady(responseCheckIn.map(response =>
        response.status shouldBe StatusCodes.OK))

      //Repeat the checkin
      val responseCheckInAgain = ws.url(Localhost + port + "/book/1/checkin")
        .post("")

      whenReady(responseCheckInAgain.map(response =>
        response.status shouldBe StatusCodes.BadRequest))
    }

  }
}
