
import java.util.concurrent.TimeUnit

import Models.FileMetaData
import akka.NotUsed
import akka.stream.{Graph, SourceShape}
import akka.stream.scaladsl.{RunnableGraph, Sink, Source}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future, Promise}
import scala.util.Try
import scala.concurrent.duration._
import cats.syntax.all._
import cats.instances.all._
import ExtensionUtils._


object FileDownloadTrigger extends AppContext {

  implicit val logger = Logger(LoggerFactory.getLogger("FileDownloadTrigger"))

  def fileDownloader() = {
    val pollInterval = FiniteDuration.apply(1, TimeUnit.MILLISECONDS)
    val sourceGraph:Graph[SourceShape[CrawlPayload], NotUsed] = new PollingSource(BFRedisClient.fetchResourceUrlsPayload, pollInterval)
    val fileResources:Source[CrawlPayload, NotUsed] = Source.fromGraph(sourceGraph)
    val res:RunnableGraph[NotUsed] = fileResources.mapAsyncUnordered(ConfReader.parallelCrawlingThreads)(transferFilesToS3FromUrl(_)).to(Sink.ignore)
    res.run()
  }

  private def getFileMetadata(urls:List[String], pageUrl:String, seedUrl:String, retryCount:Int):Future[List[Option[FileMetaData]]] = Source.fromIterator(() => urls.iterator)
            .mapAsyncUnordered(5)(fileUrl => FileUtils.uploadResource(fileUrl, pageUrl, retryCount, seedUrl))
            .runFold(List[Option[FileMetaData]]())(reduceMetadata)

  private def reduceMetadata = (list:List[Option[FileMetaData]], a2:Option[FileMetaData]) => list :+ a2
  private def transferFilesToS3FromUrl(payload:CrawlPayload):Future[Boolean] = {
    logger.info(s"file transfer is triggered for payload => $payload")
    val p = Promise[Boolean]()
    Future{
        val filteredUrls:Future[List[String]] = BFService.filterProcessedUrls(payload.resourceUrl, payload.urls)
        val finalResult:Future[_] = filteredUrls.flatMap(validUrls => {
          val filesMetadata:Future[List[Option[FileMetaData]]] = getFileMetadata(validUrls, payload.pageUrl.getOrElse(""), payload.resourceUrl, payload.retryCount.getOrElse(0))
          filesMetadata.flatMap(mt => {
            val metaDataList = mt.flatten
            val uploadStatus:Future[_] = if(!metaDataList.isEmpty) BFService.uploadFilesMetadata(payload.resourceUrl, metaDataList) else Future.successful()
            val completionStatus:Future[_] = if(payload.completed) BFService.markProcessComplete(payload.resourceUrl) else Future.successful()
            (uploadStatus |@| completionStatus).tupled
          })
        })
       Try(Await.result(finalResult, 15 minutes)).processTry(s"error in downloading and uploading files with payload => $payload").map(_ => p.success(true))
    }
    p.future
  }
}
