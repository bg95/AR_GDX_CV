package com.mygdx.game;

import net.yzwlab.javammd.jogl.JOGL;
import net.yzwlab.javammd.model.MMDModel;

import com.badlogic.gdx.math.Matrix4;

public class MMDModelInstance {

	public MMDModelInstance(MMDModel model, Matrix4 mulLeft, float frameno) {
		this.model = model;
		transform = mulLeft.getValues().clone();
		this.frameno = frameno;
	}
	
	public void draw(JOGL gl) {
		gl.glPushMatrix();
		float[] result = new float[16];
		float[] glmatrix = gl.getMatrix();
		float[][] duplicates = new float[3][16];

		Matrix.setIdentityM(duplicates[0], 0);
		Matrix.setIdentityM(duplicates[1], 0);
		Matrix.setIdentityM(duplicates[2], 0);
		Matrix.translateM(duplicates[0], 0, 0, 0, 8);
		Matrix.translateM(duplicates[1], 0, 10, 0, 0);
		Matrix.translateM(duplicates[2], 0, -10, 0, 0);
		
		model.update(frameno);
		
		for (int i = 0; i < 3; i++)
		{
			Matrix.multiplyMM(result, 0, transform, 0, duplicates[i], 0);
			Matrix.multiplyMM(result, 0, glmatrix, 0, result, 0);
			gl.setMatrix(result);
			model.draw(gl);
		}
		
		gl.glPopMatrix();
	}
	
	MMDModel model;
	float[] transform = new float[16];
	float frameno;

}
