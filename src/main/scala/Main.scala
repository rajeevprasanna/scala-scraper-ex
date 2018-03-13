


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

  val maxDepth = config.getString("crawl.depth")
  val maxDownloadFiles = config.getString("crawl.max_download_files")

  val url = "http://www.belkin.com/us/support-search?q=document:rank&show=All"
  val doc = DomUtils.fetchDocument(url)
  val allHrefs = DomUtils.getUrlsFromDoc(doc)

  val formattedUrls = DomUtils.formatUrls(url, allHrefs)
  val (resourceUrls, htmlUrls) = DomUtils.extractResourceUrls(formattedUrls)
  val randomSamples = DomUtils.randomSampleUrls(5, htmlUrls)
  val commonHtml = DomUtils.commonPartsOfTemplate(randomSamples)


  println(resourceUrls)
  println(htmlUrls)
}



