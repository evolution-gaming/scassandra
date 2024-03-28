package com.evolutiongaming.scassandra

import com.evolutiongaming.scassandra.util.PureconfigSyntax._
import pureconfig.ConfigReader

trait AuthenticationConfigImplicits {
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
