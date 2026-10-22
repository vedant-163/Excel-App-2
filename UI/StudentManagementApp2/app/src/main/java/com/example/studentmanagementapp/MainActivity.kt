package com.example.studentmanagementapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// =====================================================
// MAIN ACTIVITY
// =====================================================

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {
            var showSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1000)
                showSplash = false
            }
            if (showSplash) {
                SGSITSSplashScreen()
            } else {
                App()
            }
        }
    }
}


// =====================================================
// SGSITS BRANDING SPLASH SCREEN
// Kept separate from the existing app/authentication flow.
// =====================================================

@Composable
private fun SGSITSSplashScreen() {
    val navy = Color(0xFF102B4E)
    val gold = Color(0xFFE2B24B)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = navy
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Restrained gold accent bars keep the screen formal and college-branded.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 38.dp, start = 28.dp)
                    .width(54.dp)
                    .height(3.dp)
                    .background(gold)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 38.dp, end = 28.dp)
                    .width(54.dp)
                    .height(3.dp)
                    .background(gold)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(100.dp),
                    shadowElevation = 8.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sgsits_college_logo),
                        contentDescription = "SGSITS college emblem",
                        modifier = Modifier
                            .padding(12.dp)
                            .size(150.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "SGSITS",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "STUDENT MANAGEMENT SYSTEM",
                    color = gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(Modifier.width(30.dp).height(2.dp).background(Color.White.copy(alpha = 0.65f)))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.width(22.dp).height(3.dp).background(gold))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.width(30.dp).height(2.dp).background(Color.White.copy(alpha = 0.65f)))
                }
            }
        }
    }
}

// =====================================================
// APP NAVIGATION
// =====================================================

@Composable
fun App() {
    val context = androidx.compose.ui.platform.LocalContext.current

    var authenticated by remember {
        mutableStateOf(AuthStorage.hasValidSession(context))
    }
    var sessionChecking by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf("dashboard") }
    var showExitConfirmation by remember { mutableStateOf(false) }

    val students = remember { mutableStateListOf<Student>() }
    var dashboardStudentCount by remember { mutableStateOf(0) }

    LaunchedEffect(authenticated) {
        if (!authenticated) {
            students.clear()
            sessionChecking = false
            return@LaunchedEffect
        }

        // Academic years are sourced from the backend so all modules use the same database values.
        try {
            val dbAcademicYears = ApiClient.getAcademicYears()
            AdmissionCatalog.setAcademicYears(dbAcademicYears)
        } catch (_: Exception) {
            // Keep the offline fallback already present in AdmissionCatalog.
        }
        try {
            val dbAdmissionRounds = ApiClient.getAdmissionRounds(context)
            AdmissionCatalog.setAdmissionRounds(dbAdmissionRounds)
        } catch (_: Exception) {
            // Keep the fallback round list if the lookup API is unavailable.
        }

        sessionChecking = true
        try {
            // Fetch dashboard count from the /dashboard API — this gives the real
            // total from the database without loading all student records.
            // syncWithBackend now only fetches a small first page for the cache.
            dashboardStudentCount = ApiClient.getDashboardStudentCount(context)

            val backendStudents = StudentStorage.syncWithBackend(context)
            students.clear()
            students.addAll(backendStudents)
        } catch (e: ApiClient.ApiException) {
            // Only an authentication failure should end the session. A
            // temporary server/network failure must not log the staff member
            // out or crash the app.
            if (e.statusCode == 401) {
                AuthStorage.clear(context)
                students.clear()
                authenticated = false
            } else {
                students.clear()
                students.addAll(StudentStorage.loadStudents(context))
                dashboardStudentCount = students.size
            }
        } catch (_: Exception) {
            students.clear()
            students.addAll(StudentStorage.loadStudents(context))
            dashboardStudentCount = students.size
        } finally {
            sessionChecking = false
        }
    }

    if (sessionChecking) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF6F8FC)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFF3155C6)
                )
            }
        }
        return
    }

    if (!authenticated) {
        LoginScreen(
            onLoginSuccess = {
                // LaunchedEffect(authenticated) will run on next recomposition and
                // fetch the real dashboard count + initial student page from the API.
                authenticated = true
                currentScreen = "dashboard"
            }
        )
        return
    }

    // Handle Android system-back / edge-swipe within the app instead of exiting it.
    BackHandler(enabled = authenticated) {
        if (currentScreen != "dashboard") {
            currentScreen = "dashboard"
        } else {
            showExitConfirmation = true
        }
    }

    when (currentScreen) {
        "dashboard" -> Dashboard(
            studentsCount = dashboardStudentCount,
            onStudentClick = { currentScreen = "student" },
            onAdmissionClick = { currentScreen = "admission" },
            onFeePaymentClick = { currentScreen = "fee" },
            onSearchClick = { currentScreen = "search" },
            onReportsClick = { currentScreen = "reports" },
            onFeeStructureClick = { currentScreen = "feeStructure" },
            onProfileClick = { currentScreen = "profile" },
            onLogout = {
                AuthStorage.clear(context)
                students.clear()
                currentScreen = "dashboard"
                authenticated = false
            }
        )

        "student" -> StudentScreen(
            students = students,
            onBackClick = { currentScreen = "dashboard" }
        )

        "admission" -> AdmissionScreen(
            students = students,
            onBack = { currentScreen = "dashboard" }
        )

        "fee" -> FeePaymentScreen(
            students = students,
            onBack = { currentScreen = "dashboard" }
        )

        "search" -> SearchScreen(
            students = students,
            onBack = { currentScreen = "dashboard" }
        )

        "reports" -> ReportsScreen(
            students = students,
            onBack = { currentScreen = "dashboard" }
        )

        "feeStructure" -> FeeStructureScreen(
            onBack = { currentScreen = "dashboard" }
        )

        "profile" -> ProfileScreen(
            username = AuthStorage.getUsername(context),
            role = AuthStorage.getRole(context),
            onBack = { currentScreen = "dashboard" },
            onLogout = {
                AuthStorage.clear(context)
                students.clear()
                currentScreen = "dashboard"
                authenticated = false
            }
        )
    }

    if (showExitConfirmation && authenticated) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { androidx.compose.material3.Text("Exit app?") },
            text = { androidx.compose.material3.Text("Do you want to close the Student Management System?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    (context as? ComponentActivity)?.finish()
                }) { androidx.compose.material3.Text("Exit") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    androidx.compose.material3.Text("Stay")
                }
            }
        )
    }
}


// =====================================================
// DASHBOARD
// =====================================================

@Composable
fun Dashboard(
    studentsCount: Int,
    onStudentClick: () -> Unit,
    onAdmissionClick: () -> Unit,
    onFeePaymentClick: () -> Unit,
    onSearchClick: () -> Unit,
    onReportsClick: () -> Unit,
    onFeeStructureClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    val modules = listOf(

        DashboardModule(
            title = "Student Records",
            description = "Manage student information",
            icon = Icons.Default.Groups,
            onClick = onStudentClick
        ),

        DashboardModule(
            title = "Admissions",
            description = "Manage student admissions",
            icon = Icons.Default.Assignment,
            onClick = onAdmissionClick
        ),

        DashboardModule(
            title = "Fee & Payment",
            description = "Manage fees and payments",
            icon = Icons.Default.Payment,
            onClick = onFeePaymentClick
        ),

        DashboardModule(
            title = "Search Students",
            description = "Find student records quickly",
            icon = Icons.Default.Search,
            onClick = onSearchClick
        ),

        DashboardModule(
            title = "Reports",
            description = "View and generate reports",
            icon = Icons.Default.PieChart,
            onClick = onReportsClick
        ),

        DashboardModule(
            title = "Fee Structure",
            description = "Manage academic fee structure",
            icon = Icons.Default.ReceiptLong,
            onClick = onFeeStructureClick
        )
    )


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF6F8FC)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {


            // =================================================
            // HEADER
            // =================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "SGSITS",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF172554)
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Student Management System",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }


                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color(0xFFE8EEFF),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "College",
                        modifier = Modifier.size(22.dp),
                        tint = Color(0xFF3155C6)
                    )
                }

                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Open staff profile",
                        tint = Color(0xFF3155C6),
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = { showLogoutConfirmation = true }) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = Color(0xFF3155C6)
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(9.dp)
            )


            // =================================================
            // WELCOME CARD
            // =================================================

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(20.dp),

                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3155C6)
                ),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),

                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Welcome",
                            fontSize = 14.sp,
                            color = Color.White.copy(
                                alpha = 0.75f
                            )
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "College Administration",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text = "Manage student information from one place.",
                            fontSize = 13.sp,
                            color = Color.White.copy(
                                alpha = 0.85f
                            )
                        )
                    }


                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = studentsCount.toString(),
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Students",
                            fontSize = 12.sp,
                            color = Color.White.copy(
                                alpha = 0.8f
                            )
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(14.dp)
            )


            // =================================================
            // MODULE TITLE
            // =================================================

            Text(
                text = "Management Modules",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )


            Spacer(
                modifier = Modifier.height(5.dp)
            )


            Text(
                text = "Select a module to continue",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )


            Spacer(
                modifier = Modifier.height(15.dp)
            )


            // =================================================
            // MODULE GRID
            // =================================================

            LazyVerticalGrid(

                columns = GridCells.Fixed(2),

                modifier = Modifier.weight(1f).fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(14.dp),

                verticalArrangement =
                    Arrangement.spacedBy(14.dp)

            ) {

                items(modules) { module ->

                    ProfessionalDashboardCard(
                        module = module
                    )
                }
            }
        }
    }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Sign out?") },
            text = { Text("Are you sure you want to sign out of the Student Management System?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirmation = false
                    onLogout()
                }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}


// =====================================================
// DASHBOARD MODULE DATA
// =====================================================

data class DashboardModule(

    val title: String,

    val description: String,

    val icon: ImageVector,

    val onClick: () -> Unit
)


// =====================================================
// DASHBOARD CARD
// =====================================================

@Composable
fun ProfessionalDashboardCard(
    module: DashboardModule
) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable {
                module.onClick()
            },

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {


            // =================================================
            // ICON
            // =================================================

            Box(

                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = Color(0xFFEAF0FF),
                        shape = RoundedCornerShape(14.dp)
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(

                    imageVector = module.icon,

                    contentDescription = module.title,

                    modifier = Modifier.size(25.dp),

                    tint = Color(0xFF3155C6)
                )
            }


            Spacer(
                modifier = Modifier.height(14.dp)
            )


            // =================================================
            // TITLE
            // =================================================

            Text(

                text = module.title,

                fontSize = 17.sp,

                fontWeight = FontWeight.Bold,

                color = Color(0xFF1E293B)
            )


            Spacer(
                modifier = Modifier.height(5.dp)
            )


            // =================================================
            // DESCRIPTION
            // =================================================

            Text(

                text = module.description,

                fontSize = 12.sp,
                color = Color(0xFF64748B),
                maxLines = 2
            )


            Spacer(
                modifier = Modifier.weight(1f)
            )


            // =================================================
            // OPEN
            // =================================================

            Row(

                modifier = Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(

                    text = "Open",

                    fontSize = 12.sp,

                    fontWeight = FontWeight.SemiBold,

                    color = Color(0xFF3155C6)
                )


                Spacer(
                    modifier = Modifier.width(5.dp)
                )


                Icon(

                    imageVector = Icons.Default.ArrowForward,

                    contentDescription = "Open",

                    modifier = Modifier.size(16.dp),

                    tint = Color(0xFF3155C6)
                )
            }
        }
    }
}