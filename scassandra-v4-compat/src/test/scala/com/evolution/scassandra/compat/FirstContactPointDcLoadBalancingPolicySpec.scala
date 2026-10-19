package com.evolution.scassandra.compat

import com.evolution.scassandra.compat.FirstContactPointDcLoadBalancingPolicy.{
  ContactPoint,
  LocalDc,
  localDcOf,
}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.net.InetSocketAddress

class FirstContactPointDcLoadBalancingPolicySpec extends AnyFunSuite with Matchers {

  private val address1 = InetSocketAddress.createUnresolved("host1", 9042)
  private val address2 = InetSocketAddress.createUnresolved("host2", 9042)
  private val address3 = InetSocketAddress.createUnresolved("host3", 9042)

  private val dc1 = ContactPoint(address1, Some("dc1"))
  private val dc2 = ContactPoint(address2, Some("dc2"))
  private val unknown = ContactPoint(address3, None)

  test("local DC is the DC of the first configured contact point, regardless of the order of the driver") {
    localDcOf(List(dc2, dc1), List(address1, address2)) shouldEqual Some(LocalDc("dc1", List(dc2)))
    localDcOf(List(dc1, dc2), List(address2, address1)) shouldEqual Some(LocalDc("dc2", List(dc1)))
  }

  test("contact points with an unknown DC are skipped, and not reported as in other DCs") {
    localDcOf(List(unknown, dc1), List(address3, address1)) shouldEqual Some(LocalDc("dc1", Nil))
  }

  test("contact points missing from the configuration go last") {
    localDcOf(List(dc2, dc1), List(address1)) shouldEqual Some(LocalDc("dc1", List(dc2)))
  }

  test("no local DC if no contact point has a known DC") {
    localDcOf(List(unknown), List(address3)) shouldEqual None
  }
}
