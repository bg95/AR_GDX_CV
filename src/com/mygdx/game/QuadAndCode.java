package com.mygdx.game;

import java.util.List;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

public class QuadAndCode {

	MatOfPoint2f quad;
	String code;
	
	QuadAndCode(MatOfPoint2f quad, String code) {
		this.quad = quad;
		this.code = code;
	}
	
	
	static double quad_match_thres = 1E4;
	static double quad_match_offset = 2E6 * 4;
	static void matchQuads(List<MatOfPoint2f> src, List<MatOfPoint2f> dst, int[] matching, int[] matching_inv) {
		double[][] w = new double[src.size()][dst.size()];
		int i, j;
		i = 0;
		for (MatOfPoint2f p : src)
		{
			j = 0;
			for (MatOfPoint2f q : dst)
			{
				w[i][j] = quad_match_offset - distPoly(p, q);
				j++;
			}
			i++;
		}
		MatchingAlg.maxBipartite(w, matching, matching_inv);
		for (i = 0; i < src.size(); i++)
			if (matching[i] != -1)
			{
				j = matching[i];
				if (w[i][j] < quad_match_offset - quad_match_thres)
				{
					matching_inv[j] = -1;
					matching[i] = -1;
				}
			}
		double avg_dist = 0.0;
		int cnt_mat = 0;
		for (i = 0; i < src.size(); i++)
			if (matching[i] != -1)
			{
				cnt_mat++;
				avg_dist += quad_match_offset - w[i][matching[i]];
			}
		if (cnt_mat != 0)
		{
			avg_dist /= cnt_mat;
			//quad_match_thres += (avg_dist * 3.0 - quad_match_thres) * 0.1;
		}
	}
	
	static double distPoly(MatOfPoint2f mp, MatOfPoint2f mq) {
		Point[] p = mp.toArray();
		Point[] q = mq.toArray();
		int n = p.length;
		//assert p.length == q.length
		int maxd = 0;
		double cost = 1.0 / 0.0;
		for (int d = 0; d < n; d++)
		{
			double tc = 0.0;
			for (int i = 0; i < n; i++)
				tc += dist2(p[(i + d) % n], q[i]);
			if (tc < cost)
			{
				maxd = d;
				cost = tc;
			}
		}
		return cost;
	}
	
	static double dist2(Point a, Point b) {
		return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
	}


}
