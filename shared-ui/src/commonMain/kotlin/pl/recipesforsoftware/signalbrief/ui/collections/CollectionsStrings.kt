package pl.recipesforsoftware.signalbrief.ui.collections

/** Shared user-facing strings, following the existing shared UI string convention. */
object CollectionsStrings {
    const val TITLE = "Collections"
    const val BACK = "Back"
    const val CREATE_COLLECTION = "Create collection"
    const val NEW_COLLECTION = "New collection"
    const val RENAME_COLLECTION = "Rename collection"
    const val COLLECTION_NAME = "Collection name"
    const val RENAME = "Rename"
    const val DELETE = "Delete"
    const val CANCEL = "Cancel"
    const val CREATE = "Create"
    const val SAVE = "Save"
    const val OPTIONS = "Collection options"
    const val EMPTY_TITLE = "No collections yet"
    const val EMPTY_DESCRIPTION = "Create collections to organize your reading."
    const val DELETE_TITLE = "Delete collection?"
    const val DELETE_MESSAGE = "This will permanently remove the collection."

    fun error(error: CollectionsError): String =
        when (error) {
            CollectionsError.InvalidName -> "Enter a collection name."
            CollectionsError.NotFound -> "This collection is no longer available."
            CollectionsError.Unknown -> "Something went wrong. Please try again."
        }
}
