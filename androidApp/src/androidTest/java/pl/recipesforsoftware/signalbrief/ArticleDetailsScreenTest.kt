package pl.recipesforsoftware.signalbrief

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleCollectionAssignmentError
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleCollectionAssignmentStrings
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleCollectionAssignmentUiState
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsScreen
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsStrings
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsUiState
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesStrings

class ArticleDetailsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val article =
        Article(
            title = "Test Article Title",
            description = "Test article description",
            url = "https://example.com/1",
            imageUrl = null,
            source = Source(id = "src1", name = "Source 1"),
        )

    private fun setContent(
        uiState: ArticleDetailsUiState = ArticleDetailsUiState(article),
        onBack: () -> Unit = {},
        onBookmarkClick: () -> Unit = {},
        onOpenFullArticle: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                ArticleDetailsScreen(
                    uiState = uiState,
                    onBack = onBack,
                    onBookmarkClick = onBookmarkClick,
                    onOpenFullArticle = onOpenFullArticle,
                )
            }
        }
    }

    @Test
    fun title_renders() {
        setContent()

        composeTestRule.onNodeWithText("Test Article Title").assertIsDisplayed()
    }

    @Test
    fun source_rendersWhenPresent() {
        setContent()

        composeTestRule.onNodeWithText("Source 1").assertIsDisplayed()
    }

    @Test
    fun description_rendersWhenPresent() {
        setContent()

        composeTestRule.onNodeWithText("Test article description").assertIsDisplayed()
    }

    @Test
    fun missingSource_doesNotRenderPlaceholder() {
        setContent(uiState = ArticleDetailsUiState(article.copy(source = null)))

        composeTestRule.onNodeWithText("Source 1").assertDoesNotExist()
        composeTestRule.onNodeWithText("Unknown source").assertDoesNotExist()
    }

    @Test
    fun missingDescription_doesNotRenderPlaceholder() {
        setContent(uiState = ArticleDetailsUiState(article.copy(description = null)))

        composeTestRule.onNodeWithText("Test article description").assertDoesNotExist()
        composeTestRule.onNodeWithText("No description").assertDoesNotExist()
    }

    @Test
    fun missingTitle_doesNotRenderPlaceholder() {
        setContent(uiState = ArticleDetailsUiState(article.copy(title = null)))

        composeTestRule.onNodeWithText("Test Article Title").assertDoesNotExist()
        composeTestRule.onNodeWithText("Untitled").assertDoesNotExist()
    }

    @Test
    fun unsavedArticle_showsSaveSemantics() {
        setContent(uiState = ArticleDetailsUiState(article, isSaved = false))

        composeTestRule.onNodeWithText(TopHeadlinesStrings.BOOKMARK_SAVE).assertIsDisplayed()
    }

    @Test
    fun savedArticle_showsRemoveSemantics() {
        setContent(uiState = ArticleDetailsUiState(article, isSaved = true))

        composeTestRule.onNodeWithText(TopHeadlinesStrings.BOOKMARK_REMOVE).assertIsDisplayed()
    }

    @Test
    fun bookmarkClick_invokesCallback() {
        var bookmarkClicked = false
        setContent(onBookmarkClick = { bookmarkClicked = true })

        composeTestRule.onNodeWithText(TopHeadlinesStrings.BOOKMARK_SAVE).performClick()

        assert(bookmarkClicked) { "onBookmarkClick should have been called" }
    }

    @Test
    fun bookmarkClick_doesNotInvokeExternalOpen() {
        var externalOpened = false
        setContent(
            onBookmarkClick = {},
            onOpenFullArticle = { externalOpened = true },
        )

        composeTestRule.onNodeWithText(TopHeadlinesStrings.BOOKMARK_SAVE).performClick()

        assert(!externalOpened) { "Bookmark click must not open the article externally" }
    }

    @Test
    fun readFullArticle_invokesExternalOpenCallback() {
        var externalOpened = false
        setContent(onOpenFullArticle = { externalOpened = true })

        composeTestRule.onNodeWithText(ArticleDetailsStrings.READ_FULL_ARTICLE).performClick()

        assert(externalOpened) { "Read full article should open the article externally" }
    }

    @Test
    fun back_invokesCallback() {
        var backClicked = false
        setContent(onBack = { backClicked = true })

        composeTestRule.onNodeWithContentDescription(ArticleDetailsStrings.BACK).performClick()

        assert(backClicked) { "onBack should have been called" }
    }

    @Test
    fun imagelessArticle_rendersWithoutError() {
        setContent()

        composeTestRule.onNodeWithText("Test Article Title").assertIsDisplayed()
        composeTestRule.onNodeWithText(ArticleDetailsStrings.READ_FULL_ARTICLE).assertIsDisplayed()
    }

    @Test
    fun collectionPicker_emptyStateOffersCollectionsManagement() {
        var manageCollectionsClicked = false
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                ArticleDetailsScreen(
                    uiState = ArticleDetailsUiState(article),
                    onBack = {},
                    onBookmarkClick = {},
                    onOpenFullArticle = {},
                    collectionAssignmentUiState =
                        ArticleCollectionAssignmentUiState(
                            isLoadingCollections = false,
                            isPickerVisible = true,
                        ),
                    onCollectionAssignmentClick = {},
                    onToggleCollection = {},
                    onDismissCollectionAssignment = {},
                    onManageCollections = { manageCollectionsClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText(ArticleCollectionAssignmentStrings.EMPTY_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(ArticleCollectionAssignmentStrings.MANAGE_COLLECTIONS).performClick()

        assert(manageCollectionsClicked)
    }

    @Test
    fun collectionPicker_errorIsVisibleInsideDialog() {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                ArticleDetailsScreen(
                    uiState = ArticleDetailsUiState(article),
                    onBack = {},
                    onBookmarkClick = {},
                    onOpenFullArticle = {},
                    collectionAssignmentUiState =
                        ArticleCollectionAssignmentUiState(
                            isLoadingCollections = false,
                            isPickerVisible = true,
                            error = ArticleCollectionAssignmentError.Unknown,
                        ),
                )
            }
        }

        composeTestRule
            .onNodeWithText(ArticleCollectionAssignmentStrings.error(ArticleCollectionAssignmentError.Unknown))
            .assertIsDisplayed()
    }
}
