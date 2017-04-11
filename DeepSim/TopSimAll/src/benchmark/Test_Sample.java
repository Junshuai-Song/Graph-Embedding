package benchmark;

import java.io.IOException;

import lxctools.Log;
import lxctools.StopWatch;
import simrank.SingleRandomWalk;
import simrank.SingleRandomWalkReuse;
import simrank.SingleRandomWalk_M;
import structures.Graph;
import utils.Eval;
import utils.Print;
import conf.MyConfiguration;

/**
 * test samples nums for precision.
 * input data: 10k_5.
 * gold standard: 20 iterations.
 * @author luoxiongcai
 *
 */
public class Test_Sample {
	public static final String goldPath = MyConfiguration.basePath + "/Test_Real/simrank_moreno_crime_crime.txt.sim.txt";
	public static final String basePath = MyConfiguration.basePath + "/Test_Sample";
	public static final String logPath = MyConfiguration.basePath + "/Test_Sample/Test_Samle.log";
	
	public static void main(String[] args) throws IOException {
		Log log = new Log(logPath);
		log.info("################## moreno_crime_crime.txt graph! Test_SAMPLE");
		Graph g = new Graph(MyConfiguration.biGraphPath, MyConfiguration.totalCount);
		int[] samples = {2500,5000,10000,20000,40000};
		for (int sample : samples){
		System.out.println("sample: "+sample);
			
			// Basic MSS-RW
			log.info("power Basic MSS-RW Test_Sample: "+sample);
			SingleRandomWalk srw = new SingleRandomWalk(g,sample);
			srw.compute();
			log.info("computation done!");
			
			String outPath = basePath+"/power_singleWalk_topk_Basic_"+sample+".txt";
			Print.printByOrder(srw.getResult(), outPath, MyConfiguration.TOPK);
			log.info("output similarities done!");
			
			String prePath = MyConfiguration.basePath + "/precision.txt";
			double p = Eval.precision_simFile(goldPath, outPath+".sim.txt", prePath);
			log.info("Basic MSS-RW precision: " + p);
			
			// M + APP  MSS-RW
//			log.info("App_M = 4 MSS-RW Test_Sample: "+sample);
//			SingleRandomWalk_M srw_a_m = new SingleRandomWalk_M(g,M, sample);
//			srw_a_m.compute();
//			log.info("computation done!");
//			
//			String outPath_a_m = basePath + "/singleWalk_topk_app_M"+sample+".txt";
//			Print.printByOrder(srw_a_m.getResult(), outPath_a_m, MyConfiguration.TOPK);
//			log.info("output similarities done!");
//			
//			double p_a = Eval.precision_simFile(goldPath, outPath_a_m+".sim.txt", prePath);
//			log.info("App_M MSS-RW precision: " + p_a);
		
			//M + App + reuse: MSS-RW
//			log.info("power Reuse path times = 4, App_M = 3 MSS-RW Test_Sample: "+sample);
//			SingleRandomWalkReuse srw_a_m_r = new SingleRandomWalkReuse(g,  sample);
//			srw_a_m_r.compute();
//			log.info("computation done!");
//			
//			String outPath_a_m_r = MyConfiguration.basePath + "/Test_Sample/power_singleWalk_topk_app_M_reuse"+sample+".txt";
//			Print.printByOrder(srw_a_m_r.getResult(), outPath_a_m_r, MyConfiguration.TOPK);
//			log.info("output similarities done!");
//			
//			double p_a_r = Eval.precision_simFile(goldPath, outPath_a_m_r+".sim.txt", prePath);
//			log.info("Reuse App_M MSS-RW precision: " + p_a_r);
			
		}
		log.close();
	}

}
