package gilcu2.url_shortener.http.routes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directives, Route}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import gilcu2.url_shortener.config.HttpConfig
import gilcu2.url_shortener.shortener.Shortener

case class PostUrlRoute(shortener: Shortener)
  extends Directives {

  val routes: Route =
    post {
      parameter("url") { url =>
        onSuccess(shortener.getShort(url)) {
          result => {
            result match {
              case Some(short) =>
                complete(short)
              case _ =>
                complete(BadRequest)
            }
          }
        }
      }
    }
}
