package index

/*
 * Copyright (C) 2020-2023 Lightbend Inc. <https://www.lightbend.com>
 */

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Index {

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "my-system")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    final case class User(id: Int, name: String, age: Int)
    val collins = User(1, "collins Ochieng", 89)
    // formats for unmarshalling and marshalling
    implicit val itemFormat: RootJsonFormat[User] = jsonFormat3(User.apply) // Corrected to jsonFormat3
//    implicit val createUserRequestFormat: RootJsonFormat[CreateUserRequest] = jsonFormat2(CreateUserRequest)
    //here comes some auxiliary functions..
    def getSquare(num: Long): Long = {
      val square = num * num
      square
    }
    //route definition comes here ...

    var route: Route =
      concat(
          get {
            pathPrefix("users" / "list"){
              complete(StatusCodes.OK, collins)
            }

        },

        get {
          pathPrefix("item" / LongNumber) { value =>
            // there might be no item for a given id
            complete(StatusCodes.OK, getSquare(value).toString)

          }
        },
        post {
          pathPrefix("users" / "create") {
            entity(as[User]) { createUserRequest =>
              // Unmarshal the JSON data into a CreateUserRequest
              val newUser = User(id = 0, name = createUserRequest.name, age = createUserRequest.age)

              // Print the unmarshalled User object
              println(s"Received user data: $newUser")

              // Respond with the unmarshalled User object
              complete(StatusCodes.OK, newUser)
            }
          }
        }


      )



//server set up ....
    val bindingFuture = Http().newServerAt("localhost", 8081).bind(route)
    println(s"Server now online. Please navigate to http://localhost:8081/users/list\nPress RETURN to stop...") // Corrected the URL
    StdIn.readLine() // let it run until the user presses return
    bindingFuture
      .flatMap { binding =>
        println(s"Stopping server at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}")
        binding.unbind() // trigger unbinding from the port
      }
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
