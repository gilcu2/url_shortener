package gilcu2.url_shortener.config

import com.typesafe.config
import com.typesafe.config.ConfigFactory

trait BaseConfig {
  protected[this] val conf: config.Config = ConfigFactory.load()
}


