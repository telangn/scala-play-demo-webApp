package dataAccessLayer

import model.{Book, Customer}

object LibraryRepository {

  val customers = scala.collection.mutable.ListBuffer[Customer](
    Customer(1, "Bruce Wayne", "1 Wayne Enterprise Ave, Gotham"),
    Customer(2, "Clark Kent", "Hall of Justice, Metropolis"),
    Customer(3, "Diana Prince", "400 W. Maple, Gateway City, CA")
  )

  val books = List(
    Book(1, "Moby Dick", "Herman Melville", available = true),
    Book(2, "A Tale of Two Cities", "Charles Dickens", available = true),
    Book(3, "David Copperfield", "Charles Dickens", available = true),
    Book(42, "Hitchhiker's Guide to the Galaxy", "Douglas Adams", available = true),
    Book(24601, "Les Miserables", "Victor Hugo", available = true)
  )


  def getBooks: List[Book] = books

  def getBooksByTitle(title: String): List[Book] = {
    books.filter(_.title.toUpperCase.contains(title.toUpperCase))
  }

  def getBooksByAuthor(author: String): List[Book] = {
    books.filter(_.author.toUpperCase.contains(author.toUpperCase))
  }

  def getBooksByAuthorAndTitle(author: String, title: String): List[Book] = {
    (getBooksByTitle(title).toSet & getBooksByAuthor(author).toSet).toList
  }

  def checkoutBook(book: Book): Unit = book.available = false

  def checkinBook(book: Book): Unit = book.available = true

  def getBook(id: Int): Option[Book] = books.find(b => b.id == id)

  def getCustomer(id: Int): Option[Customer] = customers.find(c => c.id == id)

  def addCustomer(name: String, address: String): Customer = {
    val nextId = customers.maxBy(_.id).id + 1
    val newCustomer = Customer(nextId, name, address)
    customers += newCustomer
    newCustomer
  }

}