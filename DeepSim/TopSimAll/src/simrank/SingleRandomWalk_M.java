package simrank;

import java.io.IOException;
import java.util.Arrays;
import lxctools.FixedCacheMap;
import lxctools.StopWatch;

import structures.Graph;
import utils.Print;
import conf.MyConfiguration;
/**
 * use FixedHashTable<Integer>[] sim
 * do not store all similarities for each vertex.
 * @author luoxiongcai
 *
 */
public class SingleRandomWalk_M {
	protected static final int topk = MyConfiguration.TOPK;
	protected int capacity;
	protected int STEP = 5;	
	protected int COUNT;
	protected Graph g;
	protected FixedCacheMap[] sim;
	protected int SAMPLE = 10000;
	public static double[] cache;  
	
	
	@SuppressWarnings("unchecked")
	public SingleRandomWalk_M(Graph g, int M, int sample) {
		this.g = g;
		this.COUNT = g.getVCount();
		sim = new FixedCacheMap[COUNT];
		capacity = topk * M;
		SAMPLE = sample;
		for (int i = 0; i < COUNT; i++){
			sim[i] = new FixedCacheMap(capacity);
		}
		
		cache = new double[STEP+1];
		for (int i = 1; i <= STEP; i++)
			cache[i] = Math.pow(MyConfiguration.C, i);
	}

	public void compute(){
		for (int i = 0; i < COUNT; i++){
			walk(i, 2*STEP,0);
		}
			
	}
	
	/**
	 * 
	 * @param v : the start node of the random walk
	 * @param len : the length of the random walk
	 * @param initSample : already sampled count.for reuse other paths.
	 */
	protected void walk(int v, int len, int initSample){
		int maxStep =Math.max( 2 * STEP , len);
		for (int i = initSample; i < SAMPLE; i++) {
			int pathLen = 0;
			int[] path = new int[maxStep + 1];
			Arrays.fill(path, -1);
			path[0] = v;
			int cur = v;
			while (pathLen < maxStep){
				cur = g.randNeighbor(cur);
				if (cur == -1) break;
				path[++pathLen] = cur;
			}
			// compute the sim to v;
			computePathSim(path, pathLen);			
		}
	}
	

	
	/**
	 * compute the similarity of one path that starts from path[0]
	 * @param path : a path from path[0]
	 * @param pathLen : the length of the path. 
	 */
	protected void computePathSim(int[] path, int pathLen){
		if (pathLen == 0) return;
		int source = path[0];
		for (int i = 1 ; i <= STEP && 2 * i <= pathLen; i++){
			int interNode = path[i];
			int target = path[2*i];
			if (source == target) continue;
			if (isFirstMeet(path,0, 2*i)){
				double incre =  cache[i]* g.degree(interNode)/ g.degree(target) / SAMPLE;
				sim[source].put(target, (float)incre);	
			}
		}
	}
	
	public FixedCacheMap[] getResult(){
		return sim;
	}
	
	/**
	 * srcIndex < dstIndex
	 * @param path
	 * @param targetIndex
	 * @return
	 */
	public boolean isFirstMeet(int[] path, int srcIndex, int dstIndex){
		int internal = (dstIndex - srcIndex) / 2 + srcIndex;
		for (int i = srcIndex; i < internal; i++){
			if (path[i] == path[dstIndex - i + srcIndex]) return false;
		}
		return true;
	}

	public static void main(String[] args) throws IOException {
//		StopWatch.start();
//		Graph g = new Graph(MyConfiguration.biGraphPath, MyConfiguration.totalCount);
//		StopWatch.say("bi_graph construction done!");
//		SingleRandomWalk_M srw = new SingleRandomWalk_M(g, 5, 10000);
//		srw.compute();
//		StopWatch.say("SingleRandomWalk_M computation done!");
//		String outPath = MyConfiguration.basePath + "/singleWalk_topk_M.txt";
//		Print.printByOrder(srw.getResult(), outPath, MyConfiguration.TOPK);
	}

}
