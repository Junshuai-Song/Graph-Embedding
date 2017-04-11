package simrank;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import lxctools.Log;
import lxctools.StopWatch;
import structures.Graph;
import utils.Print;
import conf.MyConfiguration;
/**
 * 目前计算所有节点的TopK，复杂度也比DoubleRandomWalk低，统计的时候Double的特别麻烦？；
 * 算单个节点的时候，才有优势体现出来，直接walk一条路径即可。
 * @author luoxiongcai
 *
 */
public class SingleRandomWalk {
	protected final int topk = MyConfiguration.TOPK;
	protected int STEP = 1;	
	protected int COUNT;
	protected Graph g;
	protected double[][]sim;
	public static  int SAMPLE = 10000;		// 每一个顶点sample 1000条，走STEP步
	public static double[] cache;  
	
	public SingleRandomWalk(Graph g, int sample, int step) {
		this.STEP = step;
		this.SAMPLE = sample;
		this.g = g;
		this.COUNT = g.getVCount();
		sim = new double[COUNT][COUNT];
		cache = new double[STEP+1];
		for (int i = 1; i <= STEP; i++)
			cache[i] = Math.pow(MyConfiguration.C, i);
	}

	public void compute(){
		for (int i = 0; i < COUNT; i++){
			if(i%100==0) System.out.println("i: " + i);
			walk(i, 2*STEP,0);
			sim[i][i] = 0;
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
//			if(i%1000000==0){
//				System.out.println(i);
//			}
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
			if (target == source) continue;
			if (isFirstMeet(path,0, 2*i)){
				sim[source][target] +=  cache[i]* g.degree(interNode)/ g.degree(target) / SAMPLE;
			}
		}
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
	
	/**
	 * srcIndex > dstIndex.
	 * @param path
	 * @param srcIndex
	 * @param dstIndex
	 * @return
	 */
	public boolean isFirstMeetReverse(int[] path, int srcIndex, int dstIndex){
		int internal = (srcIndex - dstIndex) / 2 + dstIndex; 
		for (int i = dstIndex; i < internal; i++){
			if (path[i] == path[srcIndex - i + dstIndex]) return false;
		}
		return true;
	}
	
	public double[][] getResult(){
		return sim;
	}


	public static void main(String[] args) throws IOException {
//		Log log = new Log("log.txt");
//		StopWatch.start();
//		StopWatch.say("######## SingleRandomWalk ###");
//		Graph g = new Graph(MyConfiguration.u_u_graphPath, MyConfiguration.u_u_count);
////		Graph g = new Graph(base+"/movielens_user_tag.txt",20537);
////		Graph g = new Graph(MyConfiguration.dataBasePath+"/TEST.txt", 12);
////		Graph g = new Graph(MyConfiguration.realdata+"/moreno_crime_crime.txt", 1380);
//
//		StopWatch.say("u_u_graph computation done! v: " + g.getVCount());
////		log.info("u_u_graph construction done!");
//		SingleRandomWalk srw = new SingleRandomWalk(g,10000,3);
//		srw.compute();
//		StopWatch.say("u_u_graph SingleRandomWalk computation done!");
////		log.info("SingleRandomWalk computation done!");
////		log.close();
//		String outPath = MyConfiguration.basePath_out+"/u_u_graphPath_singleWalk_topk.txt";
//		Print.printByOrder(srw.getResult(), outPath, MyConfiguration.TOPK);
	}

}
