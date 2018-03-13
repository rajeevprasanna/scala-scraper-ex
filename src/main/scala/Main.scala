


import akka.actor.{ActorSystem, Props}

import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import redis.{RedisBlockingClient, RedisClient}
import redis.actors.RedisSubscriberActor
import redis.api.pubsub.{Message, PMessage}

import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._


object Main extends App {

  val config = ConfigFactory.load()
  implicit val akkaSystem = ActorSystem("web-crawler")

  val maxDepth = config.getInt("crawl.depth")
  val maxDownloadFiles = config.getInt("crawl.max_download_files")

//  val url = "http://www.belkin.com/us/support-search?q=document:rank&show=All"
//  val url = "https://www.vmware.com/in/products/horizon-apps.html"
//  val url = "https://www.citrix.com/products/netscaler-adc/"
  val url = "http://www.microchip.com/wwwproducts/en/en010668"

  val allFiles = Crawler.extractFiles(url, maxDepth, maxDownloadFiles)
  println(allFiles)
}



