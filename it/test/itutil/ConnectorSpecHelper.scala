package itutil

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.matching

trait ConnectorSpecHelper {

  def addMatchHeaders(builder: MappingBuilder): MappingBuilder = {

    val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".r
    val applicationJson = "application/json"

    builder
      .withHeader("x-forwarded-host", matching("mdtp"))
      .withHeader("date", matching("^[A-Z][a-z]{2},\\s\\d{2}\\s[A-Z][a-z]{2}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2}\\s[A-Z]{3,4}([+-]\\d{1,2})?$"))
      .withHeader("x-correlation-id", matching(uuidRegex.toString()))
      .withHeader("x-conversation-id", matching(uuidRegex.toString()))
      .withHeader("content-type", matching(applicationJson))
      .withHeader("accept", matching(applicationJson))
      .withHeader("Environment", matching("local"))
  }
}
