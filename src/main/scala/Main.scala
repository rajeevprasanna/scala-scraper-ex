
import akka.actor.{ActorSystem, Props}

import AppContext._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


object Main extends App {

  val maxDepth = config.getInt("crawl.depth")
  val maxDownloadFiles = config.getInt("crawl.max_download_files")

  def startWebCrawler():Unit = {
    BFRedisClient.fetchCrawlUrl().flatMap(urlOp => urlOp match {
      case Some(url) =>
        Crawler.extractFiles(url, maxDepth, maxDownloadFiles)
        Future{}

      case None =>
        //TODO : Add delay function
        Thread.sleep(5000)
        Future{}
    }).map(_ => startWebCrawler())
  }


  def startFileDownloader():Unit = {
      ???
  }

  startWebCrawler()
  startFileDownloader()
}



