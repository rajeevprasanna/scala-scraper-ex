import com.typesafe.scalalogging.Logger
import scala.util.{Failure, Success, Try}

object ExtensionUtils {

  implicit class TryExtension[T](tryIn:Try[T])(implicit logger: Logger) {
    def processTry = (errorMsg:String) =>
      tryIn match {
        case Success(el:T) => Some(el)

        case Failure(error) =>
          logger.error(s"error => $errorMsg. error trace =>  ${error.getStackTrace.mkString("\n")}")
          None
      }
  }

}
