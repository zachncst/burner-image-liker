import com.twitter.finagle.http.{Request, Response}
import knobs.{ClassPathResource, Config, Required}
import com.twitter.finagle.{Http, Service}
import com.twitter.util.Await
import io.finch._
import io.finch.circe._
import io.circe.generic.auto._

object WebServer extends App with Configuration {
  case class Locale(language: String, country: String)
  case class Time(locale: Locale, time: String)

  def currentTime(l: java.util.Locale): String =
    java.util.Calendar.getInstance(l).getTime.toString

  val api: Service[Request, Response] = (
    //getTodos :+: postTodo :+: deleteTodo :+: deleteTodos :+: patchTodo
    ImageLikerService.postEvent :+: ImageLikerService.getReport
  ).toServiceAs[Application.Json]

  Await.ready(Http.server.serve(s":${port}", api))
}
