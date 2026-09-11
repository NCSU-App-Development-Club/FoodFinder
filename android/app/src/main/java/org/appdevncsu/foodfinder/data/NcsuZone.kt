package org.appdevncsu.foodfinder.data

import java.time.ZoneId

/** All displayed hours are in NC State's timezone, matching the backend. */
internal val ncsuZone: ZoneId = ZoneId.of("America/New_York")
