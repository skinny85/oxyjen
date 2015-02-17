package org.oxyjen.ozone.rest

import com.ning.http.client.Response

class DispatchHandler extends (Response => OZoneResponseJson) {
  override def apply(resp: Response): OZoneResponseJson = {
    null
  }
}
