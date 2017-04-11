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
import giraph.writables.Short_2MixMsgWritable;

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
public class BatchSingleWalkVertexReuse extends
		Vertex<IntWritable, NullWritable, NullWritable, Short_2MixMsgWritable> {
	public static final String countFlag = "10k_5.txt";
	public static final int V = 10000;
	public static final int stopV = 10000;
	public static final int machineCount = 5;
	public static final int BATCH_SIZE = 5000;
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
	public static Random rand = new Random(); 
	private static final String hdfsPathBase = "hdfs://changping11:9000/user/luoxiongcai/simrank/output/v_"
			+ countFlag + "_batch_reuse";
	public static Map<Integer, FSDataOutputStream> pid2OutStream = new HashMap<Integer, FSDataOutputStream>(); 
	private FixedCacheMap results = new FixedCacheMap(M * TOPK);
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
	
	private void incrementSims(Short_2MixMsgWritable msg){
		int[] targets = msg.getTargets();
		float[] sims = msg.getSims();
		int len = msg.getTargetLen();
		for (int i = 0; i < targetLen; i++){
			if (targets[i] != this.getId().get())
				results.put(targets[i], sims[i]);
		}
		sampleCount++;
	}
	
	private void computeAndSendSim(Short_2MixMsgWritable msg){
		byte srcp = msg.getSrcp();
		int[] path = msg.getPath();
		
		short[] degrees = msg.getDegrees();
		int[] targets = new int[targetLen];
		float[] sims = new float[targetLen];
		for (int i = 1; i <= targetLen; i++ ){
			targets[i - 1] = path[(srcp + i * 2) % pathLen];
			sims[i - 1] = (float) (Math.pow(C, i) * degrees[(srcp + i) % pathLen] / degrees[(srcp + i * 2) % pathLen]);
		}
		Short_2MixMsgWritable simMsg = new Short_2MixMsgWritable((byte) 2 , (byte)targetLen);
		simMsg.setTargets(targets);
		simMsg.setSims(sims);
		sendMessage(new IntWritable(path[srcp]),simMsg );
	}

	@Override
	public void compute(Iterable<Short_2MixMsgWritable> messages) throws IOException {
		if (getNumEdges() == 0){// single point.
			voteToHalt(); 
			return;
		}
		int currentId = this.getId().get();
		int currentStep = (int) this.getSuperstep();
		
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
			for (Short_2MixMsgWritable msg : messages) {
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

			if ((currentId >= beginV) && (currentId <= endV)){ // be careful about the range.
				for (int i = sampleCount; i < SAMPLE; i++) {
					Short_2MixMsgWritable msg = new Short_2MixMsgWritable((byte)1, (byte)pathLen);
					msg.setDirectPath(0, currentId,(byte) this.getNumEdges());
					sendMessage(randNeighbor(), msg);
				}	
			}
			
		} else if (step <= 2 * STEP){ // just random walk.
			Iterator<Short_2MixMsgWritable> msgs = messages.iterator();
			while (msgs.hasNext()) {
				Short_2MixMsgWritable msg = msgs.next();
				msg.setDirectPath(step, currentId, (byte)getNumEdges());
				sendMessage(randNeighbor(), msg);
				if (step == 2 *STEP && msg.getSource() >= beginV){ // be careful here, msgs to small ids are unnecessary.s
																   // since the results have been set null.
					computeAndSendSim(msg);
				}
			}
		} else {  // random walk and compute sim.
			Iterator<Short_2MixMsgWritable> msgs = messages.iterator();
			while (msgs.hasNext()) {
				Short_2MixMsgWritable msg = msgs.next();
				// deal with possible similarity increments.
				byte flag = msg.getFlag();
				if (flag == 2){
					incrementSims(msg);
				} else if (flag == 1){
					msg.appendCircularPath(currentId, (byte)this.getNumEdges());
					sendMessage(randNeighbor(), msg); // move up from bottom.
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
				+ countFlag+"_reuse(15s_38g)");

		// job.getConfiguration().setWorkerContextClass(workerContextClass)

		job.getConfiguration().set("hadoop.job.ugi", "luoxc,supergroup");
		job.getConfiguration().set("mapred.child.java.opts",
				"-Xms25g -Xmx25g -XX:+UseSerialGC");
		job.getConfiguration().set("mapred.job.map.memory.mb", "25000");
		job.getConfiguration().set("mapred.task.timeout", "60000000");
		job.getConfiguration().setInt("giraph.numComputeThreads", machineCount);
		File jarFile = EJob.createTempJar("bin");
		job.getConfiguration().set("mapred.jar", jarFile.getAbsolutePath());

		job.getConfiguration().setVertexClass(BatchSingleWalkVertexReuse.class);
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
