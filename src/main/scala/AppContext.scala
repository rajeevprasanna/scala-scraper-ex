import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object AppContext {

  import scala.concurrent.ExecutionContext.Implicits.global
  val config = ConfigFactory.load()
  implicit val akkaSystem = ActorSystem("web-crawler")

}
