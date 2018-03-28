import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

trait AppContext {
  val config = ConfigFactory.load()
  implicit val akkaSystem = ActorSystem("web-crawler")
  implicit val executionContext = akkaSystem.dispatchers.lookup("crawler-dispatcher")
  implicit def materializer:ActorMaterializer = ActorMaterializer()
}
