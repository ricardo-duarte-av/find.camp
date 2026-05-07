package camp.find.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import camp.find.app.ui.landing.LandingScreen
import camp.find.app.ui.map.MapScreen
import camp.find.app.ui.profile.ProfileScreen
import camp.find.app.ui.spot.SpotDetailScreen
import camp.find.app.ui.trips.TripsScreen

private object Routes {
    const val LANDING = "landing"
    const val MAP = "map"
    const val TRIPS = "trips"
    const val PROFILE = "profile"
    const val SPOT_DETAIL = "spot/{spotId}"
    fun spotDetail(id: String) = "spot/$id"
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
            MapScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Routes.spotDetail(id)) },
            )
        }
        composable(Routes.TRIPS) {
            TripsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SPOT_DETAIL) { backStackEntry ->
            val spotId = backStackEntry.arguments?.getString("spotId") ?: return@composable
            SpotDetailScreen(
                spotId = spotId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
