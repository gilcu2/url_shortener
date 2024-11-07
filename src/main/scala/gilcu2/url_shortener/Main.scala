package gilcu2.url_shortener

import akka.actor.ActorSystem as ClassicActorSystem
import akka.actor.typed.ActorSystem as TypedActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import config.{HttpConfig, RedisConfig}
import encoder.HashidsEncoder
import id_generator.{BlockManager, IdGenerator}
import storage.Redis
import shortener.MyShortener
import http.Router

import scala.util.{Failure, Success}
import scala.io.StdIn
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*

import java.time.LocalDateTime
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

object Main extends LazyLogging {

  def createApp(): Behavior[Nothing] =
    Behaviors.setup { context =>
      implicit val typedActorSystem: TypedActorSystem[Nothing] = context.system
      val classicActorSystem: ClassicActorSystem = ClassicActorSystem()
      implicit val timeout: Timeout = Timeout(FiniteDuration(50, MILLISECONDS))
      implicit val executionContext: ExecutionContextExecutor = typedActorSystem.executionContext


      val redis: Redis = storage.Redis(RedisConfig.host, RedisConfig.port)(classicActorSystem)
      logger.info(s"redis created")
      val blockManager: ActorRef[BlockManager.Command] =
        context.spawn(BlockManager.create(redis), "blockManager")
      logger.info(s"blockManager created")
      val generator: ActorRef[IdGenerator.Command] =
        context.spawn(IdGenerator.create(serverId = "1", blockManager), "idGenerator")
      logger.info(s"generator created")
      val encoder: HashidsEncoder = HashidsEncoder()
      val shortener = MyShortener(generator, encoder, redis)(typedActorSystem, classicActorSystem, timeout)
      logger.info(s"shortener created")
      val router = Router(shortener)
      logger.info(s"router created http://${HttpConfig.host}:${HttpConfig.port}")

      val bindingFuture = Http()
        .newServerAt(HttpConfig.host, HttpConfig.port)
        .bind(router.routes)
      logger.info(s"binding started")
      println(s"Server listening on http://${HttpConfig.host}:${HttpConfig.port}")


      bindingFuture.onComplete {
        case Success(x) => logger.info(s"Server bind ok $x")
        case Failure(t) => logger.error(s"Server bind failure  $t")
      }

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
    }

  def runHelloApp(): Unit = {
    implicit val typedActorSystem: TypedActorSystem[Nothing] = TypedActorSystem(Behaviors.empty, "url-shortened")
    val classicActorSystem: ClassicActorSystem = ClassicActorSystem()
    implicit val timeout: Timeout = Timeout(FiniteDuration(50, MILLISECONDS))
    implicit val executionContext: ExecutionContextExecutor = typedActorSystem.executionContext

    val route =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Say hello to akka-http ${LocalDateTime.now()}</h1>"))
        }
      }


    val bindingFuture = Http()
      .newServerAt(HttpConfig.host, HttpConfig.port)
      .bind(route)
    logger.info(s"binding started")
    println(s"Server listening on http://${HttpConfig.host}:${HttpConfig.port}")

    bindingFuture.onComplete {
      case Success(x) =>
        logger.info(s"Server bind ok $x")
      //        typedActorSystem.terminate()
      //        classicActorSystem.terminate()
      case Failure(t) => logger.error(s"Server failure ok $t")
    }

  }

  def main(args: Array[String]): Unit = {
    TypedActorSystem(createApp(), "Url-shortener")
    //    runHelloApp()
    StdIn.readLine()
  }

}
