import AppContext._
import Models.FileMetaData
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


object Main extends App with LazyLogging {

  logger.debug("Starting crawling program...")

  val maxDepth = config.getInt("crawl.depth")
  val maxDownloadFiles = config.getInt("crawl.max_download_files")

  def startWebCrawler():Unit = {
    BFRedisClient.fetchCrawlUrl().flatMap(urlOp => urlOp match {
      case Some(urlPayload) =>
        Crawler.extractFiles(urlPayload.url, maxDepth, maxDownloadFiles, urlPayload.is_ajax.getOrElse(false))
        startWebCrawler()
        Future{}

      case None =>
        //TODO : Add delay function
        Thread.sleep(5000)
        startWebCrawler()
        Future{}
    })
  }


  def startFileDownloader():Unit = {
      BFRedisClient.fetchResourceUrlsPayload().map(crawlPayloadOp => crawlPayloadOp match {
        case Some(payload) =>
            val filteredUrls:Future[List[String]] = BFService.filterProcessedUrls(payload.resourceUrl, payload.urls)
            filteredUrls.map(validUrls => {
              val filesMatadata:List[FileMetaData] = validUrls.flatMap(fileUrl => FileUtils.uploadResource(fileUrl, payload.pageUrl.getOrElse("")))
              if(!filesMatadata.isEmpty) BFService.uploadFilesMetadata(payload.resourceUrl, filesMatadata)
              if(payload.completed) BFService.markProcessComplete(payload.resourceUrl)
              startFileDownloader()
            })

        case None =>
          startFileDownloader()
      })
  }


  Future{startWebCrawler()}

  Future{startFileDownloader()}
  Future{startFileDownloader()}
  Future{startFileDownloader()}
  Future{startFileDownloader()}
  Future{startFileDownloader()}
}



