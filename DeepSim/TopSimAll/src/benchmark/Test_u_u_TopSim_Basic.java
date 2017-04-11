package benchmark;

import java.io.IOException;

import lxctools.Log;
import lxctools.StopWatch;
import simrank.TopSim_Basic;
import structures.Graph;
import utils.Eval;
import utils.Print;
import conf.MyConfiguration;

/**
 * 基本的TopSim方法，枚举每条路径。
 * test samples nums for topSim precision.
 * input data: 1K.
 * gold standard: 30 iterations.
 * @author luoxiongcai, Alan
 *
 */
public class Test_u_u_TopSim_Basic {
	
	public static void main(String[] args) throws IOException {
		int f[] = {12,6,4,8,12,1,2,4,6,8,10,12,14,16,18,20,30};
		int[] testTopK = MyConfiguration.testTopK;
//		int f[] = {1};
		int file_num = 1;
//		int file_num = 5;
		for(int i=0;i<file_num;i++){
			String goldPath = MyConfiguration.out_u_u_graphPath_simrank + f[i]*1000 + "simrank_navie_top" + MyConfiguration.TOPK +".txt";
			String basePath = MyConfiguration.out_u_u_graphPath_single_dev;
			String logPath = MyConfiguration.out_u_u_graphPath_single_dev + f[i]*1000 + "single_top" + MyConfiguration.TOPK + "_Test_Sample.log";
			
//			double[] samples = {0.01,0.05,0.1,0.15,0.2,0.4,1,10,20,40,60,80,100,200,1000};
//			double[] samples = {60};
//			double[] samples = {60,80,100,200,1000};
//			double[] samples = {1000,4000};
			double[] samples = {0.1};
//			double[] samples = {4000,10000};
//			double[] samples = {100,400,1000,5000,10000};
//			double[] samples = {20,40,60,80,100};
//			int[] steps = {1,2,3};		// 测试用，先做Step=1的，之后Step=3的
			int[] steps = {2};	
			
			Log log = new Log(logPath);
			log.info("################## Test_u_u_Top" + MyConfiguration.TOPK);
			Graph g = new Graph(MyConfiguration.in_u_u_graphPath + f[i]*1000 + ".txt", MyConfiguration.u_u_count);

			for(int step: steps){
				for (double sample_ : samples){	// 这里step加上之后，路径还要进一步调整
					for(int k:testTopK){
						
						int sample = (int)(sample_*MyConfiguration.u_u_count);	// 与DoubleWalk的Sample路径个数一致
						System.out.println("f[i]:" + f[i] + "   sample: "+sample + "   step:" + step + "   testTopK:" + k);
						
						log.info("singleRandomWalk Test_Step:" + step + "_Sample: "+sample);
						TopSim_Basic srw = new TopSim_Basic(g,sample,step);
						srw.compute();
						log.info("singleRandomWalk computation done!");
						String outPath = basePath + f[i]*1000 + "single_top" + MyConfiguration.TOPK + "_step" + step + "_sample" + sample + ".txt"; 
						log.info("u_u_graph singleRandomWalk output done!");
						String prePath = basePath + f[i]*1000 + "single_top" + MyConfiguration.TOPK + "_step" + step + "_sample" + sample + "precision_.txt";
						// 计算精度，传入sim[][]数组,计算前k个相似的点输出到文件
						Print.printByOrder(srw.getResult(), outPath, MyConfiguration.TOPK, k);
						// 下面精度计算的也一样，和DoubleWalk用的是同一个
					
						log.info("Basic SingleRamdomWalk_" + f[i]*1000 + "_" + MyConfiguration.TOPK + "_ste_" + step + "_sam_" + sample + "_precision: " + Eval.precision(goldPath, outPath, prePath,k));
					}
				}
			}
			log.close();
		}
		
	}

}


