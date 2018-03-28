
import Models.FileMetaData
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}


object Main extends App with LazyLogging with AppContext {

  logger.debug("Starting web crawling program...")

  val maxDepth = config.getInt("crawl.depth")
  val maxDownloadFiles = config.getInt("crawl.max_download_files")

  def startWebCrawler():Unit = {
    Try(Await.result(BFRedisClient.fetchCrawlUrl(), 10 seconds)) match {
      case Success(Some(urlPayload)) =>
            logger.info(s"Going to start crawling for url => ${urlPayload.url} with ajax status => ${urlPayload.is_ajax}")
            Crawler.extractFiles(urlPayload.url, maxDepth, maxDownloadFiles, urlPayload.is_ajax.getOrElse(false)).map(_ => startWebCrawler())

      case Failure(_) =>
        logger.info(s"No crawl url in redis for crawl ")
        startWebCrawler()

      case x =>
        logger.error(s"seems reached invalid state => $x")
        startWebCrawler()
    }
  }


  def startFileDownloader():Unit = {
    Try(Await.result(BFRedisClient.fetchResourceUrlsPayload(), 10 seconds)) match {
      case Success(Some(payload)) =>
        val filteredUrls:Future[List[String]] = BFService.filterProcessedUrls(payload.resourceUrl, payload.urls)
        filteredUrls.map(validUrls => {
          val filesMatadata:List[FileMetaData] = validUrls.flatMap(fileUrl => FileUtils.uploadResource(fileUrl, payload.pageUrl.getOrElse("")))
          if(!filesMatadata.isEmpty) BFService.uploadFilesMetadata(payload.resourceUrl, filesMatadata)
          if(payload.completed) BFService.markProcessComplete(payload.resourceUrl)
          startFileDownloader()
        })

      case Failure(_) =>
        logger.info(s"No resource  url in redis for downloading")
        startFileDownloader()
    }
  }


  Future{startWebCrawler()}
  Future{startFileDownloader()}
}



