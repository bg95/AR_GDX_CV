package com.mygdx.game;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.BufferedImageLuminanceSource;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class CodeHelper {
	
	public static BufferedImage matToImg(Mat in) {
        BufferedImage out;
        byte[] data = new byte[in.rows() * in.cols() * (int)in.elemSize()];
        int type;
        in.get(0, 0, data);

        if(in.channels() == 1)
            type = BufferedImage.TYPE_BYTE_GRAY;
        else
            type = BufferedImage.TYPE_3BYTE_BGR;

        out = new BufferedImage(in.width(), in.height(), type);

        out.getRaster().setDataElements(0, 0, in.width(), in.height(), data);
        return out;
    }
	
	public static String decode(Mat image) {
		try {
			return readQRCode(matToImg(image), "UTF-8", new HashMap<EncodeHintType, ErrorCorrectionLevel>());
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
	public static String readQRCode(BufferedImage image, String charset, Map hintMap)
		      throws NotFoundException {
	    BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
	        new BufferedImageLuminanceSource(image)));
	    Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap,
	        hintMap);
	    return qrCodeResult.getText();
	}
}
