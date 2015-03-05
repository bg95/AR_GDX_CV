package com.mygdx.game;

import net.yzwlab.javammd.jogl.JOGL;
import net.yzwlab.javammd.model.MMDModel;

import com.badlogic.gdx.math.Matrix4;

public class MMDModelInstance {

	public MMDModelInstance(MMDModel model, Matrix4 mulLeft) {
		this.model = model;
		transform = mulLeft.getValues().clone();
	}
	
	public void draw(JOGL gl) {
		gl.glPushMatrix();
		float[] result = new float[16];
		Matrix.multiplyMM(result, 0, gl.getMatrix(), 0, transform, 0);
		gl.setMatrix(result);
		model.draw(gl);
		gl.glPopMatrix();
	}
	
	MMDModel model;
	float[] transform = new float[16];

}
