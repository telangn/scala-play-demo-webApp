package controllers

import dataAccessLayer.LibraryRepository
import model.Book
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.test.{PlaySpecification, WithServer}


class BooksIntegrationSpec extends PlaySpecification {

  val Localhost = "http://localhost:"

  implicit val bookReader: Reads[Book] = (
    (JsPath \ "id").read[Int] and
      (JsPath \ "author").read[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "available").read[Boolean]
    ) (Book.apply _)


  "Books Controller" should {
    "Provide access to all books" in new WithServer {
      val response = await(WS.url(Localhost + port + "/books").get())
      response.status must equalTo(OK)

      val books = (response.json \ "books").as[Seq[Book]]

      books.size must equalTo(5)
    }

    def searchHelper(port: Int, params: (String, String)*) = {
      import play.api.Play.current
      val response = await(WS.url(Localhost + port + "/books/search").
        withQueryString(params: _*).get())
      response.status must equalTo(OK)

      (response.json \ "books").as[Seq[Book]]
    }

    "Return search results when given an author" in new WithServer {
      val books = searchHelper(port, ("author", "dickens"))
      books.size must equalTo(2)
    }
    "Return search results when given a title" in new WithServer {
      val books = searchHelper(port, ("title", "galaxy"))
      books.size must equalTo(1)
    }
    "Return search results when given an author and title" in new WithServer {
      val books = searchHelper(port, ("title", "miserables"), ("author", "hugo"))
      books.size must equalTo(1)
    }
    "Return an empty list when no search results found" in new WithServer {
      val books = searchHelper(port, ("author", "wolfe"))
      books must be(List.empty)
    }

    "Check out books" in new WithServer {
      LibraryRepository.getBook(1).get.available = true //1

      val response = await(WS.url(Localhost + port + "/book/1/checkout")
        .post("")) //2
      response.status must equalTo(OK)
      val book = (response.json \ "book").as[Book]

      book.available must beFalse //3

      LibraryRepository.getBook(1).get.available must beFalse //4
    }

    "Checkin books" in new WithServer {
      LibraryRepository.getBook(1).get.available = false

      val response = await(WS.url(Localhost + port + "/book/1/checkin")
        .post(""))
      response.status must equalTo(OK)
      val book = (response.json \ "book").as[Book]

      book.available must beTrue

      LibraryRepository.getBook(1).get.available must beTrue
    }

    "Returns an error when checking in/out a nonexistent book" +
      "a book that does not exist" in new WithServer {
      await(WS.url(Localhost + port + "/book/99999/checkin")
        .post(""))
        .status must equalTo(BAD_REQUEST)

      await(WS.url(Localhost + port + "/book/99999/checkout")
        .post(""))
        .status must equalTo(BAD_REQUEST)
    }

    "Returns an error when repeating a checkin/out" in new WithServer {
      LibraryRepository.getBook(1).get.available = true
      await(WS.url(Localhost + port + "/book/1/checkout")
        .post(""))
        .status must equalTo(OK)

      //Repeat the checkout
      await(WS.url(Localhost + port + "/book/1/checkout")
        .post(""))
        .status must equalTo(BAD_REQUEST)

      //Now check it back in
      await(WS.url(Localhost + port + "/book/1/checkin")
        .post(""))
        .status must equalTo(OK)

      //Repeat the checkin
      await(WS.url(Localhost + port + "/book/1/checkin")
        .post(""))
        .status must equalTo(BAD_REQUEST)
    }

  }
}
