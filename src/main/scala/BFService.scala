

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model._
import AppContext._
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{Failure, Success}
import Models._

import spray.json._
import SecureKeys._

object BFService {

  implicit  def toByteString = (s:String) => ByteString(s)

  private def payload(params:String) = HttpEntity.Strict(
    contentType = ContentTypes.`application/json`,
    data = params
  )

  def uploadFilesMetadata(sourceUrl:String, files:List[FileMetaData]):Unit = {
    import FilePersistencePayloadJsonProtocol._
    val filePersistencePayload:String = FilePersistencePayload(sourceUrl, files, BF_API_SECRET).toJson.toString
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(POST, uri = SAVE_FILE_META_DATA, entity = payload(filePersistencePayload)))
    responseFuture
      .onComplete {
        case Success(res) => println(res)
        case Failure(_)   => sys.error("something wrong")
      }
  }

  def markProcessComplete(sourceUrl:String):Unit = {
    import ProcessingCompletePayloadJsonProtocol._
    val urlProcessingCompletedPayload:String = ProcessingCompletePayload(BF_API_SECRET, List(sourceUrl)).toJson.toString
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(POST, uri = MARK_CRAWL_COMPLETED_FOR_URL, entity = payload(urlProcessingCompletedPayload)))
    responseFuture
      .onComplete {
        case Success(res) => println(res)
        case Failure(_)   => sys.error("something wrong")
      }
  }

  def filterProcessedUrls(sourceUrl:String, urls:List[String]):Future[List[String]] = {
    import ProcessedFilesPayloadJsonProtocol._
    import FilteredUrlsProtocol._

    val filterProcessedFilesPayload:String = ProcessedFilesPayload(BF_API_SECRET, urls, sourceUrl).toJson.toString
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(POST, uri = FILTER_ALREADY_CRAWLED_URLS, entity = payload(filterProcessedFilesPayload)))
    val result:Future[List[String]] = responseFuture
      .map(res => {
        val urls: List[String] = res.toJson.convertTo[FilteredUrls].urls
        urls
      })
    result
  }
}
