package com.mygdx.game;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

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
	
	public static String decodeInQuad(Mat image, MatOfPoint2f quad) {
		if (false)
			return  
					"{\"pmd\":\"mmd/Models/OneDrive-2015-03-09/¯¢¨ñ½òBPMD090701_1/½òBIK200906262.pmd\", "
					+ "\"vmd\":\"mmd/VMD/doara.vmd\","
					+ "\"music\":\"\"}";

/*		if (true)
			return  Math.random() > 0.5 ?
					"{\"pmd\":\"mmd/Models/OneDrive-2015-03-09/éçnJPMD/éçnJ.pmd\", "
					+ "\"vmd\":\"mmd/VMD/Project DIVA.vmd\","
					+ "\"music\":\"\"}" :
					"";
		if (true)
			return  
					"{\"pmd\":\"mmd/Models/madoka/md_m/md_m.pmd\", "
					+ "\"vmd\":\"mmd/VMD/把你mikumiku掉.vmd\","
					+ "\"music\":\"mmd/music/コネクト-梶浦由記.mp3\"}";
		if (true)
			return  Math.random() > 1 ?
					"{\"pmd\":\"mmd/Models/洛天依ver1.10/¡¾ÂåÌìÒÀLuoTianYi¡¿.pmd\", "
					+ "\"vmd\":\"mmd/VMD/新华里-熊猫团.vmd\","
					+ "\"music\":\"luotianyi_xinhuali.mp3\"}" :
					"{\"pmd\":\"mmd/Models/neru_akita_ver1.2/neru_akita_ver1.2_masisi.pmd\", "
					+ "\"vmd\":\"mmd/VMD/doara.vmd\","
					+ "\"music\":\"not_found.mp3\"}";
*/
		Mat unwarp_webcam = new Mat(400, 400, image.type());
		MatOfPoint2f dst = new MatOfPoint2f(new Point[] {
				new Point(0, 0),
				new Point(unwarp_webcam.rows(), 0),
				new Point(unwarp_webcam.rows(), unwarp_webcam.cols()),
				new Point(0, unwarp_webcam.cols())
		});
		if (quad != null)
		{
			Mat warp = Imgproc.getPerspectiveTransform(quad, dst);
			Imgproc.warpPerspective(image, unwarp_webcam, warp, unwarp_webcam.size(), Imgproc.INTER_LINEAR);
			//UtilAR.imShow("unwarp", unwarp_webcam);
			String code = CodeHelper.decode(unwarp_webcam);
			//System.out.print("QR code: " + code + "\n");
			return code;
		}
		return "";
	}
	
	public static JSONObject decodeJSONInQuad(Mat image, MatOfPoint2f quad) {
		try {
			return new JSONObject(decodeInQuad(image, quad));
		} catch (JSONException e) {
			return null;
		}
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
