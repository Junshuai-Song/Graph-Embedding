package sjstools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import lxctools.FixedMaxPQ;
import lxctools.Log;
import lxctools.Pair;
import structures.Graph;
import utils.Eval;
import utils.Path;
import utils.Print;
import conf.MyConfiguration;
/**
 * 生成各个点采样出来的路径
 * 注意：对于单独存在的顶点或者两个点，我们特殊处理，不将其作为相应的样本
 * @author luoxiongcai,Alan
 *
 */

public class ProducePaths {
	protected final int topk = MyConfiguration.TOPK;
	protected int STEP = 1;	
	protected int COUNT;
	protected Graph g;
	protected double[][]sim;
	public static  int SAMPLE = 10000;		// 每一个顶点sample 1000条，走STEP步
	public static double[] cache;  
	public String filePath;
	public BufferedWriter out;
	
	// 其他特征
	public int nodeNum=0;	//遇到的顶点个数
	public int vertexNum=0;	//顶点个数
	public int edgeNum=0;	//边个数--得到平均度数
	
	
	public ProducePaths(Graph g, int sample, int step) {
		this.STEP = step;
		this.SAMPLE = sample;
		this.g = g;
		this.COUNT = g.getVCount();
		sim = new double[COUNT][COUNT];
		cache = new double[STEP+1];
		for (int i = 1; i <= STEP; i++)
			cache[i] = Math.pow(MyConfiguration.C, i);
	}

	
	public void producePaths(String filePath) throws IOException{
		this.filePath = filePath;
		this.out = new BufferedWriter(new FileWriter(this.filePath));
		// walk(92,2*STEP,0);	//尝试将0号点，看能否提高到最高
		for (int i = 0; i < COUNT; i++){
			if(i%100==0) System.out.println("i: " + i);
			this.out.append(i + " ");	// 标记当前是哪个顶点
			walk(i, 2*STEP,0);
			sim[i][i] = 0;
		}
		this.out.close();
	}
	
	/**
	 * 
	 * @param v : the start node of the random walk
	 * @param len : the length of the random walk
	 * @param initSample : already sampled count.for reuse other paths.
	 * @throws IOException 
	 */
	protected void walk(int v, int len, int initSample) throws IOException{
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
					// 唯一出现的可能是当前点没有连边（且是第一轮）
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
		// 如果Step=1，那么就只在长度为3的时候计算一下。下一次就是Step=2，长度为5的时候
		computePathSim(queue, pathLen, TopSim);			
		
	}
	

	
	/**
	 * compute the similarity of one path that starts from path[0]
	 * @param path : a path from path[0]
	 * @param pathLen : the length of the path. 
	 * @throws IOException 
	 */
	protected void computePathSim(Queue<Path[]> queue, int pathLen, int start) throws IOException{

		int nodesFlag1[] = new int[this.g.getVCount()+1];
		int nodesFlag2[] = new int[this.g.getVCount()+1];
		int nodesFlag3[] = new int[this.g.getVCount()+1];
		for(int i=0;i<this.g.getVCount()+1;i++) {nodesFlag1[i]=0; nodesFlag2[i]=0;nodesFlag3[i]=0;}
		Queue<Path[]> queue2 = new LinkedList<Path[]>();
//		BlockingQueue<Path[]> queue;
		Path[] path = new Path[pathLen+1];
		
		// 做一些统计量
		double maxc,minc,sum,sumTwo,sumThree;
		int countTwo=0,countThree=0;
		
		while(!queue.isEmpty()){
			Path[] path2 = new Path[pathLen+1]; path2 = queue.element().clone();
			queue2.add(path2);
			
			path = queue.remove();
			
			if (pathLen == 0) return;
			int source = path[0].cur;
			int cnt = 0;
			for (int i = start ; i <= STEP && 2 * i <= pathLen; i++){
				int interNode = path[i].cur;
				int target = path[2*i].cur;
				if(target==-1) continue;

				for(int j=0;j<=2*i;j++){
//					System.out.print(path[j].cur + " " + path[j].sample + " ");
//					this.out.append(path[j].cur + " " + path[j].sample + " ");
					nodesFlag1[path[j].cur] = 1;
				}
				for(int j=1;j<=2*i;j++){
//					System.out.print(path[j].cur + " " + path[j].sample + " ");
					this.out.append(path[j].sample + " ");
				}
				// 分别记录第二步和第三步遇到的顶点个数
				nodesFlag2[path[1].cur] = 1;
				nodesFlag3[path[2].cur] = 1;
				
//				System.out.println("");
//				this.out.append("\n");
				cnt++;	//记录一共出现了多少条路径
			}
		}
		
		int cnt1=0,cnt2=0,cnt3=0,cnt4=0;
		for(int i=0;i<this.g.getVCount()+1;i++){ 
			if(nodesFlag1[i]==1)
				cnt1++;
			if(nodesFlag2[i]==1) 
				cnt2++;
			if(nodesFlag3[i]==1)
				cnt3++;
			if(nodesFlag2[i]==1 && nodesFlag3[i]==1){
				cnt4++;
			}
		}
		//增加其他特征
		this.out.append(cnt1 + " " + cnt2 + " " + cnt3 + " ");			//一共遇到的顶点、第二步遇到的顶点数、第三步遇到的顶点数
		this.out.append(this.g.vCount + " " + this.g.eCount + " " + cnt4 + " ");	//顶点个数、边个数
		this.out.append("\n");	//换行即可
		
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
		int[] testTopK = MyConfiguration.testTopK;
		int fileNum = MyConfiguration.fileNum;
//		int fileNum = 3;
		for(int i=0;i<fileNum;i++){
			// 基本的输入路径，都一样
			String graphInPath = MyConfiguration.in_u_u_graphPath[i];
			String goldPath = MyConfiguration.out_u_u_graphPath_simrank[i] + "_simrank_navie_top" + MyConfiguration.TOPK +".txt";
			// 基本的输出路径
			String basePath = MyConfiguration.out_u_u_graphPath_producePaths[i];
			
			int[] samples = {10000};	// 暂时以1W为例
			int[] steps = {1};			// 步长也都暂时是1，即Step*2步，到达第三个点。
			
			Graph g = new Graph(graphInPath, MyConfiguration.u_u_count[i]);

			for(int step: steps){
				for (int sample_ : samples){	// 这里step加上之后，路径还要进一步调整
					int sample =sample_;	// 与DoubleWalk的Sample路径个数一致
					String outPath = basePath + "_paths_step" + step + "_sample" + sample + ".txt";
					ProducePaths pro = new ProducePaths(g,sample,step);
					pro.producePaths(outPath);
					// 计算精度，传入sim[][]数组,计算前k个相似的点输出到文件
//						Print.printByOrder(pro.getResult(), outPath, MyConfiguration.TOPK, k);
//						Eval.precision(goldPath, outPath, prePath,k);
				}
			}
		}
		System.out.println("下一步：对于生成的路径文件，在使用的时候需要按照路径上度数重新从大到小排序.C++中ProducePaths().");
	}

}
