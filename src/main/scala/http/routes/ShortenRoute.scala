package http.routes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directives, Route}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import config.HttpConfig
import http.HttpUtils.validateUri
import shortener.Shortener

case class ShortenRoute(shortener: Shortener)
  extends Directives
    with FailFastCirceSupport
    with HttpConfig {

  def getShort(url: String): Future[String] = for {
    uriOpt: Option[String] <- validateUri(url)
    shortenResult <- uriOpt match {
      case Some(uri) => shortener.getShort(url)
      case None => Future(UrlShortenResult(code = "", status = OperationFailed))
    }
  } yield shortenResult

  val routes: Route =
    pathEndOrSingleSlash {
      post {
        formFieldMap { (fields: Map[String, String]) =>
          extractRequestContext { ctx =>
            onSuccess(getShort(fields("url"))) {
              result => {
                result match {
                  case Some(short) =>
                    val url = s"${ctx.request.uri.authority.toString()}/${short}"
                    respondWithHeaders(List(RawHeader("Location", url))) {
                      complete(Created)
                    }
                  case _ =>
                    complete(BadRequest)
                }
              }
            }
          }
        }
      }
    }
}
