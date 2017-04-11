package simrank;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import lxctools.Log;
import lxctools.StopWatch;
import structures.Graph;
import utils.Path;
import utils.Print;
import conf.MyConfiguration;
/**
 * 最基本的TopSim方法，进行全枚举
 * @author songjs
 *
 */

public class TopSim_Enumerate {
	protected final int topk = MyConfiguration.TOPK;
	protected int STEP = 1;	
	protected int COUNT;
	protected Graph g;
	protected double[][]sim;
	public static  int SAMPLE = 10000;		// 每一个顶点sample 1000条，走STEP步
	public static double[] cache;  
	
	public TopSim_Enumerate(Graph g, int sample, int step) {
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
		walk(0,2*STEP,0);	//尝试将0号点，看能否提高到最高
//		for (int i = 0; i < COUNT; i++){
//			if(i%100==0) System.out.println("i: " + i);
//			walk(i, 2*STEP,0);
//			sim[i][i] = 0;
//		}
	}
	
	/**
	 * 
	 * @param v : the start node of the random walk
	 * @param len : the length of the random walk
	 * @param initSample : already sampled count.for reuse other paths.
	 */
	protected void walk(int v, int len, int initSample){
		int maxStep =Math.max( 2 * STEP , len);
		// 声明一个Path队列
		Queue<Path[]> queue = new LinkedList<Path[]>();
		Path[] path = new Path[maxStep+1];
		for(int i=0; i<=maxStep; i++){ 	//Arrays.fill(path, -1);
//			System.out.println(i + " " + maxStep);
			path[i] = new Path();
			path[i].cur = -1; 
			path[i].sample = 0.0;
		}
		path[0].cur = v; path[0].sample = SAMPLE;
		queue.add(path);
		
		
		int pathLen = 0; 
		int TopSim=1; 
		while (pathLen < maxStep){
			if(pathLen/2 == TopSim){
				computePathSim(queue, pathLen,TopSim);
				TopSim++;
			}
			int sum = queue.size();
			Path cur; 
//			System.out.println("pathLen = " + pathLen + "   sum = " + sum);
			for(int i=0;i<sum;i++){
				if(i%10000==0){
					System.out.println(i + " " + sum);
				}
				// 队列中有sum个是上一轮的，之后要删一个，加入x个
				path = new Path[maxStep+1];
				path = queue.element().clone();	// 复制
				cur = new Path();
				cur.cur = path[pathLen].cur;  cur.sample = path[pathLen].sample;	// 手动复制
				
				int degree = g.degree(cur.cur);	//获得顶点cur的度数
				if(degree==0){
					// 只能是某一轮出现遇到了单独存在的顶点
					System.out.println("!!!当前点无连边..." + cur.cur + " " + degree);
				}
//				if(degree !=0 && cur.sample >= degree){
				if(degree !=0){	
					
					// 最起码每条都要送去一条边(newSample条)
					List<Integer> edges;
					edges = g.neighbors(cur.cur);	//获得顶点cur的所有边
					double newSample = ((double)cur.sample/(double)degree);
					
					for(int j=0;j<degree;j++){
						// 对每一条边，进行sample
						Path nextCur = new Path();
						nextCur.sample = newSample;
						nextCur.cur = edges.get(j);	//edges内保存顶点的id
						
						path = new Path[maxStep+1];
						path = queue.element().clone();
						path[pathLen+1] = nextCur;
						queue.add(path);
						
//						System.out.print("路径：");
//						for(int k=0;k<=pathLen+1;k++){
//							 System.out.print( path[k].cur + " ");
//						}System.out.println();
//						for(int k=0;k<=pathLen+1;k++){
//							 System.out.print( path[k].sample + " ");
//						}System.out.println();
					}
//					cur.sample -= newSample*degree;
				}
				queue.remove();	// 最后再删除
			}
			pathLen++;
		}
		// compute the sim to v;
		computePathSim(queue, pathLen, TopSim);			
	}
	

	
	/**
	 * compute the similarity of one path that starts from path[0]
	 * @param path : a path from path[0]
	 * @param pathLen : the length of the path. 
	 */
	protected void computePathSim(Queue<Path[]> queue, int pathLen, int start){
		System.out.println("compute" + start);
		
		Queue<Path[]> queue2 = new LinkedList<Path[]>();
		
//		BlockingQueue<Path[]> queue;
		Path[] path = new Path[pathLen+1];
		while(!queue.isEmpty()){
			Path[] path2 = new Path[pathLen+1]; path2 = queue.element().clone();
			queue2.add(path2);
			
			path = queue.remove();
			
			if (pathLen == 0) return;
			int source = path[0].cur;
			for (int i = start ; i <= STEP && 2 * i <= pathLen; i++){
				// 这里是只计算当前长度的
				int interNode = path[i].cur;
				int target = path[2*i].cur;
				if (target == source) continue;
				if(target==-1) continue;
				if (isFirstMeet(path,0, 2*i)){
					
//					System.out.println(2*i + " " + path.length + " " + path[2*i].sample + " " + cache.length);
//					System.out.println(source + " " + target + " " + path[2*i].sample + " " + cache[i] + " " + g.degree(interNode)/ g.degree(target) );
					sim[source][target] +=  path[2*i].sample * cache[i]* (double)g.degree(interNode)/ (double)g.degree(target)  ;
	//				sim[source][target] +=  cache[i]* Math.min(g.degree(interNode)/ g.degree(target), this.SAMPLE) / SAMPLE;
	//				if(1.0*g.degree(interNode)/ g.degree(target) <50000){
	//					sim[source][target] +=  cache[i]* g.degree(interNode)/ g.degree(target) / SAMPLE;
	//				}
				}
			}
		}
		while(!queue2.isEmpty()){
			Path[] path2 = new Path[pathLen+1]; 
			path2 = queue2.element().clone();
			queue.add(path2);
			queue2.remove();
		}
	}
	
	/**
	 * srcIndex < dstIndex
	 * @param path
	 * @param targetIndex
	 * @return
	 */
	public boolean isFirstMeet(Path[] path, int srcIndex, int dstIndex){
		
		int internal = (dstIndex - srcIndex) / 2 + srcIndex;
		for (int i = srcIndex; i < internal; i++){
			if (path[i].cur == path[dstIndex - i + srcIndex].cur) return false;
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
