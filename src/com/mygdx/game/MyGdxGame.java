package com.mygdx.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

public class MyGdxGame implements ApplicationListener {
	public PerspectiveCamera cam;
	public CameraInputController cam_control;
	public ModelBatch model_batch;
	public Model model;
	public Model[] coord_model;
	public ModelInstance instance;
	public ModelInstance[] coord_instance;
	public Model box_model;
	public ModelInstance[] boxes_instance;
	public Environment environment;
	
	final File calib_file = new File("camera.dat");
	//final String model_filename = "models/more/textures/Miku_1_4.g3db";
	//final String model_filename = "models/more/EnoshimaJunko/TEX/junko.g3db";
	final String model_filename = "models/BRS/BRSDigitrevx/BRS.g3db";
	final int MAX_MODELS = 15;
	
	Timer timer;
	VideoCapture vc;
	CameraCalibrator calib;
	//double m_z_scale = -1.0;
	MatOfPoint2f prev_c = new MatOfPoint2f(new Mat(4, 2, CvType.CV_32FC1));

	//AssetManager assets = new AssetManager();
	//boolean loading;
	class ModelInfo
	{
		public ModelInfo(String file_name, MatOfPoint2f c) {
			asset = new AssetManager();
			loading = true;
			asset.load(file_name, Model.class);
			name = file_name;
			model = null;
			quad = c;
			double b = Math.random();
			double g = Math.random();
			double r = Math.random();
			double v = Math.max(Math.max(r, g), b) / 255.0;
			color = new Scalar(b / v, g / v, r / v);
		}
		public boolean checkLoaded() {
			if (loading && asset.update())
			{
				model = asset.get(name, Model.class);
				if (model == null)
					model = new ModelBuilder().createBox(3f, 3f, 3f,
							new Material(ColorAttribute.createDiffuse(Color.GREEN)),
							Usage.Position | Usage.Normal);
				loading = false;
				return true;
			}
			return !loading;
		}
		public MatOfPoint2f quad;
		AssetManager asset;
		String name;
		Model model;
		boolean loading;
		Scalar color;
	};
	ArrayList<ModelInfo> model_list = new ArrayList<ModelInfo>();
	
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
		
		model_batch = new ModelBatch();
		ModelBuilder builder = new ModelBuilder();
		model = builder.createBox(3f, 3f, 3f,
				new Material(ColorAttribute.createDiffuse(Color.GREEN)),
				Usage.Position | Usage.Normal);
		coord_model = new Model[3];
		coord_model[0] = builder.createArrow(0f, 0f, 0f, 1f, 0f, 0f,
				0.2f, 0.1f, 15, GL20.GL_TRIANGLES,
				new Material(ColorAttribute.createDiffuse(Color.RED)),
				Usage.Position | Usage.Normal);
		coord_model[1] = builder.createArrow(0f, 0f, 0f, 0f, 1f, 0f,
				0.2f, 0.1f, 15, GL20.GL_TRIANGLES,
				new Material(ColorAttribute.createDiffuse(Color.GREEN)),
				Usage.Position | Usage.Normal);
		coord_model[2] = builder.createArrow(0f, 0f, 0f, 0f, 0f, 1f,
				0.2f, 0.1f, 15, GL20.GL_TRIANGLES,
				new Material(ColorAttribute.createDiffuse(Color.BLUE)),
				Usage.Position | Usage.Normal);
		instance = new ModelInstance(model, 1f, 0.5f, 0.75f);
		coord_instance = new ModelInstance[3];

		box_model = builder.createBox(1f, 1f, 1f,
				//new Material(ColorAttribute.createDiffuse(new Color(0.0f, 0.0f, 1.0f, 0.3f))),
				new Material(new BlendingAttribute(0.3f)),
				Usage.Position | Usage.Normal);
		//model_list.add(new ModelInfo(model_filename));
		//assets.load(model_filename, Model.class);
		//loading = true;
		
		environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        vc = new VideoCapture(0);
        //vc.open(0);
        if (!vc.isOpened())
        	System.out.print("unable to open camera\n");

		Mat webcam = new Mat();
		if (!vc.grab())
			System.out.print("unable to grab capture\n");
		if (!vc.retrieve(webcam))
			System.out.print("unable to retrieve capture\n");
		calib = new CameraCalibrator(webcam.cols(), webcam.rows());
		calib.load(calib_file);
	}

	@Override
	public void render () {
		for (ModelInfo i : model_list)
			if (i.checkLoaded())
			{
				box_model = i.model;
			}
		
		Mat webcam = new Mat();
		if (!vc.grab())
			System.out.print("unable to grab capture\n");
		if (!vc.retrieve(webcam))
			System.out.print("unable to retrieve capture\n");
		Mat gray = new Mat(webcam.rows(), webcam.cols(), CvType.CV_8UC1);
		Mat binary = new Mat(webcam.rows(), webcam.cols(), CvType.CV_8UC1);
		Mat undist_webcam = new Mat();
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
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.print("Saving camera info failed\n");
				}
			}
			undist_webcam = webcam;
		}
		else
			Imgproc.undistort(webcam, undist_webcam, calib.getCameraMatrix(), calib.getDistortionCoefficients());

		Imgproc.cvtColor(undist_webcam, gray, Imgproc.COLOR_BGR2GRAY);
		//Imgproc.threshold(gray, binary, 80, 220, Imgproc.THRESH_BINARY);
		Imgproc.adaptiveThreshold(gray, binary, 255,
				Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 101, 30);
		//UtilAR.imShow("gray", gray);
		//UtilAR.imShow("binary", binary);
		/*
		Mat canny = new Mat();
		Imgproc.Canny(webcam, canny, 80, 220);
		UtilAR.imShow("canny", canny);
		*/
		
		Mat intrinsics;
		MatOfDouble distortion;
		intrinsics = calib.getCameraMatrix();
		distortion = new MatOfDouble(calib.getDistortionCoefficients());
		
		ArrayList<ModelInstance> instances_list = new ArrayList<ModelInstance>();
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
					new Point3(-5, 10, -3),
					new Point3(5, 10, -3),
					new Point3(5, 0, -3),
					new Point3(-5, 0, -3)
					});
			instances_list.clear();
			int i = 0;
			for (MatOfPoint2f c : approx_curves2f)
			{
				ModelInfo model_info;
				if (matching[i] == -1) //new quad
				{
					model_info = new ModelInfo(model_filename, c);
					model_list.add(model_info);
					
					if (model_list.size() > MAX_MODELS)
					{
						model_list.remove(0);
					}
					System.out.print("New model created, currently " + model_list.size() + " models\n");
				}
				else //old quad, renew
				{
					model_info = model_list.get(matching[i]);
					c = adjustPolygonToMatch(c, model_info.quad);
					model_info.quad = c;
					System.out.print("Old quad " + i + "," + matching[i] + "\n");
				}
				i++;
				if (model_info.checkLoaded())
				{
					Matrix4 transform = correctSolvePnP(object_corners, c,
							intrinsics, distortion, cam.fieldOfView, webcam.rows());
					//Matrix4 ttransl = new Matrix4();
					//ttransl.translate(0.5f, 0.5f, -0.5f);
					instances_list.add(new ModelInstance(coord_model[0], transform.cpy()));
					instances_list.add(new ModelInstance(coord_model[1], transform.cpy()));
					instances_list.add(new ModelInstance(coord_model[2], transform.cpy()));
					//instances_list.add(new ModelInstance(model_info.model, ttransl.mulLeft(transform.cpy())));
				}
				List<MatOfPoint> tmp = new ArrayList<MatOfPoint>();
				tmp.add(new MatOfPoint(c.toArray()));
				Imgproc.drawContours(undist_webcam, tmp, -1, model_info.color);
			}
			/*
			if (!approx_curves2f.isEmpty())
			{
				MatOfPoint2f c = approx_curves2f.get(0);
				c = adjustPolygonToMatch(c, prev_c);
				prev_c = c;
				Matrix4 transform = correctSolvePnP(object_corners, c,
						intrinsics, distortion, cam.fieldOfView, webcam.rows());
				Matrix4 ttransl = new Matrix4();
				ttransl.translate(0.5f, 0.5f, -0.5f);
				instances_list.add(new ModelInstance(coord_model[0], transform.cpy()));
				instances_list.add(new ModelInstance(coord_model[1], transform.cpy()));
				instances_list.add(new ModelInstance(coord_model[2], transform.cpy()));
				instances_list.add(new ModelInstance(box_model, ttransl.mulLeft(transform.cpy())));
				
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
					UtilAR.imShow("unwarp", unwarp_webcam);
					String code = CodeHelper.decode(unwarp_webcam);
					System.out.print("QR code: " + code + "\n");
				}
			}
			*/
		}

		UtilAR.imDrawBackground(undist_webcam);
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
		if (calib.isCalibrated())
		{
	        model_batch.begin(cam);
	        for (ModelInstance m : instances_list)
	        	model_batch.render(m, environment);
	        model_batch.end();
		}
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
		model.dispose();
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
		System.out.print("p0=" + p0.x + "," + p0.y + "\n");
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
		System.out.print("z_scale = " + z_scale + "\n");
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
	
	double quad_match_thres = 1E3;
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
				w[i][j] = quad_match_offset -distPoly(p, q);
				j++;
			}
			i++;
		}
		MatchingAlg.maxBipartite(w, matching, new int[dst.size()]);
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
	
}
