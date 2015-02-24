package com.mygdx.game;

public class MatchingAlg {

	public static void maxBipartite(double[][] w, int[] matl, int[] matr) {
		int n = w.length;
		int m = w[0].length;
		double[] disl = new double[n];
		double[] disr = new double[m];
		final double INF = 1.0 / 0.0;
		int[] precr = new int[m];
		boolean[] visited = new boolean[n];
		for (int i = 0; i < n; i++)
			matl[i] = -1;
		for (int i = 0; i < m; i++)
			matr[i] = -1;
		while (true)
		{
			//find longest path
			for (int i = 0; i < n; i++)
			{
				if (matl[i] == -1)
					disl[i] = 0;
				else
					disl[i] = -INF;
				visited[i] = false;
			}
			for (int i = 0; i < m; i++)
			{
				disr[i] = -INF;
				precr[i] = -1;
			}
			int maxr = -1;
			double maxdr = -INF;
			for (int t = 0; t < n; t++) //repeat n times
			{
				int maxi = -1;
				double maxd = -INF;
				for (int i = 0; i < n; i++)
					if (!visited[i] && disl[i] > maxd)
					{
						maxd = disl[i];
						maxi = i;
					}
				System.out.print("maxi = " + maxi + "\n");
				if (maxi == -1 || maxd == -INF)
					break;
				visited[maxi] = true;
				for (int j = 0; j < m; j++)
					if (disr[j] < maxd + w[maxi][j])
					{
						disr[j] = maxd + w[maxi][j];
						precr[j] = maxi;
						if (matr[j] == -1)
						{
							//renew max disr
							if (maxdr < disr[j])
							{
								maxdr = disr[j];
								maxr = j;
							}
						}
						else
						{
							//renew disl, disl[matr[j]] always smaller than disr[j] - w[matr[j]][j]
							disl[matr[j]] = disr[j] - w[matr[j]][j];
						}
					}
				for (int i = 0; i < matl.length; i++)
					System.out.print("disl[" + i + "] = " + disl[i] + "\n");
				for (int i = 0; i < matr.length; i++)
					System.out.print("disr[" + i + "] = " + disr[i] + "\n");
			}
			//if < 0, break
			for (int i = 0; i < matl.length; i++)
				System.out.print("disl[" + i + "] = " + disl[i] + "\n");
			for (int i = 0; i < matr.length; i++)
				System.out.print("disr[" + i + "] = " + disr[i] + "\n");
			System.out.print("maxdr = " + maxdr + "\n");
			if (maxdr < 0)
				break;
			//update
			int r = maxr, t;
			while (r != -1)
			{
				//match r and precr[r]. assert precr[r] != -1
				t = matl[precr[r]];
				matl[precr[r]] = r;
				matr[r] = precr[r];
				r = t;
			}
			for (int i = 0; i < matl.length; i++)
				System.out.print("matl[" + i + "] = " + matl[i] + "\n");
			for (int i = 0; i < matr.length; i++)
				System.out.print("matr[" + i + "] = " + matr[i] + "\n");
		}
	}
	
}
