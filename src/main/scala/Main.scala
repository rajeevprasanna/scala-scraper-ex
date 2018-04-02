
import Models.FileMetaData
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}


object Main extends App with LazyLogging with AppContext {

  logger.debug("Starting web crawling program...")

  val maxDepth = config.getInt("crawl.depth")
  val maxDownloadFiles = config.getInt("crawl.max_download_files")

  var crawlInProgress = false

  def startWebCrawler():Unit = {
    if(!crawlInProgress){
      Try(Await.result(BFRedisClient.fetchCrawlUrl(), 10 seconds)) match {
        case Success(Some(urlPayload)) =>
          crawlInProgress = true
          logger.info(s"Going to start crawling for url => ${urlPayload.url} with ajax status => ${urlPayload.is_ajax}")
          Try(Crawler.extractFiles(urlPayload.url, maxDepth, maxDownloadFiles, urlPayload.is_ajax.getOrElse(false))).map(_ => {
            crawlInProgress = false
          })

        case Failure(_) =>
          logger.info(s"No crawl url in redis for crawl ")

        case x =>
          logger.error(s"seems reached invalid state => $x")
      }
    }
    Thread.sleep(2000)
    startWebCrawler()
  }

  def startFileDownloader():Unit = {
    def reduceMetadata = (list:List[Option[FileMetaData]], a2:Option[FileMetaData]) => list :+ a2

    Try(Await.result(BFRedisClient.fetchResourceUrlsPayload(), 10 seconds)) match {
      case Success(Some(payload)) =>
        val filteredUrls:Future[List[String]] = BFService.filterProcessedUrls(payload.resourceUrl, payload.urls)
        filteredUrls.map(validUrls => {
          val filesMatadata:Future[List[Option[FileMetaData]]] = Source.fromIterator(() => validUrls.iterator)
            .mapAsyncUnordered(5)(fileUrl => FileUtils.uploadResource(fileUrl, payload.pageUrl.getOrElse("")))
              .runFold(List[Option[FileMetaData]]())(reduceMetadata)

          filesMatadata.map(mt => {
            val metaDataList = mt.flatten
            if(!metaDataList.isEmpty) BFService.uploadFilesMetadata(payload.resourceUrl, metaDataList)
            if(payload.completed) BFService.markProcessComplete(payload.resourceUrl)
          })
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



