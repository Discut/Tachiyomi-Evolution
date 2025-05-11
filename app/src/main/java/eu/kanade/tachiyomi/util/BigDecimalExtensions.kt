package eu.kanade.tachiyomi.util

import java.math.BigDecimal

operator fun BigDecimal.plus(decimals: Int): BigDecimal = plus(BigDecimal(decimals))
