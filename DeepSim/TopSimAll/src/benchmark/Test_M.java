package benchmark;

import java.io.IOException;

import lxctools.Log;

import simrank.SingleRandomWalk_M;
import structures.Graph;
import utils.Eval;
import utils.Print;
import conf.MyConfiguration;

/**
 * test M. the storage of similarities in each vertex is k * M
 * input data : basePath +"/biGraph_100m_5.txt"
 * golden standard simrank: 20 iterations
 * @author luoxiongcai
 *
 */
public class Test_M {
	public static final String goldPath = MyConfiguration.basePath + "/realdata/power_simrank_topk_15k_5.txt.sim.txt";
	public static final String basePath = MyConfiguration.basePath + "/Test_M";
	public static final String logPath = MyConfiguration.basePath + "/Test_M/Test_M.log";
	public static final int SAMPLE = 10000;
	public static void main(String[] args) throws IOException {
		Log log = new Log(logPath);
		log.info("################## power law graph !Test_M+ app: 15k_5");
		Graph g = new Graph(MyConfiguration.biGraphPath, MyConfiguration.totalCount);
		int[] Ms = {1,2,3,4,5,6};
		
		for (int M : Ms){
			System.out.println("Test_M: " +M);
			log.info("power Test_M: "+ M);
			SingleRandomWalk_M srw = new SingleRandomWalk_M(g,M, SAMPLE);
			srw.compute();
			log.info("computation done!");
			
			String outPath = MyConfiguration.basePath + "/Test_M/power_singleWalk_topk_approxM_"+M+".txt";
			Print.printByOrder(srw.getResult(), outPath, MyConfiguration.TOPK);
			log.info("output similarities done!");
			
			String prePath = MyConfiguration.basePath + "/precision.txt";
			double p = Eval.precision_simFile(goldPath, outPath+".sim.txt", prePath);
			log.info("precision: " + p);
		}
		log.close();
	}

}
