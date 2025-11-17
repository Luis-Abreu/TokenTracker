package co.ready.candidateassessment.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.ready.candidateassessment.presentation.design.ReadyTheme
import co.ready.candidateassessment.presentation.feature.token.TokensScreen
import co.ready.candidateassessment.presentation.feature.welcome.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReadyTheme {
                ReadyApp()
            }
        }
    }
}

@Composable
private fun ReadyApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(
                onNavigateToTokens = { navController.navigate("tokens") },
                walletAddress = Constants.walletAddress
            )
        }
        composable("tokens") {
            TokensScreen()
        }
    }
}
