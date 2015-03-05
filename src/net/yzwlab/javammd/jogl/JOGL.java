package net.yzwlab.javammd.jogl;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Stack;

import javax.imageio.ImageIO;
//import javax.media.opengl.GL;
//import javax.media.opengl.GL20;












import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.mygdx.game.Matrix;

import net.yzwlab.javammd.GLTexture;
import net.yzwlab.javammd.IGL;
import net.yzwlab.javammd.IGLTextureProvider;
import net.yzwlab.javammd.ReadException;
import net.yzwlab.javammd.image.IImage;
import net.yzwlab.javammd.image.TargaReader;
import net.yzwlab.javammd.jogl.io.FileBuffer;
import net.yzwlab.javammd.model.DataUtils;

public class JOGL implements IGL, IGLTextureProvider {

	/**
	 * �摜�T�[�r�X��ێ����܂��B
	 */
	private AWTImageService imageService;

	private File baseDir;

	private GL20 gl;
	
	final String vertexShader =
			"uniform mat4 mvp_matrix; // model-view-projection matrix\n"
		+	"uniform mat3 normal_matrix; // normal matrix\n"
		+	"uniform vec3 ec_light_dir; // light direction in eye coords\n"
		+	"uniform vec4 m_diffuse;\n"
		+	"uniform vec4 m_ambiant;\n"
		+	"uniform vec4 m_specular;\n"
		+	"uniform vec4 m_emission;\n"
		+	"uniform float m_shininess;\n"
		+	"attribute vec4 a_vertex; // vertex position\n"
		+	"attribute vec3 a_normal; // vertex normal\n"
		+	"attribute vec4 a_color; // color, unused\n"
		+	"attribute vec2 a_texcoord; // texture coordinates\n"
		+	"//varying float v_diffuse;\n"
		+	"varying vec4 v_material;\n"
		+	"varying vec4 v_material_emission;\n"
		+	"varying vec2 v_texcoord;\n"
		+	"varying vec4 v_Color;          \n"     // This will be passed into the fragment shader.
		+	"void main() {\n"
		+	"	// put vertex normal into eye coords\n"
		+	"	vec3 ec_normal = normalize(normal_matrix * a_normal);\n"
		+	"	// emit diffuse scale factor, texcoord, and position\n"
		+	"	float v_diffuse = max(dot(ec_light_dir, ec_normal), 0.0);\n"
		+	"	float v_specular = pow(abs(dot(ec_light_dir, ec_normal)), m_shininess);\n"
		+	"	v_material = vec4(v_diffuse + v_specular) + m_ambiant; //emitted\n"
		+	"	v_material_emission = m_emission; //emitted\n"
		+	"	v_texcoord = a_texcoord;\n"
		+	"	gl_Position = mvp_matrix * a_vertex;\n"
		+	"   v_Color = a_color;          \n"     // Pass the color through to the fragment shader.
		+	"}";
	
	final String fragmentShader =
			"//precision mediump float;\n"
		+	"uniform sampler2D t_reflectance;\n"
		+	"uniform vec4 i_ambient;\n"
		+	"varying float v_diffuse;\n"
		+	"varying vec4 v_material;\n"
		+	"varying vec4 v_material_emission;\n"
		+	"varying vec2 v_texcoord;\n"
		+	"void main (void) {\n"
		+	"	vec4 color = texture2D(t_reflectance, v_texcoord);\n"
		+	"	gl_FragColor = color * v_material + v_material_emission;\n"
		+	"}";

	int mVertexHandle, mNormalHandle, mTexcoordHandle;
	int mMVPMatrixHandle, mNormalMatrixHandle, mEcLightDirHandle;
	int mTReflectanceHandle, mIAmbiantHandle;
	int mDiffuseHandle, mAmbiantHandle, mSpecularHandle, mEmissionHandle, mShininessHandle;
	
	private int imode;
	private int programHandle;
	private ArrayList<Float> vertex_attrib_list = new ArrayList<Float>();
	private int vtx_cnt, nrm_cnt, tex_cnt;
	private Stack<float[]> matrix_stack = new Stack<float[]>();
	private boolean normalize_enabled;
	private float[] mvp_matrix = new float[16];
	
	final int ATTRIB_SIZE = 8, FLOAT_SIZE = 4;
	final int VTX_OFFSET = 0, NRM_OFFSET = 3, TEX_OFFSET = 6;

	
	public JOGL(File baseDir, GL20 gl) {
		if (baseDir == null || gl == null) {
			throw new IllegalArgumentException();
		}
		this.imageService = new AWTImageService();
		this.baseDir = baseDir;
		this.gl = gl;
		initShaders();
		onSurfaceCreated();
	}

	@Override
	public int getResourceContext() {
		return 0;
	}

	@Override
	public void load(byte[] filename, IGLTextureProvider.Handler handler)
			throws ReadException {
		if (filename == null || handler == null) {
			throw new IllegalArgumentException();
		}
		String sfilename = new String(DataUtils.getStringData(filename));
		int pos = sfilename.indexOf("*");
		if (pos > 0) {
			sfilename = sfilename.substring(0, pos);
		}
		File f = new File(baseDir, sfilename);
		try {
			TargaReader reader = new TargaReader();
			BufferedImage image = null;
			if (f.getName().endsWith(".tga")) {
				IImage rawImage = reader.read(imageService, new FileBuffer(f));
				image = ((AWTImageService.Image) rawImage).getImage();
			} else {
				System.out.println(f.getPath());
				image = ImageIO.read(f);
				if (image == null) {
					throw new FileNotFoundException();
				}
			}
			int sizeWidth = 1;
			while (sizeWidth < image.getWidth()) {
				sizeWidth *= 2;
			}
			int sizeHeight = 1;
			while (sizeHeight < image.getHeight()) {
				sizeHeight *= 2;
			}

			BufferedImage textureImage = new BufferedImage(sizeWidth,
					sizeHeight, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics g = textureImage.getGraphics();
			g.drawImage(image, 0, 0, sizeWidth, sizeHeight, 0, 0,
					image.getWidth(), image.getHeight(), null);
			g.dispose();
			textureImage.flush();

			GLTexture ret = new GLTexture();
			ret.setTexWidth(textureImage.getWidth());
			ret.setTexHeight(textureImage.getHeight());

			ByteBuffer imageBuffer = ByteBuffer.allocateDirect(textureImage
					.getWidth() * textureImage.getHeight() * 4);
			int[] lineBuffer = new int[textureImage.getWidth()];
			for (int y = 0; y < textureImage.getHeight(); y++) {
				textureImage.getRGB(0, y, textureImage.getWidth(), 1,
						lineBuffer, 0, textureImage.getWidth());
				for (int i = 0; i < lineBuffer.length; i++) {
					int rgba = lineBuffer[i];
					imageBuffer.put(new byte[] {
							(byte) ((rgba & 0x00ff0000) >> 16),
							(byte) ((rgba & 0x0000ff00) >> 8),
							(byte) ((rgba & 0x000000ff) >> 0),
							(byte) ((rgba & 0xff000000) >> 24) });
				}
			}

			// GLubyte *image;
			gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 4);
			gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 4);
			IntBuffer intBuf = ByteBuffer.allocateDirect(32 * Integer.SIZE / 4).order(ByteOrder.nativeOrder()).asIntBuffer();
			gl.glGenTextures(1, intBuf);
			int textureId = intBuf.get(0);

			gl.glBindTexture(GL20.GL_TEXTURE_2D, textureId);

			gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER,
					GL20.GL_LINEAR);
			gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER,
					GL20.GL_LINEAR);
			imageBuffer.position(0);
			gl.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL30.GL_RGBA8,//GL20.GL_UNSIGNED_INT, //used to be GL_RGBA8
					ret.getTexWidth(), ret.getTexHeight(), 0, GL20.GL_RGBA,
					GL20.GL_UNSIGNED_BYTE, imageBuffer);
			gl.glBindTexture(GL20.GL_TEXTURE_2D, 0); // �f�t�H���g�e�N�X�`���̊��蓖��
			ret.setTextureIds(new long[] { textureId });

			handler.onSuccess(filename, ret);
		} catch (IOException e) {
			System.out.println("Filename: " + f.getPath());
			throw new ReadException(e);
		}
	}

	@Override
	public FrontFace glGetFrontFace() {
		// TODO Auto-generated method stub
		return FrontFace.GL_CW;
	}

	@Override
	public void glFrontFace(FrontFace mode) {
		int imode = 0;
		if (mode == FrontFace.GL_CW) {
			imode = GL20.GL_CW;
		} else {
			throw new IllegalArgumentException();
		}
		gl.glFrontFace(imode);
	}
	
	@Override
	public void glBegin(C mode, int length) {
		imode = 0;
		if (mode == C.GL_TRIANGLES) {
			imode = GL20.GL_TRIANGLES;
		} else {
			throw new IllegalArgumentException();
		}
		//gl.glBegin(imode);
		System.out.println("glBegin");
		vertex_attrib_list.clear();
		vtx_cnt = nrm_cnt = tex_cnt = 0;
	}

	@Override
	public void glEnd() {
		//gl.glEnd();
		System.out.println("glEnd");
		if (imode != GL20.GL_TRIANGLES)
			throw new IllegalArgumentException();
		//TODO:
		FloatBuffer attrib_buffer = ByteBuffer.allocateDirect(vertex_attrib_list.size() * FLOAT_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer(); 
		for (Float f : vertex_attrib_list)
			attrib_buffer.put(f.floatValue());
		/*
		int buffer = gl.glGenBuffer();
		gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, buffer);
		gl.glBufferData(GL20.GL_ARRAY_BUFFER, vertex_attrib_list.size() * FLOAT_SIZE, attrib_buffer, GL20.GL_STATIC_DRAW);
		*/
		if (mVertexHandle != -1) {
			attrib_buffer.position(VTX_OFFSET);
			gl.glVertexAttribPointer(mVertexHandle, 3, GL20.GL_FLOAT, false, ATTRIB_SIZE * FLOAT_SIZE, attrib_buffer);
			//gl.glVertexAttribPointer(mVertexHandle, 3, GL20.GL_FLOAT, false, ATTRIB_SIZE * FLOAT_SIZE, VTX_OFFSET * FLOAT_SIZE);
			gl.glEnableVertexAttribArray(mVertexHandle);
		}
		if (mTexcoordHandle != -1) {
			attrib_buffer.position(TEX_OFFSET);
			gl.glVertexAttribPointer(mTexcoordHandle, 2, GL20.GL_FLOAT, false, ATTRIB_SIZE * FLOAT_SIZE, attrib_buffer);
			//gl.glVertexAttribPointer(mTexcoordHandle, 2, GL20.GL_FLOAT, false, ATTRIB_SIZE * FLOAT_SIZE, TEX_OFFSET * FLOAT_SIZE);
			gl.glEnableVertexAttribArray(mTexcoordHandle);
		}
		if (mNormalHandle != -1) {
			attrib_buffer.position(NRM_OFFSET);
			gl.glVertexAttribPointer(mNormalHandle, 3, GL20.GL_FLOAT, normalize_enabled, ATTRIB_SIZE * FLOAT_SIZE, attrib_buffer);
			//gl.glVertexAttribPointer(mNormalHandle, 3, GL20.GL_FLOAT, normalize_enabled, ATTRIB_SIZE * FLOAT_SIZE, NRM_OFFSET * FLOAT_SIZE);
			gl.glEnableVertexAttribArray(mNormalHandle);
		}
		if (mMVPMatrixHandle != -1)
			gl.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvp_matrix, 0);
		if (mEcLightDirHandle != -1)
			gl.glUniform3f(mEcLightDirHandle, 0f, 0f, -1f);

		float[] normal_matrix = new float[9];
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				normal_matrix[i + 3 * j] = mvp_matrix[i + 4 * j];
		if (mNormalMatrixHandle != -1)
			gl.glUniformMatrix3fv(mNormalMatrixHandle, 1, false, normal_matrix, 0);
		
		ShortBuffer indices = ByteBuffer.allocateDirect(vtx_cnt * Short.SIZE / 4).order(ByteOrder.nativeOrder()).asShortBuffer();
		indices.position(0);
		for (int s = 0; s < vtx_cnt; s++)
			indices.put((short) (s > 32767 ? s - 65536 : s));
		indices.position(0);
		//System.out.println("Drawing, #vtx=" + vtx_cnt + ", #nrm=" + nrm_cnt + ", #tex=" + tex_cnt);
		gl.glDrawArrays(GL20.GL_TRIANGLES, 0, vtx_cnt);
		//gl.glDrawElements(GL20.GL_TRIANGLES, vtx_cnt, GL20.GL_UNSIGNED_SHORT, indices);
	}
	
    private void extendToIndex(ArrayList<Float> a, int i) {
		while (a.size() < i + 1)
			a.add(0f);
	}
	
	@Override
	public void glVertex3f(float x, float y, float z) {
		//gl.glVertex3f(x, y, z);
		//System.out.println("vertex: " + x + "," + y + "," + z);
		extendToIndex(vertex_attrib_list, vtx_cnt * ATTRIB_SIZE + VTX_OFFSET + 2);
		vertex_attrib_list.set(vtx_cnt * ATTRIB_SIZE + VTX_OFFSET, x);
		vertex_attrib_list.set(vtx_cnt * ATTRIB_SIZE + VTX_OFFSET + 1, y);
		vertex_attrib_list.set(vtx_cnt * ATTRIB_SIZE + VTX_OFFSET + 2, z);
		vtx_cnt++;
	}

	@Override
	public void glTexCoord2f(float x, float y) {
		//gl.glTexCoord2f(x, y);
		extendToIndex(vertex_attrib_list, tex_cnt * ATTRIB_SIZE + TEX_OFFSET + 1);
		vertex_attrib_list.set(tex_cnt * ATTRIB_SIZE + TEX_OFFSET, x);
		vertex_attrib_list.set(tex_cnt * ATTRIB_SIZE + TEX_OFFSET + 1, y);
		tex_cnt++;
	}

	@Override
	public void glNormal3f(float x, float y, float z) {
		//gl.glNormal3f(x, y, z);
		extendToIndex(vertex_attrib_list, nrm_cnt * ATTRIB_SIZE + NRM_OFFSET + 2);
		vertex_attrib_list.set(nrm_cnt * ATTRIB_SIZE + NRM_OFFSET, x);
		vertex_attrib_list.set(nrm_cnt * ATTRIB_SIZE + NRM_OFFSET + 1, y);
		vertex_attrib_list.set(nrm_cnt * ATTRIB_SIZE + NRM_OFFSET + 2, z);
		nrm_cnt++;
	}

	@Override
	public void glBindTexture(C target, long texture) {
		int itarget = 0;
		if (target == C.GL_TEXTURE_2D) {
			itarget = GL20.GL_TEXTURE_2D;
		} else {
			throw new IllegalArgumentException();
		}
		gl.glBindTexture(itarget, (int) texture);
	}

	@Override
	public void glBlendFunc(C c1, C c2) {
		int ic1 = 0;
		if (c1 == C.GL_SRC_ALPHA) {
			ic1 = GL20.GL_SRC_ALPHA;
		} else {
			throw new IllegalArgumentException();
		}
		int ic2 = 0;
		if (c2 == C.GL_ONE_MINUS_SRC_ALPHA) {
			ic2 = GL20.GL_ONE_MINUS_SRC_ALPHA;
		} else {
			throw new IllegalArgumentException();
		}
		gl.glBlendFunc(ic1, ic2);
	}

	@Override
	public void glPushMatrix() {
		//gl.glPushMatrix();
		matrix_stack.push(mvp_matrix.clone());
	}

	@Override
	public void glPopMatrix() {
		//gl.glPopMatrix();
		mvp_matrix = matrix_stack.pop();
	}

	@Override
	public void glScalef(float a1, float a2, float a3) {
		//gl.glScalef(a1, a2, a3);
		Matrix.scaleM(mvp_matrix, 0, a1, a2, a3);
	}

	@Override
	public void glColor4f(float a1, float a2, float a3, float a4) {
		//gl.glColor4f(a1, a2, a3, a4);
		//TODO: do nothing?
	}

	@Override
	public void glEnable(C target) {
		int itarget = 0;
		if (target == C.GL_NORMALIZE) {
			//itarget = GL20.GL_NORMALIZE;
			normalize_enabled = true;
			return;
		} else if (target == C.GL_TEXTURE_2D) {
			itarget = GL20.GL_TEXTURE_2D;
		} else if (target == C.GL_BLEND) {
			itarget = GL20.GL_BLEND;
		} else {
			throw new IllegalArgumentException();
		}
		gl.glEnable(itarget);
	}

	@Override
	public void glDisable(C target) {
		int itarget = 0;
		if (target == C.GL_NORMALIZE) {
			//itarget = GL20.GL_NORMALIZE;
			normalize_enabled = false;
			return;
		} else if (target == C.GL_TEXTURE_2D) {
			itarget = GL20.GL_TEXTURE_2D;
		} else if (target == C.GL_BLEND) {
			itarget = GL20.GL_BLEND;
		} else {
			throw new IllegalArgumentException();
		}
		gl.glDisable(itarget);
	}

	@Override
	public boolean glIsEnabled(C target) {
		int itarget = 0;
		if (target == C.GL_NORMALIZE) {
			//itarget = GL20.GL_NORMALIZE;
			return normalize_enabled;
		} else if (target == C.GL_TEXTURE_2D) {
			itarget = GL20.GL_TEXTURE_2D;
		} else if (target == C.GL_BLEND) {
			itarget = GL20.GL_BLEND;
		} else {
			throw new IllegalArgumentException();
		}
		return gl.glIsEnabled(itarget);
	}

	@Override
	public void glMaterialfv(C c1, C c2, float[] fv) {
		//TODO
		int ic1 = 0;
		if (c1 == C.GL_FRONT_AND_BACK) {
			ic1 = GL20.GL_FRONT_AND_BACK;
		} else {
			throw new IllegalArgumentException();
		}
		int ic2 = 0;
		if (c2 == C.GL_AMBIENT) {
			//ic2 = GL20.GL_AMBIENT;
			System.out.println("set ambiant " + fv[0] + "," + fv[1] + "," + fv[2] + "," + fv[3]);
			if (mAmbiantHandle != -1)
				gl.glUniform4fv(mAmbiantHandle, 4, fv, 0);
		} else if (c2 == C.GL_DIFFUSE) {
			//ic2 = GL20.GL_DIFFUSE;
			System.out.println("set diffuse " + fv[0] + "," + fv[1] + "," + fv[2] + "," + fv[3]);
			if (mDiffuseHandle != -1)
				gl.glUniform4fv(mDiffuseHandle, 4, fv, 0);
		} else if (c2 == C.GL_EMISSION) {
			//ic2 = GL20.GL_EMISSION;
			System.out.println("set emission " + fv[0] + "," + fv[1] + "," + fv[2] + "," + fv[3]);
			if (mEmissionHandle != -1)
				gl.glUniform4fv(mEmissionHandle, 4, fv, 0);
		} else if (c2 == C.GL_SPECULAR) {
			//ic2 = GL20.GL_SPECULAR;
			System.out.println("set specular " + fv[0] + "," + fv[1] + "," + fv[2] + "," + fv[3]);
			if (mSpecularHandle != -1)
				gl.glUniform4fv(mSpecularHandle, 4, fv, 0);
		} else {
			throw new IllegalArgumentException();
		}
		//gl.glMaterialfv(ic1, ic2, fv, 0);
	}

	@Override
	public void glMaterialf(C c1, C c2, float f) {
		//TODO
		int ic1 = 0;
		if (c1 == C.GL_FRONT_AND_BACK) {
			ic1 = GL20.GL_FRONT_AND_BACK;
		} else {
			throw new IllegalArgumentException();
		}
		int ic2 = 0;
		if (c2 == C.GL_SHININESS) {
			//ic2 = GL20.GL_SHININESS;
			System.out.println("set shininess " + f);
			if (mShininessHandle != -1)
				gl.glUniform1fv(mShininessHandle, 4, new float[]{f}, 0);
		} else {
			throw new IllegalArgumentException();
		}
		//gl.glMaterialf(ic1, ic2, f);
	}

	@Override
	public long glGetBindTexture(C target) {
		// TODO
		return 0;
	}

	@Override
	public void glEnableClientState(C target) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glDisableClientState(C target) {
		// TODO Auto-generated method stub

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
				System.out.print("Shader error: " + gl.glGetShaderInfoLog(vertexShaderHandle) + "\n");
				System.out.flush();
		        gl.glDeleteShader(vertexShaderHandle);
		        vertexShaderHandle = 0;
		    }
		}
		if (vertexShaderHandle == 0)
		{
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
		    gl.glBindAttribLocation(programHandle, 1, "a_normal");
		    gl.glBindAttribLocation(programHandle, 2, "a_texcoord");
		    // Link the two shaders together into a program.
		    gl.glLinkProgram(programHandle);
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
	    mMVPMatrixHandle = gl.glGetUniformLocation(programHandle, "mvp_matrix");
	    //assert(mMVPMatrixHandle != -1);
	    mNormalMatrixHandle = gl.glGetUniformLocation(programHandle, "normal_matrix");
	    //assert(mNormalMatrixHandle != -1);
	    mEcLightDirHandle = gl.glGetUniformLocation(programHandle, "ec_light_dir");
	    //assert(mEcLightDirHandle != -1);
		mVertexHandle = gl.glGetAttribLocation(programHandle, "a_vertex");
	    //assert(mVertexHandle != -1);
		mNormalHandle = gl.glGetAttribLocation(programHandle, "a_normal");
	    //assert(mNormalHandle != -1);
		mTexcoordHandle = gl.glGetAttribLocation(programHandle, "a_texcoord");
	    //assert(mTexcoordHandle != -1);
		mTReflectanceHandle = gl.glGetUniformLocation(programHandle, "t_reflectance");
	    //assert(mTReflectanceHandle != -1);
		mIAmbiantHandle = gl.glGetUniformLocation(programHandle, "i_ambiant");
	    //assert(mIAmbiantHandle != -1);
		mDiffuseHandle = gl.glGetUniformLocation(programHandle, "m_diffuse");
		mAmbiantHandle = gl.glGetUniformLocation(programHandle, "m_ambiant");
		mSpecularHandle = gl.glGetUniformLocation(programHandle, "m_specular");
		mEmissionHandle = gl.glGetUniformLocation(programHandle, "m_emission");
		mShininessHandle = gl.glGetUniformLocation(programHandle, "m_shininess");
	    
		System.out.println("mVertexHandle = " + mVertexHandle);
		System.out.println("mNormalHandle = " + mNormalHandle);
		System.out.println("mTexcoordHandle = " + mTexcoordHandle);

	    // Tell OpenGL to use this program when rendering.
	    gl.glUseProgram(programHandle);
	}

	public void setMatrix(float[] m) {
		for (int i = 0; i < 16; i++)
			mvp_matrix[i] = m[i];
	}

}
