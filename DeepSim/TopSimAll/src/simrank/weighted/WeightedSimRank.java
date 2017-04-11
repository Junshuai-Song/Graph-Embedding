package simrank.weighted;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import lxctools.StopWatch;

import conf.MyConfiguration;
import structures.Graph;
import structures.WGraph;
import utils.Print;

/**
 * naive simrank
 * @author luoxiongcai
 *
 */
public class WeightedSimRank {
	private int STEP = 50;	
	private int COUNT;
	private WGraph wg;
	private double[][]sim, tempSim;
	
	public WeightedSimRank(WGraph wg){
		this.wg = wg;
		this.COUNT = wg.getVCount();
		    
		sim = new double[COUNT][COUNT];
		tempSim = new double[COUNT][COUNT];
		for (int i = 0; i < COUNT; i++){
			sim[i][i] = 1.0;
			tempSim[i][i] = 1.0;
		}
	}
	
	/**
	 * the main logic 
	 */
	public void compute(){
		int r = 0;
		while (r++ < STEP){
			for (int i = 0; i < COUNT; i++){
				for (int j = i+1; j < COUNT; j++){
					tempSim[i][j] = sim(i, j);
					tempSim[j][i] = tempSim[i][j];
				}
			}
			// copy ;
			for (int i = 0; i < COUNT; i++){
				for (int j = 0; j < COUNT; j++){
					sim[i][j] = tempSim[i][j];
					sim[j][i] = tempSim[j][i];
				}
			}
		}
		postProcess();
	}
	
	/**
	 * set sim(i,i) = 0
	 */
	private void postProcess(){
		for (int i = 0; i < COUNT; i++)
			sim[i][i] = 0;
	}
	
	public double sim(int v, int w){
		if (v == w) return 1;
		Map<Integer,Double> v_neis = wg.getAllEdgeWeight(v);
		Map<Integer,Double> w_neis = wg.getAllEdgeWeight(w);
		double totalWeight1 = 0;
		double totalWeight2 = 0;
		for (Entry<Integer,Double> entry : v_neis.entrySet()){
			totalWeight1 += entry.getValue();
		}
		for (Entry<Integer,Double> entry : w_neis.entrySet()){
			totalWeight2 += entry.getValue();
		}
		double result = 0;
		if (wg.degree(v) == 0 || wg.degree(w) == 0) return 0;

		
		for (int vn : wg.neighbors(v)){
			double w1 = v_neis.get(vn);
			for (int wn : wg.neighbors(w)){
				double w2 = w_neis.get(wn);
				result += w1*w2*sim[vn][wn];
			
			}
		}
		return MyConfiguration.C * result/(totalWeight1*totalWeight2) ;
	}
	
	public double[][] getResult(){
		return sim;
	}
	
	
	
	public static void main(String[] args) throws IOException {
		StopWatch.start();
		StopWatch.say("####### SimRank weighted#########");
		WGraph wg = new WGraph(MyConfiguration.biGraphPath,MyConfiguration.totalCount);
//		Graph g = new Graph(MyConfiguration.biGraphPath,MyConfiguration.totalCount);
//		Graph g = new Graph(Configuration.u_graphPath,Configuration.u_count);

		StopWatch.say("bigraph construction done! v: " + wg.getVCount());
		
		String outPath = MyConfiguration.basePath+"/weighted_simrank_topk_small_test.txt";
		WeightedSimRank sr = new WeightedSimRank(wg);
		sr.compute();
		
		StopWatch.say("bigraph simrank computation done!");
		
		Print.printByOrder(sr.getResult(), outPath, MyConfiguration.TOPK );

		
		StopWatch.say("bigraph simrank result print done!");
		
	}

}
