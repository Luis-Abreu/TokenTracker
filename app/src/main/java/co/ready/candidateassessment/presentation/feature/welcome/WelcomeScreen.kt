package co.ready.candidateassessment.presentation.feature.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import co.ready.candidateassessment.R
import co.ready.candidateassessment.app.Constants
import co.ready.candidateassessment.presentation.design.ReadyTheme

@Composable
fun WelcomeScreen(onNavigateToTokens: () -> Unit, walletAddress: String = Constants.walletAddress) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.wallet_address),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = walletAddress,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onNavigateToTokens) {
                Text(stringResource(R.string.erc20_tokens))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun WelcomeScreenPreview() {
    ReadyTheme {
        WelcomeScreen(onNavigateToTokens = {})
    }
}
