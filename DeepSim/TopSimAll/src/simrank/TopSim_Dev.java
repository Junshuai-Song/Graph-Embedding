package simrank;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import lxctools.FixedMaxPQ;
import lxctools.Pair;
import lxctools.StopWatch;
import structures.DGraph;
import structures.Graph;
import utils.Path;
import utils.Print;
import conf.MyConfiguration;
/**
 * 利用SingleWalk对DoubleWalk进行度数选择，
 * 注：对原始DoubleWalk进行处理，按照TopSim枚举的思想改进（否则一样会存在出发点高度数的问题）
 * @author luoxiongcai,Alan
 *
 */
public class TopSim_Dev {
	protected final int topk = MyConfiguration.TOPK;
	protected int STEP = 3;						// Sample步长
	protected int COUNT;  
	protected Graph g;
	protected double[][][] paths;
	protected double[][] sim;
	public int SAMPLE = 200;				// Sample路径条数，每一个顶点sample500条，走STEP步
	public static double[] cache;
	public int singleK = 10;
	
	public TopSim_Dev(Graph g, int sample, int step, int topK, int singleStep){
		this.SAMPLE = (int)(((step-singleStep)*sample*2.0)/((double)step*(topK+1.0)));	//一共还剩下这么多sample
//		if(this.SAMPLE <1.0 ) this.SAMPLE = (int)(sample*2/((double)topK+1.0));
		System.out.println(step + " " + singleStep  + " " + sample + " " + topK  + " ");
		System.out.println("SAMPLE = " + this.SAMPLE);
		
		this.STEP = step;
		this.singleK = topK;
		
		this.g = g;
		this.COUNT = g.getVCount();
		paths = new double[2][COUNT][STEP+1];
		
		sim = new double[COUNT][COUNT];
		cache = new double[STEP+1];
		for (int i = 0; i <= STEP; i++)
			cache[i] = Math.pow(MyConfiguration.C, i);
	}
	
	/**
	 * the main logic
	 */
	public void compute(double[][] candidate){
//		int temSample = this.SAMPLE;
		for (int i = 0; i < COUNT; i++){
			if(i%100==0) System.out.println("i: " + i);
			// 顶点Sample个数、这个是可调控的，一共有temSample条路径，这里控制如何做。
//			this.SAMPLE = (int)(temSample/((double)singleK+1.0));
			for(int j=0;j<COUNT;j++)
				for(int k=0;k<=STEP;k++) paths[0][j][k] = -1;
			sample(i, STEP,0);
			
			// 其他点Sample
//			this.SAMPLE = (int)(temSample/((double)singleK+1.0));
//			if(this.SAMPLE<1) this.SAMPLE = 1;	//保证其它顶点各有一条Sample，这个影响不大，甚至可以忽略
			FixedMaxPQ<Pair<Integer, Double>> maxpq = new FixedMaxPQ<Pair<Integer, Double>>(
					singleK);
			double max = 0;
			for (int j = 0; j < candidate[0].length; j++) {
				max = Math.max(candidate[i][j], max);
				if(candidate[i][j] >= MyConfiguration.MIN){
					maxpq.offer(new Pair<Integer, Double>(j, candidate[i][j]));
				}
			}
			int flag=0;
			for (Pair<Integer, Double> p : maxpq.sortedElement()) {
				flag++;
				// p.getKey()即是当前top的顶点
				int j = p.getKey();
//				sim[i][j] = candidate[i][j];	//先把1步的加入进来！
//				sim[i][j] += getSim(i,j);
//				sim[i][j] = getSim(i,j);
				for(int jj=0;jj<COUNT;jj++)
					for(int k=0;k<=STEP;k++) paths[1][jj][k] = -1;	//初始化-1
				sample(j, STEP, 1);
				computeSims(candidate, i, j);	// 每次Sample一条，统计记录一次，减少空间使用
//				sim[j][i] = sim[i][j];	// 这个去掉即可！
//				if(sim[i][j]!=0)
//					System.out.println(i + " " + j + "  sim：" + sim[i][j]);
//				System.out.print(j + " ");
			}
			sim[i][i] = 0;
		}
	}
	
	private void samplePaths(){
		for (int v = 0; v < COUNT; v++){
//			sample(v);
		}
	}
	
	private void sample(int src, int maxStep, int flag){
		// 每一次Sample的时候变化即可
		Queue<Path[]> queue = new LinkedList<Path[]>();
		Path[] path = new Path[STEP+1];
		for(int i=0; i<=STEP; i++){ 	//Arrays.fill(path, -1);
//			System.out.println(i + " " + maxStep);
			path[i] = new Path();
			path[i].cur = -1; 
			path[i].sample = 0.0;
		}
		path[0].cur = src; path[0].sample = SAMPLE;	// 用到sample的个数
		queue.add(path);
		
		int pathLen = 0; 
		int TopSim=1; 
		while (pathLen < STEP){
			if(pathLen == TopSim){
				computePath(queue, pathLen,TopSim, flag);
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
						
//						System.out.print("路径：" + src + "    ");
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
		computePath(queue, pathLen, TopSim, flag);
	}
	private void computePath(Queue<Path[]> queue, int pathLen, int step, int flag){
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
			
			paths[flag][target][step] = path[step].sample;
		}
		while(!queue2.isEmpty()){
			Path[] path2 = new Path[pathLen+1]; 
			path2 = queue2.element().clone();
			queue.add(path2);
			queue2.remove();
		}
	}
	
	private void computeSims(double[][] candidate, int src, int dst){
		sim[src][dst] = getSim(0,1);
//		System.out.println(src + " " + dst + "  sim:" + sim[src][dst]);
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
