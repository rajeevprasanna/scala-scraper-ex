import AppContext._
import Models.FileMetaData

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
      BFRedisClient.fetchResourceUrlsPayload().map(crawlPayloadOp => crawlPayloadOp match {
        case Some(payload) =>
            val filesMatadata:List[FileMetaData] = payload.urls.flatMap(FileUtils.uploadResource(_))
            BFService.uploadFilesMetadata(payload.resourceUrl, filesMatadata)
            if(payload.completed) BFService.markProcessComplete(payload.resourceUrl)

        case None =>
      }).map(_ => startFileDownloader())
  }

  startWebCrawler()
  startFileDownloader()
}



