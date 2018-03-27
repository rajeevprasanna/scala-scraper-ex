
import redis.{RedisBlockingClient, RedisClient}

import scala.concurrent.Future
import scala.concurrent.duration._
import AppContext._

import scala.concurrent.ExecutionContext.Implicits.global
import SecureKeys._
import cats._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory


object BFRedisClient {

  val logger = Logger(LoggerFactory.getLogger("BFRedisClient"))
  logger.debug("initializing redis client connection!!!")



  val redis = RedisClient(host = HOST, port=PORT, password = Some(PASSWORD), name = USERNAME)
  val redisBlocking = RedisBlockingClient(host = HOST, port=PORT, password = Some(PASSWORD), name = USERNAME)

  import spray.json._
  import DefaultJsonProtocol._
  case class CrawlPayload(urls:List[String], resourceUrl:String, completed:Boolean, pageUrl:Option[String])

  object CrawlPayloadJsonProtocol extends DefaultJsonProtocol {
    implicit val crawlPayloadFormat = jsonFormat4(CrawlPayload.apply)
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

  def publishFileUrlsToRedis(urls:List[String], resourceUrl:String, pageUrl:String, isCompleted:Boolean):Boolean = {
    import CrawlPayloadJsonProtocol._
    val payload:String = CrawlPayload(urls, resourceUrl, isCompleted, pageUrl.some).toJson.toString()
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

  case class ResourceUrlPayload(url:String, is_ajax:Option[Boolean])
  object ResourceUrlPayloadJsonProtocol extends DefaultJsonProtocol {
    implicit val resourcePayloadFormat = jsonFormat2(ResourceUrlPayload.apply)
  }
  def fetchCrawlUrl():Future[Option[ResourceUrlPayload]] = {
    import ResourceUrlPayloadJsonProtocol._
    popElementFromRedis(CRAWL_URL_QUEUE).flatMap(payloadOption => payloadOption match {
      case Some(payloadStr) =>
        //TODO : check if it is valild URL
        val payload:ResourceUrlPayload = JsonParser(payloadStr).convertTo[ResourceUrlPayload]
        Future{Some(payload)}

      case _ => Future{None}
    })
  }
}