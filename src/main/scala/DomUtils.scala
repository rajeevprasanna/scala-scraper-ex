import com.typesafe.scalalogging.Logger
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import io.github.bonigarcia.wdm.ChromeDriverManager
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.Try

import ExtensionUtils._
import URLExtensions._


object DomUtils {

  val logger = Logger(LoggerFactory.getLogger("DomUtils"))
  ChromeDriverManager.getInstance().setup()


  def extractOutLinks(url:String, isAjax:Boolean, retryCount:Int = 0):List[String] = {

    lazy val browser = JsoupBrowser()

    def fetchDocument(hitCount:Int):Option[browser.DocumentType] = {
      val logMessage = if(hitCount == 0)  s"Fetching URL => $url" else s"Retrying fetch ${hitCount}th time for url => $url"
      logger.debug(logMessage)

      if(ConfReader.resourceExtensions.contains(url.extension)){
        return None
      }

      val resp = isAjax match {
        case true =>
          Try {
            lazy val driver = new ChromeDriver(getChromeOptions()) //Using singleton instance
            driver.get(url)
            val dom = driver.getPageSource()
            driver.quit()
            parseString(dom)
          }.processTry(s"Error in getting ajax page source with url => $url")

        case false =>  Try(browser.get(url)).processTry(s"Error in getting page source with url => $url")
      }

      resp match {
        case None if hitCount >= ConfReader.maxRetryCount => fetchDocument(hitCount + 1)
        case _ => resp
      }
    }

    def parseString(dom:String):browser.DocumentType = browser.parseString(dom)

    def getUrlsFromDoc(doc:browser.DocumentType):List[String] = {
      val aDoms = Try(doc >> elementList("a")).getOrElse(Nil) //not logging error because there are different content types like xml which are not parsable. so suppressing error
      val hrefUrls = aDoms.flatMap(e => Try(e.attrs("href")).toOption).filter(_ != null)
      val allUrls = Try(ConfReader.urlPatternRegex.findAllMatchIn(doc.toString).toList.map(_.toString().stripQuotes)).processTry(s"Error in finding matches of regular expression with document => ${doc.toString}").getOrElse(Nil)
      (hrefUrls ++ allUrls).distinct
    }
    val doc = fetchDocument(retryCount)
    doc.map(getUrlsFromDoc(_)).getOrElse(Nil)
  }


  def filterPDFUrls(urls:List[String]):List[String] =  urls.partition(_.isPdfUrl)._1.map(_.stripQueryString)
  def extractResourceUrls(urls:List[String]):(List[String], List[String]) = urls.partition(_.isResourceUrl)


  def formatUrls(sourceUrl:String, urls:List[String]):List[String] = {
    val res =
    urls.flatMap(url => {
      val formatted =
      url.trim match {
        case x if x == "/" => None
        case x if x.contains("..") => Try(sourceUrl.rootUrl + x.split("\\.\\.").last).processTry(s"error in splitting url => $sourceUrl.rootUrl and x => $x") //happens with '..' //TODO: improve this by traversing line
        case x if x.startsWith("//") => None
        case x if x.startsWith("/")  => Some(sourceUrl.rootUrl + x)
        case x if x.startsWith("./")  => Some(sourceUrl.rootUrl + x.tail)
        case x if x.startsWith("http") => Some(x)
        case x if x.startsWith("#") => None
        case _ => Some(sourceUrl.stripRouteParams.stripQueryString.stripResourceComponent  + url)
      }
      formatted.map(u => u.replaceAll(" ", "%20").formatUrl())
    }).distinct
    filterOtherLangUrls(res)
  }

  def filterOtherLangUrls(urls:List[String]):List[String] =
    urls.filter(url => !ConfReader.invalidExtensions.contains(url.domainCountryExtension) && !ConfReader.invalidExtensions.contains(url.countryRoute) && !url.isBlackListedUrlPattern)



  def randomSampleUrls(n:Int, urls:List[String]):List[String] = {
    import scala.util.Random
    val maxSamples = Math.min(n, urls.length)
    Random.shuffle(urls).take(maxSamples)
  }

  def getCommonTemplateUrls(urls:List[String]):List[String] = urls.length match {
    case x if x > 4 =>
          val allUrlsList:List[List[String]] = urls.map(url => DomUtils.extractOutLinks(url, false))
          allUrlsList.map(_.distinct).flatten.groupBy(identity).toList.filter(_._2.length >= 4).map(_._1)

    case _ => Nil
  }

  private def getChromeOptions():ChromeOptions = {
    val chromePrefs:mutable.Map[String, Any] = mutable.Map[String, Any]()
    chromePrefs.put("profile.default_content_settings.popups", 0)
    chromePrefs.put("download.default_directory", ".")
    chromePrefs.put("Browser.setDownloadBehavior", "allow")

    val options = new ChromeOptions()
    options.setExperimentalOption("prefs", chromePrefs)
    options.addArguments("--disable-extensions") //to disable browser extension popup
    options.addArguments("test-type")
    options.addArguments("disable-popup-blocking")
    options.addArguments("disable-infobars")
    options.addArguments("--disable-gpu")
    options.setHeadless(true)
    options
  }
}
