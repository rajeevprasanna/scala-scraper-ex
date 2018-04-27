import DomUtils.logger
import java.net.URL
import scala.util.Try

import ExtensionUtils._

object URLExtensions {

  implicit def urlToString(x:URL) = new RichURLString(x.toString)
  implicit def toURL(x:String) = {
    val validUrl = x.startsWith("http") match {
      case true => x
      case _ => "http://" + x
    }
    new URL(validUrl)
  }

  implicit class RichURLString(url:String) {

    lazy val stripQueryString =  url.split("\\?").head
    lazy val stripRouteParams =  url.split("#").headOption.getOrElse(url)
    lazy val stripQuotes = url.toCharArray.filter(ch => ch != '\'' && ch != '\"').foldLeft("")((x,y) => x + String.valueOf(y))

    lazy val hostName:String = url.getHost

    lazy val isValid:Boolean = Try(new URL(url)).toOption != None

    def formatUrl = () => url.reverse.dropWhile(ch => ch == '/' || ch == '?').reverse
    def filterSubDomainUrls(urls:List[String]):List[String] = urls.filter(u => u.domain != "" && u.domain == domain)

    lazy val rootUrl = {
      val baseUrl = url.stripQueryString.stripRouteParams
      val trimmed = baseUrl.toArray.filter(_ != '\\').reverse.dropWhile(_ == '/').reverse.mkString
      if(trimmed.getPath == "") trimmed else trimmed.splitAt(trimmed.toString.indexOf(trimmed.getPath))._1
    }

    lazy val domain = {
      val hostName = url.getHost
      val dotCount = hostName.chars.filter(_ == '.').count
      dotCount match {
        case 0 => ""
        case 1 => hostName
        case _  =>  hostName.split("\\.").tail.reduce(_+"."+_)
      }
    }

    import org.apache.commons.io.FilenameUtils
    lazy val stripResourceComponent = {
      val lastPathComponent = FilenameUtils.getName(url.getPath())
      val res = if(lastPathComponent.contains(".")) url.split(lastPathComponent).head  else url
      if(res.last == '/') res else res + "/"
    }

    lazy val domainCountryExtension =
      Try {
        val hostName = url.getHost
        val lastIndex = hostName.lastIndexOf(".")
        hostName.substring(lastIndex+1).toLowerCase()
      }.processTry(s"Got error in extracting country domain. u => $url").getOrElse("")

    private def setOr = (s:Set[Boolean]) => s.reduce(_ || _)
    lazy val isBlackListedUrlPattern = setOr(ConfReader.blacklistedCountryPrefixes.map(hostName.startsWith(_))) ||
                                          setOr(ConfReader.blacklistedCountrySuffixes.map(hostName.endsWith(_))) ||
                                          setOr(ConfReader.blackListedPatterns.map(url.contains(_)))


    lazy val extension =
      Try(url.getPath).processTry(s"error in getUrlExtension. url => $url") match {
        case Some(path) =>
          val lastIndex = path.lastIndexOf(".")
          if(lastIndex == -1) "" else path.substring(lastIndex).toLowerCase

        case None =>
          logger.error(s"url parsing failed while getting extension. url => $url")
          ".invalid"
      }

    lazy val isPdfUrl = url.extension == ".pdf"
    lazy val isResourceUrl = ConfReader.resourceExtensions.contains(extension)
  }

}
