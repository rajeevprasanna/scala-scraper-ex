


import java.io.{BufferedReader, FileOutputStream, InputStreamReader}
import java.net.URL
import java.util.Base64

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

//  val config = ConfigFactory.load()
//  implicit val akkaSystem = ActorSystem("web-crawler")
//
//  val maxDepth = config.getInt("crawl.depth")
//  val maxDownloadFiles = config.getInt("crawl.max_download_files")
//
////  val url = "http://www.belkin.com/us/support-search?q=document:rank&show=All"
////  val url = "https://www.vmware.com/in/products/horizon-apps.html"
////  val url = "https://www.citrix.com/products/netscaler-adc/"
//  val url = "http://www.microchip.com/wwwproducts/en/en010668"
//
//  val allFiles = Crawler.extractFiles(url, maxDepth, maxDownloadFiles)
//  println(allFiles)


  val fileUrl  = "http://ww1.microchip.com/downloads/en/AppNotes/01326A.pdf"

  import org.apache.commons.io.IOUtils
  import org.apache.commons.codec.digest.DigestUtils
  def toBytes(url: URL):Array[Byte] = {
    val input = url.openStream
    try {
      IOUtils.toByteArray(input)
    }
    finally {
      input.close()
    }
  }
  val BYTES_EMPTY = Array[Byte]()
  val file_url = new URL(fileUrl)
  val content:Array[Byte] = toBytes(file_url)
  val sha256 = DigestUtils.sha256Hex(content)
  val base64EncodedContent =  Base64.getEncoder().encode(content)
  println(s"sha 256 hash => $sha256")
}



