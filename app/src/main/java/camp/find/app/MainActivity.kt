package camp.find.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import camp.find.app.navigation.AppNavigation
import camp.find.app.ui.theme.FindCampTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FindCampTheme {
                AppNavigation()
            }
        }
    }
}
