import DomUtils.logger

import scala.util.{Failure, Success, Try}

object ExtensionUtils {

  implicit class TryExtension[T](tryIn:Try[T]) {
    def processTry = (errorMsg:String) =>
      tryIn match {
        case Success(el:T) => Some(el)

        case Failure(error) =>
          logger.error(s"error => $errorMsg. error trace =>  ${error.getLocalizedMessage}")
          None
      }
  }

}
