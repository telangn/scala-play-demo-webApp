package controllers


import play.api.mvc._
import model.Book
import dataAccessLayer.LibraryRepository
import play.api.libs.json._


class Books extends Controller {

  implicit val bookFormat = Json.format[Book]

  def getBooks = Action { request =>
    Ok(Json.prettyPrint(Json.obj("books" -> LibraryRepository.getBooks)))
  }

  def getBookCount = Action { request =>
    Ok(Json.prettyPrint(Json.obj("count" -> LibraryRepository.getBookCount)))
  }

  def search(author: Option[String], title: Option[String]) = Action { request =>
    val results = {
      if (author.isDefined && title.isDefined) {
        LibraryRepository.getBooksByAuthorAndTitle(author.get, title.get)
      }
      else if (author.isDefined) LibraryRepository.getBooksByAuthor(author.get)
      else if (title.isDefined) LibraryRepository.getBooksByTitle(title.get)
      else List()
    }
    Ok(Json.prettyPrint(Json.obj("books" -> results)))
  }

  def checkout(id: Int) = Action { request =>
    val bookOpt = LibraryRepository.getBook(id)
    if (bookOpt.isEmpty) {
      BadRequest(Json.prettyPrint(Json.obj(
        "status" -> "400",
        "message" -> s"Book not found with id $id.")))
    } else if (!bookOpt.get.available) {
      BadRequest(Json.prettyPrint(Json.obj(
        "status" -> "400",
        "message" -> s"Book #$id is already checked out.")))
    } else {
      LibraryRepository.checkoutBook(bookOpt.get)
      Ok(Json.prettyPrint(Json.obj(
        "status" -> 200,
        "book" -> bookOpt.get,
        "message" -> "Book checked out!")))
    }
  }

  def checkin(id: Int) = Action { request =>
    val bookOpt = LibraryRepository.getBook(id)
    if (bookOpt.isEmpty) {
      BadRequest(Json.prettyPrint(Json.obj(
        "status" -> "400",
        "message" -> s"Book not found with id $id.")))
    } else if (bookOpt.get.available) {
      BadRequest(Json.prettyPrint(Json.obj(
        "status" -> "400",
        "message" -> s"Book #$id is already checked in.")))
    } else {
      LibraryRepository.checkinBook(bookOpt.get)
      Ok(Json.prettyPrint(Json.obj(
        "status" -> 200,
        "book" -> bookOpt.get,
        "message" -> "Book checked back in!")))
    }
  }

}
