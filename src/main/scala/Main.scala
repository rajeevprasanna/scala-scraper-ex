

import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.Future

object Main extends App with LazyLogging with AppContext {
  Future{CrawlTrigger.startCrawling()}
  Future{FileDownloadTrigger.startFileDownloader()}
//  Crawler.extractFiles("https://www.box.com", ConfReader.maxCrawlDepth, ConfReader.maxNeededFiles, false)
}