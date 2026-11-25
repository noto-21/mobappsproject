package com.group6.whereit.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun CameraScreen(
    onMediaCaptured: (path: String, isVideo: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }

    val requiredPermissions = remember {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun checkPermission(permission: String): Boolean =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    var hasCameraPermission by remember {
        mutableStateOf(checkPermission(Manifest.permission.CAMERA))
    }
    var hasAudioPermission by remember {
        mutableStateOf(checkPermission(Manifest.permission.RECORD_AUDIO))
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    var requestedFromButton by remember { mutableStateOf(false) }
    var hadRationaleBeforeRequest by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        hasCameraPermission = checkPermission(Manifest.permission.CAMERA)
        hasAudioPermission = checkPermission(Manifest.permission.RECORD_AUDIO)
        hasLocationPermission =
                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    // Re-check permissions if the user grants them in Android App Settings and comes back
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
        hasLocationPermission =
                (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                        (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) ||
                        hasLocationPermission

        if (requestedFromButton && !hasCameraPermission) {
            val hasRationaleAfter = activity != null &&
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)

            // Android suppressed the popup because the permission was permanently denied
            if (!hadRationaleBeforeRequest && !hasRationaleAfter) {
                Toast.makeText(
                    context,
                    "Please enable Camera permission in App Settings",
                    Toast.LENGTH_LONG
                ).show()
                val settingsIntent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(settingsIntent)
            }
        }
        requestedFromButton = false
    }

    fun requestAllPermissions(fromButton: Boolean) {
        val missingPermissions = requiredPermissions.filter { !checkPermission(it) }.toTypedArray()
        if (missingPermissions.isEmpty()) {
            refreshPermissions()
            return
        }

        requestedFromButton = fromButton
        hadRationaleBeforeRequest = activity != null &&
                missingPermissions.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                }
        permissionLauncher.launch(missingPermissions)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission || !hasLocationPermission) {
            requestAllPermissions(fromButton = false)
        }
    }

    if (!hasCameraPermission) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "WhereIt? needs camera, microphone, and location permissions to capture and tag your items.",
                textAlign = TextAlign.Center
            )
            Button(
                onClick = { requestAllPermissions(fromButton = true) },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Grant Permissions")
            }
        }
        return
    }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    var isVideoMode by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var isFinalizingVideo by remember { mutableStateOf(false) }
    // for remembering video after pressing stop
    val recordingHolder = remember { arrayOfNulls<Recording>(1) }

    LaunchedEffect(isVideoMode) {
        controller.setEnabledUseCases(
            if (isVideoMode) CameraController.VIDEO_CAPTURE
            else CameraController.IMAGE_CAPTURE
        )
    }

    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            recordingHolder[0]?.stop()
            controller.unbind()
        }
    }

    // Live timer while recording
    LaunchedEffect(activeRecording) {
        if (activeRecording != null) {
            recordingSeconds = 0
            while (true) {
                delay(1000L.milliseconds)
                recordingSeconds++
            }
        } else {
            recordingSeconds = 0
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx -> PreviewView(ctx).apply { this.controller = controller } },
            modifier = Modifier.fillMaxSize()
        )

        // Top recording badge when recording
        if (activeRecording != null) {
            val mins = recordingSeconds / 60
            val secs = recordingSeconds % 60
            Text(
                text = String.format(Locale.getDefault(), "● REC %02d:%02d", mins, secs),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Bottom controls: Mode selector + Shutter/Record button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hide mode switcher while actively recording
            if (activeRecording == null) {
                val chipColors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    labelColor = Color.White,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
                val chipBorder = BorderStroke(1.dp, Color.White)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isVideoMode,
                        onClick = { isVideoMode = false },
                        label = { Text("📷 Photo") },
                        colors = chipColors,
                        border = chipBorder
                    )
                    FilterChip(
                        selected = isVideoMode,
                        onClick = { isVideoMode = true },
                        label = { Text("🎥 Video") },
                        colors = chipColors,
                        border = chipBorder
                    )
                }
            }

            if (!isVideoMode) {
                // Photo capture mode
                Button(
                    onClick = {
                        isCapturing = true
                        val file =
                                File(context.filesDir, "photos/${System.currentTimeMillis()}.jpg")
                        file.parentFile?.mkdirs()

                        controller.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    isCapturing = false
                                    onMediaCaptured(file.absolutePath, false)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    isCapturing = false
                                    Toast.makeText(
                                        context,
                                        "Couldn't take photo: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    },
                    enabled = !isCapturing
                ) {
                    Text(if (isCapturing) "Saving..." else "Take Photo")
                }
            } else {
                // Video capture mode
                val isRecording = activeRecording != null
                Button(
                    onClick = {
                        if (isRecording) {
                            isFinalizingVideo = true
                            activeRecording?.stop()
                        } else {
                            val file =
                                    File(context.filesDir, "videos/${System.currentTimeMillis()}.mp4")
                            file.parentFile?.mkdirs()

                            val outputOptions = FileOutputOptions.Builder(file).build()
                            val currentlyHasAudio = checkPermission(Manifest.permission.RECORD_AUDIO)

                            @SuppressLint("MissingPermission")
                            val audioConfig = AudioConfig.create(false) // change to currentlyHasAudio for audio videos

                            val recording = controller.startRecording(
                                outputOptions,
                                audioConfig,
                                ContextCompat.getMainExecutor(context)
                            ) { event ->
                                when (event) {
                                    is VideoRecordEvent.Finalize -> {
                                        activeRecording = null
                                        isFinalizingVideo = false
                                        recordingHolder[0] = null

                                        if (!event.hasError()) {
                                            onMediaCaptured(file.absolutePath, true)
                                        } else {
                                            file.delete()
                                            android.util.Log.e(
                                                "WhereItCamera",
                                                "Video finalize error code=${event.error}: ${event.cause?.message}"
                                            )
                                            Toast.makeText(
                                                context,
                                                "Video error (${event.error}): ${event.cause?.message ?: "Try recording for at least 2 seconds"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                            recordingHolder[0] = recording
                            activeRecording = recording
                        }
                    },
                    enabled = !isFinalizingVideo,
                    colors = if (isRecording) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(
                        when {
                            isFinalizingVideo -> "Saving video..."
                            isRecording -> "Stop Recording"
                            else -> "Start Recording"
                        }
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
