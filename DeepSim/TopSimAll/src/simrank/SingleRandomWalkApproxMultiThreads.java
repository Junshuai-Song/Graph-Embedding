package simrank;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import lxctools.FixedCacheMap;
import lxctools.Pair;
import lxctools.StopWatch;

import structures.Graph;
import conf.MyConfiguration;
/**
 * FixedCacheMap for approximation
 * use multithreads.
 */
public class SingleRandomWalkApproxMultiThreads implements Runnable {
	protected CountDownLatch latch;
	protected BufferedWriter output;
	protected String path;
	protected int base;
	protected int mod = 16;
	protected int cacheNum = 1000;
	protected final int topk = MyConfiguration.TOPK;
	protected int capacity = 5 * topk;
	protected int STEP = 6;	
	protected int COUNT;
	protected Graph g;
	protected FixedCacheMap[] sim;
	protected int SAMPLE = 10000;
	public static double[] cache;  
	
	
	@SuppressWarnings("unchecked")
	public SingleRandomWalkApproxMultiThreads(Graph g, int base, String path,CountDownLatch latch) {
		this.latch = latch;
		this.base = base;
		this.g = g;
		this.path = path;
		this.COUNT = g.getVCount();
		sim = (FixedCacheMap[])new FixedCacheMap[cacheNum];
		for (int i = 0; i < cacheNum; i++){
			sim[i] = new FixedCacheMap(capacity);
		}
		
		cache = new double[STEP+1];
		for (int i = 1; i <= STEP; i++)
			cache[i] = Math.pow(MyConfiguration.C, i);
	}

	@Override
	public void run() {
		System.out.println(base +" begin to run...");
		try {
			output = new BufferedWriter(new FileWriter(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		int cachePtr = 0;
		int v = base;
		for (; v < COUNT ; v += mod){
			walk(v, 2*STEP,0);
			
			if (cachePtr == cacheNum){
				//output
				try {
					output(v - mod, cachePtr - 1);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				cachePtr =0;
				
			}
			cachePtr++;
		}
		if (cachePtr != 0 ){
			//output.
			try {
				output(v - mod,cachePtr - 1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(base +" done!");
		latch.countDown();
	}
	
	private void output(int v, int cachePtr) throws IOException{
		for (int i = cachePtr; i >=0 ; i--){
			output.append(v+"");
			int num = 0;
			for (Pair<Integer, Float> p : sim[i]){
				if (num >= MyConfiguration.TOPK) break;
				output.append(MyConfiguration.SEPARATOR+ p.getKey());
				num++;
			}
			output.append("\r\n");
			v -= mod;
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
		Set<Integer> visited = new HashSet<Integer>();
		for (int i = 1 ; i <= STEP && 2 * i <= pathLen; i++){
			int interNode = path[i];
			int target = path[2*i];
			if (visited.contains(target)) continue;
			visited.add(target);
			if (source == target) continue;
			float incre =  (float) (cache[i]* g.degree(interNode)/ g.degree(target) / SAMPLE);
			sim[((source - base) / mod) % cacheNum].put(target, incre);
		}
	}
	



	public static void main(String[] args) throws IOException, InterruptedException {
		StopWatch.start();
		Graph g = new Graph(MyConfiguration.biGraphPath, MyConfiguration.totalCount);
		StopWatch.say("biGraph construction done!");
		int mod = 16;
		CountDownLatch latch = new CountDownLatch(mod);
		Thread[] threads = new Thread[mod];
		String path = "./data/multithreads_srw_topk";
		for (int i = 0; i < mod ; i++){
			threads[i] = new Thread(new SingleRandomWalkApproxMultiThreads(g, i, path+"_"+i+".txt",latch));
			threads[i].start();
		}
		latch.await();
		StopWatch.say("multithreads computation done!");
	}



}
