
import collection.JavaConverters._

object ConfReader extends AppContext {

  val blackListedURLPatterns = config.getStringList("crawl.blacklistedUrlPatterns").asScala.toSet
  val invalidExtensions = config.getStringList("crawl.invalidExtensions").asScala.toSet
  val resourceExtensions = config.getStringList("crawl.resourceExtensions").asScala.toSet
  val blackListedcountryExtensions = invalidExtensions.map("/"+_+"/") ++ blackListedURLPatterns ++ invalidExtensions.map(_+".") //checking for patterns like jp.x.com etc

  val maxCrawlDepth = config.getInt("crawl.depth")
  val maxNeededFiles = config.getInt("crawl.max_download_files")
}
