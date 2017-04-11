package simrank;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import lxctools.StopWatch;
import structures.DGraph;
import structures.Graph;
import utils.Path;
import utils.Print;
import conf.MyConfiguration;
/**
 * double random walk on undirected graph.
 * 对原始DoubleWalk进行处理，按照TopSim枚举的思想改进（否则一样会存在出发点高度数的问题）
 * @author luoxiongcai,Alan
 *
 */
public class TopSim_doubleSample {
	protected final int topk = MyConfiguration.TOPK;
	protected int STEP = 3;						// Sample步长
	protected int COUNT;  
	protected Graph g;
	protected double[][][] paths;
	protected double[][] sim;
	public static  int SAMPLE = 200;				// Sample路径条数，每一个顶点sample500条，走STEP步
	public static double[] cache;
	
	public TopSim_doubleSample(Graph g, int sample, int step){
		this.SAMPLE = sample;
		this.STEP = step;
		
		this.g = g;
		this.COUNT = g.getVCount();
		paths = new double[COUNT][COUNT][STEP+1];
		for(int i=0;i<COUNT;i++)
			for(int j=0;j<COUNT;j++)
				for(int k=0;k<=STEP;k++) paths[i][j][k] = -1;	//初始化-1
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
		//最后路径都加完了，计算sims，找到对应的路径！(这类操作在图框架中比较方便，属于接受消息类型，聚集在一起)
		computeSims();
		
		StopWatch.say("compute sims done! ");
	}
	
	private void samplePaths(){
		for (int v = 0; v < COUNT; v++){
			sample(v);
		}
	}
	
	private void sample(int src){
		// 每一次Sample的时候变化即可
		Queue<Path[]> queue = new LinkedList<Path[]>();
		Path[] path = new Path[STEP+1];
		for(int i=0; i<=STEP; i++){ 	//Arrays.fill(path, -1);
//			System.out.println(i + " " + maxStep);
			path[i] = new Path();
			path[i].cur = -1; 
			path[i].sample = 0.0;
		}
		path[0].cur = src; path[0].sample = SAMPLE;
		queue.add(path);
		
		int pathLen = 0; 
		int TopSim=1; 
		while (pathLen < STEP){
			if(pathLen == TopSim){
				computePath(queue, pathLen,TopSim);
				// 加一个加路径的好了。
				TopSim++;
			}
			int sum = queue.size();
			Path cur; 
			
//			System.out.println("pathLen = " + pathLen + "   sum = " + sum);
			
			for(int i=0;i<sum;i++){
				// 队列中有sum个是上一轮的，之后要删一个，加入x个
				path = new Path[STEP+1];
				path = queue.element().clone();	// 复制
				cur = new Path();
				cur.cur = path[pathLen].cur;  cur.sample = path[pathLen].sample;	// 手动复制
				
				int degree = g.degree(cur.cur);	//获得顶点cur的度数
				if(degree==0){
					System.out.println("!!!Somethin wrong..." + cur.sample + " " + degree);
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
						
						path = new Path[STEP+1];
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
						path = new Path[STEP+1];
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
		computePath(queue, pathLen, TopSim);
	}
	private void computePath(Queue<Path[]> queue, int pathLen, int step){
		Queue<Path[]> queue2 = new LinkedList<Path[]>();
		
		Path[] path = new Path[pathLen+1];
		while(!queue.isEmpty()){
			Path[] path2 = new Path[pathLen+1]; path2 = queue.element().clone();
			queue2.add(path2);
			
			path = queue.remove();
			
			if (pathLen == 0) return;
			int source = path[0].cur;
			// i到step开始计算，保证计算Step=2的时候不会重复计算Step=1的路径！——并且end即为Step长度
			int target = path[step].cur;
			if (target == source) continue;
			if(target==-1) continue;
			
			paths[source][target][step] = path[step].sample;
		}
		while(!queue2.isEmpty()){
			Path[] path2 = new Path[pathLen+1]; 
			path2 = queue2.element().clone();
			queue.add(path2);
			queue2.remove();
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
	
	private double getSim(int src, int dst){
		double result = 0;
		
		for (int i = 0; i < COUNT; i++){
			for(int step=1;step<=STEP;step++){
				//Step从1开始数的
				if(paths[src][i][step]>=0 && paths[dst][i][step]>=0 ){
					result += cache[step]*paths[src][i][step]*paths[dst][i][step];
				}
			}
		}
		return result;
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
