package dev.boardwork.pose

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

/**
 * Bridges CameraX frames to ML Kit's on-device pose detector and republishes
 * results as our own [PoseSnapshot] — the rest of the app never touches an
 * ML Kit type directly, which is what keeps [FormEngine] pure and portable.
 *
 * ML Kit's pose-detection artifact is a free, on-device Google library, not
 * an open-source one; see the README for a note on swapping it for a fully
 * open alternative (e.g. a bundled MediaPipe/TFLite model) if that matters
 * to your build.
 */
class PoseAnalyzer(
    private val onResult: (PoseSnapshot) -> Unit,
    private val onNoPerson: () -> Unit = {}
) : ImageAnalysis.Analyzer {

    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(input)
            .addOnSuccessListener { pose -> handle(pose) }
            .addOnFailureListener { onNoPerson() }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun handle(pose: Pose) {
        val landmarks = LinkedHashMap<Joint, Landmark>()
        for ((joint, mlKitType) in MAPPING) {
            val lm = pose.getPoseLandmark(mlKitType) ?: continue
            landmarks[joint] = Landmark(
                Point2D(lm.position.x.toDouble(), lm.position.y.toDouble()),
                lm.inFrameLikelihood.toDouble()
            )
        }
        if (landmarks.isEmpty()) {
            onNoPerson()
            return
        }
        onResult(PoseSnapshot(landmarks, System.currentTimeMillis()))
    }

    fun close() = detector.close()

    private companion object {
        val MAPPING = mapOf(
            Joint.LEFT_SHOULDER to PoseLandmark.LEFT_SHOULDER,
            Joint.RIGHT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            Joint.LEFT_ELBOW to PoseLandmark.LEFT_ELBOW,
            Joint.RIGHT_ELBOW to PoseLandmark.RIGHT_ELBOW,
            Joint.LEFT_WRIST to PoseLandmark.LEFT_WRIST,
            Joint.RIGHT_WRIST to PoseLandmark.RIGHT_WRIST,
            Joint.LEFT_HIP to PoseLandmark.LEFT_HIP,
            Joint.RIGHT_HIP to PoseLandmark.RIGHT_HIP,
            Joint.LEFT_KNEE to PoseLandmark.LEFT_KNEE,
            Joint.RIGHT_KNEE to PoseLandmark.RIGHT_KNEE,
            Joint.LEFT_ANKLE to PoseLandmark.LEFT_ANKLE,
            Joint.RIGHT_ANKLE to PoseLandmark.RIGHT_ANKLE
        )
    }
}
