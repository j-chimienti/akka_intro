package intro

import akka.actor.ActorSystem

object IoTApp {

  val system = ActorSystem()
  val ioTSupervisor = system.actorOf(IoTSupervisor.props, "IoTSupervisor")

}
