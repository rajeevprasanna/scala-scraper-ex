package practice

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage._

import scala.concurrent.duration.FiniteDuration

object ex2 extends App {


  class TimedGate[A](silencePeriod:FiniteDuration) extends GraphStage[FlowShape[A, A]] {

    val in  = Inlet[A]("TimedGate.in")
    val out = Outlet[A]("TimedGate.out")

    val shape = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
          new TimerGraphStageLogic(shape) {
                var open = false

                setHandler(in, new InHandler {
                  override def onPush(): Unit = {
                    val elem = grab(in)
                    if(open) pull(in)
                    else {
                      push(out, elem)
                      open = true
                      scheduleOnce(None, silencePeriod)
                    }
                  }
                })

            setHandler(out, new OutHandler {
              override def onPull(): Unit = {
                pull(in)
              }
            })

            override protected def onTimer(timerKey: Any): Unit = {
              open = false
            }
          }

  }

}
