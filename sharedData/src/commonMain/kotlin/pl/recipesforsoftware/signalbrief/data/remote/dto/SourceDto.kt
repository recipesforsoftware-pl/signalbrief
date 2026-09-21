package pl.recipesforsoftware.signalbrief.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Remote transport shape of the publisher information nested in an article.
 */
@Serializable
data class SourceDto(
    val id: String? = null,
    val name: String? = null,
)
