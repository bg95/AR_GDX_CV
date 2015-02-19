package com.mygdx.game;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
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
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

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
	
	Timer timer;
	VideoCapture vc;
	CameraCalibrator calib;
	double m_z_scale;
	
	@Override
	public void create () {
		cam = new PerspectiveCamera(/*67*/40f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0f, 0f, 4f);
		cam.lookAt(0, 0, 0);
		cam.near = 0.1f;
		cam.far = 20f;
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
		coord_instance[0] = new ModelInstance(coord_model[0]);
		coord_instance[1] = new ModelInstance(coord_model[1]);
		coord_instance[2] = new ModelInstance(coord_model[2]);

		box_model = builder.createBox(1f, 1f, 1f,
				new Material(ColorAttribute.createDiffuse(new Color(0.0f, 0.0f, 1.0f, 0.3f))),
				//new Material(new BlendingAttribute(0.3f)),
				Usage.Position | Usage.Normal);
		boxes_instance = new ModelInstance[100];
		for (int i = 0; i < 100; i++)
			boxes_instance[i] = new ModelInstance(box_model);
		
		environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
        
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask(){

			@Override
			public void run() {
		        //cam.rotateAround(new Vector3(0, 0, 0), new Vector3(0, 1, 0), 1f);
		        cam.update();
			}
        	
        }, 0, 50);
        
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
	}

	@Override
	public void render () {
		
		Mat webcam = new Mat();
		if (!vc.grab())
			System.out.print("unable to grab capture\n");
		if (!vc.retrieve(webcam))
			System.out.print("unable to retrieve capture\n");
		Mat gray = new Mat(webcam.rows(), webcam.cols(), CvType.CV_8UC1);
		for (int i = 0; i < webcam.rows(); i++)
			for (int j = 0; j < webcam.cols(); j++)
			{
				double[] data = webcam.get(i, j);
				gray.put(i, j, new byte[]{
						(byte) (255.0 * (data[0] + data[1] + data[2]) / 3.0)
				});
			}
		Mat undist_webcam = new Mat();
		if (!calib.isCalibrated())
		{
			calib.processFrame(gray, webcam);
			if (Math.random() < 0.5)
				calib.addCorners();
			if (calib.getCornersBufferSize() >= 10)
			{
				calib.calibrate();
				m_z_scale = -1;
			}
			undist_webcam = webcam;
		}
		else
			Imgproc.undistort(webcam, undist_webcam, calib.getCameraMatrix(), calib.getDistortionCoefficients());
		
		/*
		Mat canny = new Mat();
		Imgproc.Canny(webcam, canny, 80, 220);
		UtilAR.imShow("canny", canny);
		*/
		
		MatOfPoint2f corners = new MatOfPoint2f();
		Size size = new Size(9, 6);
		
		if (calib.isCalibrated())
		{
			boolean corners_found = Calib3d.findChessboardCorners(webcam, size, corners, 
					Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK + Calib3d.CALIB_CB_FILTER_QUADS);
			if (corners_found)
			{
				//Imgproc.cornerSubPix(gray, corners, new Size(11, 11), new Size(-1, -1), new TermCriteria(TermCriteria.EPS, 30, 0.1));

				System.out.print("Chessboard found.\n");
				//Calib3d.drawChessboardCorners(webcam, size, corners, corners_found);
				Point[] corners_array = corners.toArray();
		
				Mat intrinsics;
				MatOfDouble distortion;
				//intrinsics = UtilAR.getDefaultIntrinsicMatrix(webcam.rows(), webcam.cols());
				//distortion = UtilAR.getDefaultDistortionCoefficients();
				intrinsics = calib.getCameraMatrix();
				distortion = new MatOfDouble(calib.getDistortionCoefficients());
				Mat rotation = new Mat(), translation = new Mat();
				Point3[] object_corners_array = new Point3[(int) (size.height * size.width)];
				for (int i = 0; i < size.height; i++)
					for (int j = 0; j < size.width; j++)
						object_corners_array[(int) (i * size.width + j)] = new Point3(j, i, 0f);
				MatOfPoint3f object_corners = new MatOfPoint3f(object_corners_array);
				
				Calib3d.solvePnP(object_corners, corners, intrinsics, distortion,
						rotation, translation);
						//false, Calib3d.CV_EPNP);
				
				Matrix4 transform = toMatrix4(rotation, translation);
				
				int C0 = 0, C1 = object_corners_array.length - 1;
				Point3 P0 = transform(transform, object_corners_array[C0]),
						P1 = transform(transform, object_corners_array[C1]); //3d points using PnP
				Point p0 = new Point(P0.x / P0.z, P0.y / P0.z),
						p1 = new Point(P1.x / P1.z, P1.y / P1.z); //projection on screen of P0, P1
				Point d_corners = new Point(corners_array[C0].x - corners_array[C1].x,
						corners_array[C0].y - corners_array[C1].y);
				Point dp = new Point(p0.x - p1.x, p0.y - p1.y);
				double alpha = cam.fieldOfView / 180.0 * Math.PI *
						Math.sqrt(d_corners.x * d_corners.x + d_corners.y * d_corners.y) / webcam.rows();
				double beta = Math.sqrt(dp.x * dp.x + dp.y * dp.y);
				double z_scale = beta / alpha;
				if (m_z_scale < 0)
					m_z_scale = z_scale;
				System.out.print("z_scale = " + z_scale + "\n");
				
				translation.put(2, 0, z_scale * translation.get(2, 0)[0]);
				transform = toMatrix4(rotation, translation);
				
				cam.up.set(0, -1, 0);
				cam.position.set(0f, 0f, 0f);
				cam.lookAt(0, 0, 1);
				cam.near = 0.1f;
				cam.far = 2000f;
				
				//translation.put(2, 0, 0.06 * translation.get(2, 0)[0]);
				//UtilAR.setCameraByRT(rotation, translation, cam);
				cam.transform(transform.inv());
				
				instance = new ModelInstance(model);
				for (int i = 0; i < 3; i++)
					coord_instance[i] = new ModelInstance(coord_model[i]);
				
				for (int i = 0; i < object_corners_array.length; i++)
				{
					Matrix4 ttransl = new Matrix4();
					ttransl.translate((float) object_corners_array[i].x - 0.5f, (float) object_corners_array[i].y - 0.5f, (float) object_corners_array[i].z - 0.5f);
					boxes_instance[i] = new ModelInstance(box_model, ttransl);
				}
				
			}
			else
			{
				System.out.print("No chessboard found.\n");
			}
		}

		UtilAR.imDrawBackground(undist_webcam);
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
 
        //cam_control.update();
        
		if (calib.isCalibrated())
		{
	        model_batch.begin(cam);
	        //model_batch.render(instance, environment);
	        
	        for (int i = 0; i < size.width * size.height; i++)
	        	if (i % 2 == 0)
	        		model_batch.render(boxes_instance[i], environment);
	        		
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
		transform_float[14] = (float) (translation.get(2, 0)[0] * m_z_scale); 
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
}
