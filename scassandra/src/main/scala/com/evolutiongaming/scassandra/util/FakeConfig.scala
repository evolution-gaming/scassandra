package com.evolutiongaming.scassandra.util

import com.datastax.oss.driver.api.core.ProtocolVersion
import com.datastax.oss.driver.api.core.`type`.codec.registry.CodecRegistry
import com.datastax.oss.driver.api.core.addresstranslation.AddressTranslator
import com.datastax.oss.driver.api.core.auth.AuthProvider
import com.datastax.oss.driver.api.core.config.{DriverConfig, DriverConfigLoader, DriverExecutionProfile, DriverOption}
import com.datastax.oss.driver.api.core.connection.ReconnectionPolicy
import com.datastax.oss.driver.api.core.context.DriverContext
import com.datastax.oss.driver.api.core.loadbalancing.LoadBalancingPolicy
import com.datastax.oss.driver.api.core.metadata.NodeStateListener
import com.datastax.oss.driver.api.core.metadata.schema.SchemaChangeListener
import com.datastax.oss.driver.api.core.retry.RetryPolicy
import com.datastax.oss.driver.api.core.session.throttling.RequestThrottler
import com.datastax.oss.driver.api.core.specex.SpeculativeExecutionPolicy
import com.datastax.oss.driver.api.core.ssl.SslEngineFactory
import com.datastax.oss.driver.api.core.time.TimestampGenerator
import com.datastax.oss.driver.api.core.tracker.RequestTracker

import java.time.Duration
import java.{lang, util}

private[scassandra] object FakeConfig {
  def createFakeContext(contents: Map[String, Any]): DriverContext = {
    val profile = new FakeProfile(contents)
    val config = new FakeConfig(profile)
    val context = new FakeContext(config)
    context
  }

  def createFakeContext(contents: (String, Any)*): DriverContext = {
    createFakeContext(Map(contents*))
  }

  private class FakeContext(config: FakeConfig) extends DriverContext {
    private def unsupported[T]: T = throw new UnsupportedOperationException("Only getConfig is supported by the fake context")

    override def getSessionName: String = ""
    override def getConfig: DriverConfig = config
    override def getConfigLoader: DriverConfigLoader = unsupported
    override def getLoadBalancingPolicies: util.Map[String, LoadBalancingPolicy] = unsupported
    override def getRetryPolicies: util.Map[String, RetryPolicy] = unsupported
    override def getSpeculativeExecutionPolicies: util.Map[String, SpeculativeExecutionPolicy] = unsupported
    override def getTimestampGenerator: TimestampGenerator = unsupported
    override def getReconnectionPolicy: ReconnectionPolicy = unsupported
    override def getAddressTranslator: AddressTranslator = unsupported
    override def getAuthProvider: util.Optional[AuthProvider] = unsupported
    override def getSslEngineFactory: util.Optional[SslEngineFactory] = unsupported
    override def getRequestTracker: RequestTracker = unsupported
    override def getRequestThrottler: RequestThrottler = unsupported
    override def getNodeStateListener: NodeStateListener = unsupported
    override def getSchemaChangeListener: SchemaChangeListener = unsupported
    override def getProtocolVersion: ProtocolVersion = unsupported
    override def getCodecRegistry: CodecRegistry = unsupported
  }

  private class FakeConfig(profile: FakeProfile) extends DriverConfig {
    private def unsupported[T]: T = throw new UnsupportedOperationException("Only getProfile is supported by the fake config")

    override def getProfile(profileName: String): DriverExecutionProfile = profile
    override def getProfiles: util.Map[String, _ <: DriverExecutionProfile] = unsupported
  }

  private class FakeProfile(contents: Map[String, Any]) extends DriverExecutionProfile {
    private def unsupported[T]: T = throw new UnsupportedOperationException("List operations not supported by the fake profile")

    override def getName: String = "FakeProfile"
    override def isDefined(option: DriverOption): Boolean = contents.isDefinedAt(option.getPath)
    override def getBoolean(option: DriverOption): Boolean = contents(option.getPath).asInstanceOf[Boolean]
    override def getBooleanList(option: DriverOption): util.List[lang.Boolean] = unsupported
    override def getInt(option: DriverOption): Int = contents(option.getPath).asInstanceOf[Int]
    override def getIntList(option: DriverOption): util.List[Integer] = unsupported
    override def getLong(option: DriverOption): Long = contents(option.getPath).asInstanceOf[Long]
    override def getLongList(option: DriverOption): util.List[lang.Long] = unsupported
    override def getDouble(option: DriverOption): Double = contents(option.getPath).asInstanceOf[Double]
    override def getDoubleList(option: DriverOption): util.List[lang.Double] = unsupported
    override def getString(option: DriverOption): String = contents(option.getPath).asInstanceOf[String]
    override def getStringList(option: DriverOption): util.List[String] = unsupported
    override def getStringMap(option: DriverOption): util.Map[String, String] = unsupported
    override def getBytes(option: DriverOption): Long = contents(option.getPath).asInstanceOf[Long]
    override def getBytesList(option: DriverOption): util.List[lang.Long] = unsupported
    override def getDuration(option: DriverOption): Duration = contents(option.getPath).asInstanceOf[Duration]
    override def getDurationList(option: DriverOption): util.List[Duration] = unsupported
    override def entrySet(): util.SortedSet[util.Map.Entry[String, AnyRef]] = unsupported
  }
}
