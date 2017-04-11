package structures;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import lxctools.LxcArrays;
import conf.MyConfiguration;

/**
 * undirected and weighted graph
 * @author luoxiongcai
 *
 */

public class DWGraph {
	private static final Random rand = new Random();
	
	private  int vCount;
	private  int eCount;
	private List<Integer>[] outs; //out neighbor list.
	private List<Integer>[] ins; //  in neighbor list.
	private List<Double>[] outs_pro; //out_pro neighbor list.
	private List<Double>[] ins_pro;
	
	public List<Double> in_all_weight;
	private List<Double> varience;
	
//	private List<Integer>[] adjs; // adjacent list.
	private Map<Integer, Double>[] edges_in; // adjacent edges with weight.
	private Map<Integer, Double>[] edges_out; // adjacent edges with weight.
	
	/**
	 * construction from an edge file.
	 * @param graphPath
	 * @throws IOException 
	 */
	public DWGraph(String graphPath, int V) throws IOException{
		this.vCount = V;
		outs = (List<Integer>[]) new ArrayList[this.vCount];
		ins = (List<Integer>[]) new ArrayList[this.vCount];
		outs_pro = (List<Double>[]) new ArrayList[this.vCount];
		ins_pro = (List<Double>[]) new ArrayList[this.vCount];
		in_all_weight = new ArrayList<Double>();
		varience = new ArrayList<Double>();
		edges_in = (Map<Integer, Double>[]) new HashMap[this.vCount];
		edges_out = (Map<Integer, Double>[]) new HashMap[this.vCount];
		for (int i = 0; i < this.vCount; i++){
			outs[i] = new ArrayList<Integer>();
			ins[i] = new ArrayList<Integer>();
			outs_pro[i] = new ArrayList<Double>();
			ins_pro[i] = new ArrayList<Double>();
			edges_in[i] = new HashMap<Integer, Double>();
			edges_out[i] = new HashMap<Integer, Double>();
		}
		BufferedReader input = new BufferedReader(new FileReader(graphPath));
		String line = null;
		while((line = input.readLine())!= null){
			String[] ids = line.split( MyConfiguration.SEPARATOR);
			if (ids.length == 2)
				addEdge(Integer.valueOf(ids[0]), Integer.valueOf(ids[1]));
			else if (ids.length == 3)
				addEdge(Integer.valueOf(ids[0]), Integer.valueOf(ids[1]), Double.valueOf(ids[2]));
			else throw new IOException("invalid edge file: should be [src, dst] or  [src, dst, w]");
		}
		input.close();
		pre_deal();
		deal_varience();
	}
	public void pre_deal(){
		// 处理权重为概率
		for(int v=0;v<this.vCount;v++){
			List<Integer> in_id = ins[v];
			Map<Integer, Double> in_weight = getAllEdgeWeight_in(v);
			double tot = this.edges_in_all(v);
			this.in_all_weight.add(tot);	//按照顺序加的
			for (int i = 0; i < this.inDegree(v); i++){
				int local = in_id.get(i);
				ins_pro[local].add( in_weight.get(local) / tot );
//				ins_pro[in_id[i]].add( f[i] / this.edges_in_all(outs.get(i)) );
			}
//			ps[i] = g.edge_out(v).get(outs.get(i)) / g.inDegree(outs.get(i)) + (i == 0 ? 0 : ps[i-1]);
//			ps[i] = 1.0 / g.inDegree(outs.get(i)) + (i == 0 ? 0 : ps[i-1]);
		}
		
	}
	
	public void deal_varience(){
		// 计算每个点，出边权重的方差
		for(int i=0;i<this.vCount;i++){
			double tot = this.edges_out_all(i);
			Map<Integer, Double> in_weight = getAllEdgeWeight_out(i);
			tot /= (double)in_weight.size();
			Set<Integer> ss = edges_out[i].keySet();
			
			double num=0.0;
			for (Integer s:ss) {
				num += ((tot-edges_out[i].get(s))*(tot-edges_out[i].get(s)));
			   // System.out.println(s+","+map.get(s));
			}
			varience.add(Math.exp(-1.0*Math.sqrt(num)));
		}
	}
	
	public void addEdge(int src, int dst){
		outs[src].add(dst);
		ins[dst].add(src);
		this.eCount++;
	}
	
	public void addEdge(int src, int dst, double w){
		outs[src].add(dst);
		ins[dst].add(src);
		
//		System.out.println("~~~~~~~~:" + edges_out.length + " " + src + " " + dst + " " + w);
		edges_out[src].put(dst, w);
		edges_in[dst].put(src, w);
//		System.out.println("~~~~~~~~:" + edges_out.length);
		
		this.eCount++;	
	}
	
	public int outDegree(int v){
		return outs[v].size();
	}
	
	public int inDegree(int v){
		return ins[v].size();
	}
	
	public Map<Integer, Double> getAllEdgeWeight_out(int u){
		return edges_out[u];
	}
	
	public Map<Integer, Double> getAllEdgeWeight_in(int u){
		return edges_in[u];
	}
	

	/**
	 * random walk des a out neighbor.
	 * -1: no neighbor.
	 * @param v
	 * @return
	 */
	public int randOutNeighbor(int v){
		int d = outDegree(v);
		if (d == 0) return -1;
		return outs[v].get(rand.nextInt(d));
	}
	
	/**
	 * random walk to a in neighbor.
	 * -1: no in neighbor.
	 * @param v
	 * @return
	 */
	public int randInNeighbor(int v){
		int d = inDegree(v);
		if (d == 0) return -1;
		// 选择第i个入点，是按照数组下标来选；不是按照真正的id号来选。
		return ins[v].get(rand.nextInt(d));
	}
	
	public List<Integer> outNeighbors(int v){
		return outs[v];
	}
	
	public List<Integer> inNeighbors(int v){
		return ins[v];
	}
	
	// 这个是把weight当做权重来选择
	// 同样分为按照in和按照out来进行选择（之后预处理出来，不要用一次算一次 —— 搭建Giraph的时候再这么搞吧）
	public int randNeighborByWeight_out(int v){
		int d = outDegree(v);
		if (d == 0) return -1;
		double[] weight = new double[d];
		double sum = 0;
		for (int i = 0; i < d; i++){
			weight[i] = outs[v].get(i);
			sum += weight[i];
		}
		for (int i = 0; i < d; i++){
			weight[i] /= sum;
			weight[i] += ( i > 0 ? weight[i-1] : 0);
		}
		double r = Math.random();
		int index = LxcArrays.insertPoint(weight, r);
		return outs[v].get(index);
	}
	
	public double evidence(int v,int m){
		// 两点公共边的个数,之后求得1/2^i的加和
		Set<Integer> s1 = edges_out[v].keySet(); 
		Set<Integer> s2 = edges_out[m].keySet();
		s1.retainAll(s2);
		
		double num=0.5;
		double value = 0.5;
		double cnt = 0.5; 
		for(int i=0;i<s1.size();i++){
			value *= cnt;
			num += (value);
		}
		return num;
	}
	public double get_varience(int id){
		// 获得顶点id处，权重的方差(出边权重！！！)
		return this.varience.get(id);
	}
	
	// 这两个可能有错，回头再仔细看
	// simRank++的时候主要就是改这里了。 为了方便，加一个之前预处理的东西
	// 它的处理我们反向追踪的时候，就正常地把分子/分母（加和不为1）的数值，作为概率好了。别的都一样
	public int randNeighborByWeight_in(int v){
		// 每个顶点处的权重，需要多一个varience(i)
		int d = inDegree(v);
		if (d == 0) return -1;
		double[] weight = new double[d];
		double sum = 0;
		for (int i = 0; i < d; i++){
			weight[i] = ins[v].get(i);
			sum += weight[i];
//			weight[i] = ins[v].get(i) * get_varience(ins[v].get(i));
//			sum += weight[i];
		}
		for (int i = 0; i < d; i++){
			weight[i] /= sum;
			weight[i] += ( i > 0 ? weight[i-1] : 0);
		}
		double r = Math.random();
		int index = LxcArrays.insertPoint(weight, r);
		return ins[v].get(index);
	}
	
	
	/**
	 * get the adjacent edges(with weight)
	 * @param v
	 * @return
	 */
	public Map<Integer, Double> edge_out(int v){
		return edges_out[v];
	}
	public Map<Integer, Double> edge_in(int v){
		return edges_in[v];
	}
	public double edges_in_all(int v){
		double tot = 0.0;
		Set<Integer> ss = edges_in[v].keySet(); 
		for (Integer s:ss) {
			tot += edges_in[v].get(s);
		   // System.out.println(s+","+map.get(s));
		}
		return tot;
	}
	public double edges_out_all(int v){
		double tot = 0.0;
		Set<Integer> ss = edges_out[v].keySet(); 
		for (Integer s:ss) {
			tot += edges_out[v].get(s);
		   // System.out.println(s+","+map.get(s));
		}
		return tot;
	}
	
	public int getVCount() {
		return vCount;
	}
	
	public int getECount() {
		return eCount;
	}
	
	/**
	 * modify the edge weight to the given w.
	 * @param src
	 * @param dst
	 * @param w
	 */
	public void updateWeight(int src, int dst, double w){
		edges_out[src].put(dst, w);
		edges_in[dst].put(src, w);
	}
	
	/**
	 * sum aggregator.
	 * @param src
	 * @param dst
	 * @param w
	 */
	public void updateMaxWeight(int src,int dst, double w){
		double old = edges_out[src].get(dst);
		edges_out[src].put(dst,Math.max(old, w));
		old = edges_in[dst].get(src);
		edges_in[dst].put(src,Math.max(old, w));
	}
	
	/**
	 * add w to the old weight of edge [src, dst].
	 * @param src
	 * @param dst
	 * @param w
	 */
	public void accumulateWeight(int src, int dst, double w){
		edges_out[src].put(dst, edges_out[src].get(dst) + w);
		edges_in[dst].put(src, edges_in[dst].get(src) + w);
	}
	

	public static void main(String[] args) throws IOException {
//		WGraph g = new WGraph(MyConfiguration.biGraphPath,MyConfiguration.totalCount);
//		System.out.println(g.vCount +"\t"+g.eCount);
//		System.out.println(g.degree(1));
	}

}
