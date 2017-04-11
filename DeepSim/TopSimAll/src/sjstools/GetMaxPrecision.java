package sjstools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import lxctools.FixedMaxPQ;
import lxctools.Log;
import lxctools.Pair;
import simrank.TopSim_Dev;
import simrank.TopSim_singleSample;
import structures.Graph;
import utils.Eval;
import utils.Path;
import utils.Print;
import conf.MyConfiguration;
/**
 * 对不同策略下的结果，我们取最高的精度来计算！
 * @author luoxiongcai,Alan
 *
 */

public class GetMaxPrecision {
	public static String precision(String prePath, String prePath1, String prePath2, String outPath, int K) throws IOException{
		// simRank Top10 与 xxxxWalk TopK相比较 —— K可取不同值
		BufferedWriter out = new BufferedWriter(new FileWriter(outPath));
		BufferedReader input = new BufferedReader(new FileReader(prePath));
		BufferedReader input1 = new BufferedReader(new FileReader(prePath1));
		BufferedReader input2 = new BufferedReader(new FileReader(prePath2));
		String line1 = null, line2 = null, line = null;
		double sum = 0;
		int total = 0;
		while ((line1 = input1.readLine()) != null){
			line2 = input2.readLine();
			line = input.readLine();
			String[] tokens = line.split(MyConfiguration.SEPARATOR);
			String[] tokens1 = line1.split(MyConfiguration.SEPARATOR);
			String[] tokens2 = line2.split(MyConfiguration.SEPARATOR);

			double pre = -1;
			int flag=1;
			if (!tokens1[0].equals(tokens[0])){
				System.out.println("error !" + tokens1[0] +"\t" + tokens[0]);
				break;	//如果不对应，直接就是出错了！
			}else{
				pre = (Math.max(Double.valueOf(tokens1[1]),Double.valueOf(tokens2[1])));
				pre = Math.max(pre,Double.valueOf(tokens[1]));
//				if(Double.valueOf(tokens[1]) > Double.valueOf(tokens1[1])){
//					pre = Double.valueOf(tokens[1]);
//					flag=1;
//				}else{
//					pre = Double.valueOf(tokens1[1]);
//					flag=2;
//				}
//				sum += pre;
//				total++;
//				out.append(tokens1[0]+MyConfiguration.SEPARATOR + pre +MyConfiguration.SEPARATOR+flag + "\n");
				
				pre = (Math.max(Double.valueOf(tokens1[1]),Double.valueOf(tokens2[1])));
				pre = Math.max(pre,Double.valueOf(tokens[1]));
				if(Double.valueOf(tokens[1]) == Double.valueOf(tokens1[1]) && Double.valueOf(tokens[1]) == Double.valueOf(tokens2[1])){
					pre = Double.valueOf(tokens[1]);
					flag=0;
				}else if(Double.valueOf(tokens[1]) >= Double.valueOf(tokens1[1]) && Double.valueOf(tokens[1]) >= Double.valueOf(tokens2[1])){
					pre = Double.valueOf(tokens[1]);
					flag=1;
				}else if(Double.valueOf(tokens1[1]) >= Double.valueOf(tokens[1]) && Double.valueOf(tokens1[1]) >= Double.valueOf(tokens2[1])){
					pre = Double.valueOf(tokens1[1]);
					flag=2;
				}else if(Double.valueOf(tokens2[1]) >= Double.valueOf(tokens[1]) && Double.valueOf(tokens2[1]) >= Double.valueOf(tokens1[1])){
					pre = Double.valueOf(tokens2[1]);
					flag=3;
				}
				sum += pre;
				total++;
				out.append(tokens1[0]+MyConfiguration.SEPARATOR + pre +MyConfiguration.SEPARATOR+flag + "\n");
			}
			
		}
		out.close();
		System.out.println("total nodes:"+total+"\tavg precision: " + sum / total +"\t");
		return (sum/total) + "";
	}
	

	public static void main(String[] args) throws IOException {
		int[] testTopK = MyConfiguration.testTopK;
		int fileNum = MyConfiguration.fileNum;
//		int fileNum = 2;
		for(int i=0;i<fileNum;i++){
			// 基本的输出路径
			String basePath = MyConfiguration.out_u_u_graphPath_topSimSingle[i];
			String basePathDev = MyConfiguration.out_u_u_graphPath_topSimDev[i];
			String logPath = basePathDev + "_topSimMax_Test.log";	// 日志写在Dev里面吧
			
			int[] samples = {10000};
			int[] steps = {3};
			
			Log log = new Log(logPath);
			log.info("################## Test_u_u_Top" + MyConfiguration.TOPK);
			
			for(int step: steps){
				for (int sample_ : samples){	// 这里step加上之后，路径还要进一步调整
					int sample =sample_;	// 与DoubleWalk的Sample路径个数一致
					// 第一步按照TopSimSingle的思路找候选集
					for(int k:testTopK){
						System.out.println("第" + i + "个文件  Step:" + step + " Sample:"+sample + "TopK:" +k);
						log.info("Test Step:" + step + " Sample:"+sample + "TopK:" +k);
						String outPath = basePathDev + "_topSimDev_top" + 10 + "_step" + step + "_sample" + sample + "_MaxOfThree" + ".txt";
						String prePath = basePath + "_topSimSingle_top" + 10 + "_step" + step + "_sample" + sample + "precision.txt";
						String prePath1 = basePathDev + "_topSimDev_top" + 30 + "_step" + step + "_sample" + sample + "_singleStep" + 1 + "_precision.txt";
						String prePath2 = basePathDev + "_topSimDev_top" + 30 + "_step" + step + "_sample" + sample + "_singleStep" + 2 + "_precision.txt";
						// 下面精度计算的也一样，和DoubleWalk用的是同一个
					
						log.info("Basic TopSimDevRamdomWalk Top" + MyConfiguration.TOPK + " step" + step + " sample" + sample + " precision: " + precision(prePath, prePath1, prePath2, outPath, MyConfiguration.TOPK));
					}
				}
			}
			log.close();
		}
		System.out.println("下一步：看最高的精度有多少提升.");
	}

}
