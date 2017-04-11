package giraph;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.giraph.master.MasterCompute;
import org.apache.hadoop.io.IntWritable;

import conf.MyConfiguration;

public class SingleWalkMasterCompute extends MasterCompute{
	private static int BATCH_SIZE = MyConfiguration.BATCH_SIZE;
	private static int CYCLE = MyConfiguration.CYCLE;

	@Override
	public void readFields(DataInput arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void write(DataOutput arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void compute() {
		int step = (int) this.getSuperstep();
		if (step % CYCLE == 0) {
			this.setAggregatedValue("VID_LOWER", new IntWritable( (step / CYCLE)  * BATCH_SIZE));
			this.setAggregatedValue("VID_UPPER", new IntWritable( (step / CYCLE + 1)  * BATCH_SIZE - 1));
		}
	}

	@Override
	public void initialize() throws InstantiationException,
			IllegalAccessException {
		this.registerPersistentAggregator("VID_LOWER", BroadcastValueAggregator.class);
		this.registerPersistentAggregator("VID_UPPER", BroadcastValueAggregator.class);
	}

	

}
