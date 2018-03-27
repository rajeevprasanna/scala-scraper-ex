import redis.{RedisBlockingClient, RedisClient}

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import AppContext._

import scala.concurrent.ExecutionContext.Implicits.global
import SecureKeys._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import spray.json._

case class CrawlPayload(urls:List[String], resourceUrl:String, completed:Boolean, pageUrl:Option[String])
object CrawlPayloadJsonProtocol extends DefaultJsonProtocol {
  implicit val crawlPayloadFormat = jsonFormat4(CrawlPayload.apply)
}

case class ResourceUrlPayload(url:String, is_ajax:Option[Boolean])
object ResourceUrlPayloadJsonProtocol extends DefaultJsonProtocol {
  implicit val resourcePayloadFormat = jsonFormat2(ResourceUrlPayload.apply)
}


object BFRedisClient {

  val logger = Logger(LoggerFactory.getLogger("BFRedisClient"))
  logger.debug("initializing redis client connection!!!")

  val redis = RedisClient(host = HOST, port=PORT, password = Some(PASSWORD), name = USERNAME)
  val redisBlocking = RedisBlockingClient(host = HOST, port=PORT, password = Some(PASSWORD), name = USERNAME)

  private def popElementFromRedis(queueName:String):Future[Option[String]] = {
    val p = Promise[Option[String]]()
    Future{
      for {
        result <- redisBlocking.blpop(Seq(queueName), 3 seconds)
        (_, payload) <- result
      } p.success(payload.utf8String.some)
    }
    p.future
  }

  def publishFileUrlsToRedis(urls:List[String], resourceUrl:String, pageUrl:String, isCompleted:Boolean):Boolean = {
    import CrawlPayloadJsonProtocol._
    val payload:String = CrawlPayload(urls, resourceUrl, isCompleted, pageUrl.some).toJson.toString()
    redis.rpush(RESOURCE_URL_PAYLOAD_QUEUE, payload)
    true
  }

  def fetchResourceUrlsPayload():Future[Option[CrawlPayload]] = {
    val p = Promise[Option[CrawlPayload]]()
    Future{
      import CrawlPayloadJsonProtocol._
      for {
        payloadOption <- popElementFromRedis(RESOURCE_URL_PAYLOAD_QUEUE)
        payloadStr <- payloadOption
      } {
        val res = JsonParser(payloadStr).convertTo[CrawlPayload].some
        p.success(res)
      }
    }
    p.future
  }


  def fetchCrawlUrl():Future[Option[ResourceUrlPayload]] = {
    val p = Promise[Option[ResourceUrlPayload]]()
    Future {
      import ResourceUrlPayloadJsonProtocol._
      for {
        payloadOption <- popElementFromRedis(CRAWL_URL_QUEUE)
        payloadStr <- payloadOption
      } {
        val res = JsonParser(payloadStr).convertTo[ResourceUrlPayload].some
        p.success(res)
      }
    }
    p.future
  }
}