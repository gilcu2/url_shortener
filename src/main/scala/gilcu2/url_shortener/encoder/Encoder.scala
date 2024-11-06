package gilcu2.url_shortener.encoder

import org.pico.hashids.Hashids

trait Encoder {

  def encode(n: Long): String

}
