package intro

import akka.actor._

class DeviceManager extends Actor with ActorLogging {

  import DeviceManager._

  var groupIdToActor : Map[String, ActorRef] = Map.empty[String, ActorRef]
  def actorToGroupId : Map[ActorRef, String] = groupIdToActor.map(_.swap)
  override def receive: Receive = {

    case trackMsg @ RequestTrackDevice(groupId, deviceId) =>
      groupIdToActor.get(groupId) match {
        case None =>
          val deviceGroup = context.actorOf(DeviceGroup.props(groupId), s"device-group-$groupId")
          groupIdToActor += groupId -> deviceGroup
          context watch deviceGroup
          deviceGroup forward trackMsg
        case Some(deviceGroup) =>
          deviceGroup forward trackMsg
      }
    case Terminated(actor) =>
      log.info(s"Device group actor terminated ${actor.path.name}")
      actorToGroupId.get(actor) map(groupId => groupIdToActor -= groupId)
  }
}


object DeviceManager {

  case class RequestTrackDevice(groupId: String, deviceId: String)
  case object DeviceRegistered
}