package giraph;

import org.apache.giraph.edge.Edge;
import org.apache.giraph.graph.Vertex;
import org.apache.giraph.io.formats.GiraphFileInputFormat;
import org.apache.giraph.io.formats.IntNullReverseTextEdgeInputFormat;
import org.apache.giraph.job.GiraphJob;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import utils.EJob;

import giraph.ioformat.DefaultOutputFormat;
import giraph.writables.CombineMixMsgWritable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import lxctools.FixedCacheMap;
import lxctools.Pair;

/**
 * 
 * @author luoxiongcai
 *
 */
public class CombineBatchSingleWalkVertexReuse extends
		Vertex<IntWritable, NullWritable, NullWritable, CombineMixMsgWritable> {
	public static final String countFlag = "10m_5.txt";
	public static final int V = 10000000;
	public static final int stopV = 100000;
	public static final int machineCount = 14;
	public static final int BATCH_SIZE = 40000;
	public static final String SEPARATOR_KV = ":";

	public static final float C = 0.6f;
	public static final int SAMPLE = 10000;
	public static final int STEP = 5; // the max length of a double path.
	public static final int TIMES = 4;
	public static final int CYCLE = STEP * TIMES + 2;
	public static final int pathLen = STEP*2 + 1;   // normally, shouldn't be larget than 127.
	public static final int targetLen = STEP;
	public static final int TOPK = 20;
	public static final int M = 4;
	public static final int MAX_MSG_SLOT = STEP;
	public static final IntWritable MINUS_ONE = new IntWritable(-1); // indicate null
	public Random rand = new Random(); 
	private static final String hdfsPathBase = "hdfs://changping11:9000/user/luoxiongcai/simrank/output/v_"
			+ countFlag + "_batch_combine_reuse";
	public static Map<Integer, FSDataOutputStream> pid2OutStream = new HashMap<Integer, FSDataOutputStream>(); 
//	private FixedCacheMap results = new FixedCacheMap(M * TOPK);
	private FixedCacheMap results = null;
	private int sampleCount = 0;
	private static final Configuration conf = new Configuration();
	
	private void flushTest() throws IOException {
		// hdfs api;
		FileSystem fs = FileSystem.get(conf);
		int pid = this.getWorkerContext().getPartitionId(getId());
		FSDataOutputStream out = pid2OutStream.get(pid);
		Path simFile = new Path(hdfsPathBase + "/part-" + pid);
		fs.setReplication(simFile, (byte)1);
		
		if ( out == null){
			out = fs.create(simFile, false, 8192); 
			pid2OutStream.put(pid, out);
		}
		
		StringBuilder sb = new StringBuilder(getId().toString());
		int i = 0;
		int size = results.size();
		
		for (Pair<Integer, Float> simPair : results) {
			if (i < size - TOPK){
				i++;
				continue;
			}

			sb.append("\t" + simPair.getKey() + SEPARATOR_KV
					+ String.format("%.6f", simPair.getValue() / this.sampleCount));
		}
		out.writeChars(sb.toString()+"\r\n");
		out.flush();
	}


	private IntWritable randNeighbor() {
		RandOutEdges<IntWritable, NullWritable> edges = (RandOutEdges<IntWritable, NullWritable>) this.getEdges();
		Edge<IntWritable, NullWritable> next = edges.randEdge();
		if (next == null)
			return MINUS_ONE;
		else
			return next.getTargetVertexId();
	}
	
	private void incrementSims(CombineMixMsgWritable msg){
		if (results == null){
			results = new FixedCacheMap(M * TOPK);
		}
		int[] targets = msg.getTargets();
		float[] sims = msg.getSims();
		short pathCount = msg.getPathCount();
		for (int i = 0; i < targetLen; i++){
			if (targets[i] != this.getId().get())
				results.put(targets[i], sims[i] * pathCount);
		}
		sampleCount += pathCount;
	}
	
	private void computeAndSendSim(CombineMixMsgWritable msg){
		byte srcp = msg.getSrcp();
		int[] path = msg.getPath();
		short[] degrees = msg.getDegrees();
		short pathCount = msg.getPathCount();
		
		int[] targets = new int[targetLen];
		float[] sims = new float[targetLen];
		for (int i = 1; i <= targetLen; i++ ){
			targets[i - 1] = path[(srcp + i * 2) % pathLen];
			sims[i - 1] = (float) (Math.pow(C, i) * degrees[(srcp + i) % pathLen] / degrees[(srcp + i * 2) % pathLen]);
		}
		CombineMixMsgWritable simMsg = new CombineMixMsgWritable((byte) 2 , (byte)targetLen, (short)pathCount);
		simMsg.setTargets(targets);
		simMsg.setSims(sims);
		sendMessage(new IntWritable(path[srcp]),simMsg);
	}
	
	// send msg to neighbours.
	private void mySendMsg(CombineMixMsgWritable msg){
		short pathCount = msg.getPathCount();
		if (pathCount == 1){
			this.sendMessage(randNeighbor(), msg);
			return;
		}
		int edgeNum = this.getNumEdges();
		int avg = pathCount / edgeNum;
		int remain = pathCount - avg * edgeNum;
		if (avg > 0){
			for (Edge<IntWritable, NullWritable> e : this.getEdges()){
				CombineMixMsgWritable toMsg = new CombineMixMsgWritable(msg);
				toMsg.setPathCount((short)avg);
				sendMessage(new IntWritable(e.getTargetVertexId().get()), toMsg);
			}	
		}
		
		for (int i = 0; i < remain; i++){
			CombineMixMsgWritable toMsg = new CombineMixMsgWritable(msg);
			toMsg.setPathCount((short)1);
			sendMessage(randNeighbor(), toMsg);
		}
	}

	@Override
	public void compute(Iterable<CombineMixMsgWritable> messages) throws IOException {
		if (getNumEdges() == 0){// single point.
			voteToHalt(); 
			return;
		}
		int currentId = this.getId().get();
		
		IntWritable low = this.getAggregatedValue("VID_LOWER");
		IntWritable up = this.getAggregatedValue("VID_UPPER");
		int beginV = low.get();
		int endV = up.get();
		if (this.getSuperstep() >= Math.ceil(stopV * 1.0 / BATCH_SIZE) * CYCLE){
			voteToHalt(); 
			return;
		}
			
		int step = (int) this.getSuperstep() % CYCLE;
		if (step == CYCLE - 1) {
			// deal with possible similarity increments or sampleCount increment.
			for (CombineMixMsgWritable msg : messages) {
				byte flag = msg.getFlag();
				if (flag == 2){
					incrementSims(msg);
				} 
			}
			// flush if possible.
			if ((currentId >= beginV) && (currentId <= endV)) {
				flushTest();
				results = null; //free memory
				voteToHalt();
			}
		} else if (step == 0) {
			if (currentId < beginV ){ // dealt vertex.
				voteToHalt();
				return;
			}
			
			// generate random walks!
			if ((currentId >= beginV) && (currentId <= endV) && sampleCount < SAMPLE){ // be careful about the range.
				int edgeNum = this.getNumEdges();
				int needSample = SAMPLE - sampleCount;
				int avg = needSample  / edgeNum;
				int remain = needSample - avg * edgeNum;
				
				if (avg > 0){
					for (Edge<IntWritable, NullWritable> e : this.getEdges()){
						CombineMixMsgWritable msg = new CombineMixMsgWritable((byte)1, (byte)pathLen, (short)avg);
						msg.setDirectPath(0, currentId,(byte) this.getNumEdges());
						sendMessage(e.getTargetVertexId(), msg);
					}
				}

				for (int i = 0; i < remain; i++){
					CombineMixMsgWritable msg = new CombineMixMsgWritable((byte)1, (byte)pathLen, (short)1);
					msg.setDirectPath(0, currentId,(byte) this.getNumEdges());
					sendMessage(randNeighbor(), msg);
				}
			}
			
		} else if (step <= 2 * STEP){ // just random walk.
			Iterator<CombineMixMsgWritable> msgs = messages.iterator();
			while (msgs.hasNext()) {
				CombineMixMsgWritable msg = msgs.next();
				msg.setDirectPath(step, currentId, (byte)getNumEdges());
				mySendMsg(msg);
				
				if (step == 2 *STEP && msg.getSource() >= beginV){ // be careful here, msgs to small ids are unnecessary.s
																   // since the results have been set null.
					computeAndSendSim(msg);
				}
			}
		} else {  // random walk and compute sim.
			Iterator<CombineMixMsgWritable> msgs = messages.iterator();
			while (msgs.hasNext()) {
				CombineMixMsgWritable msg = msgs.next();
				// deal with possible similarity increments.
				byte flag = msg.getFlag();
				if (flag == 2){
					incrementSims(msg);
				} else if (flag == 1){
					msg.appendCircularPath(currentId, (byte)this.getNumEdges());
					mySendMsg(msg);
					if (msg.getSource() >= beginV){
						computeAndSendSim(msg);
					}
					
				}
			}
		}
	}

	public static void main(String[] args) throws IOException,
			InterruptedException, ClassNotFoundException {

		Path inputPath = new Path("hdfs://changping11:9000/user/luoxiongcai/simrank/input/biGraph_"
				+ countFlag );
		Path outputPath = new Path("hdfs://changping11:9000/user/luoxiongcai/simrank/output/v_"
				+ countFlag + "_batch");

		GiraphJob job = new GiraphJob(new Configuration(), "s_walk_batch_"
				+ countFlag+"_combine_reuse_"+machineCount);

		job.getConfiguration().set("hadoop.job.ugi", "hadoop,supergroup");
		job.getConfiguration().set("mapred.child.java.opts",
				"-Xms35g -Xmx35g -XX:+UseSerialGC");
		job.getConfiguration().set("mapred.job.map.memory.mb", "35000");
		job.getConfiguration().set("mapred.task.timeout", "60000000");
		job.getConfiguration().setInt("giraph.numComputeThreads", machineCount);
		File jarFile = EJob.createTempJar("bin");
		job.getConfiguration().set("mapred.jar", jarFile.getAbsolutePath());

		job.getConfiguration().setVertexClass(CombineBatchSingleWalkVertexReuse.class);
		//set workcontext.
		job.getConfiguration().setWorkerContextClass(SingleWalkWorkContext.class);

		// random outedge class.
		job.getConfiguration().setOutEdgesClass(RandOutEdges.class);
		// set mastercompute class
		job.getConfiguration().setMasterComputeClass(
				SingleWalkMasterCompute.class);

		job.getConfiguration().setEdgeInputFormatClass( // be careful to
														// synchronize with
														// .addVertex(edge)InputPath.
				IntNullReverseTextEdgeInputFormat.class);
		job.getConfiguration().setVertexOutputFormatClass(
				DefaultOutputFormat.class);

		GiraphFileInputFormat.addEdgeInputPath(job.getConfiguration(),
				inputPath);

		FileOutputFormat.setOutputPath(job.getInternalJob(), outputPath);

		job.getConfiguration().setWorkerConfiguration(machineCount,
				machineCount, 100.0f);

		FileSystem fs = FileSystem.get(job.getConfiguration());
		if (fs.exists(outputPath)) {
			fs.delete(outputPath, true);
		}
		if (fs.exists(new Path(hdfsPathBase))) {
			fs.delete(new Path(hdfsPathBase), true);
		}
		boolean isVerbose = true;
		job.run(isVerbose);

	}

}
