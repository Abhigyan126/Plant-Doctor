package com.example.plantdoctor

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.plantdoctor.ui.theme.PlantDoctorTheme
import com.example.plantdoctor.PlantDiseaseClassifier
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.android.volley.toolbox.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Build


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlantDoctorTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "home") {
                    composable("home") { PlantDoctorScreen(navController) }
                    composable("result") { ResultScreen(navController) }
                }
            }
        }
    }
}

@Composable
fun PlantDoctorScreen(navController: NavHostController) {
    var showOptions by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf("") }

    // Permission states
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Clear any existing camera URI
            navController.currentBackStackEntry?.savedStateHandle?.remove<Uri>("camera_uri")
            navController.currentBackStackEntry?.savedStateHandle?.set("gallery_image", it)
            navController.navigate("result")
        }
    }

    // Multiple permissions launcher for storage
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            permissionMessage = "Storage permission is required to access gallery"
            showPermissionDialog = true
        } else {
            galleryLauncher.launch("image/*")
        }
    }

    // Camera launcher with FileProvider
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri.value?.let { uri ->
                // Clear any existing gallery URI
                navController.currentBackStackEntry?.savedStateHandle?.remove<Uri>("gallery_image")
                navController.currentBackStackEntry?.savedStateHandle?.set("camera_uri", uri)
                navController.navigate("result")
            }
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            permissionMessage = "Camera permission is required to take photos"
            showPermissionDialog = true
        } else {
            val uri = ComposeFileProvider.getImageUri(context)
            photoUri.value = uri
            cameraLauncher.launch(uri)
        }
    }

    // Function to check and request storage permissions
    fun checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above
            multiplePermissionsLauncher.launch(arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            ))
        } else {
            // For Android 12 and below
            multiplePermissionsLauncher.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { Text(permissionMessage) },
            confirmButton = {
                Button(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            }
        )
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "Plant Doctor",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC8E6C9))
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.plant_disease),
                contentDescription = "Plant Disease",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "Detect plant diseases with a single tap! Click 'Detect' to use your camera or gallery.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        Button(
            onClick = { showOptions = !showOptions },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Detect",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        AnimatedVisibility(visible = showOptions) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (!hasCameraPermission) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            val uri = ComposeFileProvider.getImageUri(context)
                            photoUri.value = uri
                            cameraLauncher.launch(uri)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "Take a Photo",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { checkAndRequestStoragePermissions() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "Use Gallery",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun ResultScreen(navController: NavHostController) {
    val context = LocalContext.current
    var predictionResult by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Get URIs from saved state
    val cameraUri = navController.previousBackStackEntry?.savedStateHandle?.get<Uri>("camera_uri")
    val galleryImageUri = navController.previousBackStackEntry?.savedStateHandle?.get<Uri>("gallery_image")

    val classifier = remember { PlantDiseaseClassifier(context) }

    // Clear the saved state when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            navController.previousBackStackEntry?.savedStateHandle?.remove<Uri>("camera_uri")
            navController.previousBackStackEntry?.savedStateHandle?.remove<Uri>("gallery_image")
        }
    }

    LaunchedEffect(cameraUri, galleryImageUri) {
        val uri = cameraUri ?: galleryImageUri
        if (uri != null) {
            isLoading = true
            try {
                imageBitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }

                imageBitmap?.let { bitmap ->
                    predictionResult = withContext(Dispatchers.IO) {
                        classifier.classify(bitmap)
                    }
                }
            } catch (e: Exception) {
                predictionResult = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    DisposableEffect(classifier) {
        onDispose { classifier.close() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "Analysis Result",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC8E6C9))
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        }

        if (imageBitmap != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Image(
                    bitmap = imageBitmap!!.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Analyzing...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                predictionResult != null -> {
                    val displayResult = predictionResult!!.replace("_", " ")
                    Text(
                        text = displayResult,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        color = if (displayResult.startsWith("Error"))
                            Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
                else -> {
                    Text(
                        text = "No image selected for analysis",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Button(
            onClick = { navController.navigateUp() },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Analyze Another Image",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

object ComposeFileProvider {
    fun getImageUri(context: Context): Uri {
        val directory = File(context.cacheDir, "images")
        directory.mkdirs()
        val file = File.createTempFile(
            "selected_image_",
            ".jpg",
            directory
        )
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(
            context,
            authority,
            file
        )
    }
}
@Preview(showBackground = true)
@Composable
fun PlantDoctorPreview() {
    PlantDoctorTheme {
        val navController = rememberNavController()
        PlantDoctorScreen(navController)
    }
}