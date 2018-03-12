import redis.{RedisBlockingClient, RedisClient}
import scala.concurrent.duration._

object BFRedisClient {
//
//
//  val redis = RedisClient(host = "xxxxxm", port=10443, password = Some("yyyyy"), name = "x")
//  val redisBlocking = RedisBlockingClient(host = "xxxxxm", port=10443, password = Some("yyyyy"), name = "x")
//
//
//  def popElementFromRedis() = {
//    redisBlocking.blpop(Seq("web_crawl_queue_test"), 5 seconds).map(result => {
//      result.map({
//        case (key, work) => println(s"list $key has work : ${work.utf8String}")
//        case x => println(x)
//      })
//    })
//  }
  //  redis.lpush("web_crawl_queue_test", "ex message from test app")
}
