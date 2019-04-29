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
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Core







class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    override fun onCameraViewStarted(width: Int, height: Int) {

    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {

        val rgba = inputFrame.rgba()
        val img = Mat()

//        Imgproc.circle(rgba, Point(100.0, 100.0), 0, Scalar(255.0, 0.0, 0.0, 1.0), 5)

        Imgproc.cvtColor(rgba, img, Imgproc.COLOR_BGR2GRAY)

        Imgproc.GaussianBlur(img, img, Size(5.0, 5.0), 0.0)

        Imgproc.Sobel(img, img, -1, 1, 0)

        val se = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, Size(32.0, 8.0))

        Imgproc.morphologyEx(img, img, Imgproc.MORPH_CLOSE, se)

//        findContours(image, contours, hierarchy, mode, method)
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(img, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE)
        hierarchy.release()

        try {

            for (cnt in contours) {
                val points = MatOfPoint2f()
                Core.findNonZero(cnt, points)

                val rotatedRect = Imgproc.minAreaRect(points)
                val box = MatOfPoint2f()
                Imgproc.boxPoints(rotatedRect, box)
//            Imgproc.drawContours(rgba, [cnt], 0, (255, 0, 0), 2)
//            Imgproc.drawContours(rgba, [box], 0, (0, 0, 255), 2)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return img
//        return rgba
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
    }
}
