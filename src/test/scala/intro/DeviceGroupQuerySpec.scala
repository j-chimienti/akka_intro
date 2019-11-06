package intro

/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */



import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import intro.DeviceGroupQuery._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class DeviceGroupQuerySpec()
  extends TestKit(ActorSystem("DeviceGroupTest"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  //#implicit-sender

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "DeviceGroupQuery" must {

    //#query-test-normal
    "return temperature value for working devices" in {
      val requester = TestProbe()

      val device1 = TestProbe()
      val device2 = TestProbe()

      val queryActor = system.actorOf(
        DeviceGroupQuery.props(
          actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
          requestId = 1,
          requester = requester.ref,
          timeout = 3.seconds))

      device1.expectMsg(Device.ReadTemperature(requestId = 1))
      device2.expectMsg(Device.ReadTemperature(requestId = 1))

      queryActor.tell(Device.RespondTemperature(requestId = 1, Some(1.0)), device1.ref)
      queryActor.tell(Device.RespondTemperature(requestId = 1, Some(2.0)), device2.ref)

      requester.expectMsg(
        RespondAllTemperatures(
          requestId = 1,
          temperatures = Map("device1" -> Temperature(1.0), "device2" -> Temperature(2.0))))
    }
    //#query-test-normal

    //#query-test-no-reading
    "return TemperatureNotAvailable for devices with no readings" in {
      val requester = TestProbe()

      val device1 = TestProbe()
      val device2 = TestProbe()

      val queryActor = system.actorOf(
        DeviceGroupQuery.props(
          actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
          requestId = 1,
          requester = requester.ref,
          timeout = 3.seconds))

      device1.expectMsg(Device.ReadTemperature(requestId = 1))
      device2.expectMsg(Device.ReadTemperature(requestId = 1))

      queryActor.tell(Device.RespondTemperature(requestId = 1, None), device1.ref)
      queryActor.tell(Device.RespondTemperature(requestId = 1, Some(2.0)), device2.ref)

      requester.expectMsg(
        RespondAllTemperatures(
          requestId = 1,
          temperatures =
            Map("device1" -> TemperatureNotAvailable, "device2" -> Temperature(2.0))))
    }
    //#query-test-no-reading

    //#query-test-stopped
    "return DeviceNotAvailable if device stops before answering" in {
      val requester = TestProbe()

      val device1 = TestProbe()
      val device2 = TestProbe()

      val queryActor = system.actorOf(
        DeviceGroupQuery.props(
          actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
          requestId = 1,
          requester = requester.ref,
          timeout = 3.seconds))

      device1.expectMsg(Device.ReadTemperature(requestId = 1))
      device2.expectMsg(Device.ReadTemperature(requestId = 1))

      queryActor.tell(Device.RespondTemperature(requestId = 1, Some(1.0)), device1.ref)
      device2.ref ! PoisonPill

      requester.expectMsg(
        RespondAllTemperatures(
          requestId = 1,
          temperatures = Map("device1" -> Temperature(1.0), "device2" -> DeviceNotAvailable)))
    }
    //#query-test-stopped

    //#query-test-stopped-later
    "return temperature reading even if device stops after answering" in {
      val requester = TestProbe()

      val device1 = TestProbe()
      val device2 = TestProbe()

      val queryActor = system.actorOf(
        DeviceGroupQuery.props(
          actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
          requestId = 1,
          requester = requester.ref,
          timeout = 3.seconds))

      device1.expectMsg(Device.ReadTemperature(requestId = 1))
      device2.expectMsg(Device.ReadTemperature(requestId = 1))

      queryActor.tell(Device.RespondTemperature(requestId = 1, Some(1.0)), device1.ref)
      queryActor.tell(Device.RespondTemperature(requestId = 1, Some(2.0)), device2.ref)
      device2.ref ! PoisonPill

      requester.expectMsg(
        RespondAllTemperatures(
          requestId = 1,
          temperatures = Map("device1" -> Temperature(1.0), "device2" -> Temperature(2.0))))
    }
    //#query-test-stopped-later

    //#query-test-timeout
    "return DeviceTimedOut if device does not answer in time" in {
      val requester = TestProbe()

      val device1 = TestProbe()
      val device2 = TestProbe()

      val queryActor = system.actorOf(
        DeviceGroupQuery.props(
          actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
          requestId = 1,
          requester = requester.ref,
          timeout = 1.second))

      device1.expectMsg(Device.ReadTemperature(requestId = 1))
      device2.expectMsg(Device.ReadTemperature(requestId = 1))

      queryActor.tell(Device.RespondTemperature(requestId = 1, Some(1.0)), device1.ref)

      requester.expectMsg(
        RespondAllTemperatures(
          requestId = 1,
          temperatures = Map("device1" -> Temperature(1.0), "device2" -> DeviceTimedOut)))
    }
    //#query-test-timeout

  }

}