package gilcu2.url_shortener.shortener

import scala.concurrent.Future

trait Shortener {

  def getShort(url: String): Future[Option[String]]

  def getOriginal(short: String): Future[Option[String]]

}
