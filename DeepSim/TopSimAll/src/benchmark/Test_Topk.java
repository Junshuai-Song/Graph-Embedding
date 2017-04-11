package benchmark;

import java.io.IOException;

import lxctools.Log;
import simrank.SingleRandomWalk;
import simrank.SingleRandomWalkReuse;
import simrank.SingleRandomWalk_M;
import structures.Graph;
import utils.Eval;
import utils.Print;
import conf.MyConfiguration;

/**
 * test precision with various topk.
 * @author luoxc
 *
 */
public class Test_Topk {
	public static final String goldPath = MyConfiguration.basePath + "/simrank_topk_10k_5.txt";
	public static final String basePath = MyConfiguration.basePath + "/Test_Topk";
	public static final String logPath = MyConfiguration.basePath + "/Test_Topk/Test_Topk.log";
	public static final int M = 4;
	public static final int sample = 10000;
	public static void main(String[] args) throws IOException {
		int MAXK = 80;
		int[] topks = {10,20,40,80};
		
		// simrank top-80; 
		// don't forget.
		
		//test for basic MSS-RW; M+App MSS-RW; Reuse +M +App MSS-RW;
		Log log = new Log(logPath);
		log.info("################## Test_Topk, 10k_5");
		Graph g = new Graph(MyConfiguration.biGraphPath, MyConfiguration.totalCount);
		for (int topk : topks){
			MyConfiguration.TOPK = topk;
			
			// basic MSS-RW
			log.info("Basic MSS-RW Test_Topk: "+topk);
			SingleRandomWalk srw = new SingleRandomWalk(g,sample);
			srw.compute();
			log.info("computation done!");
			
			String outPath = basePath+"/singleWalk_topk_Basic_"+topk+".txt";
			Print.printByOrder(srw.getResult(), outPath, MyConfiguration.TOPK);
			log.info("output similarities done!");
			
			String prePath = MyConfiguration.basePath + "/precision.txt";
			double p = Eval.precision(goldPath, outPath, prePath);
			log.info("Basic MSS-RW precision: " + p);
			
			// M + APP  MSS-RW
			log.info("App_M = 4 MSS-RW Test_Topk: "+topk);
			SingleRandomWalk_M srw_a_m = new SingleRandomWalk_M(g,M, sample);
			srw_a_m.compute();
			log.info("computation done!");
			
			String outPath_a_m = basePath+"/singleWalk_topk_app_M"+topk+".txt";
			Print.printByOrder(srw_a_m.getResult(), outPath_a_m, MyConfiguration.TOPK);
			log.info("output similarities done!");
			
			double p_a = Eval.precision(goldPath, outPath_a_m, prePath);
			log.info("App_M MSS-RW precision: " + p_a);
			
			//M + App + reuse: MSS-RW
			log.info("Reuse path times = 4, App_M = 4 MSS-RW Test_Topk: "+topk);
			SingleRandomWalkReuse srw_a_m_r = new SingleRandomWalkReuse(g,  sample);
			srw_a_m_r.compute();
			log.info("computation done!");
			
			String outPath_a_m_r = basePath + "/singleWalk_topk_app_M_reuse"+topk+".txt";
			Print.printByOrder(srw_a_m_r.getResult(), outPath_a_m_r, MyConfiguration.TOPK);
			log.info("output similarities done!");
			
			double p_a_m_r = Eval.precision(goldPath, outPath_a_m_r, prePath);
			log.info("Reuse App_M MSS-RW precision: " + p_a_m_r);
		}
		
		
		
	}

}
