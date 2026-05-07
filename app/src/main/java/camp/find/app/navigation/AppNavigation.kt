package camp.find.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import camp.find.app.ui.landing.LandingScreen
import camp.find.app.ui.map.MapScreen
import camp.find.app.ui.profile.ProfileScreen
import camp.find.app.ui.trips.TripsScreen

private object Routes {
    const val LANDING = "landing"
    const val MAP = "map"
    const val TRIPS = "trips"
    const val PROFILE = "profile"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LANDING,
    ) {
        composable(Routes.LANDING) {
            LandingScreen(
                onNavigateToMap = { navController.navigate(Routes.MAP) },
                onNavigateToTrips = { navController.navigate(Routes.TRIPS) },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
            )
        }
        composable(Routes.MAP) {
            MapScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TRIPS) {
            TripsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
    }
}
