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

    private var rgba : Mat? = null
    private var img : Mat? = null

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

//        Imgproc.circle(rgba, Point(100.0, 100.0), 0, Scalar(255.0, 0.0, 0.0, 1.0), 5)

//        Imgproc.cvtColor(rgba, img, Imgproc.COLOR_BGR2GRAY)

        Imgproc.GaussianBlur(img, img, Size(5.0, 5.0), 0.0)

        Imgproc.Sobel(img, img, -1, 1, 0)

        Imgproc.threshold(img, img, 127.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)

        val se = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, Size(32.0, 8.0))//TODO custom values

        Imgproc.morphologyEx(img, img, Imgproc.MORPH_CLOSE, se)

//        findContours(image, contours, hierarchy, mode, method)
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(img, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE)
        hierarchy.release()

        try {

//            val contoursApprox = mutableListOf<MatOfPoint>()
//            for (poly in contours) {
//                contoursApprox.add(MatOfPoint(*poly.toArray()))
//            }
//            for (i in 0 until contours.size) {
////                Imgproc.drawContours(img, contoursApprox, i, Scalar(255.0, 0.0, 0.0, 1.0), 2)
//                Imgproc.drawContours(rgba, contoursApprox, i, Scalar(255.0, 0.0, 0.0, 1.0), 2)
//            }

            for (cnt in contours) {
                val cntApprox = MatOfPoint2f(*cnt.toArray())
                val rotatedRect = Imgproc.minAreaRect(cntApprox)

                val area = rotatedRect.size.width * rotatedRect.size.height
                if (area < 500 || area > 15000) continue //TODO area variable or fixed source img size
                if ((rotatedRect.angle > -75 && rotatedRect.angle < -15) ||
                    (rotatedRect.angle > 15 && rotatedRect.angle < 75)
                ) continue

                val vertices = arrayOfNulls<Point>(4)
                rotatedRect.points(vertices)
                rotatedRect.points(vertices)
                for (j in 0..3) {
                    Imgproc.line(rgba, vertices[j], vertices[(j + 1) % 4], Scalar(255.0, 0.0, 0.0, 1.0), 2)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

//        return img
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
    }
}
