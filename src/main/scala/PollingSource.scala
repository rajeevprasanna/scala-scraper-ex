
import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, TimerGraphStageLogic}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.util.Try

class PollingSource[T](generator: () => Future[T], pollInterval:FiniteDuration) extends GraphStage[SourceShape[T]] with AppContext {

  val out : Outlet[T] = Outlet("pollingPayload")

  override val shape:SourceShape[T] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) {

      val buffer = new ListBuffer[T]()
      var needElementInDownStream = false

      setHandler(out, new OutHandler {
        override def onPull(): Unit =
          {
            needElementInDownStream = true
            if(!buffer.isEmpty){
              pushHead()
            } else {
              pullOrPush()
            }
          }
      })

      def pullOrPush(): Unit ={
          Try(Await.result(doPoll(), 2 seconds))
          if(!buffer.isEmpty){
            pushHead()
          } else {
            schedulePoll()
          }
      }

      def schedulePoll(): Unit = {
        scheduleOnce("poll", pollInterval)
      }

      def pushHead(): Unit = {
        buffer.headOption match {
          case Some(h)  =>    buffer.remove(0)
                              push(out, h)

          case _ => //ignore
        }
      }

      def doPoll(): Future[Unit] = {
        val p = Promise[Unit]()
        if(needElementInDownStream){
          generator().map(el => {
            buffer.append(el)
            needElementInDownStream = false
            p.success()
          })
        }else{
          p.success()
        }
        p.future
      }

      override def onTimer(timerKey: Any): Unit = {
        if(!isClosed(out)){
          pullOrPush()
        }
      }
    }
}