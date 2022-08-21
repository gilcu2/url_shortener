package http.routes

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directives, Route}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import config.HttpConfig
import shortener.Shortener


case class GetShortenedRoute(shortener: Shortener)
  extends Directives
    with FailFastCirceSupport {

  val routes: Route = path(Segment) { short =>
    get {
      onSuccess(shortener.getOriginal(short)) {
        case Some(url) =>
          respondWithHeaders(List(RawHeader("Location", url))) {
            complete(Found)
          }
        case None => complete(NotFound)
      }
    }
  }
}
