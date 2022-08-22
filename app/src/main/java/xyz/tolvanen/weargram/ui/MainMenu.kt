package xyz.tolvanen.weargram.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import dagger.hilt.android.lifecycle.HiltViewModel
import xyz.tolvanen.weargram.R
import xyz.tolvanen.weargram.Screen
import xyz.tolvanen.weargram.client.Authenticator
import javax.inject.Inject

@HiltViewModel
class MainMenuViewModel @Inject constructor(private val authenticator: Authenticator) : ViewModel() {

    fun logOut() {
        authenticator.reset()
    }

}

@Composable
fun MainMenuScreen(navController: NavController, viewModel: MainMenuViewModel) {

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 0.dp)
    ) {

        item {

            Chip(
                modifier = Modifier.fillMaxWidth(0.9f),
                onClick = {
                    viewModel.logOut()
                    navController.navigate(Screen.Home.route)
                },
                label = { Text("Log out") },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_logout_24),
                        contentDescription = null
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.surface)
            )
        }
    }


}

