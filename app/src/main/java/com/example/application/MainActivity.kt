package com.example.application

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.application.databinding.ActivityMainBinding
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var arFragment: ArFragment

    private var firstAnchor: Anchor? = null
    private var secondAnchor: Anchor? = null
    private var distanceNode: AnchorNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!isARCoreSupported()) {
            Toast.makeText(this, "❌ ARCore yo‘q yoki o‘rnatish kerak!", Toast.LENGTH_LONG).show()
            return
        }

        arFragment = supportFragmentManager.findFragmentById(R.id.ar) as ArFragment

        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            if (firstAnchor == null) {
                firstAnchor = hitResult.createAnchor()
                Toast.makeText(this, "1️⃣ Nuqta tanlandi!", Toast.LENGTH_SHORT).show()
                placeLayout(firstAnchor!!)
            } else {
                // Eski nuqtalar va masofa itemini tozalash
                secondAnchor?.detach()
                secondAnchor = hitResult.createAnchor()
                distanceNode?.let { arFragment.arSceneView.scene.removeChild(it) }
                distanceNode = null

                Toast.makeText(this, "2️⃣ Nuqta tanlandi!", Toast.LENGTH_SHORT).show()
                placeLayout(secondAnchor!!)
                showDistanceItem()
            }
        }
    }

    private fun placeLayout(anchor: Anchor) {
        ViewRenderable.builder()
            .setView(this, R.layout.item_3d)
            .build().thenAccept { renderable ->
                val anchorNode = AnchorNode(anchor).apply {
                    setParent(arFragment.arSceneView.scene)
                }

                val transformableNode = TransformableNode(arFragment.transformationSystem).apply {
                    this.renderable = renderable
                    setParent(anchorNode)
                    localScale = Vector3(0.07f, 0.07f, 0.07f)
                }
                transformableNode.select()
            }.exceptionally {
                Log.e("AR_ERROR", "❌ Layout yuklanmadi!", it)
                Toast.makeText(this, "❌ Layout yuklanmadi!", Toast.LENGTH_SHORT).show()
                null
            }
    }

    private fun showDistanceItem() {
        if (firstAnchor == null || secondAnchor == null) return

        val firstPose = firstAnchor!!.pose
        val secondPose = secondAnchor!!.pose

        val firstVector = Vector3(firstPose.tx(), firstPose.ty(), firstPose.tz())
        val secondVector = Vector3(secondPose.tx(), secondPose.ty(), secondPose.tz())

        val midPoint = Vector3(
            (firstVector.x + secondVector.x) / 2,
            (firstVector.y + secondVector.y) / 2 + 0.05f,
            (firstVector.z + secondVector.z) / 2
        )

        val distance = Vector3.subtract(firstVector, secondVector).length()
        val roundedDistance = (distance * 10).toInt() / 10.0 // 0.1 m shaklida yaxlitlash
        val formattedDistance = "$roundedDistance m"

        ViewRenderable.builder()
            .setView(this, R.layout.item_distance)
            .build()
            .thenAccept { renderable ->
                val distanceTextView = renderable.view.findViewById<TextView>(R.id.tvDistance)
                distanceTextView.text = formattedDistance

                distanceNode = AnchorNode().apply {
                    setParent(arFragment.arSceneView.scene)
                    worldPosition = midPoint
                }

                val transformableNode = TransformableNode(arFragment.transformationSystem).apply {
                    this.renderable = renderable
                    setParent(distanceNode)
                }
                transformableNode.select()

                arFragment.arSceneView.scene.addChild(distanceNode)

                arFragment.arSceneView.scene.addOnUpdateListener {
                    updateDistance()
                }
            }.exceptionally {
                Log.e("AR_ERROR", "❌ Masofa ko‘rsatilmayapti!", it)
                Toast.makeText(this, "❌ Masofa ko‘rsatilmayapti!", Toast.LENGTH_SHORT).show()
                null
            }
    }

    private fun updateDistance() {
        if (firstAnchor == null || secondAnchor == null || distanceNode == null) return

        val firstPose = firstAnchor!!.pose
        val secondPose = secondAnchor!!.pose

        val firstVector = Vector3(firstPose.tx(), firstPose.ty(), firstPose.tz())
        val secondVector = Vector3(secondPose.tx(), secondPose.ty(), secondPose.tz())

        val midPoint = Vector3(
            (firstVector.x + secondVector.x) / 2,
            (firstVector.y + secondVector.y) / 2 + 0.05f,
            (firstVector.z + secondVector.z) / 2
        )

        val distance = Vector3.subtract(firstVector, secondVector).length()
        val roundedDistance = (distance * 10).toInt() / 10.0 // 0.1 m shaklida yaxlitlash
        val formattedDistance = "$roundedDistance m"

        distanceNode!!.worldPosition = midPoint
        val distanceTextView = (distanceNode!!.children.firstOrNull() as? TransformableNode)
            ?.renderable
            ?.let { it as ViewRenderable }
            ?.view
            ?.findViewById<TextView>(R.id.tvDistance)

        distanceTextView?.text = formattedDistance
    }

    private fun isARCoreSupported(): Boolean {
        val availability = ArCoreApk.getInstance().checkAvailability(this)

        if (ArCoreApk.getInstance()
                .requestInstall(this, true) == ArCoreApk.InstallStatus.INSTALL_REQUESTED
        ) {
            Toast.makeText(this, "⏳ ARCore o‘rnatilmoqda...", Toast.LENGTH_SHORT).show()
            return false
        }

        return availability == ArCoreApk.Availability.SUPPORTED_INSTALLED
    }
}
