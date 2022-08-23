package gilcu2.url_shortener.http

import akka.http.scaladsl.server.{Directives, Route}
import gilcu2.url_shortener.http.routes.{GetOriginalRoute, PostUrlRoute}
import gilcu2.url_shortener.shortener.Shortener

class Router(shortener: Shortener)
  extends Directives {
  val routes: Route =
    PostUrlRoute(shortener).routes ~
      GetOriginalRoute(shortener).routes
}