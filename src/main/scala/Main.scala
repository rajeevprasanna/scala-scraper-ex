

import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.Future

object Main extends App with LazyLogging with AppContext {
  Future{FileDownloadTrigger.startFileDownloader()}
  Future{CrawlTrigger.startCrawling()}
//  Crawler.extractFiles("https://www.box.com", ConfReader.maxCrawlDepth, ConfReader.maxNeededFiles, false)
}