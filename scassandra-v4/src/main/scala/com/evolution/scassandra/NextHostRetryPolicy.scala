package com.evolution.scassandra

import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.datastax.oss.driver.api.core.config.DriverOption
import com.datastax.oss.driver.api.core.context.DriverContext
import com.datastax.oss.driver.api.core.retry.{RetryDecision, RetryPolicy}
import com.datastax.oss.driver.api.core.servererrors.{CoordinatorException, WriteType}
import com.datastax.oss.driver.api.core.session.Request

/**
 * Retries on the next node on unavailable and request errors, and on the same node on
 * timeouts, up to `advanced.retry-policy.max-retries` times.
 *
 * The driver creates the policy from the configuration:
 * {{{
 * advanced.retry-policy {
 *   class = com.evolution.scassandra.NextHostRetryPolicy
 *   max-retries = 3
 * }
 * }}}
 *
 * Note, that the driver never retries non-idempotent requests on write timeouts, aborted
 * requests and error responses, regardless of the policy.
 */
final class NextHostRetryPolicy private[scassandra] (maxRetries: Int) extends RetryPolicy {

  def this(context: DriverContext, profileName: String) = {
    this(context.getConfig.getProfile(profileName).getInt(NextHostRetryPolicy.MaxRetries))
  }

  // the deprecated methods are the abstract ones, the new ones delegate to them by default
  override def onReadTimeout(
    request: Request,
    consistencyLevel: ConsistencyLevel,
    blockFor: Int,
    received: Int,
    dataPresent: Boolean,
    retryCount: Int,
  ): RetryDecision = retrySame(retryCount)

  override def onWriteTimeout(
    request: Request,
    consistencyLevel: ConsistencyLevel,
    writeType: WriteType,
    blockFor: Int,
    received: Int,
    retryCount: Int,
  ): RetryDecision = retrySame(retryCount)

  override def onUnavailable(
    request: Request,
    consistencyLevel: ConsistencyLevel,
    required: Int,
    alive: Int,
    retryCount: Int,
  ): RetryDecision = {
    if (retryCount == 0) retryNext(retryCount)
    else retrySame(retryCount)
  }

  override def onRequestAborted(request: Request, error: Throwable, retryCount: Int): RetryDecision = {
    retryNext(retryCount)
  }

  override def onErrorResponse(
    request: Request,
    error: CoordinatorException,
    retryCount: Int,
  ): RetryDecision = retryNext(retryCount)

  override def close(): Unit = ()

  private def retrySame(retryCount: Int): RetryDecision = {
    if (retryCount < maxRetries) RetryDecision.RETRY_SAME
    else RetryDecision.RETHROW
  }

  private def retryNext(retryCount: Int): RetryDecision = {
    if (retryCount < maxRetries) RetryDecision.RETRY_NEXT
    else RetryDecision.RETHROW
  }
}

object NextHostRetryPolicy {

  /**
   * The maximum number of retries of a request
   */
  val MaxRetries: DriverOption = new DriverOption {
    override def getPath: String = "advanced.retry-policy.max-retries"
  }
}
