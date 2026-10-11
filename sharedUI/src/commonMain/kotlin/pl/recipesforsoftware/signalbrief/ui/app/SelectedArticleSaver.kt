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
        save = { article -> article?.toSavedValues().orEmpty() },
        restore = { values ->
            values.takeIf { it.size == ARTICLE_SAVED_VALUE_COUNT }?.toArticle()
        },
    )

/** KMP-safe [Saver] for Navigation 3 Article Details rendering payloads. */
internal val NavigationArticleSnapshotsSaver: Saver<Map<String, Article>, Any> =
    listSaver(
        save = { snapshots -> snapshots.values.flatMap(Article::toSavedValues) },
        restore = { values ->
            values
                .takeIf { it.size % ARTICLE_SAVED_VALUE_COUNT == 0 }
                ?.chunked(ARTICLE_SAVED_VALUE_COUNT)
                ?.mapNotNull { it.toArticle() }
                ?.associateBy(Article::url)
                .orEmpty()
        },
    )

private fun Article.toSavedValues(): List<Any?> =
    listOf(
        title,
        description,
        url,
        imageUrl,
        source != null,
        source?.id,
        source?.name,
    )

private fun List<Any?>.toArticle(): Article? =
    runCatching {
        val hasSource = this[4] as Boolean
        Article(
            title = this[0] as String?,
            description = this[1] as String?,
            url = this[2] as String,
            imageUrl = this[3] as String?,
            source =
                if (hasSource) {
                    Source(
                        id = this[5] as String?,
                        name = this[6] as String?,
                    )
                } else {
                    null
                },
        )
    }.getOrNull()

private const val ARTICLE_SAVED_VALUE_COUNT = 7
