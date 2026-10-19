package com.evolution.scassandra.compat

import com.evolution.scassandra.compat.FirstContactPointDcLoadBalancingPolicy.{
  ContactPoint,
  HostPort,
  LocalDc,
  localDcOf,
  warningOf,
}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class FirstContactPointDcLoadBalancingPolicySpec extends AnyFunSuite with Matchers {

  private val host1 = HostPort("host1", 9042)
  private val host2 = HostPort("host2", 9042)
  private val host3 = HostPort("host3", 9042)

  private val dc1 = ContactPoint(host1, Some("dc1"))
  private val dc2 = ContactPoint(host2, Some("dc2"))
  private val unknown = ContactPoint(host3, None)

  test("local DC is the DC of the first configured contact point, regardless of the order of the driver") {
    localDcOf(List(dc2, dc1), List(host1, host2)).map(_.name) shouldEqual Some("dc1")
    localDcOf(List(dc1, dc2), List(host2, host1)).map(_.name) shouldEqual Some("dc2")
  }

  test("single DC is not reported") {
    val localDc = localDcOf(List(dc1, dc1.copy(hostPort = host2)), List(host1, host2))

    localDc.flatMap(warningOf) shouldEqual None
  }

  test("contact points in other DCs are reported") {
    val localDc = localDcOf(List(dc2, dc1), List(host1, host2))

    localDc shouldEqual Some(LocalDc("dc1", dc1, configured = true, skipped = Nil, otherDcs = List(dc2)))
    localDc.flatMap(warningOf).getOrElse("") should include("contact points in other DCs: host2:9042 (dc2)")
  }

  test("configured contact points with an unknown DC are skipped and reported") {
    val localDc = localDcOf(List(dc1, unknown), List(host3, host1))

    localDc shouldEqual Some(LocalDc("dc1", dc1, configured = true, skipped = List(unknown), otherDcs = Nil))
    localDc.flatMap(warningOf).getOrElse("") should
      include("the DC of the contact points before it is unknown")
  }

  test("addresses of the same configured contact point with an unknown DC are not reported") {
    val sameHost = ContactPoint(host1, None)
    val localDc = localDcOf(List(sameHost, dc1), List(host1))

    localDc.map(_.name) shouldEqual Some("dc1")
    localDc.flatMap(warningOf) shouldEqual None
  }

  test("contact points not matching the configuration go last and are reported") {
    val localDc = localDcOf(List(dc2, dc1), List(host1))

    localDc.map(_.name) shouldEqual Some("dc1")
    localDcOf(List(dc2), List(host1)).flatMap(warningOf).getOrElse("") should include("it is not found in")
  }

  test("no local DC if no contact point has a known DC") {
    localDcOf(List(unknown), List(host3)) shouldEqual None
  }
}
