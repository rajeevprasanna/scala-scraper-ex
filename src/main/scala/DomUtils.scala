import java.net.URL

import Test.options
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import scala.concurrent.duration._
import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import io.github.bonigarcia.wdm.ChromeDriverManager
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

import scala.collection.mutable
import scala.util.Try

object DomUtils {

  val browser = JsoupBrowser()


  ChromeDriverManager.getInstance().setup()
  val chromePrefs:mutable.Map[String, Any] = mutable.Map[String, Any]()
  chromePrefs.put("profile.default_content_settings.popups", 0)
  chromePrefs.put("download.default_directory", ".")
  chromePrefs.put("Browser.setDownloadBehavior", "allow")

  val options = new ChromeOptions()
  options.setExperimentalOption("prefs", chromePrefs)
  options.addArguments("--disable-extensions") //to disable browser extension popup
  options.addArguments("test-type")
  options.addArguments("disable-popup-blocking")

  options.setHeadless(true)

  val MAX_RETRY_COUNT = 5

  def fetchDocument(url:String, hitCount:Int = 0, isAjax:Boolean = false):Option[browser.DocumentType] = {
    val logMessage = if(hitCount == 0)  s"Fetching URL => $url" else s"Retrying fetch ${hitCount}th time for url => $url"
    println(logMessage)

    val resp = isAjax match {
      case true =>
        Try {
          val driver = new ChromeDriver(options)
          driver.get(url)
          val dom = driver.getPageSource()
          parseString(dom)
        }.toOption

      case false =>  Try(browser.get(url)).toOption
    }

    resp match {
      case None if hitCount >= MAX_RETRY_COUNT => fetchDocument(url, hitCount + 1)
      case _ => resp
    }
  }

  def parseString(dom:String):browser.DocumentType = browser.parseString(dom)

  def getUrlsFromDoc(doc:browser.DocumentType):List[String] = {
      val aDoms = doc >> elementList("a")
      aDoms.flatMap(e => Try(e.attrs("href")).toOption).filter(_ != null)
  }

  def fetchRoot(fullURL:String):String = {
    val formatted = checkForProtocol(fullURL)
    val url = removeQueryString(formatted)
    val trimmed = url.toArray.reverse.dropWhile(_ == '/').reverse.mkString
    val url2 = new URL(trimmed)
    if(url2.getPath == "") trimmed else trimmed.split(url2.getPath).head
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
    })._1


  def extractResourceUrls(urls:List[String]):(List[String], List[String]) = urls.partition(url => {
    val extension = getUrlExtension(url)
    val resourceExtensions = Set(".pdf", ".doc", "gif", "gif", "jpg", "jpg", "png", "png", "ico", "ico", "css", "css", "sit", "sit", "eps", "eps", "wmf", "wmf", "zip", "zip", "ppt", "ppt", "mpg", "mpg", "xls", "xls", "gz", "gz", "rpm", "rpm", "tgz", "tgz", "mov", "mov", "exe", "exe", "jpeg", "jpeg", "bmp", "bmp", "js", "js", "mp4", "mp3", ".invalid")
    resourceExtensions.contains(extension)
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


  def formatUrls(sourceUrl:String, urls:List[String]):List[String] = {
    val rootUrl = fetchRoot(sourceUrl)
    val formattedSourceUrl = removeQueryString(sourceUrl)
    urls.flatMap(url => {
      val formatted =
      url.trim match {
        case x if x == "/" => None
        case x if x.startsWith("//") => None
        case x if x.startsWith("/")  => Some(rootUrl + x)
        case x if x.startsWith("./")  => Some(rootUrl + x.tail)
        case x if x.startsWith("http") => Some(x)
        case x if x.startsWith("#") => None
        case _ => Some(formattedSourceUrl + "/" + url)
      }
//      println(s"given url => ${url.trim} and formatted url => ${formatted} and sourceUrl => $sourceUrl")
      formatted.map(u => u.replaceAll(" ", "%20"))
    }).distinct
  }

  def getUrlExtension(url:String):String = {
    Try(new URL(url).getPath).toOption match {
      case Some(path) =>
        val lastIndex = path.lastIndexOf(".")
        if(lastIndex == -1) "" else path.substring(lastIndex).toLowerCase()

      case None =>
        println(s"url parsing failed while getting extension. url => $url")
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
          val allUrlsList:List[List[String]] = urls.flatMap(url => DomUtils.fetchDocument(url).map(getUrlsFromDoc(_)))
          allUrlsList.map(_.distinct).flatten.groupBy(identity).toList.filter(_._2.length >= 4).map(_._1)

    case _ => Nil
  }
}
