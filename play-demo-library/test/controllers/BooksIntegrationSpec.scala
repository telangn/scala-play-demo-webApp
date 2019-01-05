package controllers

import akka.http.scaladsl.model.StatusCodes
import model.Book
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.Future


class BooksIntegrationSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {

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

    "Provide access to all books" in {

      val response: Future[WSResponse] = ws.url(Localhost + 9000 + "/books").get()

      whenReady(response) { response =>
        response.status mustBe StatusCodes.OK.intValue
        (response.json \ "books").as[Seq[Book]].size mustBe 5
      }

    }

    def searchHelper(port: Int, params: (String, String)*) = {

      val response: Future[WSResponse] = ws.url(Localhost + port + "/books/search").
        withQueryStringParameters(params: _*).get()

      whenReady(response) { response =>
        response.status mustBe StatusCodes.OK.intValue
      }

      response
    }

    "Return search results when given an author" in {

      val response: Future[WSResponse] = searchHelper(9000, ("author", "dickens"))

      whenReady(response) { response =>
        (response.json \ "books").as[Seq[Book]].size mustBe 2
      }
    }

    "Return search results when given a title" in {

      val response: Future[WSResponse] = searchHelper(9000, ("title", "galaxy"))

      whenReady(response) { response =>
        (response.json \ "books").as[Seq[Book]].size mustBe 1
      }
    }

    "Return search results when given an author and title" in {

      val response: Future[WSResponse] = searchHelper(9000, ("title", "miserables"), ("author", "hugo"))

      whenReady(response) { response =>
        (response.json \ "books").as[Seq[Book]].size mustBe 1
      }
    }

    "Return an empty list when no search results found" in {

      val response = searchHelper(9000, ("author", "wolfe"))

      whenReady(response) { response =>
        (response.json \ "books").as[Seq[Book]].size mustBe 0
      }
    }

    "Check out books" in {

      //      LibraryRepository.getBook(1).get.available = true //1

      val availability = ws.url(Localhost + 9000 + "/book/1/availability").get()

      whenReady(availability) { response =>
        (response.json \ "availability").as[Boolean] mustBe true
      }

      val response: Future[WSResponse] = ws.url(Localhost + 9000 + "/book/1/checkout")
        .post("") //2

      whenReady(response) { response =>
        response.status mustBe StatusCodes.OK.intValue
        (response.json \ "book").as[Book].available mustBe false //3
      }

      val newAvailability = ws.url(Localhost + 9000 + "/book/1/availability").get()

      whenReady(newAvailability) { response =>
        (response.json \ "availability").as[Boolean] mustBe false
      }

      //      LibraryRepository.getBook(1).get.available mustBe false //4
    }

    "Checkin books" in {

      //      LibraryRepository.getBook(1).get.available = false

      val availability = ws.url(Localhost + 9000 + "/book/1/availability").get()

      whenReady(availability) { response =>
        (response.json \ "availability").as[Boolean] mustBe false
      }

      val response: Future[WSResponse] = ws.url(Localhost + 9000 + "/book/1/checkin")
        .post("")

      whenReady(response) { response =>
        response.status mustBe StatusCodes.OK.intValue
        (response.json \ "book").as[Book].available mustBe true //3
      }

      val newAvailability = ws.url(Localhost + 9000 + "/book/1/availability").get()

      whenReady(newAvailability) { response =>
        (response.json \ "availability").as[Boolean] mustBe true
      }

      //      LibraryRepository.getBook(1).get.available mustBe true

    }

    "Returns an error when checking in/out a nonexistent book " +
      "a book that does not exist" in {

      val responseCheckIn: Future[WSResponse] = ws.url(Localhost + 9000 + "/book/99999/checkin")
        .post("")

      whenReady(responseCheckIn) { response =>
        response.status mustBe StatusCodes.BadRequest.intValue
      }

      val responseCheckOut: Future[WSResponse] = ws.url(Localhost + 9000 + "/book/99999/checkout")
        .post("")

      whenReady(responseCheckOut) { response =>
        response.status mustBe StatusCodes.BadRequest.intValue
      }
    }

    "Returns an error when repeating a checkin/out" in {

      //      LibraryRepository.getBook(1).get.available = true

      val availability = ws.url(Localhost + 9000 + "/book/1/availability").get()

      whenReady(availability) { response =>
        (response.json \ "availability").as[Boolean] mustBe true
      }

      val responseCheckOut: Future[WSResponse] = ws.url(Localhost + 9000 + "/book/1/checkout")
        .post("")

      whenReady(responseCheckOut) { response =>
        response.status mustBe StatusCodes.OK.intValue
      }

      //Repeat the checkout
      val responseCheckOutAgain: Future[WSResponse] = ws.url(Localhost + 9000 + "/book/1/checkout")
        .post("")

      whenReady(responseCheckOutAgain) { response =>
        response.status mustBe StatusCodes.BadRequest.intValue
      }

      //Now check it back in
      val responseCheckIn: Future[WSResponse] = ws.url(Localhost + 9000 + "/book/1/checkin")
        .post("")

      whenReady(responseCheckIn) { response =>
        response.status mustBe StatusCodes.OK.intValue
      }

      //Repeat the checkin
      val responseCheckInAgain: Future[WSResponse] = ws.url(Localhost + 9000 + "/book/1/checkin")
        .post("")

      whenReady(responseCheckInAgain) { response =>
        response.status mustBe StatusCodes.BadRequest.intValue
      }
    }

  }
}
