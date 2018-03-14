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
            val filteredUrls:Future[List[String]] = BFService.filterProcessedUrls(payload.resourceUrl, payload.urls)
            filteredUrls.map(validUrls => {
              val filesMatadata:List[FileMetaData] = validUrls.flatMap(FileUtils.uploadResource(_))
              if(!filesMatadata.isEmpty) BFService.uploadFilesMetadata(payload.resourceUrl, filesMatadata)
              if(payload.completed) BFService.markProcessComplete(payload.resourceUrl)
              startFileDownloader()
            })

        case None =>
          startFileDownloader()
      })
  }

  startWebCrawler()
  startFileDownloader()
}



