package pl.recipesforsoftware.signalbrief.ui.articledetails

/** Centralized user-facing copy for the shared article collection picker. */
object ArticleCollectionAssignmentStrings {
    const val ADD_TO_COLLECTIONS = "Add to collections"
    const val TITLE = "Add to collections"
    const val CLOSE = "Close"
    const val MANAGE_COLLECTIONS = "Manage collections"
    const val EMPTY_TITLE = "No collections yet"
    const val EMPTY_DESCRIPTION = "Create a collection first to organize this article."

    fun error(error: ArticleCollectionAssignmentError): String =
        when (error) {
            ArticleCollectionAssignmentError.NotFound -> "This collection is no longer available."
            ArticleCollectionAssignmentError.Unknown -> "Couldn't update collections. Please try again."
        }
}
