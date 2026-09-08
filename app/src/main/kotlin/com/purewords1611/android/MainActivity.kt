package com.purewords1611.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.purewords1611.android.analytics.AnalyticsManager
import com.purewords1611.android.study.data.RootDestination
import com.purewords1611.android.study.ui.StudyAppRoot
import com.purewords1611.android.study.ui.StudyViewModel
import com.purewords1611.android.ui.theme.PureWords1611Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity for PureWords1611 app
 * Uses Hilt for dependency injection
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    private var deepLinkData by mutableStateOf<Pair<String, Int>?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        analyticsManager.trackAppLaunch()

        setContent {
            PureWords1611Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val viewModel: StudyViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    
                    // Handle deep link from notification
                    LaunchedEffect(deepLinkData) {
                        deepLinkData?.let { (book, chapter) ->
                            viewModel.selectChapter(book, chapter)
                            viewModel.setDestination(RootDestination.READ)
                            deepLinkData = null
                        }
                    }

                    StudyAppRoot(
                        analyticsManager = analyticsManager,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

    private fun handleIntent(intent: android.content.Intent) {
        val book = intent.getStringExtra("book")
        val chapter = intent.getIntExtra("chapter", -1)
        if ((book != null) && (chapter != -1)) {
            deepLinkData = book to chapter
        }
    }
}
