package com.mygdx.game;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;

public class CameraCalibrator {
    private static final String TAG = "OCVSample::CameraCalibrator";

    private final Size mPatternSize = new Size(6, 9);
    private final int mCornersSize = (int)(mPatternSize.width * mPatternSize.height);
    private boolean mPatternWasFound = false;
    private MatOfPoint2f mCorners = new MatOfPoint2f();
    private List<Mat> mCornersBuffer = new ArrayList<Mat>();
    private boolean mIsCalibrated = false;

    private Mat mCameraMatrix = new Mat();
    private Mat mDistortionCoefficients = new Mat();
    private int mFlags;
    private double mRms;
    private double mSquareSize = 0.0181;
    private Size mImageSize;

    public CameraCalibrator(int width, int height) {
        mImageSize = new Size(width, height);
        mFlags = Calib3d.CALIB_FIX_PRINCIPAL_POINT +
                 Calib3d.CALIB_ZERO_TANGENT_DIST +
                 Calib3d.CALIB_FIX_ASPECT_RATIO +
                 Calib3d.CALIB_FIX_K4 +
                 Calib3d.CALIB_FIX_K5;
        Mat.eye(3, 3, CvType.CV_64FC1).copyTo(mCameraMatrix);
        mCameraMatrix.put(0, 0, 1.0);
        Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(mDistortionCoefficients);
        System.out.print("Instantiated new " + this.getClass() + "\n");
    }

    public void processFrame(Mat grayFrame, Mat rgbaFrame) {
        //findPattern(grayFrame);
    	findPattern(rgbaFrame);
        renderFrame(rgbaFrame);
    }

    public void calibrate() {
        ArrayList<Mat> rvecs = new ArrayList<Mat>();
        ArrayList<Mat> tvecs = new ArrayList<Mat>();
        Mat reprojectionErrors = new Mat();
        ArrayList<Mat> objectPoints = new ArrayList<Mat>();
        objectPoints.add(Mat.zeros(mCornersSize, 1, CvType.CV_32FC3));
        calcBoardCornerPositions(objectPoints.get(0));
        for (int i = 1; i < mCornersBuffer.size(); i++) {
            objectPoints.add(objectPoints.get(0));
        }

        Calib3d.calibrateCamera(objectPoints, mCornersBuffer, mImageSize,
                mCameraMatrix, mDistortionCoefficients, rvecs, tvecs, mFlags);

        mIsCalibrated = Core.checkRange(mCameraMatrix)
                && Core.checkRange(mDistortionCoefficients);

        mRms = computeReprojectionErrors(objectPoints, rvecs, tvecs, reprojectionErrors);
        System.out.print(String.format("Average re-projection error: %f", mRms) + "\n");
        System.out.print("Camera matrix: " + mCameraMatrix.dump() + "\n");
        System.out.print("Distortion coefficients: " + mDistortionCoefficients.dump() + "\n");
    }

    public void clearCorners() {
        mCornersBuffer.clear();
    }

    private void calcBoardCornerPositions(Mat corners) {
        final int cn = 3;
        float positions[] = new float[mCornersSize * cn];

        for (int i = 0; i < mPatternSize.height; i++) {
            for (int j = 0; j < mPatternSize.width * cn; j += cn) {
                positions[(int) (i * mPatternSize.width * cn + j + 0)] =
                        (2 * (j / cn) + i % 2) * (float) mSquareSize;
                positions[(int) (i * mPatternSize.width * cn + j + 1)] =
                        i * (float) mSquareSize;
                positions[(int) (i * mPatternSize.width * cn + j + 2)] = 0;
            }
        }
        corners.create(mCornersSize, 1, CvType.CV_32FC3);
        corners.put(0, 0, positions);
    }

    private double computeReprojectionErrors(List<Mat> objectPoints,
            List<Mat> rvecs, List<Mat> tvecs, Mat perViewErrors) {
        MatOfPoint2f cornersProjected = new MatOfPoint2f();
        double totalError = 0;
        double error;
        float viewErrors[] = new float[objectPoints.size()];

        MatOfDouble distortionCoefficients = new MatOfDouble(mDistortionCoefficients);
        int totalPoints = 0;
        for (int i = 0; i < objectPoints.size(); i++) {
            MatOfPoint3f points = new MatOfPoint3f(objectPoints.get(i));
            Calib3d.projectPoints(points, rvecs.get(i), tvecs.get(i),
                    mCameraMatrix, distortionCoefficients, cornersProjected);
            error = Core.norm(mCornersBuffer.get(i), cornersProjected, Core.NORM_L2);

            int n = objectPoints.get(i).rows();
            viewErrors[i] = (float) Math.sqrt(error * error / n);
            totalError  += error * error;
            totalPoints += n;
        }
        perViewErrors.create(objectPoints.size(), 1, CvType.CV_32FC1);
        perViewErrors.put(0, 0, viewErrors);

        return Math.sqrt(totalError / totalPoints);
    }

    private void findPattern(Mat grayFrame) {
    	/*
        mPatternWasFound = Calib3d.findCirclesGridDefault(grayFrame, mPatternSize,
                mCorners, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
                */
    	mPatternWasFound = Calib3d.findChessboardCorners(grayFrame, mPatternSize, mCorners,
    			Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK + Calib3d.CALIB_CB_FILTER_QUADS);
    	//System.out.print("Pattern " + (mPatternWasFound ? "" : "not ") + "found\n");
    }

    public void addCorners() {
        if (mPatternWasFound) {
            mCornersBuffer.add(mCorners.clone());
        }
    }

    private void drawPoints(Mat rgbaFrame) {
        Calib3d.drawChessboardCorners(rgbaFrame, mPatternSize, mCorners, mPatternWasFound);
    }

    private void renderFrame(Mat rgbaFrame) {
        drawPoints(rgbaFrame);

        Core.putText(rgbaFrame, "Captured: " + mCornersBuffer.size(), new Point(rgbaFrame.cols() / 3 * 2, rgbaFrame.rows() * 0.1),
                Core.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(255, 255, 0));
    }

    public Mat getCameraMatrix() {
        return mCameraMatrix;
    }

    public Mat getDistortionCoefficients() {
        return mDistortionCoefficients;
    }

    public int getCornersBufferSize() {
        return mCornersBuffer.size();
    }

    public double getAvgReprojectionError() {
        return mRms;
    }

    public boolean isCalibrated() {
        return mIsCalibrated;
    }

    public void setCalibrated() {
        mIsCalibrated = true;
    }
    
    public void save(File f) throws IOException {
    	if (!isCalibrated())
    		return;
    	FileOutputStream fos = new FileOutputStream(f);
    	DataOutputStream dos = new DataOutputStream(fos);
    	saveDouble1CMat(mCameraMatrix, dos);
    	saveDouble1CMat(mDistortionCoefficients, dos);
    }
    
    public void load(File f) {
    	try {
        	FileInputStream fis = new FileInputStream(f);
        	DataInputStream dis = new DataInputStream(fis);
			loadDouble1CMat(dis).copyTo(mCameraMatrix);
	    	loadDouble1CMat(dis).copyTo(mDistortionCoefficients);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.print("Loading camera info failed\n");
			return;
		}
    	setCalibrated();
    }
    
    void saveDouble1CMat(Mat m, DataOutputStream dos) throws IOException {
    	dos.writeInt(m.rows());
    	dos.writeInt(m.cols());
    	for (int i = 0; i < m.rows(); i++)
    		for (int j = 0; j < m.cols(); j++)
    		{
    			dos.writeDouble(m.get(i, j)[0]);
    		}
    }
    
    Mat loadDouble1CMat(DataInputStream dis) throws IOException {
    	int r = dis.readInt();
    	int c = dis.readInt();
    	Mat m = new Mat(r, c, CvType.CV_64FC1);
    	for (int i = 0; i < r; i++)
    		for (int j = 0; j < c; j++)
    		{
				m.put(i, j, dis.readDouble());
    		}
    	return m;
    }
    
}