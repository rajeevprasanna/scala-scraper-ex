

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model._
import akka.util.ByteString

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}
import Models._
import spray.json._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import FilePersistencePayloadJsonProtocol._
import ProcessingCompletePayloadJsonProtocol._
import FilteredUrlsProtocol._
import ProcessedFilesPayloadJsonProtocol._

object BFService extends AppContext {
  implicit val logger = Logger(LoggerFactory.getLogger("BFService"))
  implicit  def toByteString = (s:String) => ByteString(s)

  private def payload(params:String) = HttpEntity.Strict(
    contentType = ContentTypes.`application/json`,
    data = params
  )

  def responseHandler = (p:Promise[Unit], f:Try[HttpResponse]) =>
    f match {
      case Success(res) => logger.debug(res.toString); p.success(Unit)
      case Failure(ex)   => logger.error(s"something wrong. error => ${ex.getLocalizedMessage}"); p.failure(ex)
    }


  def uploadFilesMetadata(sourceUrl:String, files:List[FileMetaData]):Future[Unit] = {
    logger.info(s"uploading file metadata => $files")
    val p = Promise[Unit]()
    val filePersistencePayload:String = FilePersistencePayload(sourceUrl, files, ConfReader.REST_API_SECRET).toJson.toString
    Http().singleRequest(HttpRequest(POST, uri = ConfReader.API_EP_SAVE_FILE_META_DATA, entity = payload(filePersistencePayload))).onComplete(responseHandler(p, _))
    p.future
  }

  def markProcessComplete(sourceUrl:String):Future[Unit] = {
    logger.info(s"marking processing completion for url => $sourceUrl")
    val p = Promise[Unit]()
    val urlProcessingCompletedPayload:String = ProcessingCompletePayload(ConfReader.REST_API_SECRET, List(sourceUrl)).toJson.toString
    Http().singleRequest(HttpRequest(POST, uri = ConfReader.API_EP_MARK_CRAWL_COMPLETED_FOR_URL, entity = payload(urlProcessingCompletedPayload))).onComplete(responseHandler(p, _))
    p.future
  }

  private def respToString:(ResponseEntity => Future[String]) = (entity:ResponseEntity) => entity.dataBytes.runFold(ByteString.empty) { case (acc, b) => acc ++ b }.map(_.utf8String)
  private def extractUrlList:(ResponseEntity => Future[List[String]]) = (resp:ResponseEntity) => respToString(resp).map(JsonParser(_).convertTo[FilteredUrls].urls)

  def filterProcessedUrls(sourceUrl:String, urls:List[String]):Future[List[String]] = {
    logger.info(s"filter processing for urls => $urls")
    val p = Promise[List[String]]()

    urls match {
      case _ if urls.isEmpty => p.success(Nil)
      case _ =>
          val filterProcessedFilesPayload:String = ProcessedFilesPayload(ConfReader.REST_API_SECRET, urls, sourceUrl).toJson.toString
          val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(POST, uri = ConfReader.API_EP_FILTER_ALREADY_CRAWLED_URLS, entity = payload(filterProcessedFilesPayload)))
          responseFuture.onComplete {
            case Success(res) => extractUrlList(res.entity).map(urls => p.success(urls))
            case Failure(ex) => logger.error(s"something wrong. error => ${ex.getLocalizedMessage}"); p.failure(ex)
          }
    }
    p.future
  }
}
