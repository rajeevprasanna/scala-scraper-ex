
import redis.{RedisBlockingClient, RedisClient}

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import AppContext._

import scala.concurrent.ExecutionContext.Implicits.global
import SecureKeys._

import cats._
import cats.implicits._


import spray.json._
case class CrawlPayload(urls:List[String], resourceUrl:String, completed:Boolean)
object CrawlPayloadJsonProtocol extends DefaultJsonProtocol {
  implicit val crawlPayloadFormat = jsonFormat3(CrawlPayload.apply)
}

case class ResourceUrlPayload(url:String, is_ajax:Option[Boolean])
object ResourceUrlPayloadJsonProtocol extends DefaultJsonProtocol {
  implicit val resourcePayloadFormat = jsonFormat2(ResourceUrlPayload.apply)
}


object BFRedisClient {

  val redis = RedisClient(host = HOST, port=PORT, password = Some(PASSWORD), name = USERNAME)
  val redisBlocking = RedisBlockingClient(host = HOST, port=PORT, password = Some(PASSWORD), name = USERNAME)

  private def popElementFromRedis(queueName:String):Future[Option[String]] = {
    val p = Promise[Option[String]]()
    Future{
      for {
        result <- redisBlocking.blpop(Seq(queueName), 5 seconds)
        (queue, payload) <- result if queue == queueName
      } p.success(payload.utf8String.some)
    }
    p.future
  }

  def publishFileUrlsToRedis(urls:List[String], resourceUrl:String, isCompleted:Boolean):Boolean = {
    import CrawlPayloadJsonProtocol._
    val payload:String = CrawlPayload(urls, resourceUrl, isCompleted).toJson.toString()
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
        } p.success(JsonParser(payloadStr).convertTo[CrawlPayload].some)
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
      } p.success(JsonParser(payloadStr).convertTo[ResourceUrlPayload].some)
    }
    p.future
  }
}
