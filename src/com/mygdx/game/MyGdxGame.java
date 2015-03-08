package com.mygdx.game;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import net.yzwlab.javammd.GLTexture;
import net.yzwlab.javammd.IGLTextureProvider;
import net.yzwlab.javammd.ReadException;
import net.yzwlab.javammd.jogl.JOGL;
import net.yzwlab.javammd.jogl.io.FileBuffer;
import net.yzwlab.javammd.model.MMDModel;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;

public class MyGdxGame implements ApplicationListener, IGLTextureProvider.Handler {
	public PerspectiveCamera cam;
	public CameraInputController cam_control;
	public ModelBatch model_batch;

	//File pmd_dir = new File("mmd/Models/Tda Hagane Miku APPEND/Hagane APPEND V2/Tda Hagane Miku.pmx");
	//File pmd_dir = new File("mmd/Models/Tda2698489/Tda®¹~NEAyh_Ver1.00(nCq[üÏ).pmx");
	File pmd_dir = new File("mmd/Models/洛天依ver1.10/¡¾ÂåÌìÒÀLuoTianYi¡¿.pmd");
	//File pmd_dir = new File("mmd/Models/saberlily/saberlily1.pmd");
	//File vmd_dir = new File("mmd/VMD/aoitori.vmd");
	//File vmd_dir = new File("mmd/VMD/恋愛サーキュレーション/楒垽僒乕僉儏儗乕僔儑儞-偪傃.vmd");
	File vmd_dir = new File("mmd/VMD/新华里-熊猫团.vmd");
	File pmd_filename = new File(pmd_dir.getName());
	File vmd_filename = new File(vmd_dir.getName());
	boolean loaded = false;
	JOGL jogl;
	MMDModel mmd_model;
	
	File music_file = new File("luotianyi_xinhuali.mp3");
	//Music music;
	
	static final ModelBuilder model_builder = new ModelBuilder();
	final File calib_file = new File("camera.dat");
	final int MAX_MODELS = 15;
	
	Timer timer;
	VideoCapture vc;
	CameraCalibrator calib;
	//double m_z_scale = -1.0;
	MatOfPoint2f prev_c = new MatOfPoint2f(new Mat(4, 2, CvType.CV_32FC1));
	Mat webcam, gray, binary, undist_webcam;
	//AssetManager assets = new AssetManager();

	//AssetManager assets = new AssetManager();
	//boolean loading;

	ArrayList<ModelInfo> model_list = new ArrayList<ModelInfo>();
	final float frame_rate = 30;
	
	@Override
	public void create () {
		cam = new PerspectiveCamera(/*67*/40f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.up.set(0, -1, 0);
		cam.position.set(0f, 0f, 0f);
		cam.lookAt(0, 0, 1);
		cam.near = 0.1f;
		cam.far = 2000f;
		cam.update();
		
		//cam_control = new CameraInputController(cam);
		Gdx.input.setInputProcessor(cam_control);
		Gdx.graphics.setTitle("Happy Girls' Day!");
		
		model_batch = new ModelBatch();
		
        vc = new VideoCapture(0);
        //vc.open(0);
        if (!vc.isOpened())
        	System.out.print("unable to open camera\n");

		webcam = new Mat();
		if (!vc.grab())
			System.out.print("unable to grab capture\n");
		if (!vc.retrieve(webcam))
			System.out.print("unable to retrieve capture\n");
		gray = new Mat(webcam.rows(), webcam.cols(), CvType.CV_8UC1);
		binary = new Mat(webcam.rows(), webcam.cols(), CvType.CV_8UC1);
		calib = new CameraCalibrator(webcam.cols(), webcam.rows());
		undist_webcam = new Mat();
		calib.load(calib_file);
		
		Mat tempmat = Mat.zeros(150, 400, CvType.CV_8UC3);
        Core.putText(tempmat, "Loading...", new Point(50, 100),
                Core.FONT_HERSHEY_SIMPLEX, 2.0, new Scalar(255, 255, 0));
        UtilAR.imShow("Loading...", tempmat);
		
		//testInit();
		initModel();
		
		//music = Gdx.audio.newMusic(new FileHandle(music_file));
	}

	@Override
	public void render () {
		//System.out.print("Render start\n");
		//System.out.print(System.currentTimeMillis() + "\n");
        UtilAR.imClose("Loading...");
        
		if (!vc.grab())
			System.out.print("unable to grab capture\n");
		if (!vc.retrieve(webcam))
			System.out.print("unable to retrieve capture\n");
		if (!calib.isCalibrated())
		{
			Imgproc.cvtColor(webcam, gray, Imgproc.COLOR_BGR2GRAY);
			calib.processFrame(gray, webcam);
			if (Math.random() < 0.5)
				calib.addCorners();
			if (calib.getCornersBufferSize() >= 20)
			{
				calib.calibrate();
				try {
					calib.save(calib_file);
				} catch (IOException e) {
					e.printStackTrace();
					System.out.print("Saving camera info failed\n");
				}
			}
			undist_webcam = webcam.clone();
		}
		else
			Imgproc.undistort(webcam, undist_webcam, calib.getCameraMatrix(), calib.getDistortionCoefficients());
		
		Imgproc.cvtColor(undist_webcam, gray, Imgproc.COLOR_BGR2GRAY);
		Imgproc.adaptiveThreshold(gray, binary, 255,
				Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 101, 30);
		
		Mat intrinsics;
		MatOfDouble distortion;
		intrinsics = calib.getCameraMatrix();
		distortion = new MatOfDouble(calib.getDistortionCoefficients());

		ArrayList<MMDModelInstance> instances_list = new ArrayList<MMDModelInstance>();
		if (calib.isCalibrated()) //contours
		{
			List<MatOfPoint> contours_3 = new ArrayList<MatOfPoint>();
			Imgproc.findContours(binary, contours_3, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
			//Imgproc.drawContours(undist_webcam, contours_3, -1, new Scalar(0, 0, 255));
			
			List<MatOfPoint2f> contours = new ArrayList<MatOfPoint2f>();
			List<MatOfPoint> approx_curves = new ArrayList<MatOfPoint>();
			List<MatOfPoint2f> approx_curves2f = new ArrayList<MatOfPoint2f>();
			for (MatOfPoint c : contours_3)
				contours.add(new MatOfPoint2f(c.toArray()));
			for (MatOfPoint2f curve : contours)
			{
				MatOfPoint2f approxCurve = new MatOfPoint2f();
				Imgproc.approxPolyDP(curve, approxCurve, 50, true);
				if (approxCurve.rows() == 4 &&
						isCCConvexPolygon(approxCurve))
				{
					approx_curves.add(new MatOfPoint(approxCurve.toArray()));
					approx_curves2f.add(approxCurve);
				}
			}
			
			List<MatOfPoint2f> quad_list = new ArrayList<MatOfPoint2f>();
			int[] matching = new int[approx_curves2f.size()];
			for (ModelInfo i : model_list)
				quad_list.add(i.quad);
			int[] matching_inv = new int[quad_list.size()];
			matchQuads(approx_curves2f, quad_list, matching, matching_inv);
			
			MatOfPoint3f object_corners = new MatOfPoint3f(new Point3[]
					{
						new Point3(-7, 14, -3),
						new Point3(7, 14, -3),
						new Point3(7, 0, -3),
						new Point3(-7, 0, -3)
					});
			instances_list.clear();
			int i = 0;
			i = 0;
			//System.out.println("approx_curves.size=" + approx_curves.size());
			for (ModelInfo info : model_list)
			{
				//System.out.println("matching_inv[" + i + "]=" + matching_inv[i]);
				if (matching_inv[i] == -1)
				{
					info.pause();
				}
				i++;
			}
			i = 0;
			for (MatOfPoint2f c : approx_curves2f)
			{				
				ModelInfo model_info = null;
				if (matching[i] == -1) //new quad
				{
					model_info = new ModelInfo(mmd_model, c, jogl, Gdx.audio.newMusic(new FileHandle(music_file)));
					if (model_info != null)
						model_list.add(model_info);
					if (model_list.size() > MAX_MODELS)
					{
						//model_list.remove(0);
						/*
						for (ModelInfo info : model_list)
						{
							if (info.isPlaying() == false)
							{
								model_list.remove(info);
								break;
							}
						}*/
					}
				}
				else //old quad, renew
				{
					matching_inv[matching[i]] = 1;
					model_info = model_list.get(matching[i]);
					c = adjustPolygonToMatch(c, model_info.quad);
					model_info.quad = c;
					//System.out.print("Old quad " + i + "," + matching[i] + "\n");
				}
				i++;
				//if (model_info.checkLoaded())
				{
					Matrix4 transform = correctSolvePnP(object_corners, c,
							intrinsics, distortion, cam.fieldOfView, webcam.rows());
					Matrix4 ttransl = new Matrix4();
					if (model_info != null)
						instances_list.add(new MMDModelInstance(model_info.model, transform, model_info.updateFrameNo(frame_rate)));
				}
				List<MatOfPoint> tmp = new ArrayList<MatOfPoint>();
				tmp.add(new MatOfPoint(c.toArray()));
				//if (model_info != null)
				//	Imgproc.drawContours(undist_webcam, tmp, -1, model_info.color);
				
				//music
				if (model_info != null)
					model_info.play();
/*
				//unwarp
				Mat unwarp_webcam = new Mat(400, 400, webcam.type());
				MatOfPoint2f dst = new MatOfPoint2f(new Point[] {
						new Point(0, 0),
						new Point(unwarp_webcam.rows(), 0),
						new Point(unwarp_webcam.rows(), unwarp_webcam.cols()),
						new Point(0, unwarp_webcam.cols())
				});
				if (c != null)
				{
					Mat warp = Imgproc.getPerspectiveTransform(c, dst);
					Imgproc.warpPerspective(undist_webcam, unwarp_webcam, warp, unwarp_webcam.size(), Imgproc.INTER_LINEAR);
					//UtilAR.imShow("unwarp", unwarp_webcam);
					String code = CodeHelper.decode(unwarp_webcam);
					System.out.print("QR code: " + code + "\n");
				}		*/		
			}

		}

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	    Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);
	    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	    Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER,
				GL20.GL_LINEAR);
	    Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER,
				GL20.GL_LINEAR);
		UtilAR.imDrawBackground(undist_webcam);
		
		jogl.setCurrentProgram();
		
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
		Gdx.gl.glDepthMask(true);
		Gdx.gl.glClearDepthf(1.0f / 0.0f);
		Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
		
		cam.up.set(0, -1, 0);
		cam.position.set(0f, 0f, 0f);
		cam.lookAt(0, 0, 1);
		cam.near = 0.1f;
		cam.far = 2000f;
		cam.update();
		
		jogl.glPushMatrix();
		jogl.setCamera(cam);
		if (calib.isCalibrated())
		{
	        for (MMDModelInstance m : instances_list)
	        	m.draw(jogl);
	    }
		jogl.glPopMatrix();
	}

	@Override
	public void resize(int width, int height) {
		cam.viewportWidth = Gdx.graphics.getWidth();
		cam.viewportHeight = Gdx.graphics.getHeight();
		cam.update();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
	}

	@Override
	public void dispose() {
		//model.dispose();
		vc.release();
	}
	
	Matrix4 toMatrix4(Mat rotation, Mat translation) {
		Mat rotation33 = new Mat();
		Calib3d.Rodrigues(rotation, rotation33);
		float[] transform_float = new float[16];
		for (int i = 0; i < 3; i++)
		{
			for (int j = 0; j < 3; j++)
				transform_float[i * 4 + j] = (float) rotation33.get(j, i)[0];
			transform_float[i * 4 + 3] = 0;
		}
		//transform_float[12] = transform_float[13] = transform_float[14] = 0;
		transform_float[12] = (float) translation.get(0, 0)[0]; 
		transform_float[13] = (float) translation.get(1, 0)[0]; 
		transform_float[14] = (float) (translation.get(2, 0)[0] /* m_z_scale*/); 
		transform_float[15] = 1;
		Matrix4 transform = new Matrix4(), tr_rotation = new Matrix4(transform_float);
		transform = transform.mul(tr_rotation);
		return transform;
	}
	
	Point3 transform(Matrix4 t, Point3 p) {
		float[] q = new float[] {(float) p.x, (float) p.y, (float) p.z};
		Matrix4.mulVec(t.getValues(), q);
		return new Point3(q[0], q[1], q[2]);
	}
	
	double scaleZ(Matrix4 transform, Point3 objc0, Point3 objc1, Point c0, Point c1, double fov, double webcam_rows) {
		Point3 P0 = transform(transform, objc0),
				P1 = transform(transform, objc1); //3d points using PnP
		Point p0 = new Point(P0.x / P0.z, P0.y / P0.z),
				p1 = new Point(P1.x / P1.z, P1.y / P1.z); //projection on screen of P0, P1
		Point d_corners = new Point(c0.x - c1.x, c0.y - c1.y);
		Point dp = new Point(p0.x - p1.x, p0.y - p1.y);
		double alpha = fov / 180.0 * Math.PI *
				Math.sqrt(d_corners.x * d_corners.x + d_corners.y * d_corners.y) / webcam_rows;
		double beta = Math.sqrt(dp.x * dp.x + dp.y * dp.y);
		//System.out.print("p0=" + p0.x + "," + p0.y + "\n");
		double z_scale = beta / alpha;
		return z_scale;
	}
	
	Matrix4 correctSolvePnP(MatOfPoint3f object_corners, MatOfPoint2f corners,
			Mat intrinsics, MatOfDouble distortion, double fov, double height) {
		Mat rotation = new Mat(), translation = new Mat();
		Calib3d.solvePnP(object_corners, corners, intrinsics, distortion,
				rotation, translation);
				//false, Calib3d.CV_EPNP);
		
		Matrix4 transform = toMatrix4(rotation, translation);
		Point3[] object_corners_array = object_corners.toArray();
		Point[] corners_array = corners.toArray();
		int C0 = 0, C1 = object_corners_array.length - 1;
		double z_scale = scaleZ(transform, object_corners_array[C0], object_corners_array[C1],
				corners_array[C0], corners_array[C1], fov, height);
		/*
		if (m_z_scale < 0)
			m_z_scale = z_scale;
		System.out.print("m_z_scale = " + m_z_scale + "\n");
		*/
		//System.out.print("z_scale = " + z_scale + "\n");
		translation.put(2, 0, z_scale * translation.get(2, 0)[0]);
		transform = toMatrix4(rotation, translation);
		return transform;
	}
	
	boolean isCCConvexPolygon(MatOfPoint2f p) {
		Point[] v = p.toArray();
		int n = v.length;
		int cnt = 0;
		for (int i = 0; i < n; i++)
		{
			Point n1 = v[(i + 1) % n];
			Point n2 = v[(i + 2) % n];
			Point d1 = new Point(n1.x - v[i].x, n1.y - v[i].y);
			Point d2 = new Point(n2.x - n1.x, n2.y - n1.y);
			if (d1.x * d2.y - d1.y * d2.x > 0)
				cnt++;
		}
		return cnt == n;
	}
	
	MatOfPoint2f adjustPolygonToMatch(MatOfPoint2f mp, MatOfPoint2f mq) {
		Point[] p = mp.toArray();
		Point[] q = mq.toArray();
		int n = p.length;
		//assert p.length == q.length
		int maxd = 0;
		double cost = 1.0 / 0.0;
		for (int d = 0; d < n; d++)
		{
			double tc = 0.0;
			for (int i = 0; i < n; i++)
				tc += dist2(p[(i + d) % n], q[i]);
			if (tc < cost)
			{
				maxd = d;
				cost = tc;
			}
		}
		Point[] r = new Point[n];
		for (int i = 0; i < n; i++)
			r[i] = p[(i + maxd) % n].clone();
		return new MatOfPoint2f(r);
	}
	
	double distPoly(MatOfPoint2f mp, MatOfPoint2f mq) {
		Point[] p = mp.toArray();
		Point[] q = mq.toArray();
		int n = p.length;
		//assert p.length == q.length
		int maxd = 0;
		double cost = 1.0 / 0.0;
		for (int d = 0; d < n; d++)
		{
			double tc = 0.0;
			for (int i = 0; i < n; i++)
				tc += dist2(p[(i + d) % n], q[i]);
			if (tc < cost)
			{
				maxd = d;
				cost = tc;
			}
		}
		return cost;
	}
	
	double dist2(Point a, Point b) {
		return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
	}
	
	double quad_match_thres = 1E4;
	double quad_match_offset = 2E6 * 4;
	void matchQuads(List<MatOfPoint2f> src, List<MatOfPoint2f> dst, int[] matching, int[] matching_inv) {
		double[][] w = new double[src.size()][dst.size()];
		int i, j;
		i = 0;
		for (MatOfPoint2f p : src)
		{
			j = 0;
			for (MatOfPoint2f q : dst)
			{
				w[i][j] = quad_match_offset - distPoly(p, q);
				j++;
			}
			i++;
		}
		MatchingAlg.maxBipartite(w, matching, matching_inv);
		for (i = 0; i < src.size(); i++)
			if (matching[i] != -1)
			{
				j = matching[i];
				if (w[i][j] < quad_match_offset - quad_match_thres)
				{
					matching_inv[j] = -1;
					matching[i] = -1;
				}
			}
		double avg_dist = 0.0;
		int cnt_mat = 0;
		for (i = 0; i < src.size(); i++)
			if (matching[i] != -1)
			{
				cnt_mat++;
				avg_dist += quad_match_offset - w[i][matching[i]];
			}
		if (cnt_mat != 0)
		{
			avg_dist /= cnt_mat;
			//quad_match_thres += (avg_dist * 3.0 - quad_match_thres) * 0.1;
		}
	}

	void initModel() {
		//new TestDrawer(Gdx.gl30, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());		
		jogl = new JOGL(pmd_dir.getParentFile(), Gdx.gl);

		mmd_model = new MMDModel();
		try {
			System.out.println("Start loading pmd");
			mmd_model.openPMD(new FileBuffer(pmd_dir));
			System.out.println("Start loading vmd");
			mmd_model.openVMD(new FileBuffer(vmd_dir));
		} catch (ReadException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (loaded == false) {
			try {
				mmd_model.prepare(jogl, this);
			} catch (ReadException e) {
				e.printStackTrace();
			}
			loaded = true;
		}
	}

	//IGLTextureProvider.Handler
	@Override
	public void onSuccess(byte[] filename, GLTexture desc) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onError(byte[] filename, Throwable error) {
		// TODO Auto-generated method stub
		error.printStackTrace();
	}
	
}
