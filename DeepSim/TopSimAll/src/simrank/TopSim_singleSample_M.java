package simrank;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import lxctools.FixedCacheMap;
import lxctools.StopWatch;
import structures.Graph;
import utils.Path;
import utils.Print;
import conf.MyConfiguration;
/**
 * use FixedHashTable<Integer>[] sim
 * do not store all similarities for each vertex.
 * @author luoxiongcai
 *
 */
public class TopSim_singleSample_M {
	protected static final int topk = MyConfiguration.TOPK;
	protected int capacity;
	protected int STEP = 5;	
	protected int COUNT;
	protected Graph g;
	protected FixedCacheMap[] sim;
	protected int SAMPLE = 10000;
	public static double[] cache;  
	
	
	@SuppressWarnings("unchecked")
	public TopSim_singleSample_M(Graph g, int M, int sample) {
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
			if(i%100==0) System.out.println("i: " + i);
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
//		int maxStep =Math.max( 2 * STEP , len);
//		for (int i = initSample; i < SAMPLE; i++) {
//			int pathLen = 0;
//			int[] path = new int[maxStep + 1];
//			Arrays.fill(path, -1);
//			path[0] = v;
//			int cur = v;
//			while (pathLen < maxStep){
//				cur = g.randNeighbor(cur);
//				if (cur == -1) break;
//				path[++pathLen] = cur;
//			}
//			// compute the sim to v;
//			computePathSim(path, pathLen);			
//		}
		
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
				if(degree !=0 && cur.sample >= degree){
					
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
				}else{
					
//					System.out.println("else...");
//					System.out.println("sample剩余：" + cur.sample);
					int number;
					if((int)cur.sample == cur.sample){
						number = (int)cur.sample;
					}else{
						number = (int)cur.sample + 1;
					}
//					number = Math.min(number, degree);
					for(int j=0;j<number;j++){	// 采样number个，随即找到路径走下去（但是sample会是一个小数）
						path = new Path[maxStep+1];
						path = queue.element().clone();
						
						Path nextCur = new Path();
						nextCur.sample = (double)cur.sample/(double)number;		// 对象是引用传递！
						int num = g.randNeighbor(cur.cur);	
						if (num == -1) break;
						nextCur.cur = num;
						path[pathLen+1] = nextCur;
						queue.add(path);
					}
				}
				// 之后处理多出来的边，如：sample=120，degree=100。 那么还有20条需要按照2/10发出去20条
//				
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
//	protected void computePathSim(int[] path, int pathLen){
//		if (pathLen == 0) return;
//		int source = path[0];
//		for (int i = 1 ; i <= STEP && 2 * i <= pathLen; i++){
//			int interNode = path[i];
//			int target = path[2*i];
//			if (source == target) continue;
//			if (isFirstMeet(path,0, 2*i)){
//				double incre =  cache[i]* g.degree(interNode)/ g.degree(target) / SAMPLE;
//				sim[source].put(target, (float)incre);	
//			}
//		}
//	}
	
	/**
	 * compute the similarity of one path that starts from path[0]
	 * @param path : a path from path[0]
	 * @param pathLen : the length of the path. 
	 */
	protected void computePathSim(Queue<Path[]> queue, int pathLen, int start){
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
				int interNode = path[i].cur;
				int target = path[2*i].cur;
				if (target == source) continue;
				if(target==-1) continue;
				if (isFirstMeet(path,0, 2*i)){
					
//					System.out.println(2*i + " " + path.length + " " + path[2*i].sample + " " + cache.length);
//					System.out.println(source + " " + target + " " + path[2*i].sample + " " + cache[i] + " " + g.degree(interNode)/ g.degree(target) );
					double incre =  path[2*i].sample * cache[i]* g.degree(interNode)/ g.degree(target) / SAMPLE;
					sim[source].put(target, (float)incre);	
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
	
	public FixedCacheMap[] getResult(){
		return sim;
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
