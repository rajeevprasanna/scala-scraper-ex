import redis.{RedisBlockingClient, RedisClient}

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

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


object BFRedisClient extends AppContext {

  implicit val logger = Logger(LoggerFactory.getLogger("BFRedisClient"))
  logger.info("initializing redis client connection!!!")

  val redis = RedisClient(host = ConfReader.REDIS_HOST, port=ConfReader.REDIS_PORT, password = Some(ConfReader.REDIS_PASSWORD), name = ConfReader.REDIS_USER_NAME)
  val redisBlocking = RedisBlockingClient(host = ConfReader.REDIS_HOST, port=ConfReader.REDIS_PORT, password = Some(ConfReader.REDIS_PASSWORD), name = ConfReader.REDIS_USER_NAME)

  private def popElementFromRedis(queueName:String):Future[Option[String]] = {
    val p = Promise[Option[String]]()
    Future{
      for {
        result <- redisBlocking.blpop(Seq(queueName), 1 second)
        (_, payload) <- result
      } p.success(payload.utf8String.some)
    }
    p.future
  }

  def publishFileUrlsToRedis(urls:List[String], resourceUrl:String, pageUrl:String, isCompleted:Boolean):Future[Boolean] = {
    val p = Promise[Boolean]()
    Future {
      import CrawlPayloadJsonProtocol._
      val payload:String = CrawlPayload(urls, resourceUrl, isCompleted, pageUrl.some).toJson.toString()
      redis.rpush(ConfReader.REDIS_RESOURCE_URL_PAYLOAD_QUEUE, payload)
      p.success(true)
    }

    p.future
  }

  def fetchResourceUrlsPayload():Future[CrawlPayload] = {
    val p = Promise[CrawlPayload]()
    Future{
      import CrawlPayloadJsonProtocol._
      for {
        payloadOption <- popElementFromRedis(ConfReader.REDIS_RESOURCE_URL_PAYLOAD_QUEUE)
        payloadStr <- payloadOption
      } {
        val res = JsonParser(payloadStr).convertTo[CrawlPayload]
        p.success(res)
      }
    }
    p.future
  }


  def fetchCrawlUrl():Future[ResourceUrlPayload] = {
    val p = Promise[ResourceUrlPayload]()
    Future {
      import ResourceUrlPayloadJsonProtocol._
      for {
        payloadOption <- popElementFromRedis(ConfReader.REDIS_CRAWL_URL_QUEUE)
        payloadStr <- payloadOption
      } {
        val res = JsonParser(payloadStr).convertTo[ResourceUrlPayload]
        p.success(res)
      }
    }
    p.future
  }
}