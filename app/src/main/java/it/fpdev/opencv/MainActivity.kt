package it.fpdev.opencv

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.core.Mat

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private var rgba: Mat? = null
    private var img: Mat? = null

    override fun onCameraViewStarted(width: Int, height: Int) {
        rgba = Mat()
        img = Mat()
    }

    override fun onCameraViewStopped() {
        rgba?.release()
        img?.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {

        rgba = inputFrame.rgba()
        img = inputFrame.gray()

//        return img!!

//        img + top hat - black hat
        val tophat = Mat()
        val blackhat = Mat()
        val kernel = Mat.ones(5, 5, CvType.CV_8U)
        Imgproc.morphologyEx(img, blackhat, Imgproc.MORPH_BLACKHAT, kernel)
        Imgproc.morphologyEx(img, tophat, Imgproc.MORPH_TOPHAT, kernel)

        Core.add(img!!, blackhat, img)
        Core.subtract(img!!, tophat, img)

//        return img!!
//        return blackhat
//        return tophat

        Imgproc.GaussianBlur(img, img, Size(5.0, 5.0), 0.0)

        Imgproc.Canny(img, img, 64.0, 255.0)
//        Imgproc.Sobel(img, img, CvType.CV_8U, 1, 0)
//        Imgproc.threshold(img, img, 64.0, 255.0, Imgproc.THRESH_BINARY/* + Imgproc.THRESH_OTSU*/)

//        return img!!

        val se = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, Size(32.0, 8.0))//TODO custom values
        Imgproc.morphologyEx(img, img, Imgproc.MORPH_CLOSE, se)

//        return img!!

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(img, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE)
        hierarchy.release()

        try {
            for (cnt in contours) {
                val cntApprox = MatOfPoint2f(*cnt.toArray())
                val rotatedRect = normalizeRect(Imgproc.minAreaRect(cntApprox))
                if (rotatedRect.size.width < rotatedRect.size.height) continue
                if (rotatedRect.angle < -20 || rotatedRect.angle > 20) continue
                val area = rotatedRect.size.width * rotatedRect.size.height
                val ratio = rotatedRect.size.width / rotatedRect.size.height
                if (area < 1000) continue
                if (area > 50000) continue
                if (ratio < 1) continue
                if (ratio > 10) continue

                val rectPoints = arrayOf(Point(0.0, 0.0), Point(0.0, 0.0), Point(0.0, 0.0), Point(0.0, 0.0))
                rotatedRect.points(rectPoints)

                drawContours(img!!, rectPoints)
                drawText(img!!, rectPoints[0], "${area.toInt()}")
                drawText(img!!, rectPoints[1], "${rotatedRect.angle}")
                drawText(img!!, rectPoints[2], "${ratio.toInt()}")

                drawContours(rgba!!, rectPoints)
                drawText(rgba!!, rectPoints[0], "${area.toInt()}")
                drawText(rgba!!, rectPoints[1], "${rotatedRect.angle.toInt()}")
                drawText(rgba!!, rectPoints[2], "${ratio.toInt()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

//        return img!!
        return rgba!!
    }

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    mOpenCvCameraView.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        mOpenCvCameraView.setCvCameraViewListener(this)
    }

    override fun onResume() {
        super.onResume()

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onPause() {
        super.onPause()

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val TAG = "OCVSample::Activity"
        private val COLOR_RED = Scalar(255.0, 0.0, 0.0, 1.0)
        private val COLOR_YELLOW = Scalar(255.0, 255.0, 0.0, 1.0)

        private fun normalizeRect(rect: RotatedRect): RotatedRect {
            if (rect.size.height < rect.size.width && rect.angle < -45) {
                val rect2 = rect.clone()
                rect2.size.width = rect.size.height
                rect2.size.height = rect.size.width
                rect2.angle = rect.angle + 90
//            rect2.center = rect.center
                return rect2
            }

            return rect
        }

        private fun drawContours(dst: Mat, points: Array<Point>) {
            for (j in 0 until points.size) {
                Imgproc.line(dst, points[j], points[(j + 1) % points.size], COLOR_RED, 1)
            }
        }

        private fun drawText(dst: Mat, point: Point, text: String) {
            Imgproc.putText(dst, text, point, Core.FONT_HERSHEY_SIMPLEX, 0.5, COLOR_YELLOW, 1)
        }
    }
}
