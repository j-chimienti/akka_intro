package intro

import akka.actor.{Actor, ActorLogging, Props}

class IoTSupervisor extends Actor with ActorLogging {
  override def receive: Receive = ???
}

object IoTSupervisor {

  def props : Props = Props(new IoTSupervisor())
}
