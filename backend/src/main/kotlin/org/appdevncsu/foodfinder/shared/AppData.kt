package org.appdevncsu.foodfinder.shared

import java.io.File

/** 
  * Returns the location of a directory that stores persistent data that should survive across restarts. 
  * Used for the database and image cache.
  */
fun dataDir(): File {
    val raw = System.getenv("DATA_DIR")
    return if (raw.isNullOrBlank()) File(".") else File(raw)
}
