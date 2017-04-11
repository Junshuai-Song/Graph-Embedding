package benchmark;

import java.io.IOException;

import lxctools.Log;
import lxctools.StopWatch;
import simrank.TopSim_doubleSample;
import structures.Graph;
import utils.Eval;
import utils.Print;
import conf.MyConfiguration;

/**
 * 按照UniWalk进行向TopSim改进的方法，我们要基于这个方法来进行处理
 * test samples nums for topSim precision.
 * input data: 1K.
 * gold standard: 30 iterations.
 * @author luoxiongcai, Alan
 *
 */

public class Test_u_u_TopSim_doubleSample {
	
	public static void main(String[] args) throws IOException {
		int[] testTopK = MyConfiguration.testTopK;
//		int fileNum = MyConfiguration.fileNum;
		int fileNum = 3;
		for(int i=2;i<fileNum;i++){
			// 基本的输入路径，都一样
			String graphInPath = MyConfiguration.in_u_u_graphPath[i];
			String goldPath = MyConfiguration.out_u_u_graphPath_simrank[i] + "_simrank_navie_top" + MyConfiguration.TOPK +".txt";
			
			// 基本的输出路径
			String basePath = MyConfiguration.out_u_u_graphPath_topSimDouble[i];
			String logPath = basePath + "_topSimSingle_Test.log";
			
//			int[] samples = {5,10,50,100,200,400};	// 每个点Sample数量除以100
			int[] samples = {5,10,50};	// 每个点Sample数量除以100
//			int[] steps = {1,2,3};		// 测试用，先做Step=1的，之后Step=3的
			int[] steps = {1};
			
			Log log = new Log(logPath);
			log.info("################## Test_u_u_Top" + MyConfiguration.TOPK);
			Graph g = new Graph(graphInPath, MyConfiguration.u_u_count[i]);

			for(int step: steps){
				for (int sample_ : samples){	// 这里step加上之后，路径还要进一步调整
					int sample =sample_;	// 与DoubleWalk的Sample路径个数一致
					TopSim_doubleSample srw = new TopSim_doubleSample(g,sample,step);
					srw.compute();
					for(int k:testTopK){
						System.out.println("第" + i + "个文件  Step:" + step + " Sample:"+sample + "TopK:" +k);
						log.info("Test Step:" + step + " Sample:"+sample + "TopK:" +k);
						log.info("computation done!");
						String outPath = basePath + "_topSimDouble_top" + k + "_step" + step + "_sample" + sample + ".txt"; 
						log.info("u_u_graph singleRandomWalk output done!");
						String prePath = basePath + "_topSimDouble_top" + k + "_step" + step + "_sample" + sample + "precision.txt";
						// 计算精度，传入sim[][]数组,计算前k个相似的点输出到文件
						Print.printByOrder(srw.getResult(), outPath, MyConfiguration.TOPK, k);
						// 下面精度计算的也一样，和DoubleWalk用的是同一个
					
						log.info("Basic TopSimDoubleRamdomWalk Top" + MyConfiguration.TOPK + " step" + step + " sample" + sample + " precision: " + Eval.precision(goldPath, outPath, prePath,k));
					}
				}
			}
			log.close();
		}
		
	}

}


