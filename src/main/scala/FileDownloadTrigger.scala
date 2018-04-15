
import Models.FileMetaData
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

object FileDownloadTrigger extends AppContext {

  val logger = Logger(LoggerFactory.getLogger("FileDownloadTrigger"))

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

}
