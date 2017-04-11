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
import giraph.writables.IntArrayWritable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import lxctools.FixedHashMap;
import lxctools.Pair;

/**
 * Implementation of PageRank in which vertex ids are ints, page rank values are
 * floats, and graph is unweighted.
 */
public class BatchSingleWalkVertex extends
		Vertex<IntWritable, NullWritable, NullWritable, IntArrayWritable> {
	public static final String countFlag = "10k_5.txt";
	public static final int V = 10000;
	public static final int BATCH_SIZE = 5000;
	public static final int machineCount = 5;

	

	public static final float C = 0.8f;
	public static final int SAMPLE = 10000;
	public static final int STEP = 5; // the max length of a likely float
										// random walk path;
	public static final int CYCLE = STEP * 2 + 2;
	public static final int TOPK = 20;
	public static final int M = 5;
	public static final int MAX_MSG_SLOT = STEP;
	public static final IntWritable MINUS_ONE = new IntWritable(-1); // indicate
																		// null;
	public static Random rand = new Random();

	private static final String hdfsPathBase = "hdfs://changping11:9000/user/luoxiongcai/simrank/output/v_"
			+ countFlag + "_batch_naive";
	public static Map<Integer, FSDataOutputStream> pid2OutStream = new HashMap<Integer, FSDataOutputStream>(); 
	
	private FixedHashMap results = new FixedHashMap(M * TOPK);
	private static final Configuration conf = new Configuration();
	
	private void flushTest() throws IOException {
		// hdfs api;
//		Configuration conf = new Configuration();
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
		int count = 0;
		for (Pair<Integer, Double> simPair : results) {
			if (simPair.getKey().intValue() == getId().get())
				continue;
			if (count >= SingleWalkVertex.TOPK)
				break;
			sb.append("\t" + simPair.getKey() + "->"
					+ String.format("%.6f", simPair.getValue()));
			count++;

		}
		out.writeChars(sb.toString()+"\r\n");
		out.flush();
	}


	public IntWritable randNeighbor() {
		RandOutEdges<IntWritable, NullWritable> edges = (RandOutEdges<IntWritable, NullWritable>) this.getEdges();
		Edge<IntWritable, NullWritable> next = edges.randEdge();
		if (next == null)
			return MINUS_ONE;
		else
			return next.getTargetVertexId();
	}

	@Override
	public void compute(Iterable<IntArrayWritable> messages) throws IOException {
		if (getNumEdges() == 0)
			voteToHalt(); // single point.
		int currentId = this.getId().get();

		IntWritable low = this.getAggregatedValue("VID_LOWER");
		IntWritable up = this.getAggregatedValue("VID_UPPER");
		int beginV = low.get();
		int endV = up.get();
		if (this.getSuperstep() > Math.ceil(V * 1.0 / BATCH_SIZE) * CYCLE){
			voteToHalt(); // all computation finished!
			return;
		}
			
		int step = (int) this.getSuperstep() % CYCLE;

		if (step == CYCLE - 1) {
			// deal with possible similarity increments.
			for (IntArrayWritable msg : messages) {
				int target = msg.getTarget();
				if (target != -1) {
					results.add(target, msg.getSim());
				}
			}
			// flush if possible.
			if ((currentId >= beginV) && (currentId <= endV)) {
				flushTest();
				results = null; // free the memory?
				voteToHalt();
				return;
			}
		} else if (step == 0) {
			// only vertices in the range [beginV, endV] are allowed to
			// "generate" paths.
			if (currentId < beginV || currentId > endV){
				voteToHalt();
				return;
			}
			for (int i = 0; i < SAMPLE; i++) {
				IntArrayWritable msg = new IntArrayWritable(MAX_MSG_SLOT + 1);
				msg.set(0, currentId);
				sendMessage(randNeighbor(), msg);
			}
		} else {
			Iterator<IntArrayWritable> msgs = messages.iterator();
			while (msgs.hasNext()) {
				IntArrayWritable msg = msgs.next();
				// deal with possible similarity increments.
				int target = msg.getTarget();
				if (target != -1) {
					results.add(target, msg.getSim());
					continue;
				}
				// deal and pass the path msg.
				if (step <= STEP) {
					msg.set(step, getNumEdges());
				}
				if (step % 2 == 0) { // even path . store the increment
										// similarity.
					float incre = (float)Math.pow(C, step / 2) * msg.get(step / 2)
							/ getNumEdges() / SAMPLE;
					sendMessage(new IntWritable(msg.getSource()),
							new IntArrayWritable(currentId, incre));
				}
				sendMessage(randNeighbor(), msg);
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
				+ countFlag+"_batch_naive");

		// job.getConfiguration().setWorkerContextClass(workerContextClass)

		job.getConfiguration().set("hadoop.job.ugi", "luoxc,supergroup");
		job.getConfiguration().set("mapred.child.java.opts",
				"-Xms25g -Xmx25g -XX:+UseSerialGC");
		job.getConfiguration().set("mapred.job.map.memory.mb", "25000");
		job.getConfiguration().set("mapred.task.timeout", "6000000");
		job.getConfiguration().setInt("giraph.numComputeThreads", machineCount);
		File jarFile = EJob.createTempJar("bin");
		job.getConfiguration().set("mapred.jar", jarFile.getAbsolutePath());

		job.getConfiguration().setVertexClass(BatchSingleWalkVertex.class);
		
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
