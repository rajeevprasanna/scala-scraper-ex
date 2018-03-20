import DomUtils.browser

import scala.collection.mutable

object Test //extends App
{

  import io.github.bonigarcia.wdm.ChromeDriverManager
  import org.openqa.selenium.chrome.ChromeDriver
  import org.openqa.selenium.chrome.ChromeOptions

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
  //        val capabilities = DesiredCapabilities.chrome()
  //        capabilities.setCapability(ChromeOptions.CAPABILITY, options)

  val driver = new ChromeDriver(options)
  driver.get("https://www.citrix.com/products/netscaler-adc/resources/")
//  driver.wait(20 * 1000)

  val dom = driver.getPageSource()
  def parseString(dom:String):browser.DocumentType = browser.parseString(dom)

  val allHrefs = DomUtils.getUrlsFromDoc(parseString(dom))
  val formattedUrls = DomUtils.formatUrls("https://www.citrix.com/products/netscaler-adc/resources/", allHrefs)

  val (resourceUrls, htmlUrls) = DomUtils.extractResourceUrls(formattedUrls)
  println(resourceUrls)

  driver.close()
}
