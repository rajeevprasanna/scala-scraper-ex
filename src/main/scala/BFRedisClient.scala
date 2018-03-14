
import redis.{RedisBlockingClient, RedisClient}
import scala.concurrent.Future
import scala.concurrent.duration._
import AppContext._
import scala.concurrent.ExecutionContext.Implicits.global

object BFRedisClient  extends App {

  val HOST = "****************************************"
  val PORT = 10443
  val USERNAME = "****************************************"
  val PASSWORD = "****************************************"
  val RESOURCE_URL_PAYLOAD_QUEUE = "****************************************"
  val CRAWL_URL_QUEUE = "****************************************"

  val redis = RedisClient(host = HOST, port=PORT, password = Some(PASSWORD), name = USERNAME)
  val redisBlocking = RedisBlockingClient(host = HOST, port=PORT, password = Some(PASSWORD), name = USERNAME)

  import spray.json._
  import DefaultJsonProtocol._
  case class CrawlPayload(urls:List[String], resourceUrl:String, completed:Boolean)
  object CrawlPayloadJsonProtocol extends DefaultJsonProtocol {
    implicit val crawlPayloadFormat = jsonFormat3(CrawlPayload.apply)
  }

  private def popElementFromRedis(queueName:String):Future[Option[String]] = {
    val result:Future[Option[String]] =
    redisBlocking.blpop(Seq(queueName), 5 seconds).map(result => {
      result.map({
        case (queue, payload) if queue == queueName =>
                println(s"popped element from queue => $queue has work : ${payload.utf8String}")
                payload.utf8String

        case _ => ""
      })
    })
    result
  }

  def publishFileUrlsToRedis(urls:List[String], resourceUrl:String, isCompleted:Boolean):Boolean = {
    import CrawlPayloadJsonProtocol._
    val payload:String = CrawlPayload(urls, resourceUrl, isCompleted).toJson.toString()
    redis.rpush(RESOURCE_URL_PAYLOAD_QUEUE, payload)
    true
  }

  def fetchResourceUrlsPayload():Future[Option[CrawlPayload]] = {
      import CrawlPayloadJsonProtocol._
      popElementFromRedis(RESOURCE_URL_PAYLOAD_QUEUE).flatMap(payloadOption => payloadOption match {
        case Some(payloadStr) =>
          val payload:CrawlPayload = JsonParser(payloadStr).convertTo[CrawlPayload]
          Future{Some(payload)}

        case _ => Future{None}
      })
  }

  case class ResourceUrlPayload(url:String)
  object ResourceUrlPayloadJsonProtocol extends DefaultJsonProtocol {
    implicit val resourcePayloadFormat = jsonFormat1(ResourceUrlPayload.apply)
  }
  def fetchCrawlUrl():Future[Option[String]] = {
    import ResourceUrlPayloadJsonProtocol._
    popElementFromRedis(CRAWL_URL_QUEUE).flatMap(payloadOption => payloadOption match {
      case Some(payloadStr) =>
        val payload:ResourceUrlPayload = JsonParser(payloadStr).convertTo[ResourceUrlPayload]
        Future{Some(payload.url)}

      case _ => Future{None}
    })
  }
}
