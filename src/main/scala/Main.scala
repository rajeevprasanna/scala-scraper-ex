

import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.Future

object Main extends App with LazyLogging with AppContext {
  Future{CrawlTrigger.startCrawling()}
  Future{FileDownloadTrigger.startFileDownloader()}
}