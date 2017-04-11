package simrank;

import java.io.IOException;
import lxctools.StopWatch;

import conf.MyConfiguration;
import structures.Graph;
import utils.Print;

/**
 * reuse path and no truncate M and no check for "first-meet" constraint.
 * @author luoxiongcai
 * 
 */
public class SingleRandomWalkOptimal2 extends SingleRandomWalk {
	private int[] sampleCount ;
	private int batchSize = 2500;
	private int times = 3;
	public SingleRandomWalkOptimal2(Graph g, int sample) {
		super(g,sample);
		sampleCount = new int[g.getVCount()];
	}
	
	private int checkFinished(){
		int c = 0;
		for (int sc : sampleCount){
			if (sc >= SingleRandomWalk.SAMPLE)
				c++;
		}
		return c;
	}
	
	@Override
	public void compute(){
		for (int i = 0; i < COUNT; i++){
			if ( i % batchSize == 0){
				System.out.println("stage "+(i / batchSize)+"\t"+checkFinished());
			}
			walk(i, times * STEP, sampleCount[i]);
			sampleCount[i] = SAMPLE;
			sim[i][i] = 0;
			
		}
		
		System.out.println("stage "+(g.getVCount() / batchSize)+"\t"+checkFinished());
	}
	
	@Override
	public void computePathSim(int[] path, int pathLen){
		if (pathLen == 0) return;
		for (int off = 0; off <= (times - 2)*STEP; off++){
			int source = path[off];
			if (sampleCount[source] >= SAMPLE) continue;
			sampleCount[source]++;
			for (int i = 1 ; i <=  STEP && off + 2 * i <= pathLen; i++){
				int interNode = path[off + i];
				int target = path[off + 2*i];
				if (isFirstMeet(path, off, off+2*i))
					sim[source][target] +=  cache[i]* g.degree(interNode)/ g.degree(target) / SAMPLE;
			}
		}
		

	}
	
	
	
	public static void main(String[] args) throws IOException  {
		StopWatch.start();
		Graph g  = new Graph(MyConfiguration.biGraphPath,MyConfiguration.totalCount);
	
		SingleRandomWalkOptimal2 srwo2 = new SingleRandomWalkOptimal2(g,10000);
		srwo2.compute();
		StopWatch.say("SingleRandomWalkOptimal2 computation done!");
		
		Print.printByOrder(srwo2.getResult(), MyConfiguration.basePath+"/optSingleWalk_topk_2.txt", MyConfiguration.TOPK);
	}

}
