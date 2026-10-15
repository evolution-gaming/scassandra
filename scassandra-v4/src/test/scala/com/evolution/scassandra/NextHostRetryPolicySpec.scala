package com.evolution.scassandra

import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.retry.RetryDecision
import com.datastax.oss.driver.api.core.servererrors.{OverloadedException, WriteType}
import com.datastax.oss.driver.api.core.{DefaultConsistencyLevel, DriverTimeoutException}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.annotation.nowarn

@nowarn("cat=deprecation")
class NextHostRetryPolicySpec extends AnyFunSuite with Matchers {

  private val policy = new NextHostRetryPolicy(maxRetries = 2)
  private val request = SimpleStatement.newInstance("SELECT 1")
  private val consistency = DefaultConsistencyLevel.ONE

  test("read timeout is retried on the same node") {
    policy.onReadTimeout(request, consistency, 1, 0, false, 0) shouldEqual RetryDecision.RETRY_SAME
    policy.onReadTimeout(request, consistency, 1, 0, false, 1) shouldEqual RetryDecision.RETRY_SAME
    policy.onReadTimeout(request, consistency, 1, 0, false, 2) shouldEqual RetryDecision.RETHROW
  }

  test("write timeout is retried on the same node") {
    policy.onWriteTimeout(request, consistency, WriteType.SIMPLE, 1, 0, 0) shouldEqual
      RetryDecision.RETRY_SAME
    policy.onWriteTimeout(request, consistency, WriteType.SIMPLE, 1, 0, 2) shouldEqual RetryDecision.RETHROW
  }

  test("unavailable is retried on the next node first, then on the same node") {
    policy.onUnavailable(request, consistency, 1, 0, 0) shouldEqual RetryDecision.RETRY_NEXT
    policy.onUnavailable(request, consistency, 1, 0, 1) shouldEqual RetryDecision.RETRY_SAME
    policy.onUnavailable(request, consistency, 1, 0, 2) shouldEqual RetryDecision.RETHROW
  }

  test("aborted request is retried on the next node") {
    val error = new DriverTimeoutException("timeout")
    policy.onRequestAborted(request, error, 0) shouldEqual RetryDecision.RETRY_NEXT
    policy.onRequestAborted(request, error, 2) shouldEqual RetryDecision.RETHROW
  }

  test("error response is retried on the next node") {
    val error = new OverloadedException(null)
    policy.onErrorResponse(request, error, 0) shouldEqual RetryDecision.RETRY_NEXT
    policy.onErrorResponse(request, error, 2) shouldEqual RetryDecision.RETHROW
  }
}
