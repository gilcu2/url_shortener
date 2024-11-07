package gilcu2.url_shortener

import akka.actor.ActorSystem as ClassicActorSystem
import akka.actor.typed.ActorSystem as TypedActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.http.scaladsl.Http
import akka.util.Timeout
import config.{HttpConfig, RedisConfig}
import encoder.HashidsEncoder
import id_generator.{BlockManager, IdGenerator}
import storage.Redis
import shortener.MyShortener
import http.Router

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

object Main {

  def createApp(): Behavior[Nothing] =
    Behaviors.setup { context =>
      implicit val typedActorSystem: TypedActorSystem[Nothing] = context.system
      val classicActorSystem: ClassicActorSystem = ClassicActorSystem()
      implicit val timeout: Timeout = Timeout(FiniteDuration(50, MILLISECONDS))


      val redis: Redis = storage.Redis(RedisConfig.host, RedisConfig.port)(classicActorSystem)
      val blockManager: ActorRef[BlockManager.Command] =
        context.spawn(BlockManager.create(redis), "blockManager")
      val generator: ActorRef[IdGenerator.Command] =
        context.spawn(IdGenerator.create(serverId = "1", blockManager), "idGenerator")
      val encoder: HashidsEncoder = HashidsEncoder()
      val shortener = MyShortener(generator, encoder, redis)(typedActorSystem, classicActorSystem, timeout)

      val router = Router(shortener)
      Http()
        .newServerAt(HttpConfig.host, HttpConfig.port)
        .bind(router.routes)

      //      Behaviors.receiveSignal {
      //        case (_, Terminated(_)) =>
      //          Behaviors.stopped
      //      }
      Behaviors.empty
    }

  def main(args: Array[String]): Unit = {
    TypedActorSystem(createApp(), "Url-shortener")
  }

}
