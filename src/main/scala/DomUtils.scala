import java.net.URL

import com.typesafe.scalalogging.Logger
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import io.github.bonigarcia.wdm.ChromeDriverManager
import org.apache.commons.io.FilenameUtils
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.Try

object DomUtils {

  val logger = Logger(LoggerFactory.getLogger("DomUtils"))
  ChromeDriverManager.getInstance().setup()

  val MAX_RETRY_COUNT = 3
  val URL_PATTERN_REGEX =  """(["'])https?:\/\/(.*?)(["'])""".r

  def extractOutLinks(url:String, isAjax:Boolean, retryCount:Int = 0):List[String] = {

    lazy val browser = JsoupBrowser()

    def fetchDocument(hitCount:Int):Option[browser.DocumentType] = {
      val logMessage = if(hitCount == 0)  s"Fetching URL => $url" else s"Retrying fetch ${hitCount}th time for url => $url"
      logger.info(logMessage)

      val extension = getUrlExtension(url)
      if(ConfReader.resourceExtensions.contains(extension)){
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
          }.toOption

        case false =>  Try(browser.get(url)).toOption
      }

      resp match {
        case None if hitCount >= MAX_RETRY_COUNT => fetchDocument(hitCount + 1)
        case _ => resp
      }
    }

    def parseString(dom:String):browser.DocumentType = browser.parseString(dom)

    def stripQuotes = (url:String) => url.toCharArray.filter(ch => ch != '\'' && ch != '\"').foldLeft("")((x,y) => x + String.valueOf(y))

    def getUrlsFromDoc(doc:browser.DocumentType):List[String] = {
      val aDoms = Try(doc >> elementList("a")).toOption.getOrElse(Nil)
      val hrefUrls = aDoms.flatMap(e => Try(e.attrs("href")).toOption).filter(_ != null)
      val allUrls = Try(URL_PATTERN_REGEX.findAllMatchIn(doc.toString).toList.map(_.toString()).map(stripQuotes)).toOption.getOrElse(Nil)
      (hrefUrls ++ allUrls).distinct
    }
    val doc = fetchDocument(retryCount)
    doc.map(getUrlsFromDoc(_)).getOrElse(Nil)
  }



  def fetchRoot(fullURL:String):String = {
    val formatted = checkForProtocol(fullURL)
    val url = removeQueryString(formatted)
    val trimmed = url.toArray.filter(_ != '\\').reverse.dropWhile(_ == '/').reverse.mkString
    val url2 = new URL(trimmed)
    if(url2.getPath == "") trimmed else trimmed.splitAt(trimmed.indexOf(url2.getPath))._1
  }

  def checkForProtocol(url:String):String = url.startsWith("http") match {
    case true => url
    case _ => "http://" + url
  }

  def removeQueryString(url:String):String = {
    val formatted = checkForProtocol(url)
    formatted.split("\\?").head
  }

  def filterPDFUrls(urls:List[String]):List[String] =
    urls.partition(url => {
      val extension = getUrlExtension(url)
      extension == ".pdf"
    })._1.map(removeQueryString(_))


  def extractResourceUrls(urls:List[String]):(List[String], List[String]) = urls.partition(url => {
    val extension = getUrlExtension(url)
    ConfReader.resourceExtensions.contains(extension)
  })

  def filterSubdomainUrls(resourceURL:String, urls:List[String]):List[String] = {
    val resourceDomain = getDomain(resourceURL)
    urls.filter(url => {
      val d = getDomain(url)
      d != "" && d == resourceDomain
    })
  }

  def getDomain(url:String) = {
    val hostName = new URL(url).getHost
    val dotCount = hostName.chars.filter(_ == '.').count
    dotCount match {
      case 0 => ""
      case 1 => hostName
      case _  =>  hostName.split("\\.").tail.reduce(_+"."+_)
    }
  }

  def removeResourceComponent(url: String): String = {
    val lastPathComponent = FilenameUtils.getName(new URL(url).getPath())
    val res = if(lastPathComponent.contains(".")) url.split(lastPathComponent).head  else url
    if(res.last == '/') res else res + "/"
  }

  def formatUrls(sourceUrl:String, urls:List[String]):List[String] = {
    val rootUrl = fetchRoot(sourceUrl)
    val formattedSourceUrl = removeQueryString(sourceUrl)
    val res =
    urls.flatMap(url => {
      val formatted =
      url.trim match {
        case x if x == "/" => None
        case x if x.contains("..") => Try(rootUrl + x.split("\\.\\.").last).toOption //happens with '..' //TODO: improve this by traversing line
        case x if x.startsWith("//") => None
        case x if x.startsWith("/")  => Some(rootUrl + x)
        case x if x.startsWith("./")  => Some(rootUrl + x.tail)
        case x if x.startsWith("http") => Some(x)
        case x if x.startsWith("#") => None
        case _ => Some(removeResourceComponent(formattedSourceUrl)  + url)
      }
//      println(s"given url => ${url.trim} and formatted url => ${formatted} and sourceUrl => $sourceUrl")
      formatted.map(u => u.replaceAll(" ", "%20")).map(formatUrl)
    }).distinct
    filterOtherLangUrls(res)
  }

  def formatUrl = (url:String) => url.reverse.dropWhile(ch => ch == '/' || ch == '?').reverse

  def filterOtherLangUrls(urls:List[String]):List[String] = {

    def getDomainCountryExtension(u:String):String = {
      Try {
        val hostName = new URL(u).getHost
        val lastIndex = hostName.lastIndexOf(".")
        hostName.substring(lastIndex+1).toLowerCase()
      }.toOption.getOrElse("")
    }

    def getCountryRoute(u:String):String = {
      Try{
        val path = new URL(u).getPath
        if(path.contains("/")) path.split("/").filter(_ != "").headOption.getOrElse("") else ""
      }.getOrElse("")
    }


    def containsBlackListedUrl = (url:String) => ConfReader.blackListedcountryExtensions.map(url.contains(_)).collectFirst({case x if x == true => x}).getOrElse(false)
    urls.filter(url => !ConfReader.invalidExtensions.contains(getDomainCountryExtension(url)) && !ConfReader.invalidExtensions.contains(getCountryRoute(url)) && !containsBlackListedUrl(url))
  }

  def getUrlExtension(url:String):String = {
    Try(new URL(url).getPath).toOption match {
      case Some(path) =>
        val lastIndex = path.lastIndexOf(".")
        if(lastIndex == -1) "" else path.substring(lastIndex).toLowerCase()

      case None =>
        logger.error(s"url parsing failed while getting extension. url => $url")
        ".invalid"
    }
  }

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
