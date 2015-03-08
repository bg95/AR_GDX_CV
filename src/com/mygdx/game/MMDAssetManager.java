package com.mygdx.game;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.yzwlab.javammd.GLTexture;
import net.yzwlab.javammd.IGLTextureProvider.Handler;
import net.yzwlab.javammd.ReadException;
import net.yzwlab.javammd.jogl.JOGL;
import net.yzwlab.javammd.jogl.io.FileBuffer;
import net.yzwlab.javammd.model.MMDModel;

public class MMDAssetManager implements Handler{

	Map<String, MMDModel> model_map = new HashMap<String, MMDModel>();
	JOGL jogl;
	
	public MMDAssetManager(JOGL jogl) {
		this.jogl = jogl;
	}
	
	public void load(String pmd, String vmd) throws ReadException, IOException {
		MMDModel m = new MMDModel();
		m.openPMD(new FileBuffer(new File(pmd)));
		m.openVMD(new FileBuffer(new File(vmd)));
		m.prepare(jogl, this);
		model_map.put(pmd + "\n" + vmd, m);
	}
	
	public MMDModel get(String pmd, String vmd) {
		return model_map.get(pmd + "\n" + vmd);
	}

	@Override
	public void onSuccess(byte[] filename, GLTexture desc) {
	}

	@Override
	public void onError(byte[] filename, Throwable error) {
		error.printStackTrace();
	}
	
}
