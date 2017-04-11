package benchmark;

import java.io.IOException;

import lxctools.Log;
import simrank.SimRank;
import simrank.SingleRandomWalk;
import simrank.SingleRandomWalkReuse;
import structures.Graph;
import utils.Eval;
import utils.Print;
import conf.MyConfiguration;
/**
 * OutOfMemoryError
 * @author luoxc
 *
 */
public class Test_Real {
	
	public static void main(String[] args) throws IOException {
		int topk = 20;
		int SAMPLE = 200000;
		String goldPathBath = MyConfiguration.basePath+"/Test_Real/simrank_";
		String suffix = ".sim.txt";
		String base = MyConfiguration.realdata;   // folder of reald dataset.
		String testBase = MyConfiguration.basePath+"/Test_Real";
//		String[] datas = {"moreno_crime_crime.txt","movielens_tag_movie.txt","movielens_user_tag.txt"};
//		int[] sizes = {1380,24129, 20537};
		
		String[] datas = {"movielens_user_tag.txt"};
		int[] sizes = {20537};
		String prePath = MyConfiguration.basePath + "/precision.txt";
		Log log= new Log(testBase+"/realdata_precision.log");
		log.info("COMPUTING FOR REAL DATASET! SAMPLE = " + SAMPLE );
		for (int i = 0; i < datas.length; i++){
			MyConfiguration.biGraphPath = base +"/"+datas[i];
			MyConfiguration.totalCount = sizes[i];
			MyConfiguration.TOPK = topk;
			System.out.println(datas[i]+" size: "+sizes[i]+" ...");
			log.info(datas[i]+" size: "+sizes[i]+" ...");
			Graph g = new Graph(MyConfiguration.biGraphPath,MyConfiguration.totalCount);
			
//			String outPath = testBase+"/simrank_"+datas[i];
//			SimRank sr = new SimRank(g);
//			sr.compute();
//			log.info("computing done!");
//			Print.printByOrder(sr.getResult(), outPath, MyConfiguration.TOPK );
//			System.out.println(datas[i]+" : simrank output and computing done!");
//			log.info(datas[i]+"simrank output and computing done!");
//			sr = null;
			
			String outPath_reuse = testBase + "/reuse_"+datas[i];
			SingleRandomWalk srwo = new SingleRandomWalk(g, SAMPLE);
			srwo.compute();
			log.info("computing done!");
			Print.printByOrder(srwo.getResult(), outPath_reuse, MyConfiguration.TOPK);
			System.out.println(datas[i]+" : singlerandomwalk output and computing done!");
			log.info(datas[i]+"singlerandomwalk output and computing done!");
			srwo = null;
			g = null;
			
			//eval
			double p = Eval.precision_simFile(goldPathBath+datas[i]+suffix, outPath_reuse+suffix, prePath);
			System.out.println(datas[i] +" precision: " + p);
			log.info(datas[i] +" precision: " + p);
			log.flush();
		}
		log.close();
	}

}
