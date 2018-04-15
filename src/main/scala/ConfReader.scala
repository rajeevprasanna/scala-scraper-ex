
import collection.JavaConverters._

object ConfReader extends AppContext {

  val blackListedURLPatterns = config.getStringList("crawl.blacklistedUrlPatterns").asScala.toSet
  val invalidExtensions = config.getStringList("crawl.invalidExtensions").asScala.toSet
  val resourceExtensions = config.getStringList("crawl.resourceExtensions").asScala.toSet
  val blackListedcountryExtensions = invalidExtensions.map("/"+_+"/") ++ blackListedURLPatterns ++ invalidExtensions.map(_+".") //checking for patterns like jp.x.com etc

  val maxCrawlDepth = config.getInt("crawl.depth")
  val maxNeededFiles = config.getInt("crawl.max_download_files")
  val maxCrawlPages = config.getInt("crawl.max_crawl_pages")

  val parallelCrawlingThreads = config.getInt("crawl.parallel_crawl_sources")

  val API_EP_FILTER_ALREADY_CRAWLED_URLS = config.getString("api.ep.filter_already_crawled_urls")
  val API_EP_SAVE_FILE_META_DATA = config.getString("api.ep.save_file_meta_data")
  val API_EP_MARK_CRAWL_COMPLETED_FOR_URL = config.getString("api.ep.mark_crawl_completed_for_url")
  val REST_API_SECRET = config.getString("api.ep.rest_api_secret")

  val REDIS_HOST = config.getString("redis.host")
  val REDIS_PORT = config.getInt("redis.port")
  val REDIS_USER_NAME = config.getString("redis.username")
  val REDIS_PASSWORD = config.getString("redis.password")
  val REDIS_CRAWL_URL_QUEUE = config.getString("redis.web_crawl_queue")
  val REDIS_RESOURCE_URL_PAYLOAD_QUEUE = config.getString("redis.resource_url_payload_queue")

  val S3_ACCESS_KEY = config.getString("s3.access_key")
  val S3_SECRET_KEY = config.getString("s3.secret_key")
  val S3_BUCKET_NAME = config.getString("s3.bucket_name")
}
