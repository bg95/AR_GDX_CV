package com.mygdx.game;

import java.io.File;
import java.io.IOException;

import net.yzwlab.javammd.IGLTextureProvider;
import net.yzwlab.javammd.ReadException;
import net.yzwlab.javammd.jogl.JOGL;
import net.yzwlab.javammd.jogl.io.FileBuffer;
import net.yzwlab.javammd.model.MMDModel;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;

class ModelInfo
{
	public ModelInfo(File pmd, File vmd, IGLTextureProvider.Handler handler, MatOfPoint2f c, JOGL _jogl) throws ReadException, IOException {
		jogl = _jogl;
		assert pmd.getParentFile().equals(jogl.getBaseDir());
		//loading = true;
		//assets.load(file_name, Model.class);
		/*
		this.pmd = pmd;
		this.vmd = vmd;
		pmd_name = pmd.getName();
		vmd_name = vmd.getName();
		*/
		model = new MMDModel();
		model.openPMD(new FileBuffer(pmd));
		model.openVMD(new FileBuffer(vmd));
		model.prepare(jogl, handler);
		quad = c;
		double b = Math.random();
		double g = Math.random();
		double r = Math.random();
		double v = Math.max(Math.max(r, g), b) / 255.0;
		color = new Scalar(b / v, g / v, r / v);
		frameno = 0;
	}
	
	public ModelInfo(MMDModel model, MatOfPoint2f c, JOGL _jogl) {
		jogl = _jogl;
		//loading = true;
		this.model = model;
		quad = c;
		double b = Math.random();
		double g = Math.random();
		double r = Math.random();
		double v = Math.max(Math.max(r, g), b) / 255.0;
		color = new Scalar(b / v, g / v, r / v);
		frameno = 0;
	}
	
	/*
	public boolean checkLoaded() {
		if (loading && assets.update())
		{
			model = assets.get(name, Model.class);
			if (model == null)
				model = model_builder.createBox(3f, 3f, 3f,
						new Material(ColorAttribute.createDiffuse(Color.GREEN)),
						Usage.Position | Usage.Normal);
			loading = false;
			return true;
		}
		return !loading;
	}*/
	public MatOfPoint2f quad;
	//AssetManager asset;
	//File pmd, vmd;
	//String pmd_name, vmd_name;
	//Model model;
	MMDModel model;
	//boolean loading;
	Scalar color;
	JOGL jogl;
	int frameno;
};