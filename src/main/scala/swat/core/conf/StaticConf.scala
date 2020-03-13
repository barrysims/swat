package swat.core.conf

import pureconfig.generic.auto._


object Static {

  val config: StaticConfig = pureconfig.loadConfigOrThrow[StaticConfig]
}

case class StaticConfig(dummyApi: DummyApiConfig)

case class DummyApiConfig(url: String)

