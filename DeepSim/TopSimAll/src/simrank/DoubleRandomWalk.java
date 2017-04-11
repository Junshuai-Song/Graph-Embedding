package simrank;

import java.io.IOException;

import lxctools.StopWatch;
import structures.DGraph;
import structures.Graph;
import utils.Print;
import conf.MyConfiguration;
/**
 * double random walk on undirected graph.
 * @author luoxiongcai
 *
 */
public class DoubleRandomWalk {
	protected final int topk = MyConfiguration.TOPK;
	protected int STEP = 3;						// Sample步长
	protected int COUNT;  
	protected Graph g;
	protected int[][][] paths;
	protected double[][] sim;
	public static  int SAMPLE = 200;				// Sample路径条数，每一个顶点sample500条，走STEP步
	public static double[] cache;
	
	public DoubleRandomWalk(Graph g, int sample, int step){
		this.SAMPLE = sample;
		this.STEP = step;
		
		this.g = g;
		this.COUNT = g.getVCount();
		paths = new int[COUNT][SAMPLE][STEP];
		sim = new double[COUNT][COUNT];
		cache = new double[STEP+1];
		for (int i = 0; i <= STEP; i++)
			cache[i] = Math.pow(MyConfiguration.C, i);
	}
	
	/**
	 * the main logic
	 */
	public void compute(){
		samplePaths();
		StopWatch.say("sample path done! ");
		
		computeSims();
		
		StopWatch.say("compute sims done! ");
	}
	
	private void samplePaths(){
		for (int v = 0; v < COUNT; v++){
			sample(v);
		}
	}
	
	private void sample(int src){
		for (int i = 0; i < SAMPLE; i++){
			int cur = src;
			for (int step = 0; step < STEP; step++){
				cur = g.randNeighbor(cur);
				paths[src][i][step] = cur;
				if (cur == -1) break;
			}
		}
	}
	
	private void computeSims(){
		for (int i = 0; i < COUNT; i++){
			if(i%100==0) System.out.println("i: " + i);
			for (int j = i+1; j < COUNT; j++){
				sim[i][j] = getSim(i,j);
				sim[j][i] = sim[i][j];
			}
		}
	}
	
	private double getSim(int v, int w){
		double result = 0;
		for (int i = 0; i < SAMPLE; i++){
			for (int j = 0; j < SAMPLE; j++){
				for (int step = 0; step < STEP && paths[v][i][step] != -1 &&
						paths[w][j][step] != -1; step++){
					if (paths[v][i][step] == paths[w][j][step]){
						result += cache[step+1];
						break;
					}
				}
			}
		}
		return result / (SAMPLE * SAMPLE);
	}
	
	public double[][] getResult(){
		return sim;
	}
	public static void main(String[] args) throws IOException {
//		int[] samples = {50,100,200,400,800};
//		for (int sample : samples){
//			StopWatch.start();
//			StopWatch.say("########## DoubleRandomWalk ##########");
//			Graph g = new Graph(MyConfiguration.u_u_graphPath, MyConfiguration.u_u_count);
//			
//			StopWatch.say("u_u_graph graph construction done!");
//			
//			DoubleRandomWalk srw = new DoubleRandomWalk(g,sample);
//			srw.compute();
//			StopWatch.say("u_u_graph doubleRandomWalk computation done!");
//			
//			String outPath = MyConfiguration.basePath_out+"/u_u_graphPath_doubleWalk_topk.txt";
//			Print.printByOrder(srw.getResult(), outPath, MyConfiguration.TOPK);
//			StopWatch.say("u_u_graph doubleRandomWalk output done!");
//		}
	}

}
