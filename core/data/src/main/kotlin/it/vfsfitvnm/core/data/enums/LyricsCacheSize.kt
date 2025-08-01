package it.vfsfitvnm.core.data.enums

import it.vfsfitvnm.core.data.utils.mb

@Suppress("unused", "EnumEntryName")
enum class LyricsCacheSize(val bytes: Long) {
    `64MB`(bytes = 64.mb),
    `128MB`(bytes = 128.mb),
    `256MB`(bytes = 256.mb),
    `512MB`(bytes = 512.mb),
    `1GB`(bytes = 1024.mb),
    `2GB`(bytes = 2048.mb)
}
