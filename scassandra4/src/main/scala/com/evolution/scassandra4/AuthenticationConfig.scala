package com.evolution.scassandra4

import com.evolution.scassandra4.util.PureconfigSyntax._
import pureconfig.ConfigReader

/**
  * Translated to driver 4's `PlainTextAuthProvider`
  * (`advanced.auth-provider`), see [[CreateDriverConfigLoader]].
  */
final case class AuthenticationConfig(username: String, password: Masked[String])

object AuthenticationConfig {

  def apply(username: String, password: String): AuthenticationConfig = {
    AuthenticationConfig(username, Masked(password))
  }

  implicit val configReaderAuthenticationConfig: ConfigReader[AuthenticationConfig] =
    ConfigReader.fromCursor[AuthenticationConfig] { cursor =>
      for {
        objCur <- cursor.asObjectCursor
        username <- objCur.getAt[String]("username")
        password <- objCur.getAt[String]("password")
      } yield AuthenticationConfig(
        username = username,
        password = password
      )
    }
}
