package com.mygdx.game;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.math.Matrix4;

public class TestDrawer {

	private GL30 gl;
	/** Store our model data in a float buffer. */
	private FloatBuffer mTriangle1Vertices;
	/** How many bytes per float. */
	private final int mBytesPerFloat = 4;
	/**
	 * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
	 * it positions things relative to our eye.
	 */
	private float[] mViewMatrix = new float[16];
	
	int programHandle;

	final String vertexShader =
			"uniform mat4 mvp_matrix; // model-view-projection matrix\n"
		+	"uniform mat3 normal_matrix; // normal matrix\n"
		+	"uniform vec3 ec_light_dir; // light direction in eye coords\n"
		+	"attribute vec4 a_vertex; // vertex position\n"
		+	"attribute vec3 a_normal; // vertex normal\n"
		+	"attribute vec4 a_color; // color, unused\n"
		+	"attribute vec2 a_texcoord; // texture coordinates\n"
		+	"varying float v_diffuse;\n"
		+	"varying vec2 v_texcoord;\n"
		+	"varying vec4 v_Color;          \n"     // This will be passed into the fragment shader.
		+	"void main() {\n"
		+	"	// put vertex normal into eye coords\n"
		+	"	vec3 ec_normal = normalize(normal_matrix * a_normal);\n"
		+	"	// emit diffuse scale factor, texcoord, and position\n"
		+	"	v_diffuse = max(dot(ec_light_dir, ec_normal), 0.0);\n"
		+	"	v_texcoord = a_texcoord;\n"
		+	"	gl_Position = mvp_matrix * a_vertex;\n"
		+	"   v_Color = a_color;          \n"     // Pass the color through to the fragment shader.
		+	"}";
	
	
	final String fragmentShader =
		    "//precision mediump float;       \n"     // Set the default precision to medium. We don't need as high of a
		                                            // precision in the fragment shader.
		  + "varying vec4 v_Color;          \n"     // This is the color from the vertex shader interpolated across the
		                                            // triangle per fragment.
		  + "void main()                    \n"     // The entry point for our fragment shader.
		  + "{                              \n"
		  + "   gl_FragColor = v_Color;     \n"     // Pass the color directly through the pipeline.
		  + "}                              \n";
	
	/*
	final String fragmentShader =
			"//precision mediump float;\n"
		+	"uniform sampler2D t_reflectance;\n"
		+	"uniform vec4 i_ambient;\n"
		+	"varying float v_diffuse;\n"
		+	"varying vec2 v_texcoord;\n"
		+	"void main (void) {\n"
		+	"	vec4 color = texture2D(t_reflectance, v_texcoord);\n"
		+	"	gl_FragColor = vec4(0,0,0,1);//gl_FragColor = color * (vec4(v_diffuse) + i_ambient);\n"
		+	"}";*/
	
	/** This will be used to pass in the transformation matrix. */
	private int mMVPMatrixHandle;
	 
	/** This will be used to pass in model position information. */
	private int mPositionHandle;
	 
	/** This will be used to pass in model color information. */
	private int mColorHandle;
	
	TestDrawer(GL30 _gl, int w, int h) {
		gl = _gl;
		setTriangles();
		initMatrix();
		initShaders();
		onSurfaceCreated();
		//onSurfaceChanged(w, h);
	}
	
	public void setTriangles() {
		// This triangle is red, green, and blue.
	    final float[] triangle1VerticesData = {
	            // X, Y, Z,
	            // R, G, B, A
	            -0.5f, -0.25f, 0.0f,
	            1.0f, 0.0f, 0.0f, 1.0f,
	 
	            0.5f, -0.25f, 0.0f,
	            0.0f, 0.0f, 1.0f, 1.0f,
	 
	            0.0f, 0.559016994f, 0.0f,
	            0.0f, 1.0f, 0.0f, 1.0f};
	    // Initialize the buffers.
	    mTriangle1Vertices = ByteBuffer.allocateDirect(triangle1VerticesData.length * mBytesPerFloat)
	    .order(ByteOrder.nativeOrder()).asFloatBuffer();
	    mTriangle1Vertices.put(triangle1VerticesData).position(0);
		
	}
	
	public void initMatrix() {
	    // Set the background clear color to gray.
	    gl.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);
	 
	    // Position the eye behind the origin.
	    final float eyeX = 0.0f;
	    final float eyeY = 0.0f;
	    final float eyeZ = 1.5f;
	 
	    // We are looking toward the distance
	    final float lookX = 0.0f;
	    final float lookY = 0.0f;
	    final float lookZ = -5.0f;
	 
	    // Set our up vector. This is where our head would be pointing were we holding the camera.
	    final float upX = 0.0f;
	    final float upY = 1.0f;
	    final float upZ = 0.0f;
	 
	    // Set the view matrix. This matrix can be said to represent the camera position.
	    // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
	    // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
	    Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
	}
	
	public void initShaders() {
		// Load in the vertex shader.
		int vertexShaderHandle = gl.glCreateShader(GL20.GL_VERTEX_SHADER);
		 
		if (vertexShaderHandle != 0)
		{
		    // Pass in the shader source.
		    gl.glShaderSource(vertexShaderHandle, vertexShader);
		 
		    // Compile the shader.
		    gl.glCompileShader(vertexShaderHandle);
		 
		    // Get the compilation status.
		    //final int[] compileStatus = new int[1];
		    IntBuffer compileStatus = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
		    gl.glGetShaderiv(vertexShaderHandle, GL20.GL_COMPILE_STATUS, compileStatus);
		 
		    // If the compilation failed, delete the shader.
		    if (compileStatus.get(0) == 0)
		    {
		        gl.glDeleteShader(vertexShaderHandle);
		        vertexShaderHandle = 0;
		    }
		}
		 
		if (vertexShaderHandle == 0)
		{
			System.out.print("Shader error: " + gl.glGetShaderInfoLog(vertexShaderHandle) + "\n");
			System.out.flush();
		    throw new RuntimeException("Error creating vertex shader.");
		}

		// Load in the fragment shader.
		int fragmentShaderHandle = gl.glCreateShader(GL20.GL_FRAGMENT_SHADER);
		 
		if (fragmentShaderHandle != 0)
		{
		    // Pass in the shader source.
		    gl.glShaderSource(fragmentShaderHandle, fragmentShader);
		 
		    // Compile the shader.
		    gl.glCompileShader(fragmentShaderHandle);
		 
		    // Get the compilation status.
		    //final int[] compileStatus = new int[1];
		    IntBuffer compileStatus = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
		    gl.glGetShaderiv(fragmentShaderHandle, GL20.GL_COMPILE_STATUS, compileStatus);
		 
		    // If the compilation failed, delete the shader.
		    if (compileStatus.get(0) == 0)
		    {
				System.out.print("Shader error: " + gl.glGetShaderInfoLog(fragmentShaderHandle) + "\n");
				System.out.flush();
		        gl.glDeleteShader(fragmentShaderHandle);
		        fragmentShaderHandle = 0;
		    }
		}
		 
		if (fragmentShaderHandle == 0)
		{
			System.out.print("Shader error: " + gl.glGetShaderInfoLog(fragmentShaderHandle) + "\n");
			System.out.flush();
		    throw new RuntimeException("Error creating fragment shader.");
		}
		
		// Create a program object and store the handle to it.
		programHandle = gl.glCreateProgram();
		 
		if (programHandle != 0)
		{
		    // Bind the vertex shader to the program.
		    gl.glAttachShader(programHandle, vertexShaderHandle);
		 
		    // Bind the fragment shader to the program.
		    gl.glAttachShader(programHandle, fragmentShaderHandle);
		 
		    // Bind attributes
		    gl.glBindAttribLocation(programHandle, 0, "a_vertex");
		    gl.glBindAttribLocation(programHandle, 1, "a_color");
		    gl.glBindAttribLocation(programHandle, 2, "a_normal");
		    gl.glBindAttribLocation(programHandle, 3, "a_texcoord");
		 
		    // Link the two shaders together into a program.
		    gl.glLinkProgram(programHandle);
		    System.out.println("Program info: " + gl.glGetProgramInfoLog(programHandle));
		    System.out.flush();
		 
		    // Get the link status.
		    //final int[] linkStatus = new int[1];
		    IntBuffer linkStatus = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
		    gl.glGetProgramiv(programHandle, GL20.GL_LINK_STATUS, linkStatus);
		 
		    // If the link failed, delete the program.
		    if (linkStatus.get(0) == 0)
		    {
		        gl.glDeleteProgram(programHandle);
		        programHandle = 0;
		    }
		}
		 
		if (programHandle == 0)
		{
		    throw new RuntimeException("Error creating program.");
		}
	}
	
	public void onSurfaceCreated() {
		// Set program handles. These will later be used to pass in values to the program.
	    mMVPMatrixHandle = gl.glGetUniformLocation(programHandle, "mvp_matrix");
	    mPositionHandle = gl.glGetAttribLocation(programHandle, "a_vertex");
	    mColorHandle = gl.glGetAttribLocation(programHandle, "a_color");

		int mVertexHandle = gl.glGetAttribLocation(programHandle, "a_vertex");
	    assert(mVertexHandle != -1);
		int mNormalHandle = gl.glGetAttribLocation(programHandle, "a_normal");
	    assert(mNormalHandle != -1);
		int mTexcoordHandle = gl.glGetAttribLocation(programHandle, "a_texcoord");
	    assert(mTexcoordHandle != -1);
	    mMVPMatrixHandle = gl.glGetUniformLocation(programHandle, "mvp_matrix");
	    assert(mMVPMatrixHandle != -1);
	    int mNormalMatrixHandle = gl.glGetUniformLocation(programHandle, "normal_matrix");
	    assert(mNormalMatrixHandle != -1);
	    int mEcLightDirHandle = gl.glGetUniformLocation(programHandle, "ec_light_dir");
	    assert(mEcLightDirHandle != -1);
	    /*
		int mTReflectanceHandle = gl.glGetUniformLocation(programHandle, "t_reflectance");
	    assert(mTReflectanceHandle != -1);
		int mIAmbiantHandle = gl.glGetUniformLocation(programHandle, "i_ambiant");
	    assert(mIAmbiantHandle != -1);
	    */
	    // Tell OpenGL to use this program when rendering.
	    gl.glUseProgram(programHandle);
	}
	
	private float[] mProjectionMatrix = new float[16];
	public void onSurfaceChanged(int width, int height)
	{
	    // Set the OpenGL viewport to the same size as the surface.
	    gl.glViewport(0, 0, width, height);
	 
	    // Create a new perspective projection matrix. The height will stay the same
	    // while the width will vary as per aspect ratio.
	    final float ratio = (float) width / height;
	    final float left = -ratio;
	    final float right = ratio;
	    final float bottom = -1.0f;
	    final float top = 1.0f;
	    final float near = 1.0f;
	    final float far = 10.0f;
	 
	    Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
	}
	
	private float[] mModelMatrix = new float[16];
    public void onDrawFrame()
    {
        //gl.glClear(GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT);
 
        // Do a complete rotation every 10 seconds.
        long time = System.currentTimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
 
        // Draw the triangle facing straight on.
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);
        //Matrix.setRotateEulerM(mModelMatrix, 0, 0, 0, angleInDegrees);
        //drawTriangle2(mTriangle1Vertices);
    }
    
    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
    private float[] mMVPMatrix = new float[16];
     
    /** How many elements per vertex. */
    private final int mStrideBytes = 7 * mBytesPerFloat;
     
    /** Offset of the position data. */
    private final int mPositionOffset = 0;
     
    /** Size of the position data in elements. */
    private final int mPositionDataSize = 3;
     
    /** Offset of the color data. */
    private final int mColorOffset = 3;
     
    /** Size of the color data in elements. */
    private final int mColorDataSize = 4;
     
    /**
     * Draws a triangle from the given vertex data.
     *
     * @param aTriangleBuffer The buffer containing the vertex data.
     */
    private void drawTriangle(final FloatBuffer aTriangleBuffer)
    {
        // Pass in the position information
        aTriangleBuffer.position(mPositionOffset);
        gl.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GL20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer);
     
        gl.glEnableVertexAttribArray(mPositionHandle);
     
        // Pass in the color information
        
        aTriangleBuffer.position(mColorOffset);
        gl.glVertexAttribPointer(mColorHandle, mColorDataSize, GL20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer);
     
        gl.glEnableVertexAttribArray(mColorHandle);
     
        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
     
        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        
        gl.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        gl.glDrawArrays(GL20.GL_TRIANGLES, 0, 3);
    }
    
    private void drawTriangle2(final FloatBuffer aTriangleBuffer)
    {
        // Pass in the position information
        aTriangleBuffer.position(mPositionOffset);
        gl.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GL20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer);
     
        gl.glEnableVertexAttribArray(mPositionHandle);

		// Position the eye behind the origin.
	    final float eyeX = 0.0f;
	    final float eyeY = 0.0f;
	    final float eyeZ = 1.5f;
	 
	    // We are looking toward the distance
	    final float lookX = 0.0f;
	    final float lookY = 0.0f;
	    final float lookZ = -5.0f;
	 
	    // Set our up vector. This is where our head would be pointing were we holding the camera.
	    final float upX = 0.0f;
	    final float upY = 1.0f;
	    final float upZ = 0.0f;
	 
	    float[] mViewMatrix = new float[16];
		// Set the view matrix. This matrix can be said to represent the camera position.
	    // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
	    // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
	    Matrix.setLookAtM(mViewMatrix , 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
	    
	    final float ratio = (float) 640 / 480;
	    final float left = -ratio;
	    final float right = ratio;
	    final float bottom = -1.0f;
	    final float top = 1.0f;
	    final float near = 1.0f;
	    final float far = 100.0f;
	 
	    float[] mProjectionMatrix = new float[16];
		Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
	    
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        
        gl.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        gl.glDrawArrays(GL20.GL_TRIANGLES, 0, 3);
    }
}
