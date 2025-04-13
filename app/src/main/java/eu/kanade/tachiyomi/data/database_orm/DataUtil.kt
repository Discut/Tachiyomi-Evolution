package eu.kanade.tachiyomi.data.database_orm

import eu.kanade.tachiyomi.error.TipsException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun requireValue(value: Boolean, lazyMessage: () -> Any) {
    contract {
        returns() implies value
    }
    if (!value) {
        val message = lazyMessage()
        throw TipsException(message.toString())
    }
}
