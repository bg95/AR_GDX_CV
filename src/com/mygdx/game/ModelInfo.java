package com.mygdx.game;

import java.io.File;
import java.io.IOException;

import net.yzwlab.javammd.IGLTextureProvider;
import net.yzwlab.javammd.ReadException;
import net.yzwlab.javammd.jogl.JOGL;
import net.yzwlab.javammd.jogl.io.FileBuffer;
import net.yzwlab.javammd.model.MMDModel;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;

class ModelInfo
{
	public ModelInfo(JOGL _jogl) {
		jogl = _jogl;
		double b = Math.random();
		double g = Math.random();
		double r = Math.random();
		double v = Math.max(Math.max(r, g), b) / 255.0;
		color = new Scalar(b / v, g / v, r / v);
		frameno = 0;
		time = System.currentTimeMillis();
		has_been_set = false;
	}
	
	public void set(MatOfPoint2f c, String _QR_code, MMDAssetManager asset) {
		if (has_been_set)
			return;
		quad = c;
		JSONObject desc = null;
		try {
			desc = new JSONObject(_QR_code);
		} catch (JSONException e) {
			desc = null;
		}
		if (desc != null) {
			String pmd, vmd;
			try {
				pmd = desc.getString("pmd");
				vmd = desc.getString("vmd");
			} catch (JSONException e) {
				return;
			}
			model = asset.get(pmd, vmd);
			String music_dir = null;
			music = null;
			try {
				music_dir = desc.getString("music");
				music = Gdx.audio.newMusic(new FileHandle(music_dir));
			} catch (JSONException e) {
				music = null;
			} catch (GdxRuntimeException e) { //music can't be loaded
				music = null;
			}
			if (music != null)
			{
				music.setLooping(false);
				music.setOnCompletionListener(new OnCompletionListener() {
		
					@Override
					public void onCompletion(Music music) {
						music.stop();
					}
					
				});
			}
			try {
				framerate = (float) desc.getDouble("framerate");
			} catch (JSONException e) {
				framerate = 30;
			}
			has_been_set = true;
		}
	}

	public ModelInfo(MatOfPoint2f c, String _QR_code, JOGL _jogl, MMDModel model, Music _music) {
		set(c, _QR_code, _jogl, model, _music);
	}
	
	public void set(MatOfPoint2f c, String _QR_code, JOGL _jogl, MMDModel model, Music _music) {
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
		time = System.currentTimeMillis();
		
		music = _music;
		if (music != null)
		{
			music.setLooping(false);
			music.setOnCompletionListener(new OnCompletionListener() {
	
				@Override
				public void onCompletion(Music music) {
					music.stop();
				}
				
			});
		}
	}
	
	public float updateFrameNo() {
		long ct = System.currentTimeMillis();
		long dt = ct - time;
		frameno += dt * framerate / 1000.0f;
		time = ct;
		return frameno;
	}
	
	public void play() {
		if (music != null)
			music.play();
		time = System.currentTimeMillis();
	}
	
	public void pause() {
		if (music != null)
			music.pause();
		//System.out.println("music paused");
	}
	
	public boolean isPlaying() {
		return music.isPlaying();
	}
	
	public boolean isSet() {
		return has_been_set;
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
	float frameno = 0, framerate = 30;
	long time;
	Music music;
	String QR_code;
	boolean has_been_set = false;
};