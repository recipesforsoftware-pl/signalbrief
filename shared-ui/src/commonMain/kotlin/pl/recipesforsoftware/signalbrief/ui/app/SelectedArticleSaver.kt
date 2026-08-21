package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source

/**
 * Compose [Saver] for the child-navigation selected-article state.
 *
 * The [Article] domain model is not JVM-serializable and must not assume
 * Android-only save/restore semantics, so the snapshot is flattened into
 * KMP-safe primitive/string values and rebuilt on restore.
 *
 * Source presence is stored explicitly so `source = null` remains distinct
 * from `Source(id = null, name = null)`.
 */
internal val SelectedArticleSaver: Saver<Article?, Any> =
    listSaver(
        save = { article ->
            if (article == null) {
                emptyList()
            } else {
                listOf(
                    article.title,
                    article.description,
                    article.url,
                    article.imageUrl,
                    article.source != null,
                    article.source?.id,
                    article.source?.name,
                )
            }
        },
        restore = { values ->
            if (values.isEmpty()) {
                null
            } else {
                val hasSource = values[4] as Boolean

                Article(
                    title = values[0] as String?,
                    description = values[1] as String?,
                    url = values[2] as String,
                    imageUrl = values[3] as String?,
                    source =
                        if (hasSource) {
                            Source(
                                id = values[5] as String?,
                                name = values[6] as String?,
                            )
                        } else {
                            null
                        },
                )
            }
        },
    )
