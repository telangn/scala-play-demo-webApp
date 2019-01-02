package controllers

import play.api.libs.ws._
import play.api.test._

class ApplicationIntegrationSpec extends PlaySpecification {

  "Application" should {
    "be reachable" in new WithServer {
      val response = await(WS.url("http://localhost:" + port).get()) //1

      response.status must equalTo(OK) //2
      response.body must contain("Welcome to my Community Library!") //3
    }
  }

}
