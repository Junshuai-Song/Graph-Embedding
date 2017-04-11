package simrank;


import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import lxctools.FixedCacheMap;
import lxctools.Log;
import lxctools.StopWatch;

import conf.MyConfiguration;
import structures.Graph;
import structures.WGraph;
import utils.Print;

/**
 * 
 * @author luoxiongcai
 * 
 */
public class SingleRandomWalkReuse {
	Log log = new Log(MyConfiguration.basePath+"/power_graph/path_37440_5.txt");
	private int[] sampleCount;
	private int batchSize = 2500; 
	private int times = 2;

	protected final int topk = MyConfiguration.TOPK;
	protected final int M = 100;
	protected int STEP = 10;
	protected int COUNT;
	protected int stopV;
	protected Graph g;
//	protected WGraph wg;
	protected FixedCacheMap[] sim;
//	protected double[][]sim;
	public static int SAMPLE ;
	public static double[] cache;

	public SingleRandomWalkReuse(WGraph wg, int sample) {
		this.SAMPLE = sample;
//		this.wg = wg;
		this.COUNT = wg.getVCount();
		this.stopV = Math.min(720, COUNT);
//		sim = new double[stopV][COUNT];
		sim = new FixedCacheMap[stopV];
		for (int i = 0; i < stopV; i++) {
			sim[i] = new FixedCacheMap(topk * M);
		}

		cache = new double[STEP + 1];
		for (int i = 1; i <= STEP; i++)
			cache[i] = Math.pow(MyConfiguration.C, i);
//		sampleCount = new int[g.getVCount()];
		System.out.println("SAMPLE:"+SAMPLE);
	}
	
//	public SingleRandomWalkReuse(Graph g, int sample) {
//		this.SAMPLE = sample;
//		this.g = g;
//		this.COUNT = g.getVCount();
//		this.stopV = Math.min(720, COUNT);
////		sim = new double[stopV][COUNT];
//		sim = new FixedCacheMap[stopV];
//		for (int i = 0; i < stopV; i++) {
//			sim[i] = new FixedCacheMap(topk * M);
//		}
//
//		cache = new double[STEP + 1];
//		for (int i = 1; i <= STEP; i++)
//			cache[i] = Math.pow(MyConfiguration.C, i);
////		sampleCount = new int[g.getVCount()];
//		System.out.println("SAMPLE:"+SAMPLE);
//	}

	private int checkFinished() {
		int c = 0;
		for (int sc : sampleCount) {
			if (sc >= SingleRandomWalk.SAMPLE)
				c++;
		}
		return c;
	}

	public void compute() {
//		walk(713, times * STEP, 0);

		
		for (int i = 0; i < stopV; i++) {
//			 if ( i % batchSize == 0){
//			 System.out.println("stage "+(i /
//			 batchSize)+"\t"+checkFinished());
//			 }
			walk(i, times * STEP, 0);
//			 sampleCount[i] = SAMPLE;

		}
		


//		 System.out.println("stage "+(g.getVCount() /
//		 batchSize)+"\t"+checkFinished());
	}

	/**
	 * 
	 * @param v
	 *            : the start node of the random walk
	 * @param len
	 *            : the length of the random walk
	 * @param initSample
	 *            : already sampled count.for reuse other paths.
	 */
	protected void walk(int v, int len, int initSample) {
		int maxStep = Math.max(2 * STEP, len);
		for (int i = initSample; i < SAMPLE; i++) {
			int pathLen = 0;
			int[] path = new int[maxStep + 1];
			Arrays.fill(path, -1);
			path[0] = v;
			int cur = v;
			while (pathLen < maxStep) {
//				cur = wg.randNeighborByWeight(cur);
				if (cur == -1)
					break;
				path[++pathLen] = cur;
			}
			// compute the sim to v;
			computePathSim(path, pathLen);
		}
	}

	public void computePathSim(int[] path, int pathLen) {
		if (pathLen == 0)
			return;
		for (int off = 0; off <= (times - 2) * STEP; off++) {
			int source = path[off];
//			if (source >= stopV || sampleCount[source] >= SAMPLE)
//				continue;
//			sampleCount[source]++;
			for (int i = 1; i <= STEP && off + 2 * i <= pathLen; i++) {
				int interNode = path[off + i];
				int target = path[off + 2 * i];
				
				if (source == target) continue;
//				double incre = cache[i] * wg.degree(interNode) / wg.degree(target) / SAMPLE;
				double incre = cache[i] * adjustWeight(path, off, i)/SAMPLE;
				if (isFirstMeet(path, off, off + 2 * i))
					sim[source].put(target, (float) incre);
//					sim[source][target] += incre;
			}
		}

	}
	
	private double adjustWeight(int[]path, int off, int step){
		double aw = 0;
		int v_middle = path[off+step];
		int v_end = path[off + 2*step];
//		double x1 =  normalWeight(v_end, path[off+2*step-1]);
//		double xx = normalWeight(v_middle, path[off+step+1]);
//		aw = x1 /xx ;
//		if (xx <0.00000001)
//			System.out.println(v_middle+"\t"+path[off+step+1]+"\t"+xx);
		return aw;
	}
	
//	private double normalWeight(int src, int dst){
//		Map<Integer, Double> edges = wg.getAllEdgeWeight(src);
//		double dst_w = edges.get(dst);
//		double sum = 0;
//		for (Entry<Integer, Double> edge : edges.entrySet()){
//			sum += edge.getValue();
//		}
//		return dst_w/sum;
//	}

	/**
	 * srcIndex < dstIndex
	 * 
	 * @param path
	 * @param targetIndex
	 * @return
	 */
	public boolean isFirstMeet(int[] path, int srcIndex, int dstIndex) {
		int internal = (dstIndex - srcIndex) / 2 + srcIndex;
		for (int i = srcIndex; i < internal; i++) {
			if (path[i] == path[dstIndex - i + srcIndex])
				return false;
		}
		return true;
	}

	public FixedCacheMap[] getResult() {
		return sim;
	}

	public static void main(String[] args) throws IOException {
		StopWatch.start();
		
//		Graph g = new Graph(MyConfiguration.biGraphPath, MyConfiguration.totalCount);
//		Graph g = new Graph(MyConfiguration.realdata+"/moreno_crime_crime.txt", 1380);
		WGraph wg = new WGraph(MyConfiguration.biGraphPath, MyConfiguration.totalCount);
		StopWatch.say("weighted singleRandomWalk_reuse load graph done!");
		SingleRandomWalkReuse srwo = new SingleRandomWalkReuse(wg, 10000);
		srwo.compute();
		StopWatch.say(" SingleRandomWalk_reuse computation done!");

		Print.printByOrder(srwo.getResult(), MyConfiguration.basePath
				+ "/SingleWalk_biGraph_10k_5.txt", MyConfiguration.TOPK);
		StopWatch.say(" SingleRandomWalk_reuse output results done!");
		srwo.log.close();
	}

}
